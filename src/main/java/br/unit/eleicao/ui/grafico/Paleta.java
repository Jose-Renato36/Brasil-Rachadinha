package br.unit.eleicao.ui.grafico;

import java.awt.Color;

/** Cores dos gráficos: ordem categórica fixa (validada para daltonismo) e tons neutros para texto e grade. */
public final class Paleta {

    public static final Color FUNDO = new Color(0xFCFCFB);
    public static final Color TEXTO = new Color(0x0B0B0B);
    public static final Color TEXTO_SECUNDARIO = new Color(0x52514E);
    public static final Color GRADE = new Color(0xE4E3DF);
    public static final Color EIXO = new Color(0xA8A7A1);

    /** Séries em ordem fixa: a cor segue a entidade, nunca a posição no ranking. */
    private static final Color[] SERIES = {
        new Color(0x2A78D6), new Color(0xEB6834), new Color(0x1BAF7A), new Color(0xEDA100),
        new Color(0xE87BA4), new Color(0x008300), new Color(0x4A3AA7), new Color(0xE34948)
    };

    private Paleta() {
    }

    public static Color serie(int indice) {
        return SERIES[indice % SERIES.length];
    }
}
