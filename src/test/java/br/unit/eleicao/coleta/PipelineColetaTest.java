package br.unit.eleicao.coleta;

import br.unit.eleicao.modelo.BaseDados;
import br.unit.eleicao.modelo.Candidato;
import br.unit.eleicao.persistencia.RepositorioArquivos;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Executa a coleta inteira (sem download) sobre arquivos brutos pequenos que imitam o layout
 * real do TSE e da Câmara: zip com CSV por UF em ISO-8859-1, CSVs UTF-8 com BOM, aspas etc.
 */
class PipelineColetaTest {

    private static final Charset LATIN1 = StandardCharsets.ISO_8859_1;
    private static final String CAB_CAND = "\"DT_GERACAO\";\"ANO_ELEICAO\";\"SG_UF\";\"CD_CARGO\";\"DS_CARGO\";"
            + "\"SQ_CANDIDATO\";\"NR_CANDIDATO\";\"NM_CANDIDATO\";\"NM_URNA_CANDIDATO\";\"NR_CPF_CANDIDATO\";"
            + "\"DS_SITUACAO_CANDIDATURA\";\"SG_PARTIDO\";\"DT_NASCIMENTO\";\"DS_GENERO\";\"DS_GRAU_INSTRUCAO\";"
            + "\"DS_COR_RACA\";\"DS_OCUPACAO\"";

    @Test
    void coletaCompletaSemDownload(@TempDir Path raiz) throws Exception {
        Path brutos = raiz.resolve("brutos");
        Files.createDirectories(brutos);

        // ---- TSE 2026 (atual) e 2022 (anterior)
        zip(brutos.resolve("consulta_cand_2026.zip"), "consulta_cand_2026_ZZ.csv", LATIN1, CAB_CAND,
                cand("ZZ", "DEPUTADO FEDERAL", "260001", "1111", "JOSÉ PEREIRA SANTOS", "ZÉ DA FEIRA", "12345678901",
                        "APTO", "PXA", "20/05/1970", "SUPERIOR COMPLETO"),
                cand("ZZ", "DEPUTADO FEDERAL", "260002", "2222", "MARIA DA LUZ COSTA", "MARIA LUZ", "-4",
                        "APTO", "PXB", "01/02/1980", "ENSINO MÉDIO COMPLETO"),
                cand("ZZ", "DEPUTADO FEDERAL", "260003", "3333", "FULANO NOVO", "FULANO", "-4",
                        "INAPTO", "PXA", "03/03/1990", "LÊ E ESCREVE"),
                cand("ZZ", "SENADOR", "260009", "999", "OUTRO CARGO", "OUTRO", "-4", "APTO", "PXA", "01/01/1960",
                        "SUPERIOR COMPLETO"));
        zip(brutos.resolve("bem_candidato_2026.zip"), "bem_candidato_2026_ZZ.csv", LATIN1,
                "\"SQ_CANDIDATO\";\"DS_BEM_CANDIDATO\";\"VR_BEM_CANDIDATO\"",
                "\"260001\";\"Casa\";\"200000,00\"", "\"260001\";\"Carro\";\"100000,00\"",
                "\"260002\";\"Apto\";\"50000,00\"");
        zip(brutos.resolve("consulta_cand_2022.zip"), "consulta_cand_2022_ZZ.csv", LATIN1, CAB_CAND,
                cand("ZZ", "DEPUTADO FEDERAL", "220001", "1111", "JOSÉ PEREIRA SANTOS", "ZÉ DA FEIRA", "12345678901",
                        "APTO", "PXA", "20/05/1970", "SUPERIOR COMPLETO"));
        zip(brutos.resolve("bem_candidato_2022.zip"), "bem_candidato_2022_ZZ.csv", LATIN1,
                "\"SQ_CANDIDATO\";\"VR_BEM_CANDIDATO\"", "\"220001\";\"150000,00\"");

        // ---- Câmara (UTF-8 com BOM)
        texto(brutos.resolve("deputados.csv"),
                "﻿\"uri\";\"nome\";\"idLegislaturaInicial\";\"idLegislaturaFinal\";\"nomeCivil\";\"cpf\";"
                        + "\"siglaSexo\";\"dataNascimento\"",
                "\"https://dadosabertos.camara.leg.br/api/v2/deputados/101\";\"Zé da Feira\";\"56\";\"57\";"
                        + "\"José Pereira Santos\";\"12345678901\";\"M\";\"1970-05-20\"",
                "\"https://dadosabertos.camara.leg.br/api/v2/deputados/102\";\"Maria Luz\";\"57\";\"57\";"
                        + "\"Maria da Luz Costa\";\"\";\"F\";\"1980-02-01\"",
                "\"https://dadosabertos.camara.leg.br/api/v2/deputados/103\";\"Outra UF\";\"57\";\"57\";"
                        + "\"Pessoa de Outra UF\";\"\";\"F\";\"1980-02-01\"");
        texto(brutos.resolve("votacoes-2023.csv"),
                "﻿\"id\";\"uri\";\"data\";\"siglaOrgao\";\"descricao\"",
                "\"1-1\";\"u\";\"2023-03-01\";\"PLEN\";\"Aprovado o projeto; \"\"texto\"\"\"",
                "\"1-2\";\"u\";\"2023-03-02\";\"PLEN\";\"Rejeitada a emenda\"",
                "\"1-3\";\"u\";\"2023-03-03\";\"CCJC\";\"Votação em comissão (ignorada)\"",
                "\"1-4\";\"u\";\"2023-03-04\";\"PLEN\";\"Outra\"");
        texto(brutos.resolve("votacoesVotos-2023.csv"),
                "﻿\"idVotacao\";\"uriVotacao\";\"dataHoraVoto\";\"voto\";\"deputado_id\";\"deputado_uri\";"
                        + "\"deputado_nome\";\"deputado_siglaPartido\";\"deputado_uriPartido\";\"deputado_siglaUf\"",
                "\"1-1\";\"\";\"\";\"Sim\";\"101\";\"\";\"Zé da Feira\";\"PXA\";\"\";\"ZZ\"",
                "\"1-2\";\"\";\"\";\"Não\";\"101\";\"\";\"Zé da Feira\";\"PXA\";\"\";\"ZZ\"",
                "\"1-4\";\"\";\"\";\"Sim\";\"101\";\"\";\"Zé da Feira\";\"PXA\";\"\";\"ZZ\"",
                "\"1-1\";\"\";\"\";\"Sim\";\"102\";\"\";\"Maria Luz\";\"PXB\";\"\";\"ZZ\"",
                "\"1-4\";\"\";\"\";\"Não\";\"102\";\"\";\"Maria Luz\";\"PXB\";\"\";\"ZZ\"",
                "\"1-1\";\"\";\"\";\"Sim\";\"103\";\"\";\"Outra UF\";\"PXC\";\"\";\"YY\"");
        zip(brutos.resolve("Ano-2023.csv.zip"), "Ano-2023.csv", StandardCharsets.UTF_8,
                "﻿\"txNomeParlamentar\";\"cpf\";\"ideCadastro\";\"sgUF\";\"sgPartido\";\"txtDescricao\";"
                        + "\"vlrLiquido\";\"numMes\";\"numAno\"",
                "\"Zé da Feira\";\"12345678901\";\"101\";\"ZZ\";\"PXA\";\"TELEFONIA\";\"100.50\";\"1\";\"2023\"",
                "\"Zé da Feira\";\"12345678901\";\"101\";\"ZZ\";\"PXA\";\"TELEFONIA\";\"99.50\";\"1\";\"2023\"",
                "\"Maria Luz\";\"\";\"102\";\"ZZ\";\"PXB\";\"PASSAGEM AÉREA\";\"300\";\"2\";\"2023\"",
                "\"Outra UF\";\"\";\"103\";\"YY\";\"PXC\";\"PASSAGEM AÉREA\";\"999\";\"2\";\"2023\"");
        texto(brutos.resolve("proposicoesAutores-2023.csv"),
                "﻿\"idProposicao\";\"uriProposicao\";\"idDeputadoAutor\";\"uriAutor\";\"codTipoAutor\";"
                        + "\"tipoAutor\";\"nomeAutor\";\"siglaPartidoAutor\";\"siglaUFAutor\";\"ordemAssinatura\";"
                        + "\"proponente\"",
                "\"5001\";\"\";\"101\";\"\";\"10000\";\"Deputado\";\"Zé\";\"PXA\";\"ZZ\";\"1\";\"1\"",
                "\"5002\";\"\";\"101\";\"\";\"10000\";\"Deputado\";\"Zé\";\"PXA\";\"ZZ\";\"1\";\"1\"",
                "\"5003\";\"\";\"101\";\"\";\"10000\";\"Deputado\";\"Zé\";\"PXA\";\"ZZ\";\"2\";\"0\"",
                "\"5004\";\"\";\"101\";\"\";\"10000\";\"Deputado\";\"Zé\";\"PXA\";\"ZZ\";\"1\";\"1\"");
        texto(brutos.resolve("proposicoes-2023.csv"),
                "﻿\"id\";\"uri\";\"siglaTipo\";\"numero\";\"ano\";\"ementa\";\"ultimoStatus_descricaoSituacao\"",
                "\"5001\";\"\";\"PL\";\"10\";\"2023\";\"Ementa com\nquebra de linha\";\"Transformado em Norma Jurídica\"",
                "\"5002\";\"\";\"PEC\";\"11\";\"2023\";\"Outra\";\"Aguardando Parecer\"",
                "\"5003\";\"\";\"PL\";\"12\";\"2023\";\"Coautor não proponente\";\"Aguardando Parecer\"",
                "\"5004\";\"\";\"REQ\";\"13\";\"2023\";\"Requerimento (tipo ignorado)\";\"Aprovado\"");

        List<String> log = new ArrayList<>();
        PipelineColeta pipeline = new PipelineColeta(raiz, log::add);
        BaseDados base = pipeline.executar("zz", 2026, false);

        assertEquals(3, base.getCandidatos().size(), "só deputado federal da UF");
        assertEquals(2, base.getDeputados().size(), "só deputados da UF");
        assertEquals(3, base.getTotalVotacoes(), "só votações do Plenário");

        Candidato ze = base.buscarCandidato("260001");
        assertEquals(101, ze.getIdDeputado());
        assertEquals(CruzadorIdentidades.CPF, ze.getCriterioVinculo());
        assertEquals(300000.0, ze.getPatrimonio(), 1e-6);
        assertEquals(150000.0, ze.getPatrimonioAnterior(), 1e-6);
        assertEquals(200.0, base.getDespesasDe(101).get(0).getValor(), 1e-6);
        assertEquals(2, base.getProposicoesDe(101).size(), "PL e PEC como proponente");
        assertTrue(base.getProposicoesDe(101).stream().anyMatch(p -> p.isAprovada()
                && p.getEmenta().contains("\n")));

        Candidato maria = base.buscarCandidato("260002");
        assertEquals(102, maria.getIdDeputado());
        assertEquals(CruzadorIdentidades.NOME_NASCIMENTO, maria.getCriterioVinculo());
        assertNull(maria.getPatrimonioAnterior());

        Candidato fulano = base.buscarCandidato("260003");
        assertTrue(fulano.isInapta());
        assertNull(fulano.getIdDeputado());
        assertEquals("Lê e escreve", fulano.getGrauInstrucao().getDescricao());

        // arquivos processados podem ser relidos e não guardam CPF
        Path processados = pipeline.pastaProcessada("ZZ");
        BaseDados relida = new RepositorioArquivos().carregar(processados);
        assertEquals(3, relida.getCandidatos().size());
        assertEquals(LocalDate.of(2026, 10, 4), relida.getMetadados().getDataEleicao());
        for (Path arquivo : Files.list(processados).toList()) {
            assertFalse(Files.readString(arquivo).contains("12345678901"), "CPF gravado em " + arquivo);
        }
    }

    private static String cand(String uf, String cargo, String sq, String numero, String nome, String urna,
                               String cpf, String situacao, String partido, String nasc, String grau) {
        String[] campos = {"01/09/2026", "2026", uf, "6", cargo, sq, numero, nome, urna, cpf, situacao, partido, nasc,
            "MASCULINO", grau, "PARDA", "OUTROS"};
        return "\"" + String.join("\";\"", campos) + "\"";
    }

    private static void texto(Path arquivo, String... linhas) throws IOException {
        Files.writeString(arquivo, String.join("\n", linhas) + "\n", StandardCharsets.UTF_8);
    }

    private static void zip(Path arquivo, String entrada, Charset charset, String... linhas) throws IOException {
        try (OutputStream out = Files.newOutputStream(arquivo); ZipOutputStream zip = new ZipOutputStream(out)) {
            zip.putNextEntry(new ZipEntry("leiame.pdf"));
            zip.write(new byte[]{1, 2, 3});
            zip.closeEntry();
            zip.putNextEntry(new ZipEntry(entrada));
            zip.write((String.join("\n", linhas) + "\n").getBytes(charset));
            zip.closeEntry();
        }
    }
}
