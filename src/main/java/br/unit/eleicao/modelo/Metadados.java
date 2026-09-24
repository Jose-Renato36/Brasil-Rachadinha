package br.unit.eleicao.modelo;

import java.time.LocalDate;

/** Descreve de onde veio a base carregada (UF, anos, se é demonstração). */
public class Metadados {

    private String uf;
    private int anoEleicao;
    private int anoAnterior;
    private LocalDate dataEleicao;
    private boolean demonstracao;
    private String descricao;
    private String geradoEm;
    private boolean tcuVerificado;

    public Metadados(String uf, int anoEleicao, int anoAnterior, LocalDate dataEleicao) {
        this.uf = uf;
        this.anoEleicao = anoEleicao;
        this.anoAnterior = anoAnterior;
        this.dataEleicao = dataEleicao;
    }

    public String getUf() {
        return uf;
    }

    public void setUf(String uf) {
        this.uf = uf;
    }

    public int getAnoEleicao() {
        return anoEleicao;
    }

    public void setAnoEleicao(int anoEleicao) {
        this.anoEleicao = anoEleicao;
    }

    public int getAnoAnterior() {
        return anoAnterior;
    }

    public void setAnoAnterior(int anoAnterior) {
        this.anoAnterior = anoAnterior;
    }

    /** Data usada como referência para calcular idades. */
    public LocalDate getDataEleicao() {
        return dataEleicao;
    }

    public void setDataEleicao(LocalDate dataEleicao) {
        this.dataEleicao = dataEleicao;
    }

    public boolean isDemonstracao() {
        return demonstracao;
    }

    public void setDemonstracao(boolean demonstracao) {
        this.demonstracao = demonstracao;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    /** true quando a lista do TCU foi lida: só então "nada consta" tem significado. */
    public boolean isTcuVerificado() {
        return tcuVerificado;
    }

    public void setTcuVerificado(boolean tcuVerificado) {
        this.tcuVerificado = tcuVerificado;
    }

    public String getGeradoEm() {
        return geradoEm;
    }

    public void setGeradoEm(String geradoEm) {
        this.geradoEm = geradoEm;
    }
}
