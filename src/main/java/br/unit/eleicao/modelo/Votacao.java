package br.unit.eleicao.modelo;

import br.unit.eleicao.util.Texto;

import java.time.LocalDate;

/** Votação nominal no Plenário da Câmara. */
public class Votacao implements Comparable<Votacao> {

    private final String id;
    private final LocalDate data;
    private final String descricao;
    private String proposicao = "";
    private String ementa = "";

    public Votacao(String id, LocalDate data, String descricao) {
        this.id = id;
        this.data = data;
        this.descricao = descricao == null ? "" : descricao;
    }

    /**
     * Votação "principal": decide o texto do projeto (texto-base, projeto, proposta ou medida inteira),
     * em vez de um destaque, requerimento ou emenda isolada. Heurística pela descrição, usada só para
     * sugerir as votações mais fáceis de entender na tela de pautas.
     */
    public boolean isPrincipal() {
        String d = Texto.normalizar(descricao);
        if (d.contains("REQUERIMENTO") || d.contains("DESTAQUE") || d.contains("EMENDA")) {
            return d.contains("TEXTO BASE");
        }
        return d.contains("TEXTO BASE") || d.contains("O PROJETO") || d.contains("A PROPOSTA")
                || d.contains("A MEDIDA PROVISORIA") || d.contains("O SUBSTITUTIVO") || d.contains("REDACAO FINAL");
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

    /** Identificação da proposição votada (ex.: "PL 1087/2025"), quando conhecida. */
    public String getProposicao() {
        return proposicao;
    }

    public String getEmenta() {
        return ementa;
    }

    public void setProposicao(String proposicao, String ementa) {
        this.proposicao = proposicao == null ? "" : proposicao;
        this.ementa = ementa == null ? "" : ementa;
    }

    /** Título legível: proposição + ementa quando existem, senão a descrição oficial. */
    public String getTitulo() {
        if (!proposicao.isEmpty()) {
            return proposicao + (ementa.isEmpty() ? "" : " - " + ementa);
        }
        return descricao.isEmpty() ? id : descricao;
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
        return id + " - " + getTitulo();
    }
}
