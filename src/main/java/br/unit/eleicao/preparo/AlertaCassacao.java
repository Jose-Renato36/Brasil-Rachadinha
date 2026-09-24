package br.unit.eleicao.preparo;

import br.unit.eleicao.modelo.Candidato;
import br.unit.eleicao.modelo.CandidaturaAnterior;
import br.unit.eleicao.modelo.Metadados;

/** Motivos de cassação ou indeferimento que o TSE registrou em candidaturas anteriores da pessoa. */
public class AlertaCassacao extends CriterioPreparo {

    @Override
    public String getGrupo() {
        return ALERTAS;
    }

    @Override
    public String getTitulo() {
        return "Cassações em eleições anteriores";
    }

    @Override
    public ItemPreparo avaliar(Candidato c, Metadados meta) {
        StringBuilder motivos = new StringBuilder();
        int n = 0;
        for (CandidaturaAnterior t : c.getTrajetoria()) {
            if (!t.getMotivoCassacao().isEmpty()) {
                n++;
                motivos.append(motivos.length() == 0 ? "" : "; ").append(t.getAno()).append(" (")
                        .append(t.getCargoLegivel().toLowerCase()).append("): ").append(t.getMotivoCassacao());
            }
        }
        String detalhe = "O TSE publica o motivo quando um registro ou diploma é cassado (ex.: abuso de poder, "
                + "compra de votos, falta de requisito). Pode haver recurso ou decisão posterior.";
        if (n > 0) {
            return item(Avaliacao.ATENCAO, motivos.toString(), detalhe, "TSE");
        }
        if (c.getTrajetoria().isEmpty()) {
            return null; // sem candidaturas anteriores não há o que conferir
        }
        if (!meta.isVerificada(Metadados.FONTE_CASSACAO)) {
            return item(Avaliacao.SEM_DADOS, "Arquivo de cassações não consultado", detalhe, "TSE");
        }
        return item(Avaliacao.POSITIVO, "Nenhuma cassação registrada", detalhe, "TSE");
    }
}
