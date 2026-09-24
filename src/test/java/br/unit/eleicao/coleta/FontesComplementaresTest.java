package br.unit.eleicao.coleta;

import br.unit.eleicao.modelo.BaseDados;
import br.unit.eleicao.modelo.Candidato;
import br.unit.eleicao.modelo.CandidaturaAnterior;
import br.unit.eleicao.modelo.Metadados;
import br.unit.eleicao.persistencia.RepositorioArquivos;
import br.unit.eleicao.util.Texto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Leitores das fontes complementares com arquivos pequenos no layout publicado por cada órgão. */
class FontesComplementaresTest {

    private static final Charset LATIN1 = StandardCharsets.ISO_8859_1;
    private static final Charset CP1252 = Charset.forName("windows-1252");
    /** Cabeçalho de receitas_candidatos (48 colunas, conferido em importadores públicos). */
    private static final String CAB_RECEITAS = "\"DT_GERACAO\";\"HH_GERACAO\";\"ANO_ELEICAO\";\"CD_TIPO_ELEICAO\";"
            + "\"NM_TIPO_ELEICAO\";\"CD_ELEICAO\";\"DS_ELEICAO\";\"DT_ELEICAO\";\"ST_TURNO\";\"TP_PRESTACAO_CONTAS\";"
            + "\"DT_PRESTACAO_CONTAS\";\"SQ_PRESTADOR_CONTAS\";\"SG_UF\";\"SG_UE\";\"NM_UE\";\"NR_CNPJ_PRESTADOR_CONTA\";"
            + "\"CD_CARGO\";\"DS_CARGO\";\"SQ_CANDIDATO\";\"NR_CANDIDATO\";\"NM_CANDIDATO\";\"NR_CPF_CANDIDATO\";"
            + "\"NR_CPF_VICE_CANDIDATO\";\"NR_PARTIDO\";\"SG_PARTIDO\";\"NM_PARTIDO\";\"CD_FONTE_RECEITA\";"
            + "\"DS_FONTE_RECEITA\";\"CD_ORIGEM_RECEITA\";\"DS_ORIGEM_RECEITA\";\"CD_NATUREZA_RECEITA\";"
            + "\"DS_NATUREZA_RECEITA\";\"CD_ESPECIE_RECEITA\";\"DS_ESPECIE_RECEITA\";\"CD_CNAE_DOADOR\";\"DS_CNAE_DOADOR\";"
            + "\"NR_CPF_CNPJ_DOADOR\";\"NM_DOADOR\";\"NM_DOADOR_RFB\";\"CD_ESFERA_PARTIDARIA_DOADOR\";"
            + "\"DS_ESFERA_PARTIDARIA_DOADOR\";\"SG_UF_DOADOR\";\"CD_MUNICIPIO_DOADOR\";\"NM_MUNICIPIO_DOADOR\";"
            + "\"SQ_RECEITA\";\"DT_RECEITA\";\"DS_RECEITA\";\"VR_RECEITA\"";

    private static Candidato candidato(String sq, String nome, String urna) {
        Candidato c = new Candidato(sq, nome, urna, LocalDate.of(1970, 5, 20), "FEMININO");
        c.setUf("ZZ");
        c.setCargo("DEPUTADO FEDERAL");
        return c;
    }

    private static String receita(String sq, String fonte, String origem, String id, String valor) {
        String[] v = new String[48];
        java.util.Arrays.fill(v, "#NULO");
        v[12] = "ZZ";
        v[18] = sq;
        v[27] = fonte;
        v[29] = origem;
        v[44] = id;
        v[47] = valor;
        return "\"" + String.join("\";\"", v) + "\"";
    }

    @Test
    void financiamentoSomaPorOrigemEIgnoraDoadorOriginario(@TempDir Path pasta) throws Exception {
        Candidato a = candidato("1", "ANA EXEMPLO", "ANA");
        zip(pasta.resolve("prestacao_de_contas_eleitorais_candidatos_2026.zip"), LATIN1,
                Map.of("receitas_candidatos_doador_originario_2026_ZZ.csv", new String[]{"\"SQ_CANDIDATO\";\"X\"", "\"1\";\"9\""},
                        "receitas_candidatos_2026_ZZ.csv", new String[]{CAB_RECEITAS,
                            receita("1", "Fundo Especial", "Recursos de partido político", "10", "1000,00"),
                            receita("1", "Outros Recursos", "Recursos de pessoas físicas", "11", "250,50"),
                            receita("1", "Outros Recursos", "Recursos próprios", "12", "100,00"),
                            receita("999", "Outros Recursos", "Recursos próprios", "13", "5,00")}));
        assertTrue(new FinanciamentoCampanha(pasta, "ZZ", s -> { }).processar(List.of(a), 2026));
        assertEquals(1350.5, a.getFinanciamento().getTotal(), 1e-6);
        assertEquals(1000.0, a.getFinanciamento().getPorOrigem().get("Fundo eleitoral (dinheiro público)"), 1e-6);
        assertEquals(250.5, a.getFinanciamento().getPorOrigem().get("Doações de pessoas"), 1e-6);
        assertEquals(100.0, a.getFinanciamento().getPorOrigem().get("Dinheiro do próprio candidato"), 1e-6);
    }

    @Test
    void emendasSoIndividuaisENomeUnico(@TempDir Path pasta) throws Exception {
        BaseDados base = new BaseDados(new Metadados("ZZ", 2026, 2022, LocalDate.of(2026, 10, 4)));
        Candidato dep = candidato("1", "ANA MARIA EXEMPLO", "ANA DO POVO");
        dep.adicionarCandidaturaAnterior(new CandidaturaAnterior(2022, "DEPUTADO FEDERAL", "ZZ", "PXA", "Eleito(a)"));
        Candidato novato = candidato("2", "BRUNO EXEMPLO", "BRUNO");
        base.adicionarCandidato(dep);
        base.adicionarCandidato(novato);
        String cab = "\"Código da Emenda\";\"Ano da Emenda\";\"Tipo de Emenda\";\"Código do Autor da Emenda\";"
                + "\"Nome do Autor da Emenda\";\"Número da emenda\";\"Localidade de aplicação do recurso\";"
                + "\"Código Município IBGE\";\"Nome Função\";\"Nome Subfunção\";\"Valor Empenhado\";\"Valor Liquidado\";\"Valor Pago\"";
        zip(pasta.resolve("EmendasParlamentares.zip"), CP1252, Map.of("EmendasParlamentares.csv", new String[]{cab,
            "\"1\";\"2023\";\"Emenda Individual - Transferências Especiais\";\"1\";\"ANA DO POVO\";\"1\";\"CIDADE A - ZZ\";\"1\";\"Saúde\";\"x\";\"1.000,00\";\"0\";\"800,00\"",
            "\"2\";\"2024\";\"Emenda Individual - Transferências com Finalidade Definida\";\"1\";\"ANA DO POVO\";\"2\";\"CIDADE B - ZZ\";\"2\";\"Educação\";\"x\";\"500,00\";\"0\";\"500,00\"",
            "\"3\";\"2024\";\"Emenda de Relator\";\"9\";\"ANA DO POVO\";\"3\";\"CIDADE B - ZZ\";\"2\";\"Educação\";\"x\";\"9.999,00\";\"0\";\"9.999,00\"",
            "\"4\";\"2024\";\"Emenda Individual - Transferências Especiais\";\"7\";\"BRUNO\";\"4\";\"CIDADE C - ZZ\";\"3\";\"Saúde\";\"x\";\"10,00\";\"0\";\"10,00\""}));
        assertTrue(new EmendasParlamentares(pasta, s -> { }).processar(base));
        assertNotNull(dep.getEmendas());
        assertEquals(1300.0, dep.getEmendas().getPago(), 1e-6, "emenda de relator não entra");
        assertEquals(800.0, dep.getEmendas().getPagoTransferenciaEspecial(), 1e-6);
        assertEquals(2, dep.getEmendas().getPagoPorAno().size());
        assertNull(novato.getEmendas(), "quem nunca foi parlamentar federal não é ligado");
    }

    @Test
    void sancoesPorCpfMascaradoEEmpresaDoSocio(@TempDir Path pasta) throws Exception {
        Candidato a = candidato("1", "ANA MARIA EXEMPLO", "ANA");
        Candidato homonima = candidato("2", "ANA MARIA EXEMPLO", "ANINHA");
        a.adicionarEmpresa(new br.unit.eleicao.modelo.VinculoEmpresa("12345678", "EXEMPLO LTDA", "Sócio", ""));
        Map<String, String> cpfs = Map.of("1", "11122233344", "2", "99988877766");
        String cab = "\"CADASTRO\";\"CÓDIGO DA SANÇÃO\";\"TIPO DE PESSOA\";\"CPF OU CNPJ DO SANCIONADO\";\"NOME DO SANCIONADO\";"
                + "\"NOME INFORMADO PELO ÓRGÃO SANCIONADOR\";\"RAZÃO SOCIAL - CADASTRO RECEITA\";\"NOME FANTASIA - CADASTRO RECEITA\";"
                + "\"NÚMERO DO PROCESSO\";\"CATEGORIA DA SANÇÃO\";\"DATA INÍCIO SANÇÃO\";\"DATA FINAL SANÇÃO\";\"ÓRGÃO SANCIONADOR\"";
        zip(pasta.resolve("ceis.zip"), CP1252, Map.of("20260920_CEIS.csv", new String[]{cab,
            "\"CEIS\";\"1\";\"F\";\"***.222.333-**\";\"ANA MARIA EXEMPLO\";\"\";\"\";\"\";\"1\";\"Suspensão\";\"01/01/2025\";\"01/01/2027\";\"PREFEITURA X\"",
            "\"CEIS\";\"2\";\"J\";\"12.345.678/0001-90\";\"EXEMPLO LTDA\";\"\";\"EXEMPLO LTDA\";\"\";\"2\";\"Inidoneidade\";\"01/01/2024\";\"\";\"MINISTÉRIO Y\""}));
        assertTrue(new SancoesCgu(pasta, s -> { }).processar(List.of(a, homonima), cpfs));
        assertEquals(2, a.getSancoes().size());
        assertTrue(a.getSancoes().stream().anyMatch(br.unit.eleicao.modelo.Sancao::isSobreEmpresa));
        assertTrue(homonima.getSancoes().isEmpty(), "homônima com outro CPF não é ligada");
    }

    @Test
    void servidorFederalPorNomeECpfParcial(@TempDir Path pasta) throws Exception {
        Candidato a = candidato("1", "ANA MARIA EXEMPLO", "ANA");
        String cab = "\"Id_SERVIDOR_PORTAL\";\"NOME\";\"CPF\";\"MATRICULA\";\"DESCRICAO_CARGO\";\"ORG_LOTACAO\";"
                + "\"ORG_EXERCICIO\";\"SITUACAO_VINCULO\";\"DATA_INGRESSO_SERVICOPUBLICO\"";
        zip(pasta.resolve(ServidoresFederais.ARQUIVO), CP1252, Map.of("202608_Cadastro.csv", new String[]{cab,
            "\"1\";\"ANA MARIA EXEMPLO\";\"***.222.333-**\";\"1\";\"PROFESSOR\";\"UF X\";\"UNIVERSIDADE X\";\"ATIVO PERMANENTE\";\"01/03/2010\"",
            "\"2\";\"ANA MARIA EXEMPLO\";\"***.555.666-**\";\"2\";\"MÉDICO\";\"HOSP\";\"HOSP\";\"ATIVO PERMANENTE\";\"01/03/2011\""}));
        assertTrue(new ServidoresFederais(pasta, s -> { }).processar(List.of(a), Map.of("1", "11122233344")));
        assertEquals(1, a.getVinculosServidor().size());
        assertEquals("PROFESSOR", a.getVinculosServidor().get(0).getCargo());
    }

    @Test
    void empresasDaReceitaSemCabecalho(@TempDir Path brutos) throws Exception {
        Candidato a = candidato("1", "ANA MARIA EXEMPLO", "ANA");
        Path cnpj = brutos.resolve("cnpj");
        Files.createDirectories(cnpj);
        zip(cnpj.resolve("Socios0.zip"), LATIN1, Map.of("K3241.SOCIOCSV", new String[]{
            "\"12345678\";\"2\";\"ANA MARIA EXEMPLO\";\"***222333**\";\"49\";\"20150310\";\"\";\"***000000**\";\"\";\"00\";\"5\"",
            "\"87654321\";\"2\";\"ANA MARIA EXEMPLO\";\"***999999**\";\"22\";\"20150310\";\"\";\"***000000**\";\"\";\"00\";\"5\""}));
        zip(cnpj.resolve("Empresas0.zip"), LATIN1, Map.of("K3241.EMPRECSV", new String[]{
            "\"12345678\";\"PADARIA EXEMPLO LTDA\";\"2062\";\"49\";\"10000,00\";\"01\";\"\""}));
        assertTrue(new EmpresasReceita(brutos, s -> { }).processar(List.of(a), Map.of("1", "11122233344")));
        assertEquals(1, a.getEmpresas().size());
        assertEquals("PADARIA EXEMPLO LTDA", a.getEmpresas().get(0).getRazaoSocial());
        assertEquals("Sócio-Administrador", a.getEmpresas().get(0).getQualificacao());
        assertEquals("10/03/2015", a.getEmpresas().get(0).getDataEntrada());
    }

    @Test
    void historicoPreencheBensECassacaoDaCandidaturaAntiga(@TempDir Path pasta) throws Exception {
        Candidato a = candidato("1", "ANA", "ANA");
        CandidaturaAnterior t = new CandidaturaAnterior(2020, "VEREADOR", "CIDADE/ZZ", "PXA", "Eleito(a)");
        t.setSqOrigem("200001");
        a.adicionarCandidaturaAnterior(t);
        zip(pasta.resolve("bem_candidato_2020.zip"), LATIN1, Map.of("bem_candidato_2020_ZZ.csv", new String[]{
            "\"SQ_CANDIDATO\";\"NR_ORDEM_BEM_CANDIDATO\";\"DS_BEM_CANDIDATO\";\"VR_BEM_CANDIDATO\"",
            "\"200001\";\"1\";\"Casa\";\"100000,00\"", "\"200001\";\"2\";\"Carro\";\"20000,00\"", "\"300000\";\"1\";\"X\";\"5,00\""}));
        zip(pasta.resolve("motivo_cassacao_2020.zip"), LATIN1, Map.of("motivo_cassacao_2020_ZZ.csv", new String[]{
            "\"DT_GERACAO\";\"HH_GERACAO\";\"CD_ELEICAO\";\"DS_ELEICAO\";\"ANO_ELEICAO\";\"SG_UF\";\"SG_UE\";\"SQ_CANDIDATO\";\"DS_MOTIVO_CASSACAO\"",
            "\"x\";\"x\";\"1\";\"E\";\"2020\";\"ZZ\";\"1\";\"200001\";\"Captação ilícita de sufrágio\""}));
        HistoricoTse h = new HistoricoTse(pasta, "ZZ", s -> { });
        h.processar(List.of(a), new int[]{2018, 2020});
        assertEquals(120000.0, t.getPatrimonio(), 1e-6);
        assertEquals("Captação ilícita de sufrágio", t.getMotivoCassacao());
        assertTrue(h.isBensLidos() && h.isCassacoesLidas());
        assertEquals(2, a.getSeriePatrimonio(2026).size() + (a.getPatrimonio() == null ? 1 : 0));
    }

    @Test
    void siconfiLePercentualDePessoalELimite() {
        String json = "{\"items\":[{\"exercicio\":2023,\"periodo\":3,\"cod_ibge\":2800308,\"anexo\":\"RGF-Anexo 01\","
                + "\"coluna\":\"Valor\",\"cod_conta\":\"DespesaComPessoalTotal\",\"conta\":\"DESPESA TOTAL COM PESSOAL\",\"valor\":987654.3},"
                + "{\"coluna\":\"% sobre a RCL Ajustada\",\"cod_conta\":\"DespesaComPessoalTotal\",\"conta\":\"x\",\"valor\":51.27},"
                + "{\"coluna\":\"% sobre a RCL Ajustada\",\"cod_conta\":\"LimiteMaximoDespesaComPessoalTotal\",\"conta\":\"y\",\"valor\":54}],"
                + "\"hasMore\":false}";
        Double[] v = GestaoFiscalSiconfi.interpretar(json);
        assertEquals(51.27, v[0], 1e-9);
        assertEquals(54.0, v[1], 1e-9);
        assertNull(GestaoFiscalSiconfi.interpretar("{\"items\":[]}")[0]);
    }

    @Test
    void gestaoFiscalUsaCacheSemDownload(@TempDir Path pasta) throws Exception {
        Candidato a = candidato("1", "ANA", "ANA");
        CandidaturaAnterior t = new CandidaturaAnterior(2020, "PREFEITO", "CIDADE/ZZ", "PXA", "Eleito(a)");
        t.setCodigoUe("00123");
        a.adicionarCandidaturaAnterior(t);
        Files.writeString(pasta.resolve("municipios_brasileiros_tse.csv"),
                "codigo_tse,uf,nome_municipio,capital,codigo_ibge\n123,ZZ,CIDADE,0,9900001\n");
        Path siconfi = pasta.resolve("siconfi");
        Files.createDirectories(siconfi);
        for (int ano = 2020; ano <= 2024; ano++) {
            Files.writeString(siconfi.resolve("rgf_9900001_" + ano + "_Q.json"), "{\"items\":[{\"coluna\":"
                    + "\"% sobre a RCL Ajustada\",\"cod_conta\":\"DespesaComPessoalTotal\",\"valor\":" + (45 + ano - 2020)
                    + "},{\"coluna\":\"% sobre a RCL Ajustada\",\"cod_conta\":\"LimiteMaximoDespesaComPessoalTotal\",\"valor\":54}]}");
        }
        assertTrue(new GestaoFiscalSiconfi(pasta, null, s -> { }).processar(List.of(a), 2026));
        assertEquals(5, a.getGestaoFiscal().size());
        assertFalse(a.getGestaoFiscal().get(0).isDuranteMandato(), "o ano da eleição é o 'antes'");
        assertTrue(a.getGestaoFiscal().get(1).isDuranteMandato());
        assertEquals(49.0, a.getGestaoFiscal().get(4).getPessoalRcl(), 1e-9);
    }

    @Test
    void cpfMascaradoExigeSeisDigitosIguais() {
        assertTrue(Texto.cpfCompativel("11122233344", "***.222.333-**"));
        assertTrue(Texto.cpfCompativel("11122233344", "***222333**"));
        assertFalse(Texto.cpfCompativel("11122233344", "***.222.334-**"));
        assertFalse(Texto.cpfCompativel("11122233344", "***.***.333-**"), "poucos dígitos visíveis");
        assertFalse(Texto.cpfCompativel(null, "***.222.333-**"));
    }

    @Test
    void complementosSobrevivemAGravacao(@TempDir Path dir) throws Exception {
        BaseDados base = new GeradorDadosDemo().gerar();
        new RepositorioArquivos().salvar(base, dir);
        BaseDados lida = new RepositorioArquivos().carregar(dir);
        assertTrue(lida.getMetadados().isVerificada(Metadados.FONTE_SIAPE));
        for (Candidato c : base.getCandidatos()) {
            Candidato l = lida.buscarCandidato(c.getSq());
            assertEquals(c.getEmpresas().size(), l.getEmpresas().size());
            assertEquals(c.getSancoes().size(), l.getSancoes().size());
            assertEquals(c.getVinculosServidor().size(), l.getVinculosServidor().size());
            assertEquals(c.getGestaoFiscal().size(), l.getGestaoFiscal().size());
            assertEquals(c.getSeriePatrimonio(2026), l.getSeriePatrimonio(2026));
            assertEquals(c.getFinanciamento() == null, l.getFinanciamento() == null);
            if (c.getEmendas() != null) {
                assertEquals(c.getEmendas().getPago(), l.getEmendas().getPago(), 0.01);
                assertEquals(c.getEmendas().getPagoPorAno().size(), l.getEmendas().getPagoPorAno().size());
            }
        }
    }

    private static void zip(Path arquivo, Charset charset, Map<String, String[]> entradas) throws IOException {
        try (OutputStream out = Files.newOutputStream(arquivo); ZipOutputStream z = new ZipOutputStream(out)) {
            for (Map.Entry<String, String[]> e : new java.util.TreeMap<>(entradas).entrySet()) {
                z.putNextEntry(new ZipEntry(e.getKey()));
                z.write((String.join("\r\n", e.getValue()) + "\r\n").getBytes(charset));
                z.closeEntry();
            }
        }
    }
}
