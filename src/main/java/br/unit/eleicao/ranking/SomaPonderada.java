package br.unit.eleicao.ranking;

import br.unit.eleicao.indicador.Indicador;
import br.unit.eleicao.modelo.Candidato;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Pontuação = soma(peso × nota) / soma(pesos), considerando só os indicadores em que o
 * candidato tem dados. A coluna "cobertura" mostra quantos entraram, e a cobertura mínima
 * configurada decide quem tem dados suficientes para ser pontuado.
 */
public class SomaPonderada extends MetodoRanking {

    public SomaPonderada() {
        super("Soma ponderada", "Média das notas normalizadas, ponderada pelos seus pesos. Indicadores sem dados "
                + "são ignorados e os pesos restantes são redistribuídos.");
    }

    @Override
    protected List<ItemRanking> pontuar(List<Candidato> candidatos, Map<String, Map<String, Double>> notas,
                                        List<Indicador> ativos, ConfiguracaoRanking config) {
        List<ItemRanking> itens = new ArrayList<>();
        for (Candidato c : candidatos) {
            Map<String, Double> minhas = notasDo(c, notas, ativos);
            double somaPesos = 0;
            double soma = 0;
            for (Map.Entry<String, Double> e : minhas.entrySet()) {
                int peso = config.getPeso(e.getKey());
                somaPesos += peso;
                soma += peso * e.getValue();
            }
            boolean coberturaSuficiente = minhas.size() >= Math.min(config.getCoberturaMinima(), ativos.size());
            Double pontuacao = somaPesos == 0 || !coberturaSuficiente ? null : 100.0 * soma / somaPesos;
            itens.add(new ItemRanking(c, pontuacao, minhas, minhas.size(), ativos.size()));
        }
        return itens;
    }
}
