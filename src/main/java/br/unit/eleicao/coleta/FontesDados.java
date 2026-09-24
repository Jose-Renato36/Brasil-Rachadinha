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

    /** Liga cada votação à proposição votada (título e ementa legíveis). */
    public static String votacoesProposicoes(int ano) {
        return CAMARA + "votacoesProposicoes/csv/votacoesProposicoes-" + ano + ".csv";
    }

    /** API: histórico de situações do deputado (posse, licença, reassunção), em JSON. */
    public static String historicoDeputado(int id) {
        return "https://dadosabertos.camara.leg.br/api/v2/deputados/" + id + "/historico";
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

    public static String candidatosComplementar(int ano) {
        return TSE + "consulta_cand_complementar/consulta_cand_complementar_" + ano + ".zip";
    }

    /** Página onde o usuário pode baixar os arquivos do TSE pelo navegador, se o download direto falhar. */
    public static String paginaTse(int ano) {
        return "https://dadosabertos.tse.jus.br/dataset/candidatos-" + ano;
    }

    public static String bens(int ano) {
        return TSE + "bem_candidato/bem_candidato_" + ano + ".zip";
    }

    /** Motivos de cassação/indeferimento por candidatura (SQ_CANDIDATO, DS_MOTIVO_CASSACAO). */
    public static String motivoCassacao(int ano) {
        return TSE + "motivo_cassacao/motivo_cassacao_" + ano + ".zip";
    }

    /** Prestação de contas dos candidatos (receitas_candidatos_ANO_UF.csv dentro do zip). */
    public static String prestacaoContas(int ano) {
        return TSE + "prestacao_contas/prestacao_de_contas_eleitorais_candidatos_" + ano + ".zip";
    }

    /** Emendas parlamentares ao Orçamento da União desde 2014 (Portal da Transparência / CGU). */
    public static String emendas() {
        return "https://dadosabertos-download.cgu.gov.br/PortalDaTransparencia/saida/emendas-parlamentares/"
                + "EmendasParlamentares.zip";
    }

    /** CEIS ou CNEP de um dia (AAAAMMDD); o portal só guarda arquivos recentes. */
    public static String sancoes(String cadastro, java.time.LocalDate dia) {
        return "https://portaldatransparencia.gov.br/download-de-dados/" + cadastro.toLowerCase() + "/"
                + dia.format(java.time.format.DateTimeFormatter.BASIC_ISO_DATE);
    }

    /** Servidores civis do Executivo federal de um mês (zip com AAAAMM_Cadastro.csv). */
    public static String servidoresSiape(java.time.YearMonth mes) {
        return "https://portaldatransparencia.gov.br/download-de-dados/servidores/"
                + mes.format(java.time.format.DateTimeFormatter.ofPattern("yyyyMM")) + "_Servidores_SIAPE";
    }

    /** Pasta mensal dos dados abertos do CNPJ (Socios0.zip ... Socios9.zip, Empresas0.zip ...). */
    public static String cnpj(java.time.YearMonth mes, String arquivo) {
        return "https://arquivos.receitafederal.gov.br/dados/cnpj/dados_abertos_cnpj/"
                + mes.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM")) + "/" + arquivo;
    }

    /** API do SICONFI (Tesouro Nacional): relatórios fiscais de estados e municípios, em JSON. */
    public static String siconfiRgf(int ano, char periodicidade, int periodo, char esfera, String codigoIbge) {
        return "https://apidatalake.tesouro.gov.br/ords/siconfi/tt/rgf?an_exercicio=" + ano
                + "&in_periodicidade=" + periodicidade + "&nr_periodo=" + periodo
                + "&co_tipo_demonstrativo=RGF&no_anexo=RGF-Anexo%2001&co_esfera=" + esfera
                + "&co_poder=E&id_ente=" + codigoIbge;
    }

    /**
     * Tabela de correspondência entre o código de município do TSE e o do IBGE (projeto aberto
     * betafcc/Municipios-Brasileiros-TSE, montado a partir dos arquivos do próprio TSE).
     */
    public static String municipiosTseIbge() {
        return "https://raw.githubusercontent.com/betafcc/Municipios-Brasileiros-TSE/master/municipios_brasileiros_tse.csv";
    }

    /** Pessoas Expostas Politicamente de um mês (zip com AAAAMM_PEP.csv), Portal da Transparência. */
    public static String pep(java.time.YearMonth mes) {
        return "https://portaldatransparencia.gov.br/download-de-dados/pep/"
                + mes.format(java.time.format.DateTimeFormatter.ofPattern("yyyyMM"));
    }

    /** Contratos do governo federal de um mês (zip com AAAAMM_Compras.csv), Portal da Transparência. */
    public static String comprasFederais(java.time.YearMonth mes) {
        return "https://portaldatransparencia.gov.br/download-de-dados/compras/"
                + mes.format(java.time.format.DateTimeFormatter.ofPattern("yyyyMM"));
    }

    /** Planos de governo registrados no TSE (PDFs), um zip por UF; "BR" = Presidência. */
    public static String propostaGoverno(int ano, String uf) {
        return TSE + "proposta_governo/proposta_governo_" + ano + "_" + uf.toUpperCase() + ".zip";
    }

    /** Votos por candidato, município e zona (um CSV por UF dentro do zip). */
    public static String votacao(int ano) {
        return TSE + "votacao_candidato_munzona/votacao_candidato_munzona_" + ano + ".zip";
    }

    /** Verba de gabinete dos senadores (CEAPS) de um ano; a 1ª linha do CSV é a data de atualização. */
    public static String cotaSenado(int ano) {
        return "https://www.senado.leg.br/transparencia/LAI/verba/despesa_ceaps_" + ano + ".csv";
    }

    /** Nome do arquivo local = último trecho da URL. */
    public static String nomeLocal(String url) {
        return url.substring(url.lastIndexOf('/') + 1);
    }
}
