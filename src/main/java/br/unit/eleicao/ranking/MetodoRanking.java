package br.unit.eleicao.ranking;

import br.unit.eleicao.indicador.Indicador;
import br.unit.eleicao.modelo.Candidato;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Método de agregação dos indicadores. As subclasses definem só como pontuar;
 * filtragem, normalização e ordenação são comuns (template method).
 */
public abstract class MetodoRanking {

    private final String nome;
    private final String descricao;

    protected MetodoRanking(String nome, String descricao) {
        this.nome = nome;
        this.descricao = descricao;
    }

    /** Pontua os candidatos já filtrados, a partir das notas normalizadas (0 a 1) de cada indicador. */
    protected abstract List<ItemRanking> pontuar(List<Candidato> candidatos, Map<String, Map<String, Double>> notas,
                                                 List<Indicador> ativos, ConfiguracaoRanking config);

    public List<ItemRanking> classificar(MatrizIndicadores matriz, ConfiguracaoRanking config) {
        List<Candidato> candidatos = new ArrayList<>();
        for (Candidato c : matriz.getBase().getCandidatos()) {
            if (!config.isOcultarInaptos() || !c.isInapta()) {
                candidatos.add(c);
            }
        }
        List<Indicador> ativos = new ArrayList<>();
        Map<String, Map<String, Double>> notas = new HashMap<>();
        for (Indicador ind : matriz.getIndicadores()) {
            if (config.getPeso(ind.getCodigo()) > 0) {
                ativos.add(ind);
                notas.put(ind.getCodigo(),
                        Normalizador.minMax(matriz.getResultados(ind.getCodigo()), config.sentidoEfetivo(ind)));
            }
        }
        List<ItemRanking> itens = pontuar(candidatos, notas, ativos, config);
        itens.sort(Comparator.comparing(ItemRanking::getPontuacao, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(i -> i.getCandidato().getNomeExibicao(), String.CASE_INSENSITIVE_ORDER));
        int posicao = 0;
        for (ItemRanking item : itens) {
            item.setPosicao(item.temPontuacao() ? ++posicao : 0);
        }
        return itens;
    }

    /** Notas normalizadas disponíveis de um candidato, só para os indicadores ativos. */
    protected static Map<String, Double> notasDo(Candidato c, Map<String, Map<String, Double>> notas,
                                                 List<Indicador> ativos) {
        Map<String, Double> resultado = new HashMap<>();
        for (Indicador ind : ativos) {
            Double n = notas.get(ind.getCodigo()).get(c.getSq());
            if (n != null) {
                resultado.put(ind.getCodigo(), n);
            }
        }
        return resultado;
    }

    public String getNome() {
        return nome;
    }

    public String getDescricao() {
        return descricao;
    }

    @Override
    public String toString() {
        return nome;
    }
}
