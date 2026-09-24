package br.unit.eleicao.indicador;

import br.unit.eleicao.modelo.BaseDados;
import br.unit.eleicao.modelo.Candidato;
import br.unit.eleicao.util.Texto;

/** Variação percentual do patrimônio declarado ao TSE entre a eleição anterior e a atual. */
public class VariacaoPatrimonio extends Indicador {

    public static final String CODIGO = "patrimonio";

    public VariacaoPatrimonio() {
        super(CODIGO, "Variação de patrimônio", "TSE - bens declarados pelos candidatos",
                "Variação % do total de bens declarados entre duas eleições. Dado autodeclarado, "
                        + "a valor histórico (não corrigido pela inflação).",
                Sentido.MENOR_MELHOR);
    }

    @Override
    public ResultadoIndicador calcular(Candidato c, BaseDados base) {
        int anoAtual = base.getMetadados().getAnoEleicao();
        int anoAnterior = base.getMetadados().getAnoAnterior();
        if (c.getPatrimonio() == null) {
            return ResultadoIndicador.semDados("Sem declaração de bens em " + anoAtual);
        }
        if (c.getPatrimonioAnterior() == null) {
            return ResultadoIndicador.semDados("Não foi candidato(a) em " + anoAnterior + " (ou não declarou bens)");
        }
        double anterior = c.getPatrimonioAnterior();
        if (anterior <= 0) {
            return ResultadoIndicador.semDados("Patrimônio declarado em " + anoAnterior
                    + " foi zero: variação percentual indefinida (atual " + Texto.moeda(c.getPatrimonio()) + ")");
        }
        double pct = 100.0 * (c.getPatrimonio() - anterior) / anterior;
        return ResultadoIndicador.com(pct, String.format("%d: %s → %d: %s", anoAnterior, Texto.moeda(anterior),
                anoAtual, Texto.moeda(c.getPatrimonio())));
    }

    @Override
    public String formatar(double valor) {
        return String.format(Texto.PT_BR, "%+.1f%%", valor);
    }
}
