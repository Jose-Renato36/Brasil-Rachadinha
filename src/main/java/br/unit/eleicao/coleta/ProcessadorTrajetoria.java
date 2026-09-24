package br.unit.eleicao.coleta;

import br.unit.eleicao.excecao.ArquivoInvalidoException;
import br.unit.eleicao.modelo.Candidato;
import br.unit.eleicao.modelo.CandidaturaAnterior;
import br.unit.eleicao.util.LeitorCsv;
import br.unit.eleicao.util.Texto;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Monta a trajetória política de cada candidato a partir dos arquivos consulta_cand de eleições
 * anteriores (gerais e municipais), no mesmo layout de 50 colunas. Liga a pessoa pelo CPF quando os
 * dois anos o publicam; senão, por nome civil + data de nascimento (em 2024 o TSE mascarou o CPF com "-4").
 */
public class ProcessadorTrajetoria {

    private final Path pasta;
    private final String uf;
    private final Consumer<String> log;

    public ProcessadorTrajetoria(Path pasta, String uf, Consumer<String> log) {
        this.pasta = pasta;
        this.uf = uf.toUpperCase();
        this.log = log;
    }

    /** Eleições anteriores consideradas: as 4 anteriores à atual (ex.: 2026 -> 2018, 2020, 2022, 2024). */
    public static int[] anosAnteriores(int anoEleicao) {
        return new int[]{anoEleicao - 8, anoEleicao - 6, anoEleicao - 4, anoEleicao - 2};
    }

    /** Linha de uma candidatura antiga, antes de escolher o resultado final entre os turnos. */
    private static final class Registro {
        private final CandidaturaAnterior candidatura;
        private final int turno;

        private Registro(CandidaturaAnterior candidatura, int turno) {
            this.candidatura = candidatura;
            this.turno = turno;
        }
    }

    /**
     * @param cpfPorSq CPF das candidaturas atuais (pode faltar), usado como chave mais forte
     */
    public void processar(List<Candidato> candidatos, Map<String, String> cpfPorSq, int[] anos)
            throws ArquivoInvalidoException {
        Map<String, List<Candidato>> porChave = new HashMap<>();
        for (Candidato c : candidatos) {
            String cpf = cpfPorSq.get(c.getSq());
            if (cpf != null && cpf.length() == 11) {
                porChave.computeIfAbsent("CPF:" + cpf, k -> new ArrayList<>()).add(c);
            }
            if (c.getDataNascimento() != null && !Texto.vazio(c.getNome())) {
                porChave.computeIfAbsent(chaveNome(c.getNome(), c.getDataNascimento()), k -> new ArrayList<>()).add(c);
            }
        }
        int total = 0;
        for (int ano : anos) {
            Path arquivo = pasta.resolve(FontesDados.nomeLocal(FontesDados.candidatos(ano)));
            if (!Files.exists(arquivo)) {
                log.accept("  aviso: " + arquivo.getFileName() + " ausente - trajetória sem a eleição de " + ano);
                continue;
            }
            int doAno = lerAno(arquivo, ano, porChave);
            log.accept("  " + ano + ": " + doAno + " candidaturas anteriores encontradas");
            total += doAno;
        }
        long comHistorico = candidatos.stream().filter(c -> !c.getTrajetoria().isEmpty()).count();
        log.accept("Trajetória: " + comHistorico + " de " + candidatos.size() + " candidatos já disputaram eleições ("
                + total + " candidaturas).");
    }

    private int lerAno(Path arquivo, int ano, Map<String, List<Candidato>> porChave) throws ArquivoInvalidoException {
        // SQ_CANDIDATO do ano -> (candidato atual, melhor registro): o 2º turno substitui o 1º
        Map<String, Candidato> dono = new HashMap<>();
        Map<String, Registro> escolhido = new HashMap<>();
        for (String sufixo : ArquivosBrutos.sufixos(uf)) {
            LeitorCsv aberto = ArquivosBrutos.abrirSeExistir(arquivo, ProcessadorTSE.LATIN1, sufixo);
            if (aberto != null) {
                lerEntrada(aberto, ano, porChave, dono, escolhido);
            }
        }
        for (Map.Entry<String, Registro> e : escolhido.entrySet()) {
            dono.get(e.getKey()).adicionarCandidaturaAnterior(e.getValue().candidatura);
        }
        return escolhido.size();
    }

    private void lerEntrada(LeitorCsv aberto, int ano, Map<String, List<Candidato>> porChave, Map<String, Candidato> dono,
                            Map<String, Registro> escolhido) throws ArquivoInvalidoException {
        try (LeitorCsv csv = aberto) {
            int iUf = csv.indice("SG_UF");
            int iSq = csv.indice("SQ_CANDIDATO");
            int iCargo = csv.indice("DS_CARGO");
            int iUe = csv.indiceOpcional("NM_UE");
            int iNome = csv.indice("NM_CANDIDATO");
            int iNasc = csv.indiceOpcional("DT_NASCIMENTO");
            int iCpf = csv.indiceOpcional("NR_CPF_CANDIDATO");
            int iPartido = csv.indiceOpcional("SG_PARTIDO");
            int iTurno = csv.indiceOpcional("NR_TURNO");
            int iResultado = csv.indiceOpcional("DS_SIT_TOT_TURNO");
            String[] l;
            while ((l = csv.proximaLinha()) != null) {
                String ufLinha = LeitorCsv.campo(l, iUf);
                if (!ArquivosBrutos.aceitaUf(uf, ufLinha)) {
                    continue;
                }
                String cpf = Texto.somenteDigitos(Texto.limparTse(LeitorCsv.campo(l, iCpf)));
                LocalDate nasc = Texto.parseData(LeitorCsv.campo(l, iNasc));
                Candidato c = localizar(porChave, cpf, Texto.limparTse(LeitorCsv.campo(l, iNome)), nasc);
                if (c == null) {
                    continue;
                }
                String sq = LeitorCsv.campo(l, iSq);
                Integer turno = Texto.parseInteiro(LeitorCsv.campo(l, iTurno));
                String cargo = Texto.limparTse(LeitorCsv.campo(l, iCargo));
                CandidaturaAnterior cand = new CandidaturaAnterior(ano, cargo,
                        localDaEleicao(cargo, Texto.limparTse(LeitorCsv.campo(l, iUe)), ufLinha),
                        Texto.limparTse(LeitorCsv.campo(l, iPartido)),
                        CandidaturaAnterior.resultadoSimples(LeitorCsv.campo(l, iResultado)));
                Registro r = new Registro(cand, turno == null ? 1 : turno);
                Registro atual = escolhido.get(sq);
                if (atual == null || r.turno > atual.turno) {
                    escolhido.put(sq, r);
                    dono.put(sq, c);
                }
            }
        }
    }

    /** Em eleição municipal o local é o município; em eleição geral, a UF (já implícita). */
    private static String localDaEleicao(String cargo, String unidadeEleitoral, String uf) {
        br.unit.eleicao.modelo.Cargo tipo = br.unit.eleicao.modelo.Cargo.de(cargo);
        if (tipo.isMunicipal()) {
            return unidadeEleitoral + "/" + uf;
        }
        return tipo == br.unit.eleicao.modelo.Cargo.PRESIDENTE || tipo == br.unit.eleicao.modelo.Cargo.VICE_PRESIDENTE
                ? "" : uf;
    }

    /** CPF quando os dois lados o têm; senão nome + nascimento. Só aceita correspondência única. */
    private static Candidato localizar(Map<String, List<Candidato>> porChave, String cpf, String nome, LocalDate nasc) {
        List<Candidato> achados = null;
        if (cpf.length() == 11) {
            achados = porChave.get("CPF:" + cpf);
        }
        if ((achados == null || achados.isEmpty()) && nasc != null) {
            achados = porChave.get(chaveNome(nome, nasc));
        }
        return achados != null && achados.size() == 1 ? achados.get(0) : null;
    }

    static String chaveNome(String nome, LocalDate nascimento) {
        return "NOME:" + Texto.normalizar(nome) + "|" + nascimento;
    }
}
