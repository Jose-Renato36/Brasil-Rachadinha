package br.unit.eleicao.coleta;

import br.unit.eleicao.excecao.ArquivoInvalidoException;
import br.unit.eleicao.excecao.ColetaException;
import br.unit.eleicao.modelo.Candidato;
import br.unit.eleicao.modelo.CandidaturaAnterior;
import br.unit.eleicao.modelo.Cargo;
import br.unit.eleicao.modelo.IndicadorFiscal;
import br.unit.eleicao.util.LeitorCsv;
import br.unit.eleicao.util.Texto;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * "Antes e durante" de quem já foi prefeito(a) ou governador(a): gasto com pessoal em % da receita
 * corrente líquida, ano a ano, pelo Relatório de Gestão Fiscal (Anexo 1) que o próprio ente envia ao
 * Tesouro Nacional (API do SICONFI). O ano da eleição mostra o "antes"; os seguintes, o mandato.
 */
public class GestaoFiscalSiconfi {

    /** Código IBGE de cada UF (usado como id_ente do governo estadual). */
    static final Map<String, String> CODIGO_UF = Map.ofEntries(
            Map.entry("RO", "11"), Map.entry("AC", "12"), Map.entry("AM", "13"), Map.entry("RR", "14"),
            Map.entry("PA", "15"), Map.entry("AP", "16"), Map.entry("TO", "17"), Map.entry("MA", "21"),
            Map.entry("PI", "22"), Map.entry("CE", "23"), Map.entry("RN", "24"), Map.entry("PB", "25"),
            Map.entry("PE", "26"), Map.entry("AL", "27"), Map.entry("SE", "28"), Map.entry("BA", "29"),
            Map.entry("MG", "31"), Map.entry("ES", "32"), Map.entry("RJ", "33"), Map.entry("SP", "35"),
            Map.entry("PR", "41"), Map.entry("SC", "42"), Map.entry("RS", "43"), Map.entry("MS", "50"),
            Map.entry("MT", "51"), Map.entry("GO", "52"), Map.entry("DF", "53"));
    /** Primeiro exercício com dados consistentes no SICONFI. */
    static final int PRIMEIRO_ANO = 2015;
    private static final Pattern ITEM = Pattern.compile("\\{[^{}]*\\}");

    private final Path pasta;
    private final Downloader downloader;
    private final Consumer<String> log;
    private final Map<String, IndicadorFiscal[]> cache = new HashMap<>();

    /** @param downloader null = só usa respostas já guardadas em dados/brutos/siconfi */
    public GestaoFiscalSiconfi(Path pasta, Downloader downloader, Consumer<String> log) {
        this.pasta = pasta;
        this.downloader = downloader;
        this.log = log;
    }

    /** @return true se alguma informação fiscal foi encontrada */
    public boolean processar(List<Candidato> candidatos, int anoEleicao) throws ArquivoInvalidoException {
        Map<String, String> ibgePorTse = lerMunicipios();
        int achados = 0;
        for (Candidato c : candidatos) {
            for (CandidaturaAnterior t : c.getTrajetoria()) {
                Cargo cargo = t.getTipoCargo();
                if (!t.isEleito() || (cargo != Cargo.PREFEITO && cargo != Cargo.GOVERNADOR)) {
                    continue;
                }
                String ente;
                char esfera;
                if (cargo == Cargo.PREFEITO) {
                    ente = ibgePorTse.get(Texto.somenteDigitos(t.getCodigoUe()).replaceFirst("^0+", ""));
                    esfera = 'M';
                } else {
                    String uf = t.getLocal().isEmpty() ? c.getUf() : t.getLocal();
                    ente = CODIGO_UF.get(uf);
                    esfera = 'E';
                }
                if (ente == null) {
                    continue;
                }
                String nomeEnte = (cargo == Cargo.PREFEITO ? "Prefeitura de " : "Governo de ") + t.getLocal();
                int ultimo = Math.min(t.getFimMandato(), anoEleicao - 1); // só exercícios encerrados
                for (int ano = Math.max(PRIMEIRO_ANO, t.getAno()); ano <= ultimo; ano++) {
                    IndicadorFiscal[] valor = consultar(ente, esfera, ano, nomeEnte, ano > t.getAno());
                    if (valor[0] != null) {
                        c.adicionarIndicadorFiscal(valor[0]);
                        achados++;
                    }
                }
            }
        }
        log.accept("Gestão fiscal (SICONFI): " + achados + " ano(s) de gasto com pessoal ligados a ex-prefeitos/governadores.");
        return achados > 0;
    }

    private IndicadorFiscal[] consultar(String ente, char esfera, int ano, String nomeEnte, boolean duranteMandato) {
        String chave = ente + "|" + ano + "|" + duranteMandato;
        IndicadorFiscal[] guardado = cache.get(chave);
        if (guardado != null) {
            return guardado;
        }
        IndicadorFiscal[] resultado = new IndicadorFiscal[1];
        // municípios pequenos podem publicar por semestre (S, período 2) em vez de quadrimestre (Q, período 3)
        for (char per : new char[]{'Q', 'S'}) {
            String json = obter(ente, esfera, ano, per, per == 'Q' ? 3 : 2);
            if (json == null) {
                continue;
            }
            Double[] v = interpretar(json);
            if (v[0] != null) {
                resultado[0] = new IndicadorFiscal(nomeEnte, ano, v[0], v[1], duranteMandato);
                break;
            }
        }
        cache.put(chave, resultado);
        return resultado;
    }

    private String obter(String ente, char esfera, int ano, char periodicidade, int periodo) {
        Path arquivo = pasta.resolve("siconfi").resolve("rgf_" + ente + "_" + ano + "_" + periodicidade + ".json");
        try {
            if (!Files.exists(arquivo)) {
                if (downloader == null) {
                    return null;
                }
                downloader.baixar(FontesDados.siconfiRgf(ano, periodicidade, periodo, esfera, ente), arquivo, false,
                        "application/json");
            }
            return Files.readString(arquivo, StandardCharsets.UTF_8);
        } catch (ColetaException | IOException e) {
            log.accept("  aviso: SICONFI " + ente + "/" + ano + ": " + e.getMessage());
            return null;
        }
    }

    /**
     * Extrai da resposta JSON o % da despesa total com pessoal sobre a RCL e o limite máximo.
     *
     * @return {percentual, limite}; posições null quando não encontradas
     */
    static Double[] interpretar(String json) {
        Double pessoal = null;
        Double limite = null;
        Matcher m = ITEM.matcher(json);
        while (m.find()) {
            String item = m.group();
            String coluna = Texto.normalizar(texto(item, "coluna"));
            if (!coluna.contains("SOBRE A RCL")) {
                continue;
            }
            String cod = texto(item, "cod_conta");
            String conta = Texto.normalizar(texto(item, "conta"));
            Double valor = numero(item, "valor");
            if (valor == null) {
                continue;
            }
            if (cod.equals("DespesaComPessoalTotal")
                    || (pessoal == null && cod.isEmpty() && conta.startsWith("DESPESA TOTAL COM PESSOAL"))) {
                pessoal = valor;
            } else if (cod.equals("LimiteMaximoDespesaComPessoalTotal")
                    || (limite == null && cod.isEmpty() && conta.startsWith("LIMITE MAXIMO"))) {
                limite = valor;
            }
        }
        return new Double[]{pessoal, limite};
    }

    private static String texto(String item, String campo) {
        Matcher m = Pattern.compile("\"" + campo + "\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"").matcher(item);
        return m.find() ? m.group(1) : "";
    }

    private static Double numero(String item, String campo) {
        Matcher m = Pattern.compile("\"" + campo + "\"\\s*:\\s*(-?[0-9.]+(?:[eE][-+]?[0-9]+)?)").matcher(item);
        return m.find() ? Double.valueOf(m.group(1)) : null;
    }

    /** Código do município no TSE (sem zeros à esquerda) -> código IBGE de 7 dígitos. */
    private Map<String, String> lerMunicipios() throws ArquivoInvalidoException {
        Map<String, String> mapa = new HashMap<>();
        Path arquivo = pasta.resolve(FontesDados.nomeLocal(FontesDados.municipiosTseIbge()));
        if (!Files.exists(arquivo)) {
            log.accept("  aviso: " + arquivo.getFileName() + " ausente - prefeituras sem dados fiscais");
            return mapa;
        }
        try (LeitorCsv csv = ArquivosBrutos.abrir(arquivo, StandardCharsets.UTF_8, "", "", ',')) {
            int iTse = csv.indice("codigo_tse");
            int iIbge = csv.indice("codigo_ibge");
            String[] l;
            while ((l = csv.proximaLinha()) != null) {
                String tse = LeitorCsv.campo(l, iTse).replaceFirst("^0+", "");
                String ibge = LeitorCsv.campo(l, iIbge);
                if (!tse.isEmpty() && !ibge.isEmpty()) {
                    mapa.put(tse, ibge);
                }
            }
        }
        return mapa;
    }
}
