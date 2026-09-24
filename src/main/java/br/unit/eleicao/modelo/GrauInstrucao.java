package br.unit.eleicao.modelo;

import br.unit.eleicao.util.Texto;

/** Escolaridade declarada ao TSE, em ordem crescente (o nível serve como variável ordinal nas correlações). */
public enum GrauInstrucao {
    NAO_INFORMADO(-1, "Não informado"),
    LE_ESCREVE(0, "Lê e escreve"),
    FUNDAMENTAL_INCOMPLETO(1, "Fundamental incompleto"),
    FUNDAMENTAL_COMPLETO(2, "Fundamental completo"),
    MEDIO_INCOMPLETO(3, "Médio incompleto"),
    MEDIO_COMPLETO(4, "Médio completo"),
    SUPERIOR_INCOMPLETO(5, "Superior incompleto"),
    SUPERIOR_COMPLETO(6, "Superior completo");

    private final int nivel;
    private final String descricao;

    GrauInstrucao(int nivel, String descricao) {
        this.nivel = nivel;
        this.descricao = descricao;
    }

    public int getNivel() {
        return nivel;
    }

    public String getDescricao() {
        return descricao;
    }

    /** Converte o texto do TSE (ex.: "ENSINO MÉDIO COMPLETO") ou o nome da constante. */
    public static GrauInstrucao deTexto(String texto) {
        String t = Texto.normalizar(texto).replace(' ', '_');
        for (GrauInstrucao g : values()) {
            if (g.name().equals(t)) {
                return g;
            }
        }
        t = t.replace('_', ' ');
        boolean incompleto = t.contains("INCOMPLETO");
        if (t.contains("SUPERIOR")) {
            return incompleto ? SUPERIOR_INCOMPLETO : SUPERIOR_COMPLETO;
        }
        if (t.contains("MEDIO")) {
            return incompleto ? MEDIO_INCOMPLETO : MEDIO_COMPLETO;
        }
        if (t.contains("FUNDAMENTAL")) {
            return incompleto ? FUNDAMENTAL_INCOMPLETO : FUNDAMENTAL_COMPLETO;
        }
        if (t.contains("LE E ESCREVE")) {
            return LE_ESCREVE;
        }
        return NAO_INFORMADO;
    }

    @Override
    public String toString() {
        return descricao;
    }
}
