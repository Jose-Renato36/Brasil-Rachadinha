package br.unit.eleicao.util;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class TextoTest {

    @Test
    void normalizaAcentosEPontuacao() {
        assertEquals("JOAO DA SILVA", Texto.normalizar("  João   da Silva. "));
        assertEquals("", Texto.normalizar(null));
    }

    @Test
    void decimalBrasileiroEPonto() {
        assertEquals(1234.56, Texto.parseDecimal("1.234,56"), 1e-9);
        assertEquals(1234.56, Texto.parseDecimal("1234.56"), 1e-9);
        assertNull(Texto.parseDecimal(""));
        assertNull(Texto.parseDecimal("abc"));
    }

    @Test
    void datasEmVariosFormatos() {
        LocalDate d = LocalDate.of(1980, 3, 15);
        assertEquals(d, Texto.parseData("15/03/1980"));
        assertEquals(d, Texto.parseData("1980-03-15"));
        assertEquals(d, Texto.parseData("1980-03-15T10:00:00"));
        assertNull(Texto.parseData("99/99/1980"));
    }

    @Test
    void marcadoresDoTseViramVazio() {
        assertEquals("", Texto.limparTse("#NULO#"));
        assertEquals("", Texto.limparTse("-4"));
        assertEquals("APTO", Texto.limparTse(" APTO "));
    }
}
