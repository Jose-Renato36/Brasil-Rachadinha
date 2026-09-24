package br.unit.eleicao.preparo;

import br.unit.eleicao.modelo.Candidato;
import br.unit.eleicao.modelo.GrauInstrucao;
import br.unit.eleicao.modelo.Metadados;

/** Escolaridade e ocupação declaradas ao TSE. A lei só exige saber ler e escrever. */
public class Formacao extends CriterioPreparo {

    @Override
    public String getGrupo() {
        return PREPARO;
    }

    @Override
    public String getTitulo() {
        return "Formação";
    }

    @Override
    public ItemPreparo avaliar(Candidato c, Metadados meta) {
        GrauInstrucao g = c.getGrauInstrucao();
        String ocupacao = c.getOcupacao() == null || c.getOcupacao().isBlank() ? ""
                : " Ocupação declarada: " + c.getOcupacao().toLowerCase() + ".";
        String detalhe = "Escolaridade declarada pela própria pessoa ao TSE; o curso não é informado. "
                + "A lei só exige que o candidato saiba ler e escrever." + ocupacao;
        if (g == GrauInstrucao.NAO_INFORMADO) {
            return item(Avaliacao.SEM_DADOS, "Escolaridade não informada", detalhe, "TSE");
        }
        if (g == GrauInstrucao.SUPERIOR_COMPLETO) {
            return item(Avaliacao.POSITIVO, "Ensino superior completo", detalhe, "TSE");
        }
        return item(Avaliacao.NEUTRO, g.getDescricao(), detalhe, "TSE");
    }
}
