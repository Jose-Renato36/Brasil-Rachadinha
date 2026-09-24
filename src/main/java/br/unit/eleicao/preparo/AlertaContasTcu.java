package br.unit.eleicao.preparo;

import br.unit.eleicao.modelo.Candidato;
import br.unit.eleicao.modelo.Metadados;

/** Lista do TCU de gestores com contas julgadas irregulares (decisão definitiva), enviada ao TSE. */
public class AlertaContasTcu extends CriterioPreparo {

    @Override
    public String getGrupo() {
        return ALERTAS;
    }

    @Override
    public String getTitulo() {
        return "Contas julgadas irregulares (TCU)";
    }

    @Override
    public ItemPreparo avaliar(Candidato c, Metadados meta) {
        String detalhe = "Estar na lista não torna ninguém inelegível automaticamente: quem decide é a Justiça Eleitoral.";
        int n = c.getContasIrregulares().size();
        if (n > 0) {
            return item(Avaliacao.ATENCAO, n + (n == 1 ? " processo" : " processos") + " na lista do TCU", detalhe, "TCU");
        }
        if (!meta.isVerificada("tcu")) {
            return item(Avaliacao.SEM_DADOS, "Lista do TCU não consultada nesta coleta", detalhe, "TCU");
        }
        return item(Avaliacao.POSITIVO, "Não consta na lista do TCU", "Ligação feita pelo CPF. " + detalhe, "TCU");
    }
}
