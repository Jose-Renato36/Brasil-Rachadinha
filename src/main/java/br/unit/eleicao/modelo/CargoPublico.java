package br.unit.eleicao.modelo;

/**
 * Função pública de destaque que a pessoa exerce ou exerceu nos últimos 5 anos (ministro, secretário
 * de estado, dirigente de estatal, cargo de confiança de alto escalão etc.), pela lista de Pessoas
 * Expostas Politicamente publicada pela CGU.
 */
public class CargoPublico {

    private final String funcao;
    private final String orgao;
    private final String inicio;
    private final String fim;

    public CargoPublico(String funcao, String orgao, String inicio, String fim) {
        this.funcao = funcao == null ? "" : funcao;
        this.orgao = orgao == null ? "" : orgao;
        this.inicio = inicio == null ? "" : inicio;
        this.fim = fim == null ? "" : fim;
    }

    public String getFuncao() {
        return funcao;
    }

    public String getOrgao() {
        return orgao;
    }

    public String getInicio() {
        return inicio;
    }

    /** Vazio quando a pessoa ainda está na função. */
    public String getFim() {
        return fim;
    }
}
