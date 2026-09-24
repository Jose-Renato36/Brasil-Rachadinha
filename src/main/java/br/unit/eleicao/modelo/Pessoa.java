package br.unit.eleicao.modelo;

import java.time.LocalDate;
import java.time.Period;

/** Superclasse com o que candidatos e deputados têm em comum. */
public abstract class Pessoa {

    private String nome;
    private LocalDate dataNascimento;
    private String genero;

    protected Pessoa(String nome, LocalDate dataNascimento, String genero) {
        this.nome = nome;
        this.dataNascimento = dataNascimento;
        this.genero = genero;
    }

    /** Idade completa na data de referência, ou null se a data de nascimento não for conhecida. */
    public Integer getIdade(LocalDate referencia) {
        if (dataNascimento == null || referencia == null) {
            return null;
        }
        return Period.between(dataNascimento, referencia).getYears();
    }

    /** Nome curto usado nas telas; cada subclasse decide qual nome exibir. */
    public abstract String getNomeExibicao();

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public LocalDate getDataNascimento() {
        return dataNascimento;
    }

    public void setDataNascimento(LocalDate dataNascimento) {
        this.dataNascimento = dataNascimento;
    }

    public String getGenero() {
        return genero;
    }

    public void setGenero(String genero) {
        this.genero = genero;
    }

    @Override
    public String toString() {
        return getNomeExibicao();
    }
}
