package br.unit.eleicao.coleta;

import br.unit.eleicao.modelo.BaseDados;
import br.unit.eleicao.modelo.Candidato;
import br.unit.eleicao.modelo.Cargo;
import br.unit.eleicao.modelo.Metadados;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Coleta do Brasil inteiro: lê _BRASIL.csv e _BR.csv sem contar duas vezes o que aparece nos dois. */
class ModoNacionalTest {

    private static final String CAB = "\"SG_UF\";\"DS_CARGO\";\"SQ_CANDIDATO\";\"NM_CANDIDATO\";\"NM_URNA_CANDIDATO\";"
            + "\"SG_PARTIDO\";\"NR_CANDIDATO\"";

    private static String l(String uf, String cargo, String sq, String nome) {
        return "\"" + String.join("\";\"", uf, cargo, sq, nome, nome, "PX", "10") + "\"";
    }

    private static void zip(Path arquivo, String[][] entradas) throws IOException {
        try (OutputStream out = Files.newOutputStream(arquivo); ZipOutputStream z = new ZipOutputStream(out)) {
            for (String[] e : entradas) {
                z.putNextEntry(new ZipEntry(e[0]));
                z.write(e[1].getBytes(ProcessadorTSE.LATIN1));
                z.closeEntry();
            }
        }
    }

    @Test
    void leTodosOsEstadosEAPresidenciaSemDuplicar(@TempDir Path pasta) throws Exception {
        String presidente = l("BR", "PRESIDENTE", "900", "FULANA PRESIDENTE");
        zip(pasta.resolve("consulta_cand_2026.zip"), new String[][]{
            {"consulta_cand_2026_BRASIL.csv", CAB + "\n" + l("AA", "GOVERNADOR", "1", "ANA") + "\n"
                    + l("BB", "SENADOR", "2", "BIA") + "\n" + presidente + "\n"},
            {"consulta_cand_2026_BR.csv", CAB + "\n" + presidente + "\n"},
            {"consulta_cand_2026_AA.csv", CAB + "\n" + l("AA", "GOVERNADOR", "1", "ANA") + "\n"}});
        String cabBens = "\"SG_UF\";\"SQ_CANDIDATO\";\"NR_ORDEM_BEM_CANDIDATO\";\"DS_BEM_CANDIDATO\";\"VR_BEM_CANDIDATO\"";
        String bemPresidente = "\"BR\";\"900\";\"1\";\"Casa\";\"500000,00\"";
        zip(pasta.resolve("bem_candidato_2026.zip"), new String[][]{
            {"bem_candidato_2026_BRASIL.csv", cabBens + "\n" + bemPresidente + "\n\"AA\";\"1\";\"1\";\"Carro\";\"10000,00\"\n"},
            {"bem_candidato_2026_BR.csv", cabBens + "\n" + bemPresidente + "\n"}});

        BaseDados base = new BaseDados(new Metadados("BR", 2026, 2022, null));
        new ProcessadorTSE(pasta, "BR", 2026, 2022, s -> { }).processar(base);

        assertEquals(3, base.getCandidatos().size());
        Candidato pres = base.buscarCandidato("900");
        assertEquals(Cargo.PRESIDENTE, pres.getTipoCargo());
        assertEquals("BR", pres.getUf());
        assertEquals(500000.0, pres.getPatrimonio(), 1e-6, "bem que está nos dois arquivos conta uma vez");
        assertEquals("BB", base.buscarCandidato("2").getUf());
    }
}
