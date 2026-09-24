package br.unit.eleicao.preparo;

import br.unit.eleicao.modelo.Candidato;
import br.unit.eleicao.modelo.CandidaturaAnterior;
import br.unit.eleicao.modelo.Metadados;

/** Quantos anos de mandato a pessoa exerceu, separando Executivo (gestão) e Legislativo. */
public class ExperienciaEletiva extends CriterioPreparo {

    @Override
    public String getGrupo() {
        return PREPARO;
    }

    @Override
    public String getTitulo() {
        return "Experiência em cargos eletivos";
    }

    @Override
    public ItemPreparo avaliar(Candidato c, Metadados meta) {
        int ano = meta.getAnoEleicao();
        int executivo = 0;
        int legislativo = 0;
        StringBuilder lista = new StringBuilder();
        for (CandidaturaAnterior t : c.getTrajetoria()) {
            int anos = t.anosExercidos(ano);
            if (anos == 0) {
                continue;
            }
            if (t.getTipoCargo().isExecutivo()) {
                executivo += anos;
            } else {
                legislativo += anos;
            }
            lista.append(lista.length() == 0 ? "" : "; ").append(t.getCargoLegivel())
                    .append(t.getLocal().isEmpty() ? "" : " (" + t.getLocal() + ")")
                    .append(", eleito(a) em ").append(t.getAno());
        }
        int primeiro = ano - 8;
        String janela = "Considera as eleições de " + primeiro + " em diante (arquivos do TSE usados pela ferramenta); "
                + "mandatos mais antigos não aparecem.";
        if (executivo + legislativo == 0) {
            return item(Avaliacao.NEUTRO, "Nenhum mandato encontrado desde " + primeiro,
                    "Pode ser a primeira vez em cargo eletivo. " + janela, "TSE");
        }
        String resumo = anos(executivo + legislativo) + " de mandato"
                + (executivo > 0 && legislativo > 0 ? " (" + anos(executivo) + " no Executivo, " + anos(legislativo)
                + " no Legislativo)" : executivo > 0 ? " no Executivo" : " no Legislativo");
        return item(Avaliacao.POSITIVO, resumo, lista + ". " + janela, "TSE");
    }

    static String anos(int n) {
        return n + (n == 1 ? " ano" : " anos");
    }
}
