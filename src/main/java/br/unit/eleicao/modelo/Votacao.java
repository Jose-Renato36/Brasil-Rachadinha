package br.unit.eleicao.modelo;

import java.time.LocalDate;

/** Votação nominal no Plenário da Câmara. */
public class Votacao implements Comparable<Votacao> {

    private final String id;
    private final LocalDate data;
    private final String descricao;

    public Votacao(String id, LocalDate data, String descricao) {
        this.id = id;
        this.data = data;
        this.descricao = descricao == null ? "" : descricao;
    }

    public String getId() {
        return id;
    }

    public LocalDate getData() {
        return data;
    }

    public String getDescricao() {
        return descricao;
    }

    public String getUrlCamara() {
        return "https://dadosabertos.camara.leg.br/api/v2/votacoes/" + id;
    }

    /** Ordem cronológica (mais antigas primeiro). */
    @Override
    public int compareTo(Votacao outra) {
        if (data == null || outra.data == null) {
            return id.compareTo(outra.id);
        }
        int c = data.compareTo(outra.data);
        return c != 0 ? c : id.compareTo(outra.id);
    }

    @Override
    public String toString() {
        return id + " - " + descricao;
    }
}
