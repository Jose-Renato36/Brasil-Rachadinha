package br.unit.eleicao.web;

import br.unit.eleicao.excecao.DadosException;
import br.unit.eleicao.util.Texto;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;

/**
 * Servidor HTTP embutido no JDK (com.sun.net.httpserver): entrega a interface (HTML, CSS, JS
 * guardados em src/main/resources/web) e responde à API em /api/... com JSON.
 * Escuta só em 127.0.0.1: a página abre no navegador do próprio computador (localhost).
 */
public class ServidorWeb {

    private final ServicoApi api;
    private HttpServer servidor;

    public ServidorWeb(ServicoApi api) {
        this.api = api;
    }

    /** Sobe o servidor na porta pedida ou, se ocupada, nas seguintes. Devolve a porta usada. */
    public int iniciar(int portaInicial) throws IOException {
        IOException ultima = null;
        for (int porta = portaInicial; porta < portaInicial + 10; porta++) {
            try {
                servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", porta), 0);
                servidor.createContext("/api/", this::atenderApi);
                servidor.createContext("/planos/", this::atenderPlano);
                servidor.createContext("/", this::atenderArquivo);
                servidor.setExecutor(Executors.newFixedThreadPool(4));
                servidor.start();
                return porta;
            } catch (BindException e) {
                ultima = e;
            }
        }
        throw new IOException("Nenhuma porta livre entre " + portaInicial + " e " + (portaInicial + 9), ultima);
    }

    public void parar() {
        if (servidor != null) {
            servidor.stop(0);
        }
    }

    // ------------------------------------------------------------------ API

    private void atenderApi(HttpExchange troca) throws IOException {
        String rota = troca.getRequestURI().getPath().substring("/api/".length());
        Map<String, String> p = parametros(troca);
        try {
            Object resposta = rotear(rota, troca.getRequestMethod(), p);
            responder(troca, 200, "application/json; charset=utf-8", Json.escrever(resposta));
        } catch (RotaInexistente e) {
            responder(troca, 404, "application/json; charset=utf-8", erro("Rota desconhecida: /api/" + rota));
        } catch (DadosException | IllegalArgumentException e) {
            responder(troca, 400, "application/json; charset=utf-8", erro(e.getMessage()));
        } catch (RuntimeException e) {
            e.printStackTrace();
            responder(troca, 500, "application/json; charset=utf-8", erro("Erro interno: " + e));
        }
    }

    /** Liga cada endereço /api/... a um método do serviço. */
    private Object rotear(String rota, String metodo, Map<String, String> p) throws DadosException, RotaInexistente {
        boolean post = "POST".equalsIgnoreCase(metodo);
        switch (rota) {
            case "estado":
                return api.estado();
            case "abrir":
                return api.abrir(obrigatorio(p, "base"));
            case "ranking":
                return api.ranking(p);
            case "candidatos":
                return api.candidatos(p);
            case "busca":
                return api.busca(p.get("q"), p.get("uf"));
            case "candidato":
                return api.candidato(obrigatorio(p, "sq"));
            case "distribuicao":
                return api.distribuicao(p.get("tipo"), p.get("cargo"), p.get("uf"));
            case "variaveis":
                return api.variaveis();
            case "correlacao":
                return api.correlacao(obrigatorio(p, "x"), obrigatorio(p, "y"), p.get("cargo"), p.get("uf"));
            case "votacoes":
                Integer limite = Texto.parseInteiro(p.get("limite"));
                return api.votacoes(p.get("busca"), "1".equals(p.get("principais")), limite == null ? 40 : limite);
            case "posicao":
                exigirPost(post);
                return api.definirPosicao(obrigatorio(p, "idVotacao"), p.get("voto"), p.get("tema"));
            case "coleta":
                if (!post) {
                    return api.statusColeta();
                }
                Integer ano = Texto.parseInteiro(p.get("ano"));
                return api.iniciarColeta(obrigatorio(p, "uf"), ano == null ? 2026 : ano, !"0".equals(p.get("baixar")));
            default:
                throw new RotaInexistente();
        }
    }

    /** Sinaliza endereço /api/... que não existe (vira HTTP 404). */
    private static final class RotaInexistente extends Exception {
    }

    private static void exigirPost(boolean post) throws DadosException {
        if (!post) {
            throw new DadosException("Use POST para alterar dados.");
        }
    }

    private static String obrigatorio(Map<String, String> p, String nome) throws DadosException {
        String v = p.get(nome);
        if (Texto.vazio(v)) {
            throw new DadosException("Parâmetro obrigatório ausente: " + nome);
        }
        return v;
    }

    private static String erro(String mensagem) {
        Map<String, Object> m = new HashMap<>();
        m.put("erro", mensagem);
        return Json.escrever(m);
    }

    /** Junta os parâmetros da URL (?a=1&b=2) e do corpo de formulários (POST). */
    private static Map<String, String> parametros(HttpExchange troca) throws IOException {
        Map<String, String> p = new HashMap<>();
        lerPares(troca.getRequestURI().getRawQuery(), p);
        if ("POST".equalsIgnoreCase(troca.getRequestMethod())) {
            try (InputStream in = troca.getRequestBody()) {
                lerPares(new String(in.readAllBytes(), StandardCharsets.UTF_8), p);
            }
        }
        return p;
    }

    private static void lerPares(String texto, Map<String, String> destino) {
        if (Texto.vazio(texto)) {
            return;
        }
        for (String par : texto.split("&")) {
            int i = par.indexOf('=');
            String chave = i < 0 ? par : par.substring(0, i);
            String valor = i < 0 ? "" : par.substring(i + 1);
            destino.put(URLDecoder.decode(chave, StandardCharsets.UTF_8), URLDecoder.decode(valor, StandardCharsets.UTF_8));
        }
    }

    // ------------------------------------------------------------------ arquivos da interface

    private void atenderArquivo(HttpExchange troca) throws IOException {
        String caminho = troca.getRequestURI().getPath();
        if (caminho.equals("/") || caminho.isEmpty()) {
            caminho = "/index.html";
        }
        if (caminho.contains("..")) {
            responder(troca, 400, "text/plain; charset=utf-8", "Caminho inválido");
            return;
        }
        try (InputStream in = ServidorWeb.class.getResourceAsStream("/web" + caminho)) {
            if (in == null) {
                responder(troca, 404, "text/plain; charset=utf-8", "Não encontrado: " + caminho);
                return;
            }
            enviar(troca, 200, tipo(caminho), in.readAllBytes());
        }
    }

    /** PDF do plano de governo copiado para a pasta "planos" da base carregada. */
    private void atenderPlano(HttpExchange troca) throws IOException {
        String nome = troca.getRequestURI().getPath().substring("/planos/".length());
        java.nio.file.Path pasta = api.pastaPlanos();
        if (!nome.matches("[A-Za-z0-9_]+\\.pdf") || pasta == null) {
            responder(troca, 404, "text/plain; charset=utf-8", "Plano não encontrado");
            return;
        }
        java.nio.file.Path arquivo = pasta.resolve(nome);
        if (!java.nio.file.Files.isRegularFile(arquivo)) {
            responder(troca, 404, "text/plain; charset=utf-8", "Plano não encontrado");
            return;
        }
        enviar(troca, 200, "application/pdf", java.nio.file.Files.readAllBytes(arquivo));
    }

    private static String tipo(String caminho) {
        if (caminho.endsWith(".html")) {
            return "text/html; charset=utf-8";
        }
        if (caminho.endsWith(".css")) {
            return "text/css; charset=utf-8";
        }
        if (caminho.endsWith(".js")) {
            return "text/javascript; charset=utf-8";
        }
        if (caminho.endsWith(".svg")) {
            return "image/svg+xml";
        }
        return "application/octet-stream";
    }

    private static void responder(HttpExchange troca, int status, String tipo, String corpo) throws IOException {
        enviar(troca, status, tipo, corpo.getBytes(StandardCharsets.UTF_8));
    }

    private static void enviar(HttpExchange troca, int status, String tipo, byte[] corpo) throws IOException {
        troca.getResponseHeaders().set("Content-Type", tipo);
        troca.getResponseHeaders().set("Cache-Control", "no-store");
        troca.sendResponseHeaders(status, corpo.length);
        try (OutputStream out = troca.getResponseBody()) {
            out.write(corpo);
        }
    }
}
