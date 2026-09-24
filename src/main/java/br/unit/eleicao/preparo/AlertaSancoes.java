package br.unit.eleicao.preparo;

import br.unit.eleicao.modelo.Candidato;
import br.unit.eleicao.modelo.Metadados;
import br.unit.eleicao.modelo.Sancao;

/** Punições nos cadastros CEIS e CNEP da CGU, da pessoa ou de empresas em que é sócia. */
public class AlertaSancoes extends CriterioPreparo {

    @Override
    public String getGrupo() {
        return ALERTAS;
    }

    @Override
    public String getTitulo() {
        return "Sanções administrativas (CEIS/CNEP)";
    }

    @Override
    public ItemPreparo avaliar(Candidato c, Metadados meta) {
        String detalhe = "Cadastros da CGU de pessoas e empresas proibidas de contratar com o poder público "
                + "ou punidas pela Lei Anticorrupção. Ligação só pelo CPF (dígitos visíveis) e nome completo.";
        int pessoa = 0;
        int empresa = 0;
        for (Sancao s : c.getSancoes()) {
            if ("CEAF".equals(s.getCadastro())) {
                continue; // expulsões têm item próprio
            }
            if (s.isSobreEmpresa()) {
                empresa++;
            } else {
                pessoa++;
            }
        }
        if (pessoa + empresa > 0) {
            String resumo = (pessoa > 0 ? pessoa + " em nome da pessoa" : "")
                    + (pessoa > 0 && empresa > 0 ? " e " : "")
                    + (empresa > 0 ? empresa + " em empresa(s) de que é sócia" : "");
            return item(Avaliacao.ATENCAO, resumo, detalhe, "CGU");
        }
        if (!meta.isVerificada(Metadados.FONTE_SANCOES)) {
            return item(Avaliacao.SEM_DADOS, "Cadastros da CGU não consultados", detalhe, "CGU");
        }
        return item(Avaliacao.POSITIVO, "Nada consta", detalhe, "CGU");
    }
}
