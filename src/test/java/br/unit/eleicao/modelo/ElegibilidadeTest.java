package br.unit.eleicao.modelo;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ElegibilidadeTest {

    @Test
    void situacoesDoTse() {
        assertEquals(Elegibilidade.APTA, Elegibilidade.de("APTO", "DEFERIDO"));
        assertEquals(Elegibilidade.APTA, Elegibilidade.de("", "DEFERIDO COM RECURSO"));
        assertEquals(Elegibilidade.SUB_JUDICE, Elegibilidade.de("APTO", "INDEFERIDO COM RECURSO"));
        assertEquals(Elegibilidade.INAPTA, Elegibilidade.de("INAPTO", "INDEFERIDO"));
        assertEquals(Elegibilidade.INAPTA, Elegibilidade.de("", "RENÚNCIA"));
        assertEquals(Elegibilidade.INAPTA, Elegibilidade.de("", "CANCELADO"));
        // agosto de 2026: situação ainda "#NE" (vira vazio) e sem detalhe
        assertEquals(Elegibilidade.EM_ANALISE, Elegibilidade.de("", ""));
        assertEquals(Elegibilidade.EM_ANALISE, Elegibilidade.de("CADASTRADO", null));
    }
}
