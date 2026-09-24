package br.unit.eleicao.coleta;

/**
 * Endereços dos arquivos públicos usados. Todos são arquivos em lote (CSV/ZIP) mantidos
 * pelo TSE e pela Câmara; baixá-los uma vez e guardar em disco evita depender da API a cada uso.
 */
public final class FontesDados {

    private static final String CAMARA = "https://dadosabertos.camara.leg.br/arquivos/";
    private static final String TSE = "https://cdn.tse.jus.br/estatistica/sead/odsele/";

    private FontesDados() {
    }

    public static String deputados() {
        return CAMARA + "deputados/csv/deputados.csv";
    }

    public static String votacoes(int ano) {
        return CAMARA + "votacoes/csv/votacoes-" + ano + ".csv";
    }

    public static String votos(int ano) {
        return CAMARA + "votacoesVotos/csv/votacoesVotos-" + ano + ".csv";
    }

    public static String proposicoes(int ano) {
        return CAMARA + "proposicoes/csv/proposicoes-" + ano + ".csv";
    }

    public static String autores(int ano) {
        return CAMARA + "proposicoesAutores/csv/proposicoesAutores-" + ano + ".csv";
    }

    public static String cota(int ano) {
        return "https://www.camara.leg.br/cotas/Ano-" + ano + ".csv.zip";
    }

    public static String candidatos(int ano) {
        return TSE + "consulta_cand/consulta_cand_" + ano + ".zip";
    }

    public static String bens(int ano) {
        return TSE + "bem_candidato/bem_candidato_" + ano + ".zip";
    }

    /** Nome do arquivo local = último trecho da URL. */
    public static String nomeLocal(String url) {
        return url.substring(url.lastIndexOf('/') + 1);
    }
}
