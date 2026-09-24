package br.unit.eleicao.modelo;

/** Valor de um indicador num ano, com o valor de referência (estado ou Brasil) no mesmo ano, se houver. */
public class PontoSerie {

    private final int ano;
    private final double valor;
    private final Double referencia;

    public PontoSerie(int ano, double valor, Double referencia) {
        this.ano = ano;
        this.valor = valor;
        this.referencia = referencia;
    }

    public int getAno() {
        return ano;
    }

    public double getValor() {
        return valor;
    }

    public Double getReferencia() {
        return referencia;
    }
}
