package br.unit.eleicao.coleta;

import br.unit.eleicao.excecao.ArquivoInvalidoException;
import br.unit.eleicao.indicador.Assiduidade;
import br.unit.eleicao.indicador.GastoCota;
import br.unit.eleicao.indicador.ResultadoIndicador;
import br.unit.eleicao.modelo.BaseDados;
import br.unit.eleicao.modelo.Candidato;
import br.unit.eleicao.modelo.Deputado;
import br.unit.eleicao.modelo.Despesa;
import br.unit.eleicao.modelo.Metadados;
import br.unit.eleicao.modelo.Proposicao;
import br.unit.eleicao.modelo.ResumoMandato;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Resumo do mandato de quem foi deputado federal na legislatura anterior (ex.: 2019-2023),
 * lido dos mesmos arquivos em lote da Câmara. Mostra o que a pessoa fez naquele mandato.
 */
public class MandatoAnteriorCamara {

    private final Path pasta;
    private final String uf;
    private final Consumer<String> log;

    public MandatoAnteriorCamara(Path pasta, String uf, Consumer<String> log) {
        this.pasta = pasta;
        this.uf = uf;
        this.log = log;
    }

    /** Anos da legislatura anterior à que termina na eleição (2026 -> 2019 a 2022). */
    public static int[] anos(int anoEleicao) {
        return new int[]{anoEleicao - 7, anoEleicao - 6, anoEleicao - 5, anoEleicao - 4};
    }

    public void processar(List<Candidato> candidatos, Map<String, String> cpfPorSq, int anoEleicao,
                          Downloader downloader) throws ArquivoInvalidoException {
        int legislatura = PipelineColeta.legislatura(anoEleicao) - 1;
        LocalDate inicio = LocalDate.of(anoEleicao - 7, 2, 1);
        LocalDate fim = LocalDate.of(anoEleicao - 3, 1, 31);
        BaseDados anterior = new BaseDados(new Metadados(uf, anoEleicao - 4, anoEleicao - 8, null));
        ProcessadorCamara camara = new ProcessadorCamara(pasta, uf, anos(anoEleicao), legislatura, inicio, s -> { });
        camara.processar(anterior);
        if (anterior.getDeputados().isEmpty()) {
            log.accept("  sem arquivos da legislatura " + legislatura + " - mandatos anteriores na Câmara sem dados");
            return;
        }
        new HistoricoDeputados(pasta, downloader, s -> { }).preencher(anterior.getDeputados(), legislatura, inicio, fim);
        Map<String, CruzadorIdentidades.Vinculo> vinculos = new CruzadorIdentidades().associar(candidatos,
                anterior.getDeputados(), cpfPorSq, camara.getCpfPorDeputado(), Map.of());
        String periodo = inicio.getYear() + "–" + fim.getYear();
        int n = 0;
        for (Candidato c : candidatos) {
            CruzadorIdentidades.Vinculo v = vinculos.get(c.getSq());
            // vínculos só por nome são arriscados para um resumo de mandato: exige CPF ou nome + nascimento
            if (v == null || v.getCriterio().contains("CONFERIR")) {
                continue;
            }
            Deputado d = anterior.getDeputado(v.getIdDeputado());
            c.adicionarAtuacao(resumir(d, anterior, periodo));
            n++;
        }
        log.accept("  legislatura " + legislatura + " (" + periodo + "): " + anterior.getDeputados().size()
                + " deputados de " + uf + ", " + n + " candidatos com resumo do mandato");
    }

    /** Monta o resumo de um deputado numa base (também usado para o mandato atual). */
    public static ResumoMandato resumir(Deputado d, BaseDados base, String periodo) {
        ResumoMandato r = new ResumoMandato("Câmara dos Deputados", "Deputado(a) federal", periodo,
                d.getUrlCamara());
        ResultadoIndicador presenca = Assiduidade.calcularPara(d, base);
        r.setPresenca(presenca.temDados() ? presenca.getValor() : null, presenca.getDetalhe());
        Map<String, Proposicao> distintas = new LinkedHashMap<>();
        for (Proposicao p : base.getProposicoesDe(d.getId())) {
            distintas.putIfAbsent(p.getId(), p);
        }
        List<Proposicao> lista = new ArrayList<>(distintas.values());
        long aprovadas = lista.stream().filter(Proposicao::isAprovada).count();
        r.setProjetos(lista.size(), (int) aprovadas);
        lista.sort(Comparator.comparing(Proposicao::isAprovada).reversed().thenComparing(Proposicao::getAno,
                Comparator.reverseOrder()));
        for (Proposicao p : lista) {
            r.adicionarDestaque(p.getIdentificacao() + (p.isAprovada() ? " (aprovado)" : "") + " – " + p.getEmenta());
        }
        List<Despesa> despesas = base.getDespesasDe(d.getId());
        r.setGastoMensal(despesas.isEmpty() ? null : GastoCota.mediaMensal(despesas));
        return r;
    }
}
