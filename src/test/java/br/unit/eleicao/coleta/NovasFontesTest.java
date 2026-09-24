package br.unit.eleicao.coleta;

import br.unit.eleicao.modelo.Candidato;
import br.unit.eleicao.modelo.CandidaturaAnterior;
import br.unit.eleicao.modelo.Metadados;
import br.unit.eleicao.modelo.VinculoEmpresa;
import br.unit.eleicao.preparo.Avaliacao;
import br.unit.eleicao.preparo.ExperienciaGestao;
import br.unit.eleicao.preparo.AlertaExpulsao;
import br.unit.eleicao.preparo.ResumoCidadao;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Cargos públicos (PEP), expulsões (CEAF), contratos, planos, verba do Senado, despesas e votos. */
class NovasFontesTest {

    private static final Charset LATIN1 = StandardCharsets.ISO_8859_1;
    private static final Charset CP1252 = Charset.forName("windows-1252");

    private static Candidato candidato(String sq, String nome, String cargo) {
        Candidato c = new Candidato(sq, nome, nome, LocalDate.of(1970, 5, 20), "FEMININO");
        c.setUf("SE");
        c.setCargo(cargo);
        return c;
    }

    @Test
    void pepLigaCargoDeGestaoPorNomeECpfParcial(@TempDir Path pasta) throws Exception {
        Candidato a = candidato("1", "ANA MARIA EXEMPLO", "GOVERNADOR");
        zip(pasta.resolve(CargosPublicosPep.ARQUIVO), LATIN1, "202608_PEP.csv",
                "\"CPF\";\"Nome_PEP\";\"Sigla_Função\";\"Descrição_Função\";\"Nível_Função\";\"Nome_Órgão\";"
                        + "\"Data_Início_Exercício\";\"Data_Fim_Exercício\";\"Data_Fim_Carência\"",
                "\"***.222.333-**\";\"ANA MARIA EXEMPLO\";\"SEC\";\"SECRETÁRIO DE ESTADO\";\"\";\"GOVERNO DE SERGIPE\";"
                        + "\"01/01/2019\";\"31/03/2022\";\"31/03/2027\"",
                "\"***.999.999-**\";\"ANA MARIA EXEMPLO\";\"MIN\";\"MINISTRO\";\"\";\"MINISTÉRIO X\";\"01/01/2020\";\"\";\"\"");
        assertTrue(new CargosPublicosPep(pasta, s -> { }).processar(List.of(a), Map.of("1", "11122233344")));
        assertEquals(1, a.getCargosPublicos().size(), "homônima com outro CPF fica de fora");
        assertEquals("SECRETÁRIO DE ESTADO", a.getCargosPublicos().get(0).getFuncao());
        Metadados meta = new Metadados("SE", 2026, 2022, LocalDate.of(2026, 10, 4));
        meta.marcarVerificada(Metadados.FONTE_CARGOS_PUBLICOS);
        assertEquals(Avaliacao.POSITIVO, new ExperienciaGestao().avaliar(a, meta).getAvaliacao());
    }

    @Test
    void ceafViraExpulsaoENaoSancaoDeEmpresa(@TempDir Path pasta) throws Exception {
        Candidato a = candidato("1", "ANA MARIA EXEMPLO", "SENADOR");
        zip(pasta.resolve("ceaf.zip"), CP1252, "20260920_CEAF.csv",
                "\"CADASTRO\";\"CPF OU CNPJ DO SANCIONADO\";\"NOME DO SANCIONADO\";\"CATEGORIA DA SANÇÃO\";"
                        + "\"DATA INÍCIO SANÇÃO\";\"DATA FINAL SANÇÃO\";\"ÓRGÃO SANCIONADOR\"",
                "\"CEAF\";\"***.222.333-**\";\"ANA MARIA EXEMPLO\";\"Demissão\";\"05/03/2021\";\"\";\"MINISTÉRIO X\"");
        SancoesCgu s = new SancoesCgu(pasta, x -> { });
        assertTrue(s.processar(List.of(a), Map.of("1", "11122233344")));
        assertTrue(s.getLidos().contains("CEAF"));
        assertEquals("CEAF", a.getSancoes().get(0).getCadastro());
        Metadados meta = new Metadados("SE", 2026, 2022, LocalDate.of(2026, 10, 4));
        meta.marcarVerificada(Metadados.FONTE_EXPULSOES);
        assertEquals(Avaliacao.ATENCAO, new AlertaExpulsao().avaliar(a, meta).getAvaliacao());
    }

    @Test
    void contratosFederaisDaEmpresaDoSocio(@TempDir Path brutos) throws Exception {
        Candidato a = candidato("1", "ANA", "DEPUTADO FEDERAL");
        a.adicionarEmpresa(new VinculoEmpresa("12345678", "EXEMPLO LTDA", "Sócio", ""));
        Path compras = brutos.resolve(ContratosFederais.PASTA);
        Files.createDirectories(compras);
        zip(compras.resolve("202401.zip"), CP1252, "202401_Compras.csv",
                "\"Número do Contrato\";\"Objeto\";\"Código Contratado\";\"Nome Contratado\";\"Valor Inicial Compra\";"
                        + "\"Nome Órgão\";\"Data Assinatura Contrato\"",
                "\"1/2024\";\"Limpeza\";\"12345678000190\";\"EXEMPLO LTDA\";\"150000,00\";\"MINISTÉRIO X\";\"10/01/2024\"",
                "\"2/2024\";\"Outro\";\"99999999000190\";\"OUTRA\";\"1,00\";\"MINISTÉRIO X\";\"10/01/2024\"");
        assertTrue(new ContratosFederais(brutos, s -> { }).processar(List.of(a)));
        assertEquals(1, a.getContratos().size());
        assertEquals(150000.0, a.getContratos().get(0).getValor(), 1e-6);
    }

    @Test
    void planoDeGovernoEhCopiadoPeloNumeroDaCandidatura(@TempDir Path raiz) throws Exception {
        Path brutos = raiz.resolve("brutos");
        Files.createDirectories(brutos);
        Candidato gov = candidato("260001234567", "ANA", "GOVERNADOR");
        Candidato dep = candidato("260009999999", "BIA", "DEPUTADO FEDERAL");
        try (OutputStream out = Files.newOutputStream(brutos.resolve("proposta_governo_2026_SE.zip"));
             ZipOutputStream z = new ZipOutputStream(out)) {
            for (String nome : new String[]{"2026SE260001234567_01.pdf", "2026SE260009999999_01.pdf"}) {
                z.putNextEntry(new ZipEntry(nome));
                z.write("%PDF-1.4 teste".getBytes(StandardCharsets.ISO_8859_1));
                z.closeEntry();
            }
        }
        Path destino = raiz.resolve("processados");
        assertTrue(new PlanosGoverno(brutos, s -> { }).processar(List.of(gov, dep), "SE", 2026, destino));
        assertEquals(List.of("260001234567_1.pdf"), gov.getPlanosGoverno());
        assertTrue(dep.getPlanosGoverno().isEmpty(), "só cargos do Executivo");
        assertTrue(Files.exists(destino.resolve("planos").resolve("260001234567_1.pdf")));
    }

    @Test
    void verbaDoSenadoPulaLinhaDeAtualizacao(@TempDir Path pasta) throws Exception {
        Files.write(pasta.resolve("despesa_ceaps_2025.csv"), ("\"ULTIMA ATUALIZACAO\";\"08/09/2026 02:03\"\r\n"
                + "\"ANO\";\"MES\";\"SENADOR\";\"TIPO_DESPESA\";\"CNPJ_CPF\";\"FORNECEDOR\";\"DOCUMENTO\";\"DATA\";"
                + "\"DETALHAMENTO\";\"VALOR_REEMBOLSADO\";\"COD_DOCUMENTO\"\r\n"
                + "\"2025\";\"1\";\"ANA EXEMPLO\";\"Passagens\";\"\";\"\";\"\";\"\";\"\";\"1000,00\";\"1\"\r\n"
                + "\"2025\";\"1\";\"ANA EXEMPLO\";\"Aluguel\";\"\";\"\";\"\";\"\";\"\";\"500,00\";\"2\"\r\n"
                + "\"2025\";\"2\";\"ANA EXEMPLO\";\"Aluguel\";\"\";\"\";\"\";\"\";\"\";\"500,00\";\"3\"\r\n").getBytes(CP1252));
        Map<String, Double> gasto = new CotaSenado(pasta, s -> { }).gastoMensal(new int[]{2024, 2025});
        assertEquals(1000.0, gasto.get("ANA EXEMPLO"), 1e-6, "R$ 2.000 em 2 meses");
    }

    @Test
    void despesasDeCampanhaEmCategoriasSimples() {
        assertEquals("Anúncios na internet", FinanciamentoCampanha.tipoDespesa("Despesa com Impulsionamento de Conteúdos"));
        assertEquals("Propaganda (impressos, adesivos, carro de som)",
                FinanciamentoCampanha.tipoDespesa("Publicidade por materiais impressos"));
        assertEquals("Serviços (advogados, contadores, pesquisas)",
                FinanciamentoCampanha.tipoDespesa("Serviços advocatícios"));
        assertEquals("Outras despesas", FinanciamentoCampanha.tipoDespesa("Taxas bancárias"));
    }

    @Test
    void votosDoPrimeiroTurnoSomadosPorZona(@TempDir Path pasta) throws Exception {
        Candidato a = candidato("1", "ANA", "GOVERNADOR");
        CandidaturaAnterior t = new CandidaturaAnterior(2022, "DEPUTADO ESTADUAL", "SE", "PXA", "Eleito(a)");
        t.setSqOrigem("220001");
        a.adicionarCandidaturaAnterior(t);
        zip(pasta.resolve("votacao_candidato_munzona_2022.zip"), LATIN1, "votacao_candidato_munzona_2022_SE.csv",
                "\"NR_TURNO\";\"SG_UF\";\"CD_MUNICIPIO\";\"NR_ZONA\";\"SQ_CANDIDATO\";\"QT_VOTOS_NOMINAIS\"",
                "\"1\";\"SE\";\"31054\";\"1\";\"220001\";\"1200\"",
                "\"1\";\"SE\";\"31054\";\"2\";\"220001\";\"800\"",
                "\"2\";\"SE\";\"31054\";\"1\";\"220001\";\"999\"");
        HistoricoTse h = new HistoricoTse(pasta, "SE", s -> { });
        h.processar(List.of(a), new int[]{2022});
        assertEquals(2000L, t.getVotos());
        assertTrue(h.isVotosLidos());
    }

    @Test
    void resumoParaOCidadaoEmFrasesCurtas() {
        Candidato a = candidato("1", "ANA", "GOVERNADOR");
        a.setSituacao("APTO");
        a.setDetalheSituacao("DEFERIDO");
        a.adicionarCandidaturaAnterior(new CandidaturaAnterior(2020, "PREFEITO", "CIDADE/SE", "PXA", "Eleito(a)"));
        a.setPatrimonio(1_200_000.0);
        Metadados meta = new Metadados("SE", 2026, 2022, LocalDate.of(2026, 10, 4));
        meta.marcarVerificada(Metadados.FONTE_PLANOS);
        List<String> frases = new java.util.ArrayList<>();
        new ResumoCidadao().montar(a, meta).forEach(f -> frases.add(f.getTexto()));
        assertTrue(frases.contains("Já foi prefeito(a) (CIDADE/SE) de 2021 a 2024."), frases.toString());
        assertTrue(frases.contains("Declarou R$ 1,2 milhão em bens."), frases.toString());
        assertTrue(frases.stream().anyMatch(f -> f.startsWith("Não encontramos plano de governo")));
        assertEquals("R$ 350 mil", ResumoCidadao.moedaCurta(350_000));
    }

    @Test
    void semArquivoNaoHaDado(@TempDir Path pasta) throws Exception {
        Candidato a = candidato("1", "ANA", "GOVERNADOR");
        assertEquals(false, new CargosPublicosPep(pasta, s -> { }).processar(List.of(a), new HashMap<>()));
        assertNull(a.getDespesasCampanha());
    }

    private static void zip(Path arquivo, Charset charset, String entrada, String... linhas) throws IOException {
        try (OutputStream out = Files.newOutputStream(arquivo); ZipOutputStream z = new ZipOutputStream(out)) {
            z.putNextEntry(new ZipEntry(entrada));
            z.write((String.join("\r\n", linhas) + "\r\n").getBytes(charset));
            z.closeEntry();
        }
    }
}
