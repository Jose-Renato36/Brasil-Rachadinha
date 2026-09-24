package br.unit.eleicao.ranking;

import br.unit.eleicao.indicador.Indicador;
import br.unit.eleicao.modelo.Candidato;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * TOPSIS: pontua pela proximidade relativa à solução ideal (melhor valor em tudo) e distância
 * da anti-ideal (pior valor em tudo). Como exige todos os critérios, só pontua quem tem dados
 * em todos os indicadores com peso maior que zero.
 */
public class Topsis extends MetodoRanking {

    public Topsis() {
        super("TOPSIS", "Proximidade à solução ideal. Só classifica quem tem dados em todos os indicadores "
                + "com peso maior que zero.");
    }

    @Override
    protected List<ItemRanking> pontuar(List<Candidato> candidatos, Map<String, Map<String, Double>> notas,
                                        List<Indicador> ativos, ConfiguracaoRanking config) {
        double somaPesos = 0;
        for (Indicador ind : ativos) {
            somaPesos += config.getPeso(ind.getCodigo());
        }
        // Com notas min-max já orientadas, o ideal é o maior valor ponderado e o anti-ideal o menor
        double[] ideal = new double[ativos.size()];
        double[] antiIdeal = new double[ativos.size()];
        java.util.Arrays.fill(ideal, Double.NEGATIVE_INFINITY);
        java.util.Arrays.fill(antiIdeal, Double.POSITIVE_INFINITY);
        List<Candidato> completos = new ArrayList<>();
        for (Candidato c : candidatos) {
            if (notasDo(c, notas, ativos).size() == ativos.size() && !ativos.isEmpty()) {
                completos.add(c);
                for (int j = 0; j < ativos.size(); j++) {
                    double v = ponderado(c, ativos.get(j), notas, config, somaPesos);
                    ideal[j] = Math.max(ideal[j], v);
                    antiIdeal[j] = Math.min(antiIdeal[j], v);
                }
            }
        }
        List<ItemRanking> itens = new ArrayList<>();
        for (Candidato c : candidatos) {
            Map<String, Double> minhas = notasDo(c, notas, ativos);
            Double pontuacao = null;
            if (completos.contains(c)) {
                double dMais = 0;
                double dMenos = 0;
                for (int j = 0; j < ativos.size(); j++) {
                    double v = ponderado(c, ativos.get(j), notas, config, somaPesos);
                    dMais += (v - ideal[j]) * (v - ideal[j]);
                    dMenos += (v - antiIdeal[j]) * (v - antiIdeal[j]);
                }
                dMais = Math.sqrt(dMais);
                dMenos = Math.sqrt(dMenos);
                pontuacao = dMais + dMenos == 0 ? 50.0 : 100.0 * dMenos / (dMais + dMenos);
            }
            itens.add(new ItemRanking(c, pontuacao, minhas, minhas.size(), ativos.size()));
        }
        return itens;
    }

    private static double ponderado(Candidato c, Indicador ind, Map<String, Map<String, Double>> notas,
                                    ConfiguracaoRanking config, double somaPesos) {
        return config.getPeso(ind.getCodigo()) / somaPesos * notas.get(ind.getCodigo()).get(c.getSq());
    }
}
