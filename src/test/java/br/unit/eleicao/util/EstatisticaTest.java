package br.unit.eleicao.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EstatisticaTest {

    @Test
    void mediaEDesvioAmostral() {
        double[] v = {2, 4, 4, 4, 5, 5, 7, 9};
        assertEquals(5.0, Estatistica.media(v), 1e-9);
        assertEquals(2.138, Estatistica.desvioPadrao(v), 1e-3);
    }

    @Test
    void zScore() {
        assertEquals(1.5, Estatistica.zScore(13, 10, 2), 1e-9);
        assertTrue(Double.isNaN(Estatistica.zScore(13, 10, 0)));
    }

    @Test
    void pearsonPerfeitoEInverso() {
        double[] x = {1, 2, 3, 4, 5};
        assertEquals(1.0, Estatistica.pearson(x, new double[]{2, 4, 6, 8, 10}), 1e-9);
        assertEquals(-1.0, Estatistica.pearson(x, new double[]{5, 4, 3, 2, 1}), 1e-9);
    }

    @Test
    void pearsonIndefinidoComPoucosDadosOuVarianciaZero() {
        assertTrue(Double.isNaN(Estatistica.pearson(new double[]{1, 2}, new double[]{3, 4})));
        assertTrue(Double.isNaN(Estatistica.pearson(new double[]{1, 1, 1}, new double[]{3, 4, 5})));
        assertThrows(IllegalArgumentException.class,
                () -> Estatistica.pearson(new double[]{1, 2, 3}, new double[]{1, 2}));
    }

    @Test
    void regressaoLinear() {
        double[] ab = Estatistica.regressaoLinear(new double[]{0, 1, 2}, new double[]{1, 3, 5});
        assertEquals(1.0, ab[0], 1e-9);
        assertEquals(2.0, ab[1], 1e-9);
    }
}
