package br.unit.eleicao.modelo;

/** Contrato do governo federal com uma empresa de que o candidato é sócio (Portal da Transparência). */
public class ContratoPublico {

    private final String cnpj;
    private final String empresa;
    private final String orgao;
    private final String objeto;
    private final double valor;
    private final String data;

    public ContratoPublico(String cnpj, String empresa, String orgao, String objeto, double valor, String data) {
        this.cnpj = cnpj;
        this.empresa = empresa;
        this.orgao = orgao;
        this.objeto = objeto;
        this.valor = valor;
        this.data = data;
    }

    public String getCnpj() {
        return cnpj;
    }

    public String getEmpresa() {
        return empresa;
    }

    public String getOrgao() {
        return orgao;
    }

    public String getObjeto() {
        return objeto;
    }

    public double getValor() {
        return valor;
    }

    public String getData() {
        return data;
    }
}
