package br.unit.eleicao.util;

import br.unit.eleicao.excecao.ArquivoInvalidoException;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LeitorCsvTest {

    private static LeitorCsv leitor(String conteudo) throws ArquivoInvalidoException {
        return new LeitorCsv(new ByteArrayInputStream(conteudo.getBytes(StandardCharsets.UTF_8)),
                StandardCharsets.UTF_8, ';', "teste.csv");
    }

    @Test
    void aspasSeparadorEQuebraDeLinhaDentroDoCampo() throws Exception {
        try (LeitorCsv csv = leitor("﻿\"id\";\"texto\"\n1;\"a;b \"\"c\"\"\nlinha 2\"\n2;simples\n")) {
            assertEquals(0, csv.indice("ID"));
            assertEquals(1, csv.indice("inexistente", "texto"));
            assertArrayEquals(new String[]{"1", "a;b \"c\"\nlinha 2"}, csv.proximaLinha());
            assertArrayEquals(new String[]{"2", "simples"}, csv.proximaLinha());
            assertNull(csv.proximaLinha());
        }
    }

    @Test
    void colunaObrigatoriaAusenteLancaExcecao() throws Exception {
        try (LeitorCsv csv = leitor("a;b\n")) {
            assertEquals(-1, csv.indiceOpcional("c"));
            assertThrows(ArquivoInvalidoException.class, () -> csv.indice("c"));
        }
    }

    @Test
    void campoForaDoLimiteDevolveVazio() {
        assertEquals("", LeitorCsv.campo(new String[]{"x"}, 3));
        assertEquals("", LeitorCsv.campo(new String[]{"x"}, -1));
    }
}
