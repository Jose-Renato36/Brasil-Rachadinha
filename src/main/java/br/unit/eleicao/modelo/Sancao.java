package br.unit.eleicao.modelo;

/**
 * Sanção administrativa registrada nos cadastros da CGU: CEIS (inidôneos e suspensos, impedidos de
 * contratar com o poder público) e CNEP (punidos pela Lei Anticorrupção). Pode recair sobre a própria
 * pessoa ou sobre uma empresa da qual é sócia.
 */
public class Sancao {

    private final String cadastro;
    private final String sancionado;
    private final String categoria;
    private final String orgao;
    private final String inicio;
    private final String fim;
    private final boolean sobreEmpresa;

    public Sancao(String cadastro, String sancionado, String categoria, String orgao, String inicio, String fim,
                  boolean sobreEmpresa) {
        this.cadastro = cadastro;
        this.sancionado = sancionado;
        this.categoria = categoria;
        this.orgao = orgao;
        this.inicio = inicio;
        this.fim = fim;
        this.sobreEmpresa = sobreEmpresa;
    }

    public String getCadastro() {
        return cadastro;
    }

    /** Nome da pessoa ou empresa punida, como publicado. */
    public String getSancionado() {
        return sancionado;
    }

    public String getCategoria() {
        return categoria;
    }

    public String getOrgao() {
        return orgao;
    }

    public String getInicio() {
        return inicio;
    }

    public String getFim() {
        return fim;
    }

    public boolean isSobreEmpresa() {
        return sobreEmpresa;
    }
}
