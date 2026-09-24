package br.unit.eleicao.indicador;

import br.unit.eleicao.modelo.BaseDados;
import br.unit.eleicao.modelo.Candidato;
import br.unit.eleicao.modelo.Deputado;
import br.unit.eleicao.modelo.Votacao;
import br.unit.eleicao.util.Texto;

import java.time.LocalDate;
import java.util.Map;

/**
 * Percentual de votações nominais do Plenário em que o deputado registrou voto, contando só o
 * período em que estava em exercício (licenças e suplências não contam como falta).
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
        if (votos.isEmpty()) {
            return ResultadoIndicador.semDados("Nenhum voto nominal registrado no Plenário");
        }
        return d.getExercicios().isEmpty() ? porPrimeiroEUltimoVoto(votos, base) : porExercicio(d, votos, base);
    }

    /** Denominador = votações ocorridas enquanto o deputado estava em exercício (histórico da Câmara). */
    private ResultadoIndicador porExercicio(Deputado d, Map<String, String> votos, BaseDados base) {
        int possiveis = 0;
        int presentes = 0;
        for (Votacao v : base.getVotacoesOrdenadas()) {
            if (v.getData() != null && d.emExercicio(v.getData())) {
                possiveis++;
                if (votos.containsKey(v.getId())) {
                    presentes++;
                }
            }
        }
        if (possiveis == 0) {
            return ResultadoIndicador.semDados("Nenhuma votação no período em exercício");
        }
        return ResultadoIndicador.com(100.0 * presentes / possiveis, String.format(
                "Votou em %d de %d votações nominais do Plenário enquanto estava em exercício", presentes, possiveis));
    }

    /** Aproximação quando não há histórico: votações entre o primeiro e o último voto registrado. */
    private ResultadoIndicador porPrimeiroEUltimoVoto(Map<String, String> votos, BaseDados base) {
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
        return ResultadoIndicador.com(100.0 * presentes / total, String.format(
                "Votou em %d de %d votações nominais (%s a %s; período estimado, sem histórico de licenças)",
                presentes, total, Texto.formatarData(inicio), Texto.formatarData(fim)));
    }

    @Override
    public String getTituloSimples() {
        return "Presença nas votações";
    }

    @Override
    public String getPergunta() {
        return "Quanto importa que a pessoa compareça e vote no Plenário?";
    }

    @Override
    public String resumir(double valor) {
        return "Votou em " + Texto.percentual(valor) + " das votações";
    }

    @Override
    public String formatar(double valor) {
        return Texto.percentual(valor);
    }
}
