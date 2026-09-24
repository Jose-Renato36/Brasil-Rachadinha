package br.unit.eleicao.preparo;

import br.unit.eleicao.modelo.Candidato;
import br.unit.eleicao.modelo.Metadados;

/**
 * Um critério do quadro "Preparo para o cargo". Cada subclasse olha um aspecto (idade, formação,
 * experiência, alertas) e devolve um {@link ItemPreparo}. Os critérios mostram fatos verificáveis;
 * nenhum deles vira nota nem decide o voto.
 */
public abstract class CriterioPreparo {

    public static final String REQUISITOS = "Requisitos da lei";
    public static final String PREPARO = "Formação e experiência";
    public static final String ALERTAS = "Alertas em registros públicos";

    /** Grupo em que o item aparece na tela. */
    public abstract String getGrupo();

    public abstract String getTitulo();

    /**
     * @return o item avaliado, ou null quando o critério não se aplica a este candidato
     */
    public abstract ItemPreparo avaliar(Candidato c, Metadados meta);

    /** Atalho para as subclasses montarem o item com grupo e título já preenchidos. */
    protected ItemPreparo item(Avaliacao a, String resumo, String detalhe, String fonte) {
        return new ItemPreparo(getGrupo(), getTitulo(), a, resumo, detalhe, fonte);
    }
}
