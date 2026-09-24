package br.unit.eleicao.modelo;

/** Vínculo como servidor(a) civil do Poder Executivo federal (cadastro SIAPE, Portal da Transparência). */
public class VinculoServidor {

    private final String cargo;
    private final String orgao;
    private final String situacao;
    private final String ingresso;

    public VinculoServidor(String cargo, String orgao, String situacao, String ingresso) {
        this.cargo = cargo == null ? "" : cargo;
        this.orgao = orgao == null ? "" : orgao;
        this.situacao = situacao == null ? "" : situacao;
        this.ingresso = ingresso == null ? "" : ingresso;
    }

    public String getCargo() {
        return cargo;
    }

    public String getOrgao() {
        return orgao;
    }

    /** Ex.: "ATIVO PERMANENTE", "CEDIDO/REQUISITADO", "APOSENTADO". */
    public String getSituacao() {
        return situacao;
    }

    public String getIngresso() {
        return ingresso;
    }
}
