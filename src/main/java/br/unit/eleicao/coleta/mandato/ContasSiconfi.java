package br.unit.eleicao.coleta.mandato;

import br.unit.eleicao.coleta.Downloader;
import br.unit.eleicao.modelo.FormaResumo;
import br.unit.eleicao.modelo.SerieMandato;
import br.unit.eleicao.modelo.TemaMandato;
import br.unit.eleicao.util.Texto;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Contas de prefeituras e governos estaduais enviadas ao Tesouro (API do SICONFI):
 * <ul>
 *   <li>Relatório de Gestão Fiscal, Anexo 1: gasto com pessoal em % da receita corrente líquida e o
 *       limite da Lei de Responsabilidade Fiscal ({@code DespesaComPessoalTotal},
 *       {@code LimiteMaximoDespesaComPessoalTotal}, coluna "% sobre a RCL Ajustada").</li>
 *   <li>Relatório Resumido da Execução Orçamentária, Anexo 1, 6º bimestre: investimento em % da despesa
 *       ({@code Investimentos} / {@code TotalDespesas}, despesas liquidadas) e quanto sobrou ou faltou no
 *       ano ({@code TotalReceitas} realizada menos {@code TotalDespesas} liquidada, em % da receita).</li>
 * </ul>
 * Os anexos de saúde e educação (mínimos de 15% e 25%) não são publicados por esta API.
 */
public class ContasSiconfi extends FonteMandato {

    private static final String BASE = "https://apidatalake.tesouro.gov.br/ords/siconfi/tt/";
    private static final Pattern ITEM = Pattern.compile("\\{[^{}]*\\}");
    private static final String COL_LIQUIDADA = "DESPESAS LIQUIDADAS ATE O BIMESTRE";
    private static final String COL_RECEITA = "ATE O BIMESTRE C";

    public ContasSiconfi(Path brutos, Downloader downloader, Consumer<String> log) {
        super(brutos, downloader, log);
    }

    @Override
    public String getNome() {
        return "siconfi";
    }

    @Override
    public boolean aplica(MandatoExecutivo m) {
        return m.isMunicipal() || m.isEstadual();
    }

    @Override
    protected long getPausaMs() {
        return 1100; // a API do SICONFI aceita 1 pedido por segundo
    }

    @Override
    public List<SerieMandato> coletar(MandatoExecutivo m) {
        char esfera = m.isMunicipal() ? 'M' : 'E';
        SerieMandato pessoal = nova(m, TemaMandato.CONTAS, "Gasto com pessoal", "%", FormaResumo.VARIACAO_PONTOS, false,
                "Tesouro Nacional – SICONFI (Relatório de Gestão Fiscal)",
                "Salários e encargos em % da receita corrente líquida. A Lei de Responsabilidade Fiscal limita a 54% "
                        + "(prefeituras) e 49% (estados) no Poder Executivo.");
        SerieMandato investimento = nova(m, TemaMandato.CONTAS, "Investimento", "%", FormaResumo.MEDIA, true,
                "Tesouro Nacional – SICONFI (Relatório Resumido da Execução Orçamentária)",
                "Parte da despesa do ano usada em obras, equipamentos e compra de imóveis.");
        SerieMandato resultado = nova(m, TemaMandato.CONTAS, "Sobrou ou faltou dinheiro no ano", "%", FormaResumo.MEDIA,
                null, "Tesouro Nacional – SICONFI (Relatório Resumido da Execução Orçamentária)",
                "Receita arrecadada menos despesa liquidada, em % da receita. Positivo: sobrou; negativo: gastou mais "
                        + "do que arrecadou no ano.");
        for (int ano = m.getPrimeiroAno(); ano <= m.getUltimoAno(); ano++) {
            Double[] p = pessoal(m, esfera, ano);
            if (p[0] != null) {
                pessoal.adicionar(ano, p[0], null);
                if (p[1] != null) {
                    pessoal.setLimite(p[1]);
                }
            }
            String rreo = obterTexto(BASE + "rreo?an_exercicio=" + ano + "&nr_periodo=6&co_tipo_demonstrativo=RREO"
                    + "&no_anexo=RREO-Anexo%2001&co_esfera=" + esfera + "&id_ente=" + m.getCodigoIbge(),
                    "rreo1_" + m.getCodigoIbge() + "_" + ano + ".json");
            Double[] o = interpretarRreo(rreo);
            if (o[0] != null) {
                investimento.adicionar(ano, o[0], null);
            }
            if (o[1] != null) {
                resultado.adicionar(ano, o[1], null);
            }
        }
        List<SerieMandato> series = new ArrayList<>();
        for (SerieMandato s : new SerieMandato[]{pessoal, investimento, resultado}) {
            if (!s.isVazia()) {
                series.add(s);
            }
        }
        return series;
    }

    /** Municípios pequenos podem publicar o RGF por semestre (S, período 2) em vez de quadrimestre (Q, 3). */
    private Double[] pessoal(MandatoExecutivo m, char esfera, int ano) {
        for (char per : new char[]{'Q', 'S'}) {
            String json = obterTexto(BASE + "rgf?an_exercicio=" + ano + "&in_periodicidade=" + per + "&nr_periodo="
                    + (per == 'Q' ? 3 : 2) + "&co_tipo_demonstrativo=RGF&no_anexo=RGF-Anexo%2001&co_esfera=" + esfera
                    + "&co_poder=E&id_ente=" + m.getCodigoIbge(), "rgf1_" + m.getCodigoIbge() + "_" + ano + "_" + per + ".json");
            Double[] v = interpretarRgf(json);
            if (v[0] != null) {
                return v;
            }
        }
        return new Double[2];
    }

    /** @return {% gasto com pessoal sobre a RCL, limite máximo}; posições null quando ausentes */
    public static Double[] interpretarRgf(String json) {
        Double pessoal = null;
        Double limite = null;
        if (json == null) {
            return new Double[2];
        }
        Matcher m = ITEM.matcher(json);
        while (m.find()) {
            String item = m.group();
            if (!Texto.normalizar(texto(item, "coluna")).contains("SOBRE A RCL")) {
                continue;
            }
            String cod = texto(item, "cod_conta");
            Double valor = numero(item, "valor");
            if (valor == null) {
                continue;
            }
            if (cod.equals("DespesaComPessoalTotal")) {
                pessoal = valor;
            } else if (cod.equals("LimiteMaximoDespesaComPessoalTotal")) {
                limite = valor;
            }
        }
        return new Double[]{pessoal, limite};
    }

    /** @return {investimento em % da despesa liquidada, resultado em % da receita}; posições null quando ausentes */
    public static Double[] interpretarRreo(String json) {
        if (json == null) {
            return new Double[2];
        }
        Double investimentos = null;
        Double despesas = null;
        Double receitas = null;
        Matcher m = ITEM.matcher(json);
        while (m.find()) {
            String item = m.group();
            String cod = texto(item, "cod_conta");
            String coluna = Texto.normalizar(texto(item, "coluna"));
            Double valor = numero(item, "valor");
            if (valor == null) {
                continue;
            }
            if (cod.equals("Investimentos") && coluna.startsWith(COL_LIQUIDADA) && investimentos == null) {
                investimentos = valor;
            } else if (cod.equals("TotalDespesas") && coluna.startsWith(COL_LIQUIDADA)) {
                despesas = valor;
            } else if (cod.equals("TotalReceitas") && coluna.equals(COL_RECEITA)) {
                receitas = valor;
            }
        }
        Double pctInvestimento = investimentos != null && despesas != null && despesas > 0
                ? 100 * investimentos / despesas : null;
        Double pctResultado = receitas != null && despesas != null && receitas > 0
                ? 100 * (receitas - despesas) / receitas : null;
        return new Double[]{pctInvestimento, pctResultado};
    }

    private static String texto(String item, String campo) {
        Matcher m = Pattern.compile("\"" + campo + "\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"").matcher(item);
        return m.find() ? m.group(1) : "";
    }

    private static Double numero(String item, String campo) {
        Matcher m = Pattern.compile("\"" + campo + "\"\\s*:\\s*(-?[0-9.]+(?:[eE][-+]?[0-9]+)?)").matcher(item);
        return m.find() ? Double.valueOf(m.group(1)) : null;
    }
}
