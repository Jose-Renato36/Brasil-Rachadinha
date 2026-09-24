package br.unit.eleicao.ranking;

import br.unit.eleicao.indicador.ResultadoIndicador;
import br.unit.eleicao.indicador.Sentido;

import java.util.HashMap;
import java.util.Map;

/** Normalização min-max: leva cada indicador para a escala 0 (pior) a 1 (melhor) entre os candidatos com dados. */
public final class Normalizador {

    /** Valor usado quando todos os candidatos têm o mesmo valor (nenhum se distingue). */
    public static final double NEUTRO = 0.5;

    private Normalizador() {
    }

    public static Map<String, Double> minMax(Map<String, ResultadoIndicador> resultados, Sentido sentido) {
        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;
        for (ResultadoIndicador r : resultados.values()) {
            if (r.temDados()) {
                min = Math.min(min, r.getValor());
                max = Math.max(max, r.getValor());
            }
        }
        Map<String, Double> notas = new HashMap<>();
        for (Map.Entry<String, ResultadoIndicador> e : resultados.entrySet()) {
            ResultadoIndicador r = e.getValue();
            if (!r.temDados()) {
                continue;
            }
            double nota = max == min ? NEUTRO : (r.getValor() - min) / (max - min);
            notas.put(e.getKey(), sentido == Sentido.MAIOR_MELHOR ? nota : 1 - nota);
        }
        return notas;
    }
}
