package br.unit.eleicao.modelo;

/** Empresa em que a pessoa aparece no quadro de sócios (Cadastro Nacional da Pessoa Jurídica, Receita Federal). */
public class VinculoEmpresa {

    private final String cnpj;
    private final String razaoSocial;
    private final String qualificacao;
    private final String dataEntrada;

    public VinculoEmpresa(String cnpj, String razaoSocial, String qualificacao, String dataEntrada) {
        this.cnpj = cnpj == null ? "" : cnpj;
        this.razaoSocial = razaoSocial == null ? "" : razaoSocial;
        this.qualificacao = qualificacao == null ? "" : qualificacao;
        this.dataEntrada = dataEntrada == null ? "" : dataEntrada;
    }

    /** Os 8 primeiros dígitos do CNPJ (identificam a empresa, independentemente da filial). */
    public String getCnpj() {
        return cnpj;
    }

    public String getRazaoSocial() {
        return razaoSocial;
    }

    /** Papel na empresa (ex.: "Sócio-Administrador"). */
    public String getQualificacao() {
        return qualificacao;
    }

    public String getDataEntrada() {
        return dataEntrada;
    }
}
