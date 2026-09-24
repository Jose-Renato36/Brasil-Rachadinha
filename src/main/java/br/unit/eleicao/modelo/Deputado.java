package br.unit.eleicao.modelo;

import br.unit.eleicao.util.Texto;

import java.time.LocalDate;

/** Parlamentar com mandato na Câmara na legislatura analisada. */
public class Deputado extends Pessoa {

    private final int id;
    private String nomeParlamentar;
    private String partido;
    private String uf;

    public Deputado(int id, String nomeParlamentar, String nomeCivil, LocalDate dataNascimento, String genero) {
        super(nomeCivil, dataNascimento, genero);
        this.id = id;
        this.nomeParlamentar = nomeParlamentar;
    }

    @Override
    public String getNomeExibicao() {
        return Texto.vazio(nomeParlamentar) ? getNome() : nomeParlamentar;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Deputado && ((Deputado) o).id == id;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }

    public String getUrlCamara() {
        return "https://www.camara.leg.br/deputados/" + id;
    }

    public int getId() {
        return id;
    }

    public String getNomeParlamentar() {
        return nomeParlamentar;
    }

    public void setNomeParlamentar(String nomeParlamentar) {
        this.nomeParlamentar = nomeParlamentar;
    }

    public String getPartido() {
        return partido;
    }

    public void setPartido(String partido) {
        this.partido = partido;
    }

    public String getUf() {
        return uf;
    }

    public void setUf(String uf) {
        this.uf = uf;
    }
}
