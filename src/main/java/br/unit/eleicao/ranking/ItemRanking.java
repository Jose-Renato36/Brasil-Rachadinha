package br.unit.eleicao.ranking;

import br.unit.eleicao.modelo.Candidato;

import java.util.Collections;
import java.util.Map;

/** Uma linha do ranking: candidato, pontuação (0 a 100) e as notas normalizadas usadas. */
public class ItemRanking {

    private final Candidato candidato;
    private final Double pontuacao;
    private final Map<String, Double> notas;
    private final int indicadoresUsados;
    private final int indicadoresPedidos;
    private int posicao;

    public ItemRanking(Candidato candidato, Double pontuacao, Map<String, Double> notas, int indicadoresUsados,
                       int indicadoresPedidos) {
        this.candidato = candidato;
        this.pontuacao = pontuacao;
        this.notas = notas;
        this.indicadoresUsados = indicadoresUsados;
        this.indicadoresPedidos = indicadoresPedidos;
    }

    public boolean temPontuacao() {
        return pontuacao != null;
    }

    public Candidato getCandidato() {
        return candidato;
    }

    public Double getPontuacao() {
        return pontuacao;
    }

    /** Nota normalizada (0 a 1) de um indicador, ou null se o candidato não tem dados nele. */
    public Double getNota(String codigo) {
        return notas.get(codigo);
    }

    public Map<String, Double> getNotas() {
        return Collections.unmodifiableMap(notas);
    }

    public int getIndicadoresUsados() {
        return indicadoresUsados;
    }

    public int getIndicadoresPedidos() {
        return indicadoresPedidos;
    }

    public String getCobertura() {
        return indicadoresUsados + "/" + indicadoresPedidos;
    }

    public int getPosicao() {
        return posicao;
    }

    void setPosicao(int posicao) {
        this.posicao = posicao;
    }
}
