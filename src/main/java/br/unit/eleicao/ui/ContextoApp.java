package br.unit.eleicao.ui;

import br.unit.eleicao.indicador.AlinhamentoPauta;
import br.unit.eleicao.indicador.CatalogoIndicadores;
import br.unit.eleicao.indicador.Indicador;
import br.unit.eleicao.modelo.BaseDados;
import br.unit.eleicao.ranking.MatrizIndicadores;

import java.util.ArrayList;
import java.util.List;

/** Estado compartilhado entre as abas: base carregada, indicadores e resultados já calculados. */
public class ContextoApp {

    private final List<Indicador> indicadores = CatalogoIndicadores.todos();
    private final List<PainelDados> paineis = new ArrayList<>();
    private BaseDados base;
    private MatrizIndicadores matriz;

    public void registrar(PainelDados painel) {
        paineis.add(painel);
    }

    public void setBase(BaseDados base) {
        this.base = base;
        this.matriz = new MatrizIndicadores(base, indicadores);
        notificar();
    }

    /** Chamado pela aba Pautas: só o alinhamento precisa ser recalculado. */
    public void posicoesAlteradas() {
        matriz.recalcular(AlinhamentoPauta.CODIGO);
        notificar();
    }

    private void notificar() {
        for (PainelDados p : paineis) {
            p.dadosAtualizados(this);
        }
    }

    public boolean temBase() {
        return base != null;
    }

    public BaseDados getBase() {
        return base;
    }

    public MatrizIndicadores getMatriz() {
        return matriz;
    }

    public List<Indicador> getIndicadores() {
        return indicadores;
    }
}
