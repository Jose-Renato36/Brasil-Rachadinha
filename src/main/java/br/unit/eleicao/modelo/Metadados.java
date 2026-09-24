package br.unit.eleicao.modelo;

import java.time.LocalDate;

/** Descreve de onde veio a base carregada (UF, anos, se é demonstração). */
public class Metadados {

    public static final String FONTE_CASSACAO = "cassacao";
    public static final String FONTE_BENS_ANTERIORES = "bens_anteriores";
    public static final String FONTE_RECEITAS = "receitas";
    public static final String FONTE_EMENDAS = "emendas";
    public static final String FONTE_EMPRESAS = "empresas";
    public static final String FONTE_SANCOES = "sancoes";
    public static final String FONTE_SIAPE = "siape";
    public static final String FONTE_MANDATOS = "mandatos";
    public static final String FONTE_EXPULSOES = "ceaf";
    public static final String FONTE_CARGOS_PUBLICOS = "pep";
    public static final String FONTE_CONTRATOS = "contratos";
    public static final String FONTE_PLANOS = "planos";
    public static final String FONTE_DESPESAS = "despesas";
    public static final String FONTE_VOTOS = "votos";

    private String uf;
    private int anoEleicao;
    private int anoAnterior;
    private LocalDate dataEleicao;
    private boolean demonstracao;
    private String descricao;
    private String geradoEm;
    private boolean tcuVerificado;
    private final java.util.Set<String> fontesVerificadas = new java.util.TreeSet<>();

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

    /** Fontes opcionais que foram de fato lidas nesta coleta (ex.: "sancoes", "siape"). */
    public java.util.Set<String> getFontesVerificadas() {
        return java.util.Collections.unmodifiableSet(fontesVerificadas);
    }

    public void marcarVerificada(String fonte) {
        fontesVerificadas.add(fonte);
    }

    /** true se a fonte foi consultada; permite dizer "não consta" em vez de "sem dados". */
    public boolean isVerificada(String fonte) {
        return fontesVerificadas.contains(fonte) || ("tcu".equals(fonte) && tcuVerificado);
    }

    public String getGeradoEm() {
        return geradoEm;
    }

    public void setGeradoEm(String geradoEm) {
        this.geradoEm = geradoEm;
    }
}
