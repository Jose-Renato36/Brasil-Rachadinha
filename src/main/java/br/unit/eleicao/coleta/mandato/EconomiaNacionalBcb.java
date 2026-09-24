package br.unit.eleicao.coleta.mandato;

import br.unit.eleicao.coleta.Downloader;
import br.unit.eleicao.modelo.FormaResumo;
import br.unit.eleicao.modelo.SerieMandato;
import br.unit.eleicao.modelo.TemaMandato;
import br.unit.eleicao.util.Texto;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Números do país no período de um mandato presidencial, pelo Sistema Gerenciador de Séries do Banco
 * Central (api.bcb.gov.br/dados/serie/bcdata.sgs.N/dados): 7326 = crescimento real do PIB no ano;
 * 13522 = IPCA acumulado em 12 meses (usamos dezembro); 13762 = dívida bruta do governo geral em % do PIB
 * (dezembro). Resposta: [{"data":"01/12/2022","valor":"5.79"}, ...].
 */
public class EconomiaNacionalBcb extends FonteMandato {

    static final int PIB_REAL = 7326;
    static final int IPCA_12_MESES = 13522;
    static final int DIVIDA_BRUTA = 13762;
    private static final Pattern PONTO = Pattern.compile(
            "\"data\"\\s*:\\s*\"(\\d{2})/(\\d{2})/(\\d{4})\"\\s*,\\s*\"valor\"\\s*:\\s*\"([^\"]*)\"");

    public EconomiaNacionalBcb(Path brutos, Downloader downloader, Consumer<String> log) {
        super(brutos, downloader, log);
    }

    @Override
    public String getNome() {
        return "bcb-sgs";
    }

    @Override
    public boolean aplica(MandatoExecutivo m) {
        return m.isFederal();
    }

    @Override
    public List<SerieMandato> coletar(MandatoExecutivo m) {
        List<SerieMandato> series = new ArrayList<>();
        String contexto = " A economia do país também depende do cenário mundial, do Congresso e, no caso da inflação, "
                + "do Banco Central, que é autônomo desde 2021.";
        adicionar(series, m, PIB_REAL, TemaMandato.ECONOMIA, "Crescimento do PIB", false, FormaResumo.MEDIA, true,
                "Crescimento real (já descontada a inflação) da economia no ano." + contexto);
        adicionar(series, m, IPCA_12_MESES, TemaMandato.ECONOMIA, "Inflação (IPCA)", true, FormaResumo.MEDIA, false,
                "Alta dos preços no ano (IPCA de janeiro a dezembro)." + contexto);
        adicionar(series, m, DIVIDA_BRUTA, TemaMandato.CONTAS, "Dívida pública bruta", true, FormaResumo.VARIACAO_PONTOS,
                false, "Dívida bruta do governo geral (União, estados e municípios) em % do PIB, em dezembro de cada ano.");
        return series;
    }

    private void adicionar(List<SerieMandato> series, MandatoExecutivo m, int codigo, TemaMandato tema, String titulo,
                           boolean dezembro, FormaResumo forma, boolean maiorMelhor, String explicacao) {
        String url = "https://api.bcb.gov.br/dados/serie/bcdata.sgs." + codigo + "/dados?formato=json&dataInicial=01/01/"
                + m.getPrimeiroAno() + "&dataFinal=31/12/" + m.getUltimoAno();
        Map<Integer, Double> anos = interpretar(obterTexto(url, "sgs" + codigo + "_" + m.getPrimeiroAno() + "_"
                + m.getUltimoAno() + ".json"), dezembro);
        SerieMandato s = nova(m, tema, titulo, "%", forma, maiorMelhor, "Banco Central – SGS (série " + codigo + ")",
                explicacao);
        for (Map.Entry<Integer, Double> e : anos.entrySet()) {
            if (m.contem(e.getKey())) {
                s.adicionar(e.getKey(), e.getValue(), null);
            }
        }
        if (!s.isVazia()) {
            series.add(s);
        }
    }

    /**
     * @param soDezembro true para séries mensais (fica o valor de dezembro de cada ano); false para anuais
     */
    public static Map<Integer, Double> interpretar(String json, boolean soDezembro) {
        Map<Integer, Double> anos = new TreeMap<>();
        if (json == null) {
            return anos;
        }
        Matcher m = PONTO.matcher(json);
        while (m.find()) {
            if (soDezembro && !m.group(2).equals("12")) {
                continue;
            }
            Double v = Texto.parseDecimal(m.group(4));
            if (v != null) {
                anos.put(Integer.parseInt(m.group(3)), v);
            }
        }
        return anos;
    }
}
