package br.unit.eleicao.util;

/**
 * Estatística descritiva implementada à mão (sem bibliotecas externas),
 * para que cada fórmula usada no relatório possa ser conferida.
 */
public final class Estatistica {

    private Estatistica() {
    }

    public static double media(double[] valores) {
        if (valores.length == 0) {
            return Double.NaN;
        }
        double soma = 0;
        for (double v : valores) {
            soma += v;
        }
        return soma / valores.length;
    }

    /** Desvio-padrão amostral (divide por n - 1). */
    public static double desvioPadrao(double[] valores) {
        if (valores.length < 2) {
            return Double.NaN;
        }
        double m = media(valores);
        double soma = 0;
        for (double v : valores) {
            soma += (v - m) * (v - m);
        }
        return Math.sqrt(soma / (valores.length - 1));
    }

    /** Quantos desvios-padrão o valor está acima (+) ou abaixo (-) da média. */
    public static double zScore(double valor, double media, double desvio) {
        if (Double.isNaN(desvio) || desvio == 0) {
            return Double.NaN;
        }
        return (valor - media) / desvio;
    }

    /** Coeficiente de correlação de Pearson; NaN se houver menos de 3 pares ou variância zero. */
    public static double pearson(double[] x, double[] y) {
        if (x.length != y.length) {
            throw new IllegalArgumentException("Vetores com tamanhos diferentes: " + x.length + " e " + y.length);
        }
        int n = x.length;
        if (n < 3) {
            return Double.NaN;
        }
        double mx = media(x);
        double my = media(y);
        double sxy = 0;
        double sxx = 0;
        double syy = 0;
        for (int i = 0; i < n; i++) {
            sxy += (x[i] - mx) * (y[i] - my);
            sxx += (x[i] - mx) * (x[i] - mx);
            syy += (y[i] - my) * (y[i] - my);
        }
        if (sxx == 0 || syy == 0) {
            return Double.NaN;
        }
        return sxy / Math.sqrt(sxx * syy);
    }

    /** Coeficientes {a, b} da reta de mínimos quadrados y = a + b·x. */
    public static double[] regressaoLinear(double[] x, double[] y) {
        double mx = media(x);
        double my = media(y);
        double sxy = 0;
        double sxx = 0;
        for (int i = 0; i < x.length; i++) {
            sxy += (x[i] - mx) * (y[i] - my);
            sxx += (x[i] - mx) * (x[i] - mx);
        }
        double b = sxx == 0 ? 0 : sxy / sxx;
        return new double[]{my - b * mx, b};
    }

    public static double minimo(double[] valores) {
        double min = Double.POSITIVE_INFINITY;
        for (double v : valores) {
            min = Math.min(min, v);
        }
        return min;
    }

    public static double maximo(double[] valores) {
        double max = Double.NEGATIVE_INFINITY;
        for (double v : valores) {
            max = Math.max(max, v);
        }
        return max;
    }

    /** Leitura em linguagem simples da força de uma correlação (convenção usual em ciências sociais). */
    public static String interpretarCorrelacao(double r) {
        if (Double.isNaN(r)) {
            return "indefinida (dados insuficientes)";
        }
        double a = Math.abs(r);
        String forca = a < 0.1 ? "desprezível" : a < 0.3 ? "fraca" : a < 0.5 ? "moderada" : a < 0.7 ? "forte"
                : "muito forte";
        if (a < 0.1) {
            return forca;
        }
        return forca + (r > 0 ? " e positiva" : " e negativa");
    }
}
