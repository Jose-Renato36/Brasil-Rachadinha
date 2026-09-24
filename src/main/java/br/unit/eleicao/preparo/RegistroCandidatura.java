package br.unit.eleicao.preparo;

import br.unit.eleicao.modelo.Candidato;
import br.unit.eleicao.modelo.Elegibilidade;
import br.unit.eleicao.modelo.Metadados;

/** Situação do registro na Justiça Eleitoral (quem julga elegibilidade, Ficha Limpa, filiação etc.). */
public class RegistroCandidatura extends CriterioPreparo {

    @Override
    public String getGrupo() {
        return REQUISITOS;
    }

    @Override
    public String getTitulo() {
        return "Registro na Justiça Eleitoral";
    }

    @Override
    public ItemPreparo avaliar(Candidato c, Metadados meta) {
        Elegibilidade e = c.getElegibilidade();
        String detalhe = e.getExplicacao()
                + " A Justiça Eleitoral confere filiação, domicílio eleitoral, quitação e a Lei da Ficha Limpa.";
        String situacao = c.getDetalheSituacao() == null || c.getDetalheSituacao().isBlank()
                ? e.getRotulo() : e.getRotulo() + " (" + c.getDetalheSituacao().toLowerCase() + ")";
        switch (e) {
            case APTA:
                return item(Avaliacao.POSITIVO, situacao, detalhe, "TSE");
            case SUB_JUDICE:
                return item(Avaliacao.ATENCAO, situacao, detalhe, "TSE");
            case INAPTA:
                return item(Avaliacao.NEGATIVO, situacao, detalhe, "TSE");
            default:
                return item(Avaliacao.NEUTRO, situacao, detalhe, "TSE");
        }
    }
}
