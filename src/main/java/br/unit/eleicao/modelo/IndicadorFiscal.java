package br.unit.eleicao.modelo;

/**
 * Gasto com pessoal de uma prefeitura ou governo estadual em um ano, em % da receita corrente líquida,
 * com o limite máximo da Lei de Responsabilidade Fiscal (Relatório de Gestão Fiscal enviado ao Tesouro/SICONFI).
 * Mostra o antes e o durante de um mandato no Executivo: não mede a qualidade da gestão sozinho.
 */
public class IndicadorFiscal implements Comparable<IndicadorFiscal> {

    private final String ente;
    private final int ano;
    private final double pessoalRcl;
    private final Double limite;
    private final boolean duranteMandato;

    public IndicadorFiscal(String ente, int ano, double pessoalRcl, Double limite, boolean duranteMandato) {
        this.ente = ente;
        this.ano = ano;
        this.pessoalRcl = pessoalRcl;
        this.limite = limite;
        this.duranteMandato = duranteMandato;
    }

    public boolean isAcimaDoLimite() {
        return limite != null && pessoalRcl > limite;
    }

    public String getEnte() {
        return ente;
    }

    public int getAno() {
        return ano;
    }

    public double getPessoalRcl() {
        return pessoalRcl;
    }

    public Double getLimite() {
        return limite;
    }

    public boolean isDuranteMandato() {
        return duranteMandato;
    }

    @Override
    public int compareTo(IndicadorFiscal o) {
        int c = ente.compareTo(o.ente);
        return c != 0 ? c : Integer.compare(ano, o.ano);
    }
}
