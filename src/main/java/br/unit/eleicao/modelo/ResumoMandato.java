package br.unit.eleicao.modelo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * O que a pessoa fez num mandato legislativo com dados abertos (Câmara ou Senado): presença,
 * projetos apresentados, aprovados e gasto de gabinete. É descritivo: não entra na nota.
 */
public class ResumoMandato {

    private final String casa;
    private final String cargo;
    private final String periodo;
    private final String url;
    private Double presenca;
    private String detalhePresenca = "";
    private int projetos;
    private int aprovados;
    private Double gastoMensal;
    private String observacaoProjetos = "";
    private final List<String> destaques = new ArrayList<>();

    public ResumoMandato(String casa, String cargo, String periodo, String url) {
        this.casa = casa;
        this.cargo = cargo;
        this.periodo = periodo;
        this.url = url == null ? "" : url;
    }

    public void setPresenca(Double presenca, String detalhe) {
        this.presenca = presenca;
        this.detalhePresenca = detalhe == null ? "" : detalhe;
    }

    public void setProjetos(int projetos, int aprovados) {
        this.projetos = projetos;
        this.aprovados = aprovados;
    }

    /** Observação sobre os projetos (ex.: "12 como autor(a) principal"). */
    public void setObservacaoProjetos(String observacao) {
        this.observacaoProjetos = observacao == null ? "" : observacao;
    }

    public String getObservacaoProjetos() {
        return observacaoProjetos;
    }

    public void setGastoMensal(Double gastoMensal) {
        this.gastoMensal = gastoMensal;
    }

    /** Proposições em destaque (aprovadas primeiro), no formato "PL 123/2020 – ementa". */
    public void adicionarDestaque(String texto) {
        if (destaques.size() < 5) {
            destaques.add(texto);
        }
    }

    public String getCasa() {
        return casa;
    }

    public String getCargo() {
        return cargo;
    }

    public String getPeriodo() {
        return periodo;
    }

    public String getUrl() {
        return url;
    }

    public Double getPresenca() {
        return presenca;
    }

    public String getDetalhePresenca() {
        return detalhePresenca;
    }

    public int getProjetos() {
        return projetos;
    }

    public int getAprovados() {
        return aprovados;
    }

    public Double getGastoMensal() {
        return gastoMensal;
    }

    public List<String> getDestaques() {
        return Collections.unmodifiableList(destaques);
    }
}
