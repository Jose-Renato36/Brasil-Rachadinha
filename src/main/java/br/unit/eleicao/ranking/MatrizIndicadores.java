package br.unit.eleicao.ranking;

import br.unit.eleicao.indicador.Indicador;
import br.unit.eleicao.indicador.ResultadoIndicador;
import br.unit.eleicao.modelo.BaseDados;
import br.unit.eleicao.modelo.Candidato;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Resultados brutos de todos os indicadores para todos os candidatos, calculados uma vez.
 * Mover um slider só refaz a normalização e a soma, não os cálculos: por isso o ranking é instantâneo.
 */
public class MatrizIndicadores {

    private final BaseDados base;
    private final List<Indicador> indicadores;
    /** código do indicador -> (sq do candidato -> resultado). */
    private final Map<String, Map<String, ResultadoIndicador>> resultados = new HashMap<>();

    public MatrizIndicadores(BaseDados base, List<Indicador> indicadores) {
        this.base = base;
        this.indicadores = indicadores;
        for (Indicador ind : indicadores) {
            recalcular(ind);
        }
    }

    public final void recalcular(Indicador indicador) {
        resultados.put(indicador.getCodigo(), indicador.calcularTodos(base.getCandidatos(), base));
    }

    public void recalcular(String codigo) {
        for (Indicador ind : indicadores) {
            if (ind.getCodigo().equals(codigo)) {
                recalcular(ind);
            }
        }
    }

    public ResultadoIndicador get(String codigo, Candidato c) {
        Map<String, ResultadoIndicador> m = resultados.get(codigo);
        return m == null ? null : m.get(c.getSq());
    }

    public Map<String, ResultadoIndicador> getResultados(String codigo) {
        return Collections.unmodifiableMap(resultados.getOrDefault(codigo, Collections.emptyMap()));
    }

    public List<Indicador> getIndicadores() {
        return indicadores;
    }

    public BaseDados getBase() {
        return base;
    }
}
