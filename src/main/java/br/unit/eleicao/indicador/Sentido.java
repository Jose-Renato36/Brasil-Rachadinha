package br.unit.eleicao.indicador;

/** Direção de "melhor" usada na normalização. O usuário pode inverter qualquer uma na tela de ranking. */
public enum Sentido {
    MAIOR_MELHOR("maior é melhor"),
    MENOR_MELHOR("menor é melhor");

    private final String descricao;

    Sentido(String descricao) {
        this.descricao = descricao;
    }

    public Sentido inverso() {
        return this == MAIOR_MELHOR ? MENOR_MELHOR : MAIOR_MELHOR;
    }

    @Override
    public String toString() {
        return descricao;
    }
}
