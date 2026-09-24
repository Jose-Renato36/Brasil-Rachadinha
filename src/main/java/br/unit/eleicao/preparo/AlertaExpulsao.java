package br.unit.eleicao.preparo;

import br.unit.eleicao.modelo.Candidato;
import br.unit.eleicao.modelo.Metadados;
import br.unit.eleicao.modelo.Sancao;

/** Cadastro de Expulsões da Administração Federal (CEAF, CGU): demissão, destituição, cassação de aposentadoria. */
public class AlertaExpulsao extends CriterioPreparo {

    @Override
    public String getGrupo() {
        return ALERTAS;
    }

    @Override
    public String getTitulo() {
        return "Expulsão do serviço público federal";
    }

    @Override
    public ItemPreparo avaliar(Candidato c, Metadados meta) {
        String detalhe = "Servidores federais punidos com demissão, destituição de cargo ou cassação de aposentadoria "
                + "após processo administrativo. Ligação por nome completo e CPF parcial.";
        StringBuilder resumo = new StringBuilder();
        for (Sancao s : c.getSancoes()) {
            if ("CEAF".equals(s.getCadastro())) {
                resumo.append(resumo.length() == 0 ? "" : "; ")
                        .append(s.getCategoria().isEmpty() ? "Expulsão" : s.getCategoria())
                        .append(s.getOrgao().isEmpty() ? "" : " (" + s.getOrgao() + ")")
                        .append(s.getInicio().isEmpty() ? "" : ", " + s.getInicio());
            }
        }
        if (resumo.length() > 0) {
            return item(Avaliacao.ATENCAO, resumo.toString(), detalhe, "CGU");
        }
        if (!meta.isVerificada(Metadados.FONTE_EXPULSOES)) {
            return item(Avaliacao.SEM_DADOS, "Cadastro de expulsões não consultado", detalhe, "CGU");
        }
        return item(Avaliacao.POSITIVO, "Nada consta", detalhe, "CGU");
    }
}
