package br.unit.eleicao.coleta;

import br.unit.eleicao.excecao.ColetaException;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Downloads que o servidor recusa (403): segunda tentativa pelo curl e mensagem com o que fazer à mão. */
class DownloaderTest {

    private static HttpServer servidor(int recusas, AtomicInteger pedidos) throws Exception {
        HttpServer s = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        s.createContext("/arquivo.zip", troca -> {
            int n = pedidos.incrementAndGet();
            byte[] corpo = (n <= recusas ? "bloqueado" : "PKconteudo").getBytes(StandardCharsets.UTF_8);
            troca.sendResponseHeaders(n <= recusas ? 403 : 200, corpo.length);
            try (OutputStream out = troca.getResponseBody()) {
                out.write(corpo);
            }
        });
        s.start();
        return s;
    }

    private static boolean temCurl() {
        try {
            return new ProcessBuilder("curl", "--version").start().waitFor() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    @Test
    void depoisDo403TentaPeloCurl(@TempDir Path pasta) throws Exception {
        Assumptions.assumeTrue(temCurl(), "curl não instalado");
        AtomicInteger pedidos = new AtomicInteger();
        HttpServer s = servidor(1, pedidos);
        try {
            String url = "http://127.0.0.1:" + s.getAddress().getPort() + "/arquivo.zip";
            Path baixado = new Downloader(x -> { }).baixar(url, pasta, false);
            assertEquals("PKconteudo", Files.readString(baixado));
            assertEquals(2, pedidos.get(), "1º pedido pelo Java (recusado), 2º pelo curl");
        } finally {
            s.stop(0);
        }
    }

    @Test
    void semSaidaDizOndeSalvarOArquivo(@TempDir Path pasta) throws Exception {
        HttpServer s = servidor(99, new AtomicInteger());
        try {
            String url = "http://127.0.0.1:" + s.getAddress().getPort() + "/arquivo.zip";
            ColetaException e = assertThrows(ColetaException.class, () -> new Downloader(x -> { }).baixar(url, pasta, false));
            assertTrue(e.getMessage().contains("HTTP 403"));
            assertTrue(e.getMessage().contains("Baixe este arquivo pelo navegador"));
            assertTrue(e.getMessage().contains(pasta.resolve("arquivo.zip").toAbsolutePath().toString()));
        } finally {
            s.stop(0);
        }
    }

    @Test
    void refererDeCadaSite() {
        assertEquals("https://dadosabertos.tse.jus.br/",
                Downloader.referer("https://cdn.tse.jus.br/estatistica/sead/odsele/consulta_cand/consulta_cand_2026.zip"));
        assertEquals("https://portaldatransparencia.gov.br/download-de-dados",
                Downloader.referer("https://portaldatransparencia.gov.br/download-de-dados/ceis/20260920"));
    }
}
