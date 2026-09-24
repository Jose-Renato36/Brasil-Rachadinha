package br.unit.eleicao.indicador;

import br.unit.eleicao.modelo.BaseDados;
import br.unit.eleicao.modelo.Candidato;
import br.unit.eleicao.modelo.Deputado;
import br.unit.eleicao.modelo.Despesa;
import br.unit.eleicao.util.Estatistica;
import br.unit.eleicao.util.Texto;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Gasto médio mensal da cota parlamentar (CEAP) em z-score contra os pares da mesma UF.
 * Compara-se dentro da UF porque o teto da cota varia por estado (distância até Brasília).
 */
public class GastoCota extends Indicador {

    public static final String CODIGO = "gasto_cota";

    public GastoCota() {
        super(CODIGO, "Gasto da cota", "Câmara dos Deputados - Cota para Exercício da Atividade Parlamentar",
                "Gasto médio mensal da cota em desvios-padrão em relação aos deputados da mesma UF "
                        + "(0 = na média; positivo = gasta mais que os pares).",
                Sentido.MENOR_MELHOR);
    }

    /** Média mensal = total gasto / número de meses distintos com alguma despesa. */
    public static double mediaMensal(List<Despesa> despesas) {
        double total = 0;
        Set<String> meses = new HashSet<>();
        for (Despesa d : despesas) {
            total += d.getValor();
            meses.add(d.getCompetencia());
        }
        return meses.isEmpty() ? Double.NaN : total / meses.size();
    }

    /**
     * O z-score depende de todos os pares da mesma UF (a cota tem teto diferente em cada estado),
     * por isso este indicador sobrescreve o cálculo em grupo.
     */
    @Override
    public Map<String, ResultadoIndicador> calcularTodos(List<Candidato> candidatos, BaseDados base) {
        Map<Integer, Double> mediasPorDeputado = new HashMap<>();
        Map<String, List<Double>> porUf = new HashMap<>();
        for (Deputado d : base.getDeputados()) {
            List<Despesa> despesas = base.getDespesasDe(d.getId());
            if (!despesas.isEmpty()) {
                double m = mediaMensal(despesas);
                mediasPorDeputado.put(d.getId(), m);
                porUf.computeIfAbsent(String.valueOf(d.getUf()), k -> new ArrayList<>()).add(m);
            }
        }
        Map<String, double[]> estatisticas = new HashMap<>(); // uf -> {media, desvio, n}
        for (Map.Entry<String, List<Double>> e : porUf.entrySet()) {
            double[] valores = e.getValue().stream().mapToDouble(Double::doubleValue).toArray();
            estatisticas.put(e.getKey(), new double[]{Estatistica.media(valores), Estatistica.desvioPadrao(valores),
                valores.length});
        }

        Map<String, ResultadoIndicador> resultado = new LinkedHashMap<>();
        for (Candidato c : candidatos) {
            Deputado d = deputadoDe(c, base);
            if (d == null) {
                resultado.put(c.getSq(), semMandato());
                continue;
            }
            Double mediaDep = mediasPorDeputado.get(d.getId());
            double[] est = estatisticas.get(String.valueOf(d.getUf()));
            if (mediaDep == null) {
                resultado.put(c.getSq(), ResultadoIndicador.semDados("Nenhuma despesa de cota encontrada"));
            } else if (est[2] < 3 || Double.isNaN(est[1]) || est[1] == 0) {
                resultado.put(c.getSq(), ResultadoIndicador.semDados(
                        "Pares insuficientes na UF para comparar (" + (int) est[2] + ")"));
            } else {
                double z = Estatistica.zScore(mediaDep, est[0], est[1]);
                resultado.put(c.getSq(), ResultadoIndicador.com(z, String.format(
                        "Média mensal %s; média dos deputados de %s %s (desvio %s, %d deputados)",
                        Texto.moeda(mediaDep), d.getUf(), Texto.moeda(est[0]), Texto.moeda(est[1]), (int) est[2])));
            }
        }
        return resultado;
    }

    @Override
    public ResultadoIndicador calcular(Candidato candidato, BaseDados base) {
        return calcularTodos(List.of(candidato), base).get(candidato.getSq());
    }

    @Override
    public String getTituloSimples() {
        return "Economia da verba de gabinete";
    }

    @Override
    public String getPergunta() {
        return "Quanto importa que a pessoa gaste menos da cota parlamentar do que os colegas do estado?";
    }

    @Override
    public String resumir(double valor) {
        if (Math.abs(valor) < 0.25) {
            return "Gasta perto da média do estado";
        }
        return (valor > 0 ? "Gasta acima" : "Gasta abaixo") + " da média do estado ("
                + Texto.decimal(Math.abs(valor), 1) + " desvio" + (Math.abs(valor) >= 1.95 ? "s" : "") + ")";
    }

    @Override
    public String formatar(double valor) {
        return "z = " + String.format(Texto.PT_BR, "%+.2f", valor);
    }
}
