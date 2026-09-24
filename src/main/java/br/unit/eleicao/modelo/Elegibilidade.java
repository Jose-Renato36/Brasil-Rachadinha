package br.unit.eleicao.modelo;

import br.unit.eleicao.util.Texto;

/**
 * Situação da candidatura em linguagem simples. É usada como filtro, nunca como nota.
 * Deriva de DS_SITUACAO_CANDIDATURA (APTO/INAPTO/CADASTRADO) e DS_DETALHE_SITUACAO_CAND
 * (DEFERIDO, INDEFERIDO COM RECURSO, RENÚNCIA...).
 */
public enum Elegibilidade {
    APTA("Apta", "Candidatura aceita pela Justiça Eleitoral."),
    EM_ANALISE("Em análise", "A Justiça Eleitoral ainda não julgou o registro."),
    SUB_JUDICE("Com recurso", "Registro negado, mas há recurso: o nome continua na urna até a decisão final."),
    INAPTA("Inapta", "Registro negado, cancelado ou com renúncia: votos não serão válidos.");

    private static final String[] PALAVRAS_INAPTA = {
        "INDEFERIDO", "CANCELADO", "CASSADO", "RENUNCIA", "FALECIDO", "NAO CONHECIMENTO", "INAPTO"
    };

    private final String rotulo;
    private final String explicacao;

    Elegibilidade(String rotulo, String explicacao) {
        this.rotulo = rotulo;
        this.explicacao = explicacao;
    }

    public static Elegibilidade de(String situacao, String detalhe) {
        String sit = Texto.normalizar(situacao);
        String det = Texto.normalizar(detalhe);
        boolean negativa = false;
        for (String palavra : PALAVRAS_INAPTA) {
            if (det.contains(palavra) || sit.contains(palavra)) {
                negativa = true;
            }
        }
        // decisão negativa com recurso pendente: o nome continua na urna até o julgamento final
        if (negativa && det.contains("COM RECURSO") || sit.contains("SUB JUDICE")) {
            return SUB_JUDICE;
        }
        if (negativa) {
            return INAPTA;
        }
        // "DEFERIDO COM RECURSO" = registro aceito, com recurso de terceiros: continua apta
        if (det.contains("DEFERIDO") || sit.equals("APTO") || sit.startsWith("APTO ")) {
            return APTA;
        }
        return EM_ANALISE;
    }

    public String getRotulo() {
        return rotulo;
    }

    public String getExplicacao() {
        return explicacao;
    }
}
