package br.unit.eleicao.coleta;

import br.unit.eleicao.modelo.Candidato;
import br.unit.eleicao.modelo.CandidaturaAnterior;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProcessadorTrajetoriaTest {

    private static final String CAB = "\"SG_UF\";\"NM_UE\";\"NR_TURNO\";\"DS_CARGO\";\"SQ_CANDIDATO\";\"NM_CANDIDATO\";"
            + "\"NR_CPF_CANDIDATO\";\"SG_PARTIDO\";\"DT_NASCIMENTO\";\"DS_SIT_TOT_TURNO\"";

    private static String linha(String ue, int turno, String cargo, String sq, String nome, String cpf, String partido,
                                String nasc, String resultado) {
        return String.join(";", "\"ZZ\"", q(ue), q(String.valueOf(turno)), q(cargo), q(sq), q(nome), q(cpf), q(partido),
                q(nasc), q(resultado));
    }

    private static String q(String s) {
        return "\"" + s + "\"";
    }

    private static void zip(Path arquivo, String entrada, String... linhas) throws IOException {
        try (OutputStream out = Files.newOutputStream(arquivo); ZipOutputStream z = new ZipOutputStream(out)) {
            z.putNextEntry(new ZipEntry(entrada));
            z.write((CAB + "\n" + String.join("\n", linhas) + "\n").getBytes(ProcessadorTSE.LATIN1));
            z.closeEntry();
        }
    }

    @Test
    void ligaPorCpfOuNomeENascimentoEEscolheOTurnoFinal(@TempDir Path pasta) throws Exception {
        zip(pasta.resolve("consulta_cand_2022.zip"), "consulta_cand_2022_ZZ.csv",
                linha("ZZ", 1, "DEPUTADO ESTADUAL", "22001", "ANA MARIA SOUZA", "11111111111", "PXA", "01/01/1980", "ELEITO POR QP"));
        // 2024: CPF mascarado ("-4"), liga por nome + nascimento; o 2º turno substitui o 1º
        zip(pasta.resolve("consulta_cand_2024.zip"), "consulta_cand_2024_ZZ.csv",
                linha("CIDADE A", 1, "PREFEITO", "24001", "ANA MARIA SOUZA", "-4", "PXA", "01/01/1980", "2º TURNO"),
                linha("CIDADE A", 2, "PREFEITO", "24001", "ANA MARIA SOUZA", "-4", "PXA", "01/01/1980", "NÃO ELEITO"),
                linha("CIDADE B", 1, "VEREADOR", "24002", "JOSE LIMA", "-4", "PXB", "05/05/1970", "SUPLENTE"),
                linha("CIDADE C", 1, "VEREADOR", "24003", "JOSE LIMA", "-4", "PXC", "09/09/1999", "ELEITO"));

        Candidato ana = new Candidato("26001", "ANA MARIA SOUZA", "ANA", LocalDate.of(1980, 1, 1), "");
        Candidato jose = new Candidato("26002", "JOSÉ LIMA", "ZÉ", LocalDate.of(1970, 5, 5), "");
        Candidato nova = new Candidato("26003", "PESSOA NOVA", "NOVA", LocalDate.of(1990, 1, 1), "");
        List<Candidato> todos = new ArrayList<>(List.of(ana, jose, nova));

        new ProcessadorTrajetoria(pasta, "ZZ", s -> { }).processar(todos, Map.of("26001", "11111111111"),
                new int[]{2018, 2020, 2022, 2024});

        assertEquals(2, ana.getTrajetoria().size());
        CandidaturaAnterior prefeitura = ana.getTrajetoria().get(0);
        assertEquals(2024, prefeitura.getAno());
        assertEquals("Não eleito(a)", prefeitura.getResultado(), "resultado do 2º turno");
        assertEquals("CIDADE A/ZZ", prefeitura.getLocal());
        CandidaturaAnterior estadual = ana.getTrajetoria().get(1);
        assertTrue(estadual.isEleito());
        assertEquals("Deputado(a) estadual", estadual.getCargoLegivel());
        assertEquals("ZZ", estadual.getLocal(), "eleição geral: só a UF");
        assertEquals(estadual, ana.getMandatoAtual(2026), "eleita em 2022: mandato até 2026");

        assertEquals(1, jose.getTrajetoria().size(), "homônimo com outra data de nascimento não entra");
        assertEquals("Suplente", jose.getTrajetoria().get(0).getResultado());
        assertNull(jose.getMandatoAtual(2026));
        assertFalse(jose.jaFoiEleito());
        assertTrue(nova.getTrajetoria().isEmpty());
    }

    @Test
    void mandatoMunicipalAtual() {
        CandidaturaAnterior vereador = new CandidaturaAnterior(2024, "VEREADOR", "X", "P", "Eleito(a)");
        assertEquals(2028, vereador.getFimMandato());
        CandidaturaAnterior antigo = new CandidaturaAnterior(2020, "VEREADOR", "X", "P", "Eleito(a)");
        Candidato c = new Candidato("1", "A", "A", null, "");
        c.adicionarCandidaturaAnterior(antigo);
        assertNull(c.getMandatoAtual(2026), "mandato 2021-2024 já acabou");
        c.adicionarCandidaturaAnterior(vereador);
        assertEquals(vereador, c.getMandatoAtual(2026));
        assertEquals("Eleito(a)", CandidaturaAnterior.resultadoSimples("ELEITO POR MÉDIA"));
        assertEquals("Sem resultado", CandidaturaAnterior.resultadoSimples("#NULO#"));
    }
}
