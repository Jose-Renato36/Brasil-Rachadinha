package br.unit.eleicao.modelo;

/** Como resumir uma série de indicador no período do mandato. */
public enum FormaResumo {
    /** Crescimento em % entre o último ano antes da posse e o último ano com dados (PIB, empregos). */
    VARIACAO_PERCENTUAL,
    /** Diferença em pontos (taxas em %, notas como o IDEB). */
    VARIACAO_PONTOS,
    /** Média dos anos do mandato (taxas anuais, como crescimento do PIB e inflação). */
    MEDIA,
    /** Soma dos anos do mandato (contagens, como medidas provisórias). */
    SOMA
}
