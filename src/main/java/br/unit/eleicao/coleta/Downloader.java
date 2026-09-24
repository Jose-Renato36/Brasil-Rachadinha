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
    /** Navegador comum: alguns servidores públicos (CDN do TSE, Portal da Transparência) recusam clientes automáticos. */
    static final String NAVEGADOR = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) "
            + "Chrome/128.0.0.0 Safari/537.36";

    private final HttpClient cliente;
    private final Consumer<String> log;

    public Downloader(Consumer<String> log) {
        this.log = log;
        this.cliente = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1) // alguns firewalls de órgãos públicos barram HTTP/2 de programas
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
                .header("User-Agent", NAVEGADOR)
                .header("Accept", accept)
                .header("Accept-Language", "pt-BR,pt;q=0.9,en;q=0.8")
                .header("Referer", referer(url))
                .timeout(Duration.ofMinutes(60))
                .GET().build();
        ColetaException ultimaFalha = null;
        for (int tentativa = 1; tentativa <= TENTATIVAS; tentativa++) {
            try {
                log.accept("  baixando " + url + (tentativa > 1 ? " (tentativa " + tentativa + ")" : ""));
                HttpResponse<Path> resposta = cliente.send(requisicao, HttpResponse.BodyHandlers.ofFile(temporario));
                int status = resposta.statusCode();
                if (status != 200) {
                    Files.deleteIfExists(temporario);
                    ultimaFalha = new ColetaException("HTTP " + status + " em " + url);
                    if (status == 404) {
                        break; // arquivo não existe (ex.: ano ainda sem dados): não adianta repetir
                    }
                    if (status == 401 || status == 403 || status == 429) {
                        // bloqueio de programa: tenta pelo curl do sistema, que o servidor costuma aceitar
                        if (baixarComCurl(url, temporario, accept)) {
                            Files.move(temporario, destino, StandardCopyOption.REPLACE_EXISTING);
                            log.accept("  ok (via curl): " + destino.getFileName() + " (" + Files.size(destino) / 1024 + " KB)");
                            return destino;
                        }
                        break;
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
        throw new ColetaException(ultimaFalha.getMessage() + "\n    -> Baixe este arquivo pelo navegador (" + url
                + ") e salve como " + destino.toAbsolutePath() + " ; depois rode a coleta de novo.", ultimaFalha);
    }

    /** Página de origem de cada site, como faria um navegador que clicou no link. */
    static String referer(String url) {
        if (url.contains("tse.jus.br")) {
            return "https://dadosabertos.tse.jus.br/";
        }
        if (url.contains("portaldatransparencia.gov.br") || url.contains("cgu.gov.br")) {
            return "https://portaldatransparencia.gov.br/download-de-dados";
        }
        if (url.contains("camara.leg.br")) {
            return "https://dadosabertos.camara.leg.br/";
        }
        if (url.contains("senado.leg.br")) {
            return "https://www12.senado.leg.br/dados-abertos";
        }
        int i = url.indexOf('/', url.indexOf("//") + 2);
        return i > 0 ? url.substring(0, i + 1) : url;
    }

    /**
     * Segunda tentativa com o curl do sistema (vem no Windows 10/11, macOS e Linux). Devolve false se o curl não
     * existir ou também falhar.
     */
    private boolean baixarComCurl(String url, Path destino, String accept) {
        try {
            log.accept("  servidor recusou o programa (bloqueio); tentando pelo curl do sistema...");
            Process p = new ProcessBuilder("curl", "-L", "-f", "-sS", "--retry", "2", "-A", NAVEGADOR,
                    "-H", "Accept: " + accept, "-H", "Accept-Language: pt-BR,pt;q=0.9", "-e", referer(url),
                    "-o", destino.toString(), url)
                    .redirectErrorStream(true).start();
            String saida = new String(p.getInputStream().readAllBytes()).strip();
            int codigo = p.waitFor();
            if (codigo == 0 && Files.exists(destino) && Files.size(destino) > 0) {
                return true;
            }
            log.accept("  curl também falhou" + (saida.isEmpty() ? "" : ": " + saida));
        } catch (IOException e) {
            log.accept("  curl não encontrado neste computador");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        try {
            Files.deleteIfExists(destino);
        } catch (IOException ignorada) {
            // arquivo parcial: será sobrescrito na próxima tentativa
        }
        return false;
    }
}
