package br.unit.eleicao.modelo;

/** Total gasto da cota parlamentar (CEAP) por deputado, mês e categoria. */
public class Despesa {

    private final int idDeputado;
    private final int ano;
    private final int mes;
    private final String categoria;
    private final double valor;

    public Despesa(int idDeputado, int ano, int mes, String categoria, double valor) {
        this.idDeputado = idDeputado;
        this.ano = ano;
        this.mes = mes;
        this.categoria = categoria;
        this.valor = valor;
    }

    /** Chave "aaaa-mm" usada para contar meses distintos com gasto. */
    public String getCompetencia() {
        return String.format("%04d-%02d", ano, mes);
    }

    public int getIdDeputado() {
        return idDeputado;
    }

    public int getAno() {
        return ano;
    }

    public int getMes() {
        return mes;
    }

    public String getCategoria() {
        return categoria;
    }

    public double getValor() {
        return valor;
    }
}
