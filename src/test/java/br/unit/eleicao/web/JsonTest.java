package br.unit.eleicao.web;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JsonTest {

    @Test
    void escreveEstruturasEscapandoTexto() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("nome", "Zé \"da\" Feira\n");
        m.put("nota", 7.5);
        m.put("semDados", Double.NaN);
        m.put("lista", Arrays.asList(1, true, null));
        assertEquals("{\"nome\":\"Zé \\\"da\\\" Feira\\n\",\"nota\":7.5,\"semDados\":null,\"lista\":[1,true,null]}",
                Json.escrever(m));
    }
}
