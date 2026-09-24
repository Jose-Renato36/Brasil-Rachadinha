package br.unit.eleicao.ui;

import br.unit.eleicao.modelo.Candidato;

import java.util.function.Function;

/** Variável numérica disponível para os gráficos de correlação (idade, escolaridade, indicadores...). */
public class VariavelAnalise {

    private final String nome;
    private final Function<Candidato, Double> extrator;

    public VariavelAnalise(String nome, Function<Candidato, Double> extrator) {
        this.nome = nome;
        this.extrator = extrator;
    }

    /** Valor para o candidato, ou null se não houver dado. */
    public Double valor(Candidato c) {
        return extrator.apply(c);
    }

    public String getNome() {
        return nome;
    }

    @Override
    public String toString() {
        return nome;
    }
}
