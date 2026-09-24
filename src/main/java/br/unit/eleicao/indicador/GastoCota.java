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

    /** O z-score depende de todos os pares, por isso este indicador sobrescreve o cálculo em grupo. */
    @Override
    public Map<String, ResultadoIndicador> calcularTodos(List<Candidato> candidatos, BaseDados base) {
        Map<Integer, Double> mediasPorDeputado = new HashMap<>();
        for (Deputado d : base.getDeputados()) {
            List<Despesa> despesas = base.getDespesasDe(d.getId());
            if (!despesas.isEmpty()) {
                mediasPorDeputado.put(d.getId(), mediaMensal(despesas));
            }
        }
        List<Double> lista = new ArrayList<>(mediasPorDeputado.values());
        double[] valores = new double[lista.size()];
        for (int i = 0; i < valores.length; i++) {
            valores[i] = lista.get(i);
        }
        double media = Estatistica.media(valores);
        double desvio = Estatistica.desvioPadrao(valores);

        Map<String, ResultadoIndicador> resultado = new LinkedHashMap<>();
        for (Candidato c : candidatos) {
            Deputado d = deputadoDe(c, base);
            if (d == null) {
                resultado.put(c.getSq(), semMandato());
                continue;
            }
            Double mediaDep = mediasPorDeputado.get(d.getId());
            if (mediaDep == null) {
                resultado.put(c.getSq(), ResultadoIndicador.semDados("Nenhuma despesa de cota encontrada"));
            } else if (valores.length < 3 || Double.isNaN(desvio) || desvio == 0) {
                resultado.put(c.getSq(), ResultadoIndicador.semDados(
                        "Pares insuficientes na UF para comparar (" + valores.length + ")"));
            } else {
                double z = Estatistica.zScore(mediaDep, media, desvio);
                resultado.put(c.getSq(), ResultadoIndicador.com(z, String.format(
                        "Média mensal %s; média da UF %s (desvio %s, %d deputados)",
                        Texto.moeda(mediaDep), Texto.moeda(media), Texto.moeda(desvio), valores.length)));
            }
        }
        return resultado;
    }

    @Override
    public ResultadoIndicador calcular(Candidato candidato, BaseDados base) {
        return calcularTodos(List.of(candidato), base).get(candidato.getSq());
    }

    @Override
    public String formatar(double valor) {
        return "z = " + String.format(Texto.PT_BR, "%+.2f", valor);
    }
}
