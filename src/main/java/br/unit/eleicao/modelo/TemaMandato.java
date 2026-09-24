package br.unit.eleicao.modelo;

/** Assuntos em que os indicadores de um mandato no Executivo são agrupados na tela. */
public enum TemaMandato {
    ECONOMIA("Economia"),
    EMPREGO("Emprego"),
    EDUCACAO("Educação"),
    CONTAS("Contas públicas"),
    LEIS("Projetos e leis");

    private final String rotulo;

    TemaMandato(String rotulo) {
        this.rotulo = rotulo;
    }

    public String getRotulo() {
        return rotulo;
    }
}
