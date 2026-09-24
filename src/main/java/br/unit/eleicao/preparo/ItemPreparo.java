package br.unit.eleicao.preparo;

/** Resultado de um critério para um candidato: uma linha do quadro "Preparo para o cargo". */
public class ItemPreparo {

    private final String grupo;
    private final String titulo;
    private final Avaliacao avaliacao;
    private final String resumo;
    private final String detalhe;
    private final String fonte;

    public ItemPreparo(String grupo, String titulo, Avaliacao avaliacao, String resumo, String detalhe, String fonte) {
        this.grupo = grupo;
        this.titulo = titulo;
        this.avaliacao = avaliacao;
        this.resumo = resumo;
        this.detalhe = detalhe == null ? "" : detalhe;
        this.fonte = fonte;
    }

    public String getGrupo() {
        return grupo;
    }

    public String getTitulo() {
        return titulo;
    }

    public Avaliacao getAvaliacao() {
        return avaliacao;
    }

    /** Frase curta, ex.: "47 anos na posse (mínimo: 35)". */
    public String getResumo() {
        return resumo;
    }

    /** Explicação do critério e de suas limitações. */
    public String getDetalhe() {
        return detalhe;
    }

    public String getFonte() {
        return fonte;
    }
}
