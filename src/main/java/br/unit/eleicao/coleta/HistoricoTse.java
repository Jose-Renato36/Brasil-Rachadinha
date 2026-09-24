package br.unit.eleicao.coleta;

import br.unit.eleicao.excecao.ArquivoInvalidoException;
import br.unit.eleicao.modelo.Candidato;
import br.unit.eleicao.modelo.CandidaturaAnterior;
import br.unit.eleicao.util.LeitorCsv;
import br.unit.eleicao.util.Texto;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Completa cada candidatura anterior da trajetória com dados do mesmo ano no TSE: o total de bens
 * declarado naquela eleição (série do patrimônio) e o motivo de cassação, quando houver.
 * A ligação usa o SQ_CANDIDATO daquele ano, já descoberto ao montar a trajetória.
 */
public class HistoricoTse {

    private final Path pasta;
    private final String uf;
    private final Consumer<String> log;
    private boolean bensLidos;
    private boolean cassacoesLidas;

    public HistoricoTse(Path pasta, String uf, Consumer<String> log) {
        this.pasta = pasta;
        this.uf = uf.toUpperCase();
        this.log = log;
    }

    public void processar(List<Candidato> candidatos, int[] anos) throws ArquivoInvalidoException {
        for (int ano : anos) {
            Map<String, CandidaturaAnterior> porSq = new HashMap<>();
            for (Candidato c : candidatos) {
                for (CandidaturaAnterior t : c.getTrajetoria()) {
                    if (t.getAno() == ano && !t.getSqOrigem().isEmpty()) {
                        porSq.put(t.getSqOrigem(), t);
                    }
                }
            }
            if (porSq.isEmpty()) {
                continue;
            }
            Path bens = pasta.resolve(FontesDados.nomeLocal(FontesDados.bens(ano)));
            if (Files.exists(bens)) {
                bensLidos = true;
                Map<String, Double> soma = ProcessadorTSE.somarBens(pasta, uf, ano, porSq.keySet(), log);
                for (Map.Entry<String, CandidaturaAnterior> e : porSq.entrySet()) {
                    // o arquivo existe: quem não aparece nele declarou não ter bens
                    e.getValue().setPatrimonio(soma.getOrDefault(e.getKey(), 0.0));
                }
            }
            lerVotos(ano, porSq);
            int cassacoes = lerCassacoes(ano, porSq);
            if (cassacoes > 0) {
                log.accept("  " + ano + ": " + cassacoes + " candidatura(s) anterior(es) com motivo de cassação");
            }
        }
        log.accept("Histórico TSE: bens anteriores " + (bensLidos ? "lidos" : "ausentes") + "; cassações "
                + (cassacoesLidas ? "lidas" : "ausentes") + ".");
    }

    private int lerCassacoes(int ano, Map<String, CandidaturaAnterior> porSq) throws ArquivoInvalidoException {
        Path arquivo = pasta.resolve(FontesDados.nomeLocal(FontesDados.motivoCassacao(ano)));
        if (!Files.exists(arquivo)) {
            return 0;
        }
        cassacoesLidas = true;
        Set<String> marcados = new HashSet<>();
        for (String sufixo : ArquivosBrutos.sufixos(uf)) {
            LeitorCsv aberto = ArquivosBrutos.abrirSeExistir(arquivo, ProcessadorTSE.LATIN1, sufixo);
            if (aberto == null) {
                continue;
            }
            try (LeitorCsv csv = aberto) {
                int iSq = csv.indice("SQ_CANDIDATO");
                int iMotivo = csv.indice("DS_MOTIVO_CASSACAO");
                String[] l;
                while ((l = csv.proximaLinha()) != null) {
                    CandidaturaAnterior t = porSq.get(LeitorCsv.campo(l, iSq));
                    String motivo = Texto.limparTse(LeitorCsv.campo(l, iMotivo));
                    if (t == null || motivo.isEmpty()) {
                        continue;
                    }
                    String atual = t.getMotivoCassacao();
                    if (!atual.contains(motivo)) {
                        t.setMotivoCassacao(atual.isEmpty() ? motivo : atual + ", " + motivo);
                    }
                    marcados.add(LeitorCsv.campo(l, iSq));
                }
            }
        }
        return marcados.size();
    }

    private boolean votosLidos;

    public boolean isVotosLidos() {
        return votosLidos;
    }

    /** Soma dos votos nominais do 1º turno por candidatura (votacao_candidato_munzona_ANO_UF.csv). */
    private void lerVotos(int ano, Map<String, CandidaturaAnterior> porSq) throws ArquivoInvalidoException {
        Path arquivo = pasta.resolve(FontesDados.nomeLocal(FontesDados.votacao(ano)));
        if (!Files.exists(arquivo)) {
            return;
        }
        votosLidos = true;
        Map<String, Long> soma = new HashMap<>();
        Set<String> vistas = new HashSet<>();
        for (String sufixo : ArquivosBrutos.sufixos(uf)) {
            LeitorCsv aberto = ArquivosBrutos.abrirSeExistir(arquivo, ProcessadorTSE.LATIN1, sufixo);
            if (aberto == null) {
                continue;
            }
            try (LeitorCsv csv = aberto) {
                int iSq = csv.indice("SQ_CANDIDATO");
                int iTurno = csv.indiceOpcional("NR_TURNO");
                int iVotos = csv.indice("QT_VOTOS_NOMINAIS", "QT_VOTOS_NOMINAIS_VALIDOS");
                int iMun = csv.indiceOpcional("CD_MUNICIPIO");
                int iZona = csv.indiceOpcional("NR_ZONA");
                String[] l;
                while ((l = csv.proximaLinha()) != null) {
                    String sq = LeitorCsv.campo(l, iSq);
                    String turno = LeitorCsv.campo(l, iTurno);
                    if (!porSq.containsKey(sq) || !(turno.isEmpty() || turno.equals("1"))) {
                        continue;
                    }
                    // no modo nacional a mesma linha pode vir em _BRASIL e _BR
                    if (!vistas.add(sq + "|" + LeitorCsv.campo(l, iMun) + "|" + LeitorCsv.campo(l, iZona))) {
                        continue;
                    }
                    Integer v = Texto.parseInteiro(LeitorCsv.campo(l, iVotos));
                    if (v != null) {
                        soma.merge(sq, (long) v, Long::sum);
                    }
                }
            }
        }
        for (Map.Entry<String, CandidaturaAnterior> e : porSq.entrySet()) {
            e.getValue().setVotos(soma.getOrDefault(e.getKey(), 0L));
        }
    }

    public boolean isBensLidos() {
        return bensLidos;
    }

    public boolean isCassacoesLidas() {
        return cassacoesLidas;
    }
}
