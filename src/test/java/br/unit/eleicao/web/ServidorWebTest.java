package br.unit.eleicao.web;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Sobe o servidor de verdade (com a demonstração) e faz pedidos HTTP como o navegador faria. */
class ServidorWebTest {

    @TempDir
    Path raiz;
    private ServidorWeb servidor;
    private int porta;
    private final HttpClient cliente = HttpClient.newBuilder().proxy(HttpClient.Builder.NO_PROXY).build();

    @BeforeEach
    void subir() throws Exception {
        ServicoApi api = new ServicoApi(raiz);
        api.abrir("demo");
        servidor = new ServidorWeb(api);
        porta = servidor.iniciar(18_080);
    }

    @AfterEach
    void parar() {
        servidor.parar();
    }

    private HttpResponse<String> get(String caminho) throws Exception {
        return cliente.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + porta + caminho)).build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> post(String caminho, String corpo) throws Exception {
        return cliente.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + porta + caminho))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(corpo)).build(), HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void entregaInterfaceEApi() throws Exception {
        HttpResponse<String> pagina = get("/");
        assertEquals(200, pagina.statusCode());
        assertTrue(pagina.body().contains("Voto com Dados"));
        assertEquals(200, get("/app.js").statusCode());
        assertEquals(404, get("/nao-existe.txt").statusCode());

        HttpResponse<String> estado = get("/api/estado");
        assertTrue(estado.body().contains("\"demo\":true"));
        HttpResponse<String> ranking = get("/api/ranking?peso.assiduidade=10&cobertura=2");
        assertEquals(200, ranking.statusCode());
        assertTrue(ranking.body().contains("\"posicao\":1"));
        assertEquals(400, get("/api/candidato").statusCode(), "sq obrigatório");
        assertEquals(404, get("/api/qualquer").statusCode());
    }

    @Test
    void posicaoDoUsuarioLigaOAlinhamento() throws Exception {
        assertEquals(400, get("/api/posicao?idVotacao=DEMO-1&voto=Sim").statusCode(), "alteração exige POST");
        HttpResponse<String> r = post("/api/posicao", "idVotacao=DEMO-150&voto=Sim&tema=sa%C3%BAde");
        assertEquals(200, r.statusCode(), r.body());
        assertTrue(r.body().contains("\"posicoes\":1"));
        String perfil = get("/api/candidato?sq=DEMO0001").body();
        assertTrue(perfil.contains("\"alinhamento\""));
        assertEquals(400, post("/api/posicao", "idVotacao=DEMO-150&voto=Talvez").statusCode());
    }
}
