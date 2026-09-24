package br.unit.eleicao.modelo;

import br.unit.eleicao.util.Texto;

import java.time.LocalDate;
import java.util.Objects;

/** Candidatura registrada no TSE para o cargo analisado. */
public class Candidato extends Pessoa {

    private final String sq;
    private String nomeUrna;
    private String numero;
    private String partido;
    private String uf;
    private String cargo;
    private GrauInstrucao grauInstrucao = GrauInstrucao.NAO_INFORMADO;
    private String corRaca;
    private String ocupacao;
    private String situacao;
    private String detalheSituacao;
    private Boolean reeleicao;
    private Double patrimonio;
    private Double patrimonioAnterior;
    private Integer idDeputado;
    private String criterioVinculo;

    public Candidato(String sq, String nome, String nomeUrna, LocalDate dataNascimento, String genero) {
        super(nome, dataNascimento, genero);
        this.sq = Objects.requireNonNull(sq, "sq");
        this.nomeUrna = nomeUrna;
    }

    /** Situação da candidatura usada como filtro (nunca como nota). */
    public Elegibilidade getElegibilidade() {
        return Elegibilidade.de(situacao, detalheSituacao);
    }

    public boolean isInapta() {
        return getElegibilidade() == Elegibilidade.INAPTA;
    }

    public boolean temMandatoNaCamara() {
        return idDeputado != null;
    }

    /** Vínculos feitos só por nome devem ser conferidos manualmente (risco de homônimos). */
    public boolean isVinculoDuvidoso() {
        return criterioVinculo != null && criterioVinculo.contains("CONFERIR");
    }

    @Override
    public String getNomeExibicao() {
        return Texto.vazio(nomeUrna) ? getNome() : nomeUrna;
    }

    /** Dois objetos representam a mesma candidatura se têm o mesmo sequencial do TSE. */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Candidato)) {
            return false;
        }
        return sq.equals(((Candidato) o).sq);
    }

    @Override
    public int hashCode() {
        return sq.hashCode();
    }

    @Override
    public String toString() {
        return getNomeExibicao() + " (" + partido + (Texto.vazio(numero) ? "" : " " + numero) + ")";
    }

    public String getSq() {
        return sq;
    }

    public String getNomeUrna() {
        return nomeUrna;
    }

    public void setNomeUrna(String nomeUrna) {
        this.nomeUrna = nomeUrna;
    }

    public String getNumero() {
        return numero;
    }

    public void setNumero(String numero) {
        this.numero = numero;
    }

    public String getPartido() {
        return partido;
    }

    public void setPartido(String partido) {
        this.partido = partido;
    }

    public String getUf() {
        return uf;
    }

    public void setUf(String uf) {
        this.uf = uf;
    }

    public String getCargo() {
        return cargo;
    }

    public void setCargo(String cargo) {
        this.cargo = cargo;
    }

    public GrauInstrucao getGrauInstrucao() {
        return grauInstrucao;
    }

    public void setGrauInstrucao(GrauInstrucao grauInstrucao) {
        this.grauInstrucao = grauInstrucao == null ? GrauInstrucao.NAO_INFORMADO : grauInstrucao;
    }

    public String getCorRaca() {
        return corRaca;
    }

    public void setCorRaca(String corRaca) {
        this.corRaca = corRaca;
    }

    public String getOcupacao() {
        return ocupacao;
    }

    public void setOcupacao(String ocupacao) {
        this.ocupacao = ocupacao;
    }

    public String getSituacao() {
        return situacao;
    }

    public void setSituacao(String situacao) {
        this.situacao = situacao;
    }

    public String getDetalheSituacao() {
        return detalheSituacao;
    }

    public void setDetalheSituacao(String detalheSituacao) {
        this.detalheSituacao = detalheSituacao;
    }

    /** Se o TSE informa que a pessoa tenta a reeleição (arquivo complementar); null = não informado. */
    public Boolean getReeleicao() {
        return reeleicao;
    }

    public void setReeleicao(Boolean reeleicao) {
        this.reeleicao = reeleicao;
    }

    public Double getPatrimonio() {
        return patrimonio;
    }

    public void setPatrimonio(Double patrimonio) {
        this.patrimonio = patrimonio;
    }

    public Double getPatrimonioAnterior() {
        return patrimonioAnterior;
    }

    public void setPatrimonioAnterior(Double patrimonioAnterior) {
        this.patrimonioAnterior = patrimonioAnterior;
    }

    public Integer getIdDeputado() {
        return idDeputado;
    }

    public String getCriterioVinculo() {
        return criterioVinculo;
    }

    /** Liga a candidatura ao cadastro de deputado da Câmara, registrando como o vínculo foi feito. */
    public void vincularDeputado(Integer idDeputado, String criterio) {
        this.idDeputado = idDeputado;
        this.criterioVinculo = idDeputado == null ? null : criterio;
    }
}
