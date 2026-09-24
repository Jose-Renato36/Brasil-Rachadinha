package br.unit.eleicao.preparo;

/** Como um item do quadro de preparo aparece para o eleitor (cor e ícone na tela). */
public enum Avaliacao {
    POSITIVO("ok"),
    NEUTRO("info"),
    ATENCAO("atencao"),
    NEGATIVO("negativo"),
    SEM_DADOS("sem-dados");

    private final String classe;

    Avaliacao(String classe) {
        this.classe = classe;
    }

    /** Nome curto usado pela interface para escolher cor e ícone. */
    public String getClasse() {
        return classe;
    }
}
