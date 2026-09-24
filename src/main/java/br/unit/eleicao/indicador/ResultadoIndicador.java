package br.unit.eleicao.indicador;

/**
 * Valor de um indicador para um candidato. "Sem dados" é um estado próprio (valor null),
 * nunca zero: zero significaria desempenho ruim, e ausência de dado não é desempenho.
 */
public final class ResultadoIndicador {

    private final Double valor;
    private final String detalhe;

    private ResultadoIndicador(Double valor, String detalhe) {
        this.valor = valor;
        this.detalhe = detalhe;
    }

    public static ResultadoIndicador com(double valor, String detalhe) {
        if (Double.isNaN(valor) || Double.isInfinite(valor)) {
            return semDados("valor indefinido");
        }
        return new ResultadoIndicador(valor, detalhe);
    }

    public static ResultadoIndicador semDados(String motivo) {
        return new ResultadoIndicador(null, motivo);
    }

    public boolean temDados() {
        return valor != null;
    }

    /** Valor bruto; só deve ser chamado se {@link #temDados()} for verdadeiro. */
    public double getValor() {
        if (valor == null) {
            throw new IllegalStateException("Indicador sem dados: " + detalhe);
        }
        return valor;
    }

    public String getDetalhe() {
        return detalhe;
    }
}
