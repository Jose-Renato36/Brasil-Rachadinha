package br.unit.eleicao.coleta.mandato;

import br.unit.eleicao.modelo.Candidato;
import br.unit.eleicao.modelo.CandidaturaAnterior;
import br.unit.eleicao.modelo.Cargo;
import br.unit.eleicao.modelo.FormaResumo;
import br.unit.eleicao.modelo.SerieMandato;
import br.unit.eleicao.modelo.TemaMandato;
import br.unit.eleicao.util.LeitorXlsx;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Leitores das fontes de "antes e depois" com respostas no formato real de cada órgão. */
class IndicadoresMandatoTest {

    /** Trecho de resposta real do SICONFI (RREO-Anexo 01, Itapema/SC, 2023, 6º bimestre). */
    private static final String RREO_REAL = "{\"items\":["
            + item("TotalReceitas", "TOTAL DAS RECEITAS (V) = (III + IV)", "PREVISÃO INICIAL", "526000000")
            + "," + item("TotalReceitas", "TOTAL DAS RECEITAS (V) = (III + IV)", "Até o Bimestre (c)", "584323462.63")
            + "," + item("Investimentos", "INVESTIMENTOS", "DESPESAS EMPENHADAS ATÉ O BIMESTRE (f)", "126668111.34")
            + "," + item("Investimentos", "INVESTIMENTOS", "DESPESAS LIQUIDADAS ATÉ O BIMESTRE (h)", "108027470.91")
            + "," + item("TotalDespesas", "TOTAL DAS DESPESAS (XII) = (X + XI)", "DESPESAS LIQUIDADAS ATÉ O BIMESTRE (h)",
            "608083407.04") + "],\"hasMore\":false}";

    private static String item(String cod, String conta, String coluna, String valor) {
        return "{\"exercicio\":2023,\"periodo\":6,\"cod_ibge\":4208302,\"anexo\":\"RREO-Anexo 01\",\"coluna\":\"" + coluna
                + "\",\"cod_conta\":\"" + cod + "\",\"conta\":\"" + conta + "\",\"valor\":" + valor + "}";
    }

    @Test
    void siconfiLeInvestimentoEResultadoDoAno() {
        Double[] v = ContasSiconfi.interpretarRreo(RREO_REAL);
        assertEquals(100 * 108027470.91 / 608083407.04, v[0], 1e-9);
        assertEquals(100 * (584323462.63 - 608083407.04) / 584323462.63, v[1], 1e-9);
        assertArrayEquals(new Double[2], ContasSiconfi.interpretarRreo(null));
    }

    @Test
    void siconfiLePercentualDePessoalELimite() {
        String json = "{\"items\":[{\"coluna\":\"Valor\",\"cod_conta\":\"DespesaComPessoalTotal\",\"valor\":987654.3},"
                + "{\"coluna\":\"% sobre a RCL Ajustada\",\"cod_conta\":\"DespesaComPessoalTotal\",\"valor\":51.27},"
                + "{\"coluna\":\"% sobre a RCL Ajustada\",\"cod_conta\":\"LimiteMaximoDespesaComPessoalTotal\",\"valor\":54}]}";
        Double[] v = ContasSiconfi.interpretarRgf(json);
        assertEquals(51.27, v[0], 1e-9);
        assertEquals(54.0, v[1], 1e-9);
        assertNull(ContasSiconfi.interpretarRgf("{\"items\":[]}")[0]);
    }

    private static String ibge(String var, String codigo, String serie) {
        return "{\"id\":\"" + var + "\",\"variavel\":\"x\",\"unidade\":\"Mil Reais\",\"resultados\":[{\"classificacoes\":[],"
                + "\"series\":[{\"localidade\":{\"id\":\"" + codigo + "\",\"nivel\":{\"id\":\"N6\",\"nome\":\"Município\"},"
                + "\"nome\":\"Cidade - SE\"},\"serie\":{" + serie + "}}]}]}";
    }

    @Test
    void apiIbgeLeVariasVariaveisEIgnoraSemDado() {
        String json = "[" + ibge("37", "2800308", "\"2020\":\"1000\",\"2021\":\"1100\",\"2022\":\"...\"") + ","
                + ibge("543", "2800308", "\"2020\":\"20000.5\",\"2021\":\"-\"") + "]";
        Map<String, Map<String, Double>> r = ApiIbge.ler(json);
        assertEquals(Map.of("2020", 1000.0, "2021", 1100.0), r.get("37"));
        assertEquals(Map.of("2020", 20000.5), r.get("543"));
        Map<String, Double> trimestral = new LinkedHashMap<>();
        for (int q = 1; q <= 4; q++) {
            trimestral.put("2023" + "0" + q, 8.0 + q);
        }
        trimestral.put("202401", 7.0); // ano incompleto não entra
        assertEquals(Map.of(2023, 10.5), ApiIbge.mediaAnualDeTrimestres(trimestral));
    }

    @Test
    void bancoCentralUsaDezembroNasSeriesMensais() {
        String json = "[{\"data\":\"01/11/2021\",\"valor\":\"10.74\"},{\"data\":\"01/12/2021\",\"valor\":\"10.06\"},"
                + "{\"data\":\"01/12/2022\",\"valor\":\"5.79\"}]";
        assertEquals(Map.of(2021, 10.06, 2022, 5.79), EconomiaNacionalBcb.interpretar(json, true));
        assertEquals(3, EconomiaNacionalBcb.interpretar("[{\"data\":\"01/01/2019\",\"valor\":\"1.2\"},"
                + "{\"data\":\"01/01/2020\",\"valor\":\"-3.3\"},{\"data\":\"01/01/2021\",\"valor\":\"4.8\"}]", false).size());
    }

    @Test
    void serieResumeComparandoComAReferencia() {
        SerieMandato s = new SerieMandato("x", Cargo.PREFEITO, "Prefeitura de CIDADE/SE", 2020, 2024,
                TemaMandato.ECONOMIA, "PIB por pessoa", "R$", FormaResumo.VARIACAO_PERCENTUAL, true);
        s.setReferenciaNome("Sergipe");
        s.adicionar(2019, 90, 95.0);
        s.adicionar(2020, 100, 100.0); // antes (ano da eleição)
        s.adicionar(2021, 110, 104.0);
        s.adicionar(2022, 125, 110.0);
        assertEquals(25.0, s.resultado(false), 1e-9);
        assertEquals(10.0, s.resultado(true), 1e-9);
        assertTrue(s.melhorQueReferencia());
        assertEquals("PIB por pessoa: +25,0% (Sergipe: +10,0%) de 2020 a 2022", s.getResumo());
        SerieMandato mpv = new SerieMandato("y", Cargo.PRESIDENTE, "Governo federal", 2018, 2022, TemaMandato.LEIS,
                "Medidas provisórias editadas", "", FormaResumo.SOMA, null);
        mpv.adicionar(2019, 48, null);
        mpv.adicionar(2020, 107, null);
        assertEquals(155.0, mpv.resultado(false), 1e-9);
        assertNull(mpv.melhorQueReferencia());
    }

    @Test
    void xlsxDoInepComNomesAbreviadosECabecalhoNaDecimaLinha() throws Exception {
        List<String[]> mun = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            mun.add(new String[]{i == 0 ? "Ministério da Educação" : ""});
        }
        mun.add(new String[]{"SG_UF", "CO_MUNICIPIO", "NO_MUNICIPIO", "REDE", "VL_OBSERVADO_2019", "VL_OBSERVADO_2021",
            "VL_OBSERVADO_2023"});
        mun.add(new String[]{"SE", "2800308", "Aracaju", "Estadual", "4.1", "4.3", "4.4"});
        mun.add(new String[]{"SE", "2800308", "Aracaju", "Municipal", "4.6", "-", "5.2"});
        IdebInep ideb = new IdebInep(Path.of("."), null, s -> { }, 2026);
        ideb.lerMunicipios(new LeitorXlsx(new ByteArrayInputStream(xlsx(Map.of("Municípios", mun)))));

        List<String[]> ufs = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            ufs.add(new String[]{""});
        }
        ufs.add(new String[]{"", "", "VL_OBSERVADO_2019", "VL_OBSERVADO_2021", "VL_OBSERVADO_2023"});
        ufs.add(new String[]{"R. G. do Norte", "Estadual", "3.5", "3.6", "3.8"});
        ufs.add(new String[]{"", "Pública (4)", "3.9", "4.0", "4.1"});
        ufs.add(new String[]{"Brasil", "Estadual", "4.4", "4.5", "4.6"});
        Map<String, List<String[]>> abas = new LinkedHashMap<>();
        abas.put("UF e Regiões (AI)", java.util.Collections.singletonList(ufs.get(9)));
        abas.put("UF e Regiões (AF)", ufs);
        ideb.lerAgregados(new LeitorXlsx(new ByteArrayInputStream(xlsx(abas))), false);

        MandatoExecutivo prefeito = new MandatoExecutivo(Cargo.PREFEITO, "Prefeitura de ARACAJU/SE", "2800308", "SE",
                2020, 2024, 2025);
        List<SerieMandato> s = ideb.coletar(prefeito);
        assertEquals(1, s.size());
        assertEquals(0.6, s.get(0).resultado(false), 1e-9, "2019 (antes) → 2023");
        MandatoExecutivo governo = new MandatoExecutivo(Cargo.GOVERNADOR, "Governo do Rio Grande do Norte", "24", "RN",
                2018, 2022, 2025);
        List<SerieMandato> g = ideb.coletar(governo);
        assertEquals("IDEB (rede estadual, anos finais)", g.get(0).getTitulo());
        assertEquals(4.5, g.get(0).getPontos().stream().filter(p -> p.getAno() == 2021).findFirst().get().getReferencia(),
                1e-9);
    }

    @Test
    void prefeituraGanhaPibComReferenciaEContasDoCache(@TempDir Path brutos) throws Exception {
        Candidato c = new Candidato("1", "ANA", "ANA", LocalDate.of(1970, 1, 1), "FEMININO");
        c.setUf("SE");
        c.setCargo("GOVERNADOR");
        CandidaturaAnterior t = new CandidaturaAnterior(2020, "PREFEITO", "CIDADE/SE", "PXA", "Eleito(a)");
        t.setCodigoUe("00123");
        c.adicionarCandidaturaAnterior(t);
        Files.writeString(brutos.resolve("municipios_brasileiros_tse.csv"),
                "codigo_tse,uf,nome_municipio,capital,codigo_ibge\n123,SE,CIDADE,0,2899999\n");
        Path pib = brutos.resolve("mandatos").resolve("ibge-pib");
        Files.createDirectories(pib);
        Files.writeString(pib.resolve("t5938_v37-543_2899999.json"), "[" + ibge("37", "2899999",
                "\"2020\":\"1000\",\"2021\":\"1200\",\"2022\":\"1300\"") + "," + ibge("543", "2899999", "\"2020\":\"10\"") + "]");
        Files.writeString(pib.resolve("t5938_v37-543_28.json"), "[" + ibge("37", "28",
                "\"2020\":\"50000\",\"2021\":\"52000\",\"2022\":\"55000\"") + "]");
        Path siconfi = brutos.resolve("mandatos").resolve("siconfi");
        Files.createDirectories(siconfi);
        Files.writeString(siconfi.resolve("rreo1_2899999_2023.json"), RREO_REAL);

        assertTrue(new IndicadoresMandato(brutos, null, s -> { }, 2026).processar(List.of(c)));
        SerieMandato crescimento = c.getSeriesMandato().stream().filter(s -> s.getTitulo().equals("Crescimento do PIB"))
                .findFirst().orElseThrow();
        assertEquals("Prefeitura de CIDADE/SE", crescimento.getEnte());
        assertEquals("Sergipe", crescimento.getReferenciaNome());
        assertEquals(30.0, crescimento.resultado(false), 1e-9);
        assertEquals(10.0, crescimento.resultado(true), 1e-9);
        assertEquals(1_300_000.0, crescimento.getUltimoDoMandato().getValor(), 1e-6, "IBGE publica em R$ mil");
        assertTrue(c.getSeriesMandato().stream().anyMatch(s -> s.getTitulo().equals("Investimento")));
        assertFalse(c.getSeriesMandato().stream().anyMatch(s -> s.getTitulo().equals("PIB por pessoa")
                && s.resultado(false) != null), "só há o ano de 2020: sem variação no mandato");
    }

    @Test
    void presidenteContaMedidasProvisoriasEProjetosDoExecutivo(@TempDir Path brutos) throws Exception {
        Files.writeString(brutos.resolve("proposicoesAutores-2019.csv"),
                "\"idProposicao\";\"uriProposicao\";\"idDeputadoAutor\";\"uriAutor\";\"codTipoAutor\";\"tipoAutor\";"
                        + "\"nomeAutor\";\"siglaPartidoAutor\";\"uriPartidoAutor\";\"siglaUFAutor\";\"ordemAssinatura\";\"proponente\"\n"
                        + "\"1\";\"\";\"\";\"\";\"30000\";\"Órgão do Poder Executivo\";\"Poder Executivo\";\"\";\"\";\"\";\"1\";\"1\"\n"
                        + "\"2\";\"\";\"\";\"\";\"30000\";\"Órgão do Poder Executivo\";\"Poder Executivo\";\"\";\"\";\"\";\"1\";\"1\"\n"
                        + "\"3\";\"\";\"\";\"\";\"30000\";\"Órgão do Poder Executivo\";\"Poder Executivo\";\"\";\"\";\"\";\"1\";\"1\"\n"
                        + "\"4\";\"\";\"204554\";\"\";\"10000\";\"Deputado(a)\";\"Fulano\";\"PXA\";\"\";\"SE\";\"1\";\"1\"\n");
        Files.writeString(brutos.resolve("proposicoes-2019.csv"),
                "\"id\";\"uri\";\"siglaTipo\";\"numero\";\"ano\";\"ementa\";\"dataApresentacao\";\"ultimoStatus_descricaoSituacao\"\n"
                        + "\"1\";\"\";\"MPV\";\"870\";\"2019\";\"x\";\"2019-01-01T00:00:00\";\"Transformado em Norma Jurídica\"\n"
                        + "\"2\";\"\";\"MPV\";\"871\";\"2019\";\"x\";\"2019-01-18T00:00:00\";\"Perda de Eficácia\"\n"
                        + "\"3\";\"\";\"PL\";\"1\";\"2019\";\"x\";\"2019-02-04T00:00:00\";\"Aguardando Apreciação pelo Senado Federal\"\n"
                        + "\"4\";\"\";\"PL\";\"2\";\"2019\";\"x\";\"2019-02-04T00:00:00\";\"Transformado em Norma Jurídica\"\n");
        ProjetosExecutivo p = new ProjetosExecutivo(brutos, null, s -> { });
        MandatoExecutivo m = new MandatoExecutivo(Cargo.PRESIDENTE, "Governo federal", "1", "BR", 2018, 2022, 2025);
        assertArrayEquals(new int[]{2, 1, 1, 1}, p.contar(2019, m));
        assertNull(p.contar(2020, m), "sem arquivo do ano");
    }

    /** Monta um .xlsx mínimo (textos em sharedStrings) com as abas e linhas informadas. */
    static byte[] xlsx(Map<String, List<String[]>> abas) throws IOException {
        List<String> textos = new ArrayList<>();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ZipOutputStream z = new ZipOutputStream(out)) {
            StringBuilder wb = new StringBuilder("<workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" "
                    + "xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\"><sheets>");
            StringBuilder rels = new StringBuilder("<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">");
            int n = 0;
            for (Map.Entry<String, List<String[]>> aba : abas.entrySet()) {
                n++;
                wb.append("<sheet name=\"").append(aba.getKey()).append("\" sheetId=\"").append(n).append("\" r:id=\"rId")
                        .append(n).append("\"/>");
                rels.append("<Relationship Id=\"rId").append(n).append("\" Target=\"worksheets/sheet").append(n)
                        .append(".xml\"/>");
                StringBuilder sh = new StringBuilder("<worksheet><sheetData>");
                int r = 0;
                for (String[] linha : aba.getValue()) {
                    r++;
                    sh.append("<row r=\"").append(r).append("\">");
                    for (int c = 0; c < linha.length; c++) {
                        if (linha[c].isEmpty()) {
                            continue;
                        }
                        String ref = (char) ('A' + c) + String.valueOf(r);
                        if (linha[c].matches("-?\\d+(\\.\\d+)?")) {
                            sh.append("<c r=\"").append(ref).append("\"><v>").append(linha[c]).append("</v></c>");
                        } else {
                            textos.add(linha[c]);
                            sh.append("<c r=\"").append(ref).append("\" t=\"s\"><v>").append(textos.size() - 1).append("</v></c>");
                        }
                    }
                    sh.append("</row>");
                }
                sh.append("</sheetData></worksheet>");
                z.putNextEntry(new ZipEntry("xl/worksheets/sheet" + n + ".xml"));
                z.write(sh.toString().getBytes(StandardCharsets.UTF_8));
                z.closeEntry();
            }
            z.putNextEntry(new ZipEntry("xl/workbook.xml"));
            z.write((wb + "</sheets></workbook>").getBytes(StandardCharsets.UTF_8));
            z.closeEntry();
            z.putNextEntry(new ZipEntry("xl/_rels/workbook.xml.rels"));
            z.write((rels + "</Relationships>").getBytes(StandardCharsets.UTF_8));
            z.closeEntry();
            StringBuilder ss = new StringBuilder("<sst>");
            for (String t : textos) {
                ss.append("<si><t>").append(t).append("</t></si>");
            }
            z.putNextEntry(new ZipEntry("xl/sharedStrings.xml"));
            z.write((ss + "</sst>").getBytes(StandardCharsets.UTF_8));
            z.closeEntry();
        }
        return out.toByteArray();
    }
}
