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
    /** Cabeçalho real de consulta_cand_2026 (50 colunas, conferido em arquivo publicado pelo TSE). */
    private static final String[] COLUNAS_CAND = {"DT_GERACAO", "HH_GERACAO", "ANO_ELEICAO", "CD_TIPO_ELEICAO",
        "NM_TIPO_ELEICAO", "NR_TURNO", "CD_ELEICAO", "DS_ELEICAO", "DT_ELEICAO", "TP_ABRANGENCIA", "SG_UF", "SG_UE",
        "NM_UE", "CD_CARGO", "DS_CARGO", "SQ_CANDIDATO", "NR_CANDIDATO", "NM_CANDIDATO", "NM_URNA_CANDIDATO",
        "NM_SOCIAL_CANDIDATO", "NR_CPF_CANDIDATO", "DS_EMAIL", "CD_SITUACAO_CANDIDATURA", "DS_SITUACAO_CANDIDATURA",
        "TP_AGREMIACAO", "NR_PARTIDO", "SG_PARTIDO", "NM_PARTIDO", "NR_FEDERACAO", "NM_FEDERACAO", "SG_FEDERACAO",
        "DS_COMPOSICAO_FEDERACAO", "SQ_COLIGACAO", "NM_COLIGACAO", "DS_COMPOSICAO_COLIGACAO", "SG_UF_NASCIMENTO",
        "DT_NASCIMENTO", "NR_TITULO_ELEITORAL_CANDIDATO", "CD_GENERO", "DS_GENERO", "CD_GRAU_INSTRUCAO",
        "DS_GRAU_INSTRUCAO", "CD_ESTADO_CIVIL", "DS_ESTADO_CIVIL", "CD_COR_RACA", "DS_COR_RACA", "CD_OCUPACAO",
        "DS_OCUPACAO", "CD_SIT_TOT_TURNO", "DS_SIT_TOT_TURNO"};
    private static final String CAB_CAND = "\"" + String.join("\";\"", COLUNAS_CAND) + "\"";

    @Test
    void coletaCompletaSemDownload(@TempDir Path raiz) throws Exception {
        Path brutos = raiz.resolve("brutos");
        Files.createDirectories(brutos);

        // ---- TSE 2026 (atual) e 2022 (anterior)
        zip(brutos.resolve("consulta_cand_2026.zip"), "consulta_cand_2026_ZZ.csv", LATIN1, CAB_CAND,
                cand("ZZ", "DEPUTADO FEDERAL", "260001", "1111", "JOSÉ PEREIRA SANTOS", "ZÉ DA FEIRA", "12345678901",
                        "#NE", "PXA", "20/05/1970", "SUPERIOR COMPLETO"),
                cand("ZZ", "DEPUTADO FEDERAL", "260002", "2222", "MARIA DA LUZ COSTA", "MARIA LUZ", "98765432100",
                        "#NE", "PXB", "01/02/1980", "ENSINO MÉDIO COMPLETO"),
                cand("ZZ", "DEPUTADO FEDERAL", "260003", "3333", "FULANO NOVO", "FULANO", "11122233344",
                        "#NE", "PXA", "03/03/1990", "LÊ E ESCREVE"),
                cand("ZZ", "SENADOR", "260009", "999", "OUTRO CARGO", "OUTRO", "-4", "APTO", "PXA", "01/01/1960",
                        "SUPERIOR COMPLETO"));
        zip(brutos.resolve("bem_candidato_2026.zip"), "bem_candidato_2026_ZZ.csv", LATIN1,
                "\"SQ_CANDIDATO\";\"DS_BEM_CANDIDATO\";\"VR_BEM_CANDIDATO\"",
                "\"260001\";\"Casa\";\"200000,00\"", "\"260001\";\"Carro\";\"100000,00\"",
                "\"260002\";\"Apto\";\"50000,00\"");
        zip(brutos.resolve("consulta_cand_complementar_2026.zip"), "consulta_cand_complementar_2026_ZZ.csv", LATIN1,
                "\"CD_ELEICAO\";\"SQ_CANDIDATO\";\"CD_DETALHE_SITUACAO_CAND\";\"DS_DETALHE_SITUACAO_CAND\";\"ST_REELEICAO\"",
                "\"6259\";\"260001\";\"2\";\"DEFERIDO\";\"S\"",
                "\"6259\";\"260002\";\"2\";\"INDEFERIDO COM RECURSO\";\"S\"",
                "\"6259\";\"260003\";\"3\";\"RENÚNCIA\";\"N\"");
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
                        + "\"deputado_nome\";\"deputado_siglaPartido\";\"deputado_uriPartido\";\"deputado_siglaUf\";"
                        + "\"deputado_idLegislatura\";\"deputado_urlFoto\"",
                "\"1-1\";\"\";\"2023-03-01T10:00:00\";\"Sim\";\"101\";\"\";\"Zé da Feira\";\"PXZ\";\"\";\"ZZ\";\"57\";\"foto101.jpg\"",
                "\"1-2\";\"\";\"2023-03-02T10:00:00\";\"Não\";\"101\";\"\";\"Zé da Feira\";\"PXA\";\"\";\"ZZ\";\"57\";\"foto101.jpg\"",
                "\"1-4\";\"\";\"2023-03-04T10:00:00\";\"Sim\";\"101\";\"\";\"Zé da Feira\";\"PXA\";\"\";\"ZZ\";\"57\";\"foto101.jpg\"",
                "\"1-1\";\"\";\"2023-03-01T10:00:00\";\"Sim\";\"102\";\"\";\"Maria Luz\";\"PXB\";\"\";\"ZZ\";\"57\";\"\"",
                "\"1-4\";\"\";\"2023-03-04T10:00:00\";\"Não\";\"102\";\"\";\"Maria Luz\";\"PXB\";\"\";\"ZZ\";\"57\";\"\"",
                "\"1-1\";\"\";\"2023-03-01T10:00:00\";\"Sim\";\"103\";\"\";\"Outra UF\";\"PXC\";\"\";\"YY\";\"57\";\"\"",
                "\"1-2\";\"\";\"2023-03-02T10:00:00\";\"Sim\";\"999\";\"\";\"Legislatura antiga\";\"PXC\";\"\";\"ZZ\";\"56\";\"\"");
        texto(brutos.resolve("votacoesProposicoes-2023.csv"),
                "﻿\"idVotacao\";\"uriVotacao\";\"data\";\"descricao\";\"proposicao_id\";\"proposicao_uri\";"
                        + "\"proposicao_titulo\";\"proposicao_ementa\"",
                "\"1-1\";\"\";\"2023-03-01\";\"\";\"5001\";\"\";\"PL 10/2023\";\"Dispõe sobre algo\"");
        zip(brutos.resolve("Ano-2023.csv.zip"), "Ano-2023.csv", StandardCharsets.UTF_8,
                "﻿\"txNomeParlamentar\";\"cpf\";\"ideCadastro\";\"sgUF\";\"sgPartido\";\"txtDescricao\";"
                        + "\"vlrLiquido\";\"numMes\";\"numAno\"",
                "\"Zé da Feira\";\"12345678901\";\"101\";\"ZZ\";\"PXA\";\"TELEFONIA\";\"100.50\";\"3\";\"2023\"",
                "\"Zé da Feira\";\"12345678901\";\"101\";\"ZZ\";\"PXA\";\"TELEFONIA\";\"99.50\";\"3\";\"2023\"",
                "\"Zé da Feira\";\"12345678901\";\"101\";\"ZZ\";\"PXA\";\"TELEFONIA\";\"5000\";\"1\";\"2023\"",
                "\"LIDERANÇA DO PXA\";\"\";\"\";\"\";\"\";\"TELEFONIA\";\"7000\";\"3\";\"2023\"",
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
        assertEquals(2, base.getDeputados().size(), "só deputados da UF e da legislatura atual");
        assertEquals(3, base.getTotalVotacoes(), "só votações do Plenário");

        Candidato ze = base.buscarCandidato("260001");
        assertEquals(101, ze.getIdDeputado());
        assertEquals(CruzadorIdentidades.CPF, ze.getCriterioVinculo());
        assertEquals(300000.0, ze.getPatrimonio(), 1e-6);
        assertEquals(150000.0, ze.getPatrimonioAnterior(), 1e-6);
        assertEquals(1, base.getDespesasDe(101).size(), "janeiro/2023 é da legislatura anterior");
        assertEquals(200.0, base.getDespesasDe(101).get(0).getValor(), 1e-6);
        assertEquals("PXA", base.getDeputado(101).getPartido(), "partido do voto mais recente");
        assertEquals("foto101.jpg", base.getDeputado(101).getUrlFoto());
        assertEquals("PL 10/2023", base.getVotacao("1-1").getProposicao());
        assertEquals(br.unit.eleicao.modelo.Elegibilidade.APTA, ze.getElegibilidade());
        assertEquals(Boolean.TRUE, ze.getReeleicao());
        assertEquals(2, base.getProposicoesDe(101).size(), "PL e PEC como proponente");
        assertTrue(base.getProposicoesDe(101).stream().anyMatch(p -> p.isAprovada()
                && p.getEmenta().contains("\n")));

        Candidato maria = base.buscarCandidato("260002");
        assertEquals(102, maria.getIdDeputado());
        assertEquals(CruzadorIdentidades.NOME_NASCIMENTO, maria.getCriterioVinculo(), "CPF só no TSE");
        assertNull(maria.getPatrimonioAnterior());
        assertEquals(br.unit.eleicao.modelo.Elegibilidade.SUB_JUDICE, maria.getElegibilidade());

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

    /** Linha no formato real: campos não usados vêm como "#NULO"/-1, igual ao arquivo do TSE. */
    private static String cand(String uf, String cargo, String sq, String numero, String nome, String urna,
                               String cpf, String situacao, String partido, String nasc, String grau) {
        java.util.Map<String, String> v = new java.util.HashMap<>();
        v.put("DT_GERACAO", "17/08/2026");
        v.put("ANO_ELEICAO", "2026");
        v.put("NR_TURNO", "1");
        v.put("SG_UF", uf);
        v.put("DS_CARGO", cargo);
        v.put("CD_CARGO", "DEPUTADO FEDERAL".equals(cargo) ? "6" : "5");
        v.put("SQ_CANDIDATO", sq);
        v.put("NR_CANDIDATO", numero);
        v.put("NM_CANDIDATO", nome);
        v.put("NM_URNA_CANDIDATO", urna);
        v.put("NR_CPF_CANDIDATO", cpf);
        v.put("DS_EMAIL", "NÃO DIVULGÁVEL");
        v.put("CD_SITUACAO_CANDIDATURA", "-3");
        v.put("DS_SITUACAO_CANDIDATURA", situacao);
        v.put("SG_PARTIDO", partido);
        v.put("DT_NASCIMENTO", nasc);
        v.put("DS_GENERO", "MASCULINO");
        v.put("DS_GRAU_INSTRUCAO", grau);
        v.put("DS_COR_RACA", "PARDA");
        v.put("DS_OCUPACAO", "OUTROS");
        StringBuilder sb = new StringBuilder();
        for (String col : COLUNAS_CAND) {
            if (sb.length() > 0) {
                sb.append(';');
            }
            sb.append('"').append(v.getOrDefault(col, "#NULO")).append('"');
        }
        return sb.toString();
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
