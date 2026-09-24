package br.unit.eleicao.preparo;

import br.unit.eleicao.modelo.Candidato;
import br.unit.eleicao.modelo.CandidaturaAnterior;
import br.unit.eleicao.modelo.Cargo;
import br.unit.eleicao.modelo.Metadados;

/**
 * Se a pessoa já exerceu o mesmo cargo (ex.: governador tentando reeleição) ou uma função do mesmo tipo
 * (ex.: prefeito(a) disputando governo: Executivo; vereador(a) disputando Assembleia: Legislativo).
 */
public class ExperienciaMesmaFuncao extends CriterioPreparo {

    @Override
    public String getGrupo() {
        return PREPARO;
    }

    @Override
    public String getTitulo() {
        return "Experiência na mesma função";
    }

    @Override
    public ItemPreparo avaliar(Candidato c, Metadados meta) {
        Cargo alvo = c.getTipoCargo();
        if (alvo == Cargo.OUTRO) {
            return null;
        }
        String funcao = alvo.isExecutivo() ? "Executivo (administrar orçamento, equipe e serviços)"
                : "Legislativo (fazer leis, fiscalizar e aprovar o orçamento)";
        String detalhe = "O cargo disputado é do " + funcao + ".";
        CandidaturaAnterior mesmoCargo = null;
        CandidaturaAnterior mesmoPoder = null;
        for (CandidaturaAnterior t : c.getTrajetoria()) {
            if (!t.isEleito()) {
                continue;
            }
            Cargo antes = t.getTipoCargo();
            if (antes == alvo && mesmoCargo == null) {
                mesmoCargo = t;
            } else if (antes.isExecutivo() == alvo.isExecutivo() && antes != Cargo.OUTRO && mesmoPoder == null) {
                mesmoPoder = t;
            }
        }
        if (mesmoCargo != null) {
            return item(Avaliacao.POSITIVO, "Já exerceu este cargo (eleito(a) em " + mesmoCargo.getAno() + ")",
                    detalhe, "TSE");
        }
        if (mesmoPoder != null) {
            return item(Avaliacao.POSITIVO, "Função parecida: " + mesmoPoder.getCargoLegivel().toLowerCase()
                    + " (eleito(a) em " + mesmoPoder.getAno() + ")", detalhe, "TSE");
        }
        return item(Avaliacao.NEUTRO, "Sem mandato anterior no " + (alvo.isExecutivo() ? "Executivo" : "Legislativo"),
                detalhe + " Experiência fora da política (profissão, empresas, serviço público) aparece em outras partes da ficha.",
                "TSE");
    }
}
