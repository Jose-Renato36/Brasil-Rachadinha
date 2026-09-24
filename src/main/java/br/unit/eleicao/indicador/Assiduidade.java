package br.unit.eleicao.indicador;

import br.unit.eleicao.modelo.BaseDados;
import br.unit.eleicao.modelo.Candidato;
import br.unit.eleicao.modelo.Deputado;
import br.unit.eleicao.modelo.Votacao;
import br.unit.eleicao.util.Texto;

import java.time.LocalDate;
import java.util.Map;

/**
 * Percentual de votações nominais do Plenário em que o deputado registrou voto.
 * Denominador: votações ocorridas entre o primeiro e o último voto registrado dele
 * (aproximação do período de exercício, que cobre suplentes e licenças no início/fim).
 */
public class Assiduidade extends Indicador {

    public static final String CODIGO = "assiduidade";

    public Assiduidade() {
        super(CODIGO, "Assiduidade", "Câmara dos Deputados - votações nominais do Plenário",
                "% de votações nominais do Plenário com voto registrado, no período em que exerceu o mandato.",
                Sentido.MAIOR_MELHOR);
    }

    @Override
    public ResultadoIndicador calcular(Candidato candidato, BaseDados base) {
        Deputado d = deputadoDe(candidato, base);
        if (d == null) {
            return semMandato();
        }
        Map<String, String> votos = base.getVotosDe(d.getId());
        LocalDate inicio = null;
        LocalDate fim = null;
        int presentes = 0;
        for (String idVotacao : votos.keySet()) {
            Votacao v = base.getVotacao(idVotacao);
            if (v == null || v.getData() == null) {
                continue;
            }
            presentes++;
            if (inicio == null || v.getData().isBefore(inicio)) {
                inicio = v.getData();
            }
            if (fim == null || v.getData().isAfter(fim)) {
                fim = v.getData();
            }
        }
        if (presentes == 0) {
            return ResultadoIndicador.semDados("Nenhum voto nominal registrado no Plenário");
        }
        int total = base.contarVotacoesEntre(inicio, fim);
        double pct = 100.0 * presentes / total;
        return ResultadoIndicador.com(pct, String.format("Votou em %d de %d votações nominais (%s a %s)",
                presentes, total, Texto.formatarData(inicio), Texto.formatarData(fim)));
    }

    @Override
    public String formatar(double valor) {
        return Texto.percentual(valor);
    }
}
