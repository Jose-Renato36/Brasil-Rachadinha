package br.unit.eleicao.coleta;

import br.unit.eleicao.excecao.ColetaException;

import java.io.IOException;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.function.Consumer;

/** Baixa arquivos com HttpClient, guardando-os em disco (cache): se já existe, não baixa de novo. */
public class Downloader {

    private static final int TENTATIVAS = 3;

    private final HttpClient cliente;
    private final Consumer<String> log;

    public Downloader(Consumer<String> log) {
        this.log = log;
        this.cliente = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(30))
                .proxy(ProxySelector.getDefault())
                .build();
    }

    public Path baixar(String url, Path pasta, boolean forcar) throws ColetaException {
        return baixar(url, pasta.resolve(FontesDados.nomeLocal(url)), forcar, "application/json, */*;q=0.8");
    }

    /** Baixa para um arquivo com nome escolhido, pedindo o formato indicado no cabeçalho Accept. */
    public Path baixar(String url, Path destino, boolean forcar, String accept) throws ColetaException {
        Path pasta = destino.getParent();
        try {
            if (!forcar && Files.exists(destino) && Files.size(destino) > 0) {
                log.accept("  (cache) " + destino.getFileName());
                return destino;
            }
            Files.createDirectories(pasta);
        } catch (IOException e) {
            throw new ColetaException("Não foi possível preparar " + pasta + ": " + e.getMessage(), e);
        }
        Path temporario = pasta.resolve(destino.getFileName() + ".parcial");
        HttpRequest requisicao = HttpRequest.newBuilder(URI.create(url))
                // o CDN do TSE recusa (403) alguns clientes automáticos; um User-Agent de navegador evita isso
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) decisao-eleitoral/0.2")
                .header("Accept", accept)
                .timeout(Duration.ofMinutes(20))
                .GET().build();
        ColetaException ultimaFalha = null;
        for (int tentativa = 1; tentativa <= TENTATIVAS; tentativa++) {
            try {
                log.accept("  baixando " + url + (tentativa > 1 ? " (tentativa " + tentativa + ")" : ""));
                HttpResponse<Path> resposta = cliente.send(requisicao, HttpResponse.BodyHandlers.ofFile(temporario));
                if (resposta.statusCode() != 200) {
                    Files.deleteIfExists(temporario);
                    ultimaFalha = new ColetaException("HTTP " + resposta.statusCode() + " em " + url);
                    if (resposta.statusCode() == 404) {
                        break; // arquivo não existe (ex.: ano ainda sem dados): não adianta repetir
                    }
                    continue;
                }
                Files.move(temporario, destino, StandardCopyOption.REPLACE_EXISTING);
                log.accept("  ok: " + destino.getFileName() + " (" + Files.size(destino) / 1024 + " KB)");
                return destino;
            } catch (IOException e) {
                ultimaFalha = new ColetaException("Falha de rede em " + url + ": " + e.getMessage(), e);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new ColetaException("Download interrompido: " + url, e);
            }
            try {
                Thread.sleep(2000L * tentativa);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new ColetaException("Download interrompido: " + url, e);
            }
        }
        throw ultimaFalha;
    }
}
