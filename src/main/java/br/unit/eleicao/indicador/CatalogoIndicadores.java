package br.unit.eleicao.indicador;

import java.util.List;

/** Lista dos indicadores do MVP. A elegibilidade não está aqui: ela é filtro, não nota. */
public final class CatalogoIndicadores {

    private CatalogoIndicadores() {
    }

    public static List<Indicador> todos() {
        return List.of(new Assiduidade(), new GastoCota(), new ProducaoLegislativa(), new VariacaoPatrimonio(),
                new AlinhamentoPauta());
    }
}
