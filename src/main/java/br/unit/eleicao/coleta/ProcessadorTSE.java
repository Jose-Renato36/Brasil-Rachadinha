package br.unit.eleicao.coleta;

import br.unit.eleicao.excecao.ArquivoInvalidoException;
import br.unit.eleicao.modelo.BaseDados;
import br.unit.eleicao.modelo.Candidato;
import br.unit.eleicao.modelo.Cargo;
import br.unit.eleicao.modelo.GrauInstrucao;
import br.unit.eleicao.util.LeitorCsv;
import br.unit.eleicao.util.Texto;

import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Lê os arquivos do TSE (consulta_cand e bem_candidato, que vêm em ZIP com um CSV por UF,
 * em ISO-8859-1) e monta as candidaturas a deputado federal da UF, com o patrimônio
 * declarado na eleição atual e na anterior.
 */
public class ProcessadorTSE {

    public static final Charset LATIN1 = Charset.forName("ISO-8859-1");

    private final Path pasta;
    private final String uf;
    private final int anoEleicao;
    private final int anoAnterior;
    private final Consumer<String> log;
    private final Map<String, String> cpfPorSq = new HashMap<>();

    public ProcessadorTSE(Path pasta, String uf, int anoEleicao, int anoAnterior, Consumer<String> log) {
        this.pasta = pasta;
        this.uf = uf.toUpperCase();
        this.anoEleicao = anoEleicao;
        this.anoAnterior = anoAnterior;
        this.log = log;
    }

    /** Pequena estrutura interna: candidatura lida + CPF (que não fica guardado no modelo). */
    private static final class Registro {
        private final Candidato candidato;
        private final String cpf;

        private Registro(Candidato candidato, String cpf) {
            this.candidato = candidato;
            this.cpf = cpf;
        }
    }

    public void processar(BaseDados base) throws ArquivoInvalidoException {
        Path arqCand = pasta.resolve(FontesDados.nomeLocal(FontesDados.candidatos(anoEleicao)));
        if (!Files.exists(arqCand)) {
            throw new ArquivoInvalidoException("Arquivo de candidaturas do TSE não encontrado: "
                    + arqCand.toAbsolutePath() + "\nBaixe " + FontesDados.candidatos(anoEleicao)
                    + " e coloque nessa pasta.");
        }
        List<Registro> atuais = lerCandidatos(arqCand, true);
        lerComplementar(atuais);
        Map<String, Double> bensAtuais = lerBens(anoEleicao);

        // eleição anterior: qualquer cargo na mesma UF, para achar a declaração de bens de quem já concorreu
        Map<String, Double> bensAnteriores = lerBens(anoAnterior);
        Map<String, String> sqAnteriorPorChave = new HashMap<>();
        Path arqAnt = pasta.resolve(FontesDados.nomeLocal(FontesDados.candidatos(anoAnterior)));
        if (Files.exists(arqAnt)) {
            for (Registro r : lerCandidatos(arqAnt, false)) {
                String sq = r.candidato.getSq();
                if (r.cpf.length() == 11) {
                    sqAnteriorPorChave.putIfAbsent("CPF:" + r.cpf, sq);
                }
                sqAnteriorPorChave.putIfAbsent(chaveNome(r.candidato), sq);
            }
        } else {
            log.accept("  aviso: " + arqAnt.getFileName() + " ausente - variação de patrimônio ficará sem dados");
        }

        int comAnterior = 0;
        for (Registro r : atuais) {
            Candidato c = r.candidato;
            c.setPatrimonio(bensAtuais.isEmpty() ? null : bensAtuais.getOrDefault(c.getSq(), 0.0));
            String sqAnt = r.cpf.length() == 11 ? sqAnteriorPorChave.get("CPF:" + r.cpf) : null;
            if (sqAnt == null) {
                sqAnt = sqAnteriorPorChave.get(chaveNome(c));
            }
            if (sqAnt != null && !bensAnteriores.isEmpty()) {
                c.setPatrimonioAnterior(bensAnteriores.getOrDefault(sqAnt, 0.0));
                comAnterior++;
            }
            if (r.cpf.length() == 11) {
                cpfPorSq.put(c.getSq(), r.cpf);
            }
            base.adicionarCandidato(c);
        }
        log.accept("TSE: " + atuais.size() + " candidaturas em " + uf + " (" + comAnterior
                + " também concorreram em " + anoAnterior + ").");
    }

    public Map<String, String> getCpfPorSq() {
        return cpfPorSq;
    }

    /** Nome civil + data de nascimento: chave de identidade quando o CPF não está disponível. */
    static String chaveNome(Candidato c) {
        return "NOME:" + Texto.normalizar(c.getNome()) + "|" + c.getDataNascimento();
    }

    private List<Registro> lerCandidatos(Path arquivo, boolean somenteCargo) throws ArquivoInvalidoException {
        Map<String, Registro> porSq = new LinkedHashMap<>();
        try (LeitorCsv csv = ArquivosBrutos.abrir(arquivo, LATIN1, "_" + uf + ".csv")) {
            int iUf = csv.indice("SG_UF");
            int iCargo = csv.indice("DS_CARGO");
            int iSq = csv.indice("SQ_CANDIDATO");
            int iNum = csv.indiceOpcional("NR_CANDIDATO");
            int iNome = csv.indice("NM_CANDIDATO");
            int iUrna = csv.indice("NM_URNA_CANDIDATO");
            int iPartido = csv.indice("SG_PARTIDO");
            int iNasc = csv.indiceOpcional("DT_NASCIMENTO");
            int iGenero = csv.indiceOpcional("DS_GENERO");
            int iGrau = csv.indiceOpcional("DS_GRAU_INSTRUCAO");
            int iCor = csv.indiceOpcional("DS_COR_RACA");
            int iOcup = csv.indiceOpcional("DS_OCUPACAO");
            int iSit = csv.indiceOpcional("DS_SITUACAO_CANDIDATURA");
            int iDetalhe = csv.indiceOpcional("DS_DETALHE_SITUACAO_CAND");
            int iCpf = csv.indiceOpcional("NR_CPF_CANDIDATO");
            String[] l;
            while ((l = csv.proximaLinha()) != null) {
                if (!uf.equalsIgnoreCase(LeitorCsv.campo(l, iUf))) {
                    continue;
                }
                String cargo = Texto.limparTse(LeitorCsv.campo(l, iCargo));
                Cargo tipo = Cargo.de(cargo);
                // eleição atual: todos os cargos disputados na UF (presidente não aparece nos arquivos por UF)
                if (somenteCargo && (tipo == Cargo.PRESIDENTE || tipo == Cargo.VICE_PRESIDENTE)) {
                    continue;
                }
                String sq = LeitorCsv.campo(l, iSq);
                if (porSq.containsKey(sq)) {
                    continue; // mesma candidatura repetida (ex.: segundo turno)
                }
                Candidato c = new Candidato(sq, Texto.limparTse(LeitorCsv.campo(l, iNome)),
                        Texto.limparTse(LeitorCsv.campo(l, iUrna)), Texto.parseData(LeitorCsv.campo(l, iNasc)),
                        Texto.limparTse(LeitorCsv.campo(l, iGenero)));
                c.setNumero(Texto.limparTse(LeitorCsv.campo(l, iNum)));
                c.setPartido(Texto.limparTse(LeitorCsv.campo(l, iPartido)));
                c.setUf(uf);
                c.setCargo(cargo);
                c.setGrauInstrucao(GrauInstrucao.deTexto(LeitorCsv.campo(l, iGrau)));
                c.setCorRaca(Texto.limparTse(LeitorCsv.campo(l, iCor)));
                c.setOcupacao(Texto.limparTse(LeitorCsv.campo(l, iOcup)));
                c.setSituacao(Texto.limparTse(LeitorCsv.campo(l, iSit)));
                c.setDetalheSituacao(Texto.limparTse(LeitorCsv.campo(l, iDetalhe)));
                porSq.put(sq, new Registro(c, Texto.somenteDigitos(Texto.limparTse(LeitorCsv.campo(l, iCpf)))));
            }
        }
        return new ArrayList<>(porSq.values());
    }

    /**
     * Arquivo complementar (opcional): detalhe da situação (DEFERIDO, INDEFERIDO COM RECURSO...) e se a
     * pessoa tenta a reeleição. Em 2026 o arquivo principal trazia a situação ainda como "#NE".
     */
    private void lerComplementar(List<Registro> registros) throws ArquivoInvalidoException {
        Path arquivo = pasta.resolve(FontesDados.nomeLocal(FontesDados.candidatosComplementar(anoEleicao)));
        if (!Files.exists(arquivo)) {
            log.accept("  aviso: " + arquivo.getFileName() + " ausente - situação detalhada e reeleição sem dados");
            return;
        }
        Map<String, Candidato> porSq = new HashMap<>();
        for (Registro r : registros) {
            porSq.put(r.candidato.getSq(), r.candidato);
        }
        int lidos = 0;
        try (LeitorCsv csv = ArquivosBrutos.abrir(arquivo, LATIN1, "_" + uf + ".csv")) {
            int iSq = csv.indice("SQ_CANDIDATO");
            int iDetalhe = csv.indiceOpcional("DS_DETALHE_SITUACAO_CAND");
            int iReeleicao = csv.indiceOpcional("ST_REELEICAO");
            String[] l;
            while ((l = csv.proximaLinha()) != null) {
                Candidato c = porSq.get(LeitorCsv.campo(l, iSq));
                if (c == null) {
                    continue;
                }
                String detalhe = Texto.limparTse(LeitorCsv.campo(l, iDetalhe));
                if (!detalhe.isEmpty()) {
                    c.setDetalheSituacao(detalhe);
                }
                String reeleicao = Texto.limparTse(LeitorCsv.campo(l, iReeleicao));
                if (!reeleicao.isEmpty()) {
                    c.setReeleicao(reeleicao.equalsIgnoreCase("S"));
                }
                lidos++;
            }
        }
        log.accept("  complementar: " + lidos + " candidaturas com situação detalhada");
    }

    /** Soma dos bens declarados por candidatura; mapa vazio se o arquivo não existir. */
    private Map<String, Double> lerBens(int ano) throws ArquivoInvalidoException {
        Map<String, Double> soma = new HashMap<>();
        Path arquivo = pasta.resolve(FontesDados.nomeLocal(FontesDados.bens(ano)));
        if (!Files.exists(arquivo)) {
            log.accept("  aviso: " + arquivo.getFileName() + " ausente - sem declaração de bens de " + ano);
            return soma;
        }
        try (LeitorCsv csv = ArquivosBrutos.abrir(arquivo, LATIN1, "_" + uf + ".csv")) {
            int iSq = csv.indice("SQ_CANDIDATO");
            int iValor = csv.indice("VR_BEM_CANDIDATO");
            String[] l;
            while ((l = csv.proximaLinha()) != null) {
                Double v = Texto.parseDecimal(LeitorCsv.campo(l, iValor));
                if (v != null) {
                    soma.merge(LeitorCsv.campo(l, iSq), v, Double::sum);
                }
            }
        }
        return soma;
    }
}
