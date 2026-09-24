package br.unit.eleicao;

import br.unit.eleicao.persistencia.RepositorioArquivos;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** O site abre sozinho os dados reais quando existem; a demonstração só sem nenhuma coleta. */
class AppTest {

    private static void base(Path processados, String uf, long quando) throws Exception {
        Path pasta = processados.resolve(uf);
        Files.createDirectories(pasta);
        Files.writeString(pasta.resolve(RepositorioArquivos.META), "uf=" + uf);
        Files.writeString(pasta.resolve(RepositorioArquivos.CANDIDATOS), "sq");
        Files.setLastModifiedTime(pasta.resolve(RepositorioArquivos.META), FileTime.fromMillis(quando));
    }

    @Test
    void escolheBrasilDepoisOUltimoEstadoDepoisDemonstracao(@TempDir Path dados) throws Exception {
        Path processados = dados.resolve("processados");
        assertEquals("demo", App.baseReal(processados), "nenhuma coleta");
        base(processados, "SE", 1_000);
        base(processados, "AL", 2_000);
        assertEquals("AL", App.baseReal(processados), "estado coletado por último");
        base(processados, "BR", 500);
        assertEquals("BR", App.baseReal(processados), "Brasil inteiro tem prioridade");
    }
}
