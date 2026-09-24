package br.unit.eleicao.persistencia;

import br.unit.eleicao.excecao.ArquivoInvalidoException;
import br.unit.eleicao.excecao.DadosException;
import br.unit.eleicao.modelo.BaseDados;
import br.unit.eleicao.modelo.Candidato;
import br.unit.eleicao.modelo.CandidaturaAnterior;
import br.unit.eleicao.modelo.Deputado;
import br.unit.eleicao.modelo.Despesa;
import br.unit.eleicao.modelo.GrauInstrucao;
import br.unit.eleicao.modelo.Metadados;
import br.unit.eleicao.modelo.PosicaoUsuario;
import br.unit.eleicao.modelo.Proposicao;
import br.unit.eleicao.modelo.Votacao;
import br.unit.eleicao.util.EscritorCsv;
import br.unit.eleicao.util.LeitorCsv;
import br.unit.eleicao.util.Texto;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Persistência em arquivos texto (CSV UTF-8 com ';'), que funcionam como o cache local
 * dos dados já processados de uma UF. Assim o programa abre sem depender das APIs públicas.
 */
public class RepositorioArquivos {

    public static final String META = "meta.properties";
    public static final String CANDIDATOS = "candidatos.csv";
    public static final String DEPUTADOS = "deputados.csv";
    public static final String VOTACOES = "votacoes.csv";
    public static final String VOTOS = "votos.csv";
    public static final String DESPESAS = "despesas.csv";
    public static final String PROPOSICOES = "proposicoes.csv";
    public static final String POSICOES = "posicoes.csv";
    public static final String EXERCICIOS = "exercicios.csv";
    public static final String TRAJETORIA = "trajetoria.csv";

    // ------------------------------------------------------------------ leitura

    public BaseDados carregar(Path dir) throws DadosException {
        if (!Files.isDirectory(dir)) {
            throw new ArquivoInvalidoException("Diretório de dados não encontrado: " + dir.toAbsolutePath());
        }
        BaseDados base = new BaseDados(lerMetadados(dir.resolve(META)));
        base.setDiretorio(dir);
        lerDeputados(base, dir.resolve(DEPUTADOS));
        lerExercicios(base, dir.resolve(EXERCICIOS));
        lerCandidatos(base, dir.resolve(CANDIDATOS));
        lerTrajetoria(base, dir.resolve(TRAJETORIA));
        lerVotacoes(base, dir.resolve(VOTACOES));
        lerVotos(base, dir.resolve(VOTOS));
        lerDespesas(base, dir.resolve(DESPESAS));
        lerProposicoes(base, dir.resolve(PROPOSICOES));
        carregarPosicoes(base);
        return base;
    }

    public static boolean contemBase(Path dir) {
        return Files.isRegularFile(dir.resolve(META)) && Files.isRegularFile(dir.resolve(CANDIDATOS));
    }

    private Metadados lerMetadados(Path arquivo) throws ArquivoInvalidoException {
        Properties p = new Properties();
        try (Reader r = Files.newBufferedReader(arquivo, StandardCharsets.UTF_8)) {
            p.load(r);
        } catch (IOException e) {
            throw new ArquivoInvalidoException("Não foi possível ler " + arquivo + ": " + e.getMessage(), e);
        }
        int ano = Integer.parseInt(p.getProperty("anoEleicao", "2026"));
        Metadados m = new Metadados(p.getProperty("uf", "?"), ano,
                Integer.parseInt(p.getProperty("anoAnterior", String.valueOf(ano - 4))),
                Texto.parseData(p.getProperty("dataEleicao", ano + "-10-04")));
        m.setDemonstracao(Boolean.parseBoolean(p.getProperty("demonstracao", "false")));
        m.setDescricao(p.getProperty("descricao", ""));
        m.setGeradoEm(p.getProperty("geradoEm", ""));
        return m;
    }

    private void lerCandidatos(BaseDados base, Path arquivo) throws ArquivoInvalidoException {
        try (LeitorCsv csv = new LeitorCsv(arquivo, StandardCharsets.UTF_8)) {
            int iSq = csv.indice("sq");
            int iNome = csv.indice("nome");
            int iUrna = csv.indice("nomeUrna");
            int iNum = csv.indice("numero");
            int iPartido = csv.indice("partido");
            int iUf = csv.indice("uf");
            int iCargo = csv.indice("cargo");
            int iNasc = csv.indice("dataNascimento");
            int iGenero = csv.indice("genero");
            int iGrau = csv.indice("grauInstrucao");
            int iCor = csv.indice("corRaca");
            int iOcup = csv.indice("ocupacao");
            int iSit = csv.indice("situacao");
            int iPat = csv.indice("patrimonio");
            int iPatAnt = csv.indice("patrimonioAnterior");
            int iDep = csv.indice("idDeputado");
            int iCrit = csv.indice("criterioVinculo");
            int iDetalhe = csv.indiceOpcional("detalheSituacao");
            int iReeleicao = csv.indiceOpcional("reeleicao");
            String[] l;
            while ((l = csv.proximaLinha()) != null) {
                Candidato c = new Candidato(LeitorCsv.campo(l, iSq), LeitorCsv.campo(l, iNome),
                        LeitorCsv.campo(l, iUrna), Texto.parseData(LeitorCsv.campo(l, iNasc)),
                        LeitorCsv.campo(l, iGenero));
                c.setNumero(LeitorCsv.campo(l, iNum));
                c.setPartido(LeitorCsv.campo(l, iPartido));
                c.setUf(LeitorCsv.campo(l, iUf));
                c.setCargo(LeitorCsv.campo(l, iCargo));
                c.setGrauInstrucao(GrauInstrucao.deTexto(LeitorCsv.campo(l, iGrau)));
                c.setCorRaca(LeitorCsv.campo(l, iCor));
                c.setOcupacao(LeitorCsv.campo(l, iOcup));
                c.setSituacao(LeitorCsv.campo(l, iSit));
                c.setDetalheSituacao(LeitorCsv.campo(l, iDetalhe));
                String reeleicao = LeitorCsv.campo(l, iReeleicao);
                c.setReeleicao(reeleicao.isEmpty() ? null : Boolean.parseBoolean(reeleicao));
                c.setPatrimonio(Texto.parseDecimal(LeitorCsv.campo(l, iPat)));
                c.setPatrimonioAnterior(Texto.parseDecimal(LeitorCsv.campo(l, iPatAnt)));
                Integer idDep = Texto.parseInteiro(LeitorCsv.campo(l, iDep));
                if (idDep != null && base.getDeputado(idDep) != null) {
                    c.vincularDeputado(idDep, LeitorCsv.campo(l, iCrit));
                }
                base.adicionarCandidato(c);
            }
        }
    }

    private void lerDeputados(BaseDados base, Path arquivo) throws ArquivoInvalidoException {
        if (!Files.exists(arquivo)) {
            return;
        }
        try (LeitorCsv csv = new LeitorCsv(arquivo, StandardCharsets.UTF_8)) {
            int iId = csv.indice("id");
            int iNomeParl = csv.indice("nomeParlamentar");
            int iNomeCivil = csv.indice("nomeCivil");
            int iPartido = csv.indice("partido");
            int iUf = csv.indice("uf");
            int iNasc = csv.indice("dataNascimento");
            int iGenero = csv.indice("genero");
            int iFoto = csv.indiceOpcional("urlFoto");
            String[] l;
            while ((l = csv.proximaLinha()) != null) {
                Integer id = Texto.parseInteiro(LeitorCsv.campo(l, iId));
                if (id == null) {
                    continue;
                }
                Deputado d = new Deputado(id, LeitorCsv.campo(l, iNomeParl), LeitorCsv.campo(l, iNomeCivil),
                        Texto.parseData(LeitorCsv.campo(l, iNasc)), LeitorCsv.campo(l, iGenero));
                d.setPartido(LeitorCsv.campo(l, iPartido));
                d.setUf(LeitorCsv.campo(l, iUf));
                d.setUrlFoto(LeitorCsv.campo(l, iFoto));
                base.adicionarDeputado(d);
            }
        }
    }

    private void lerTrajetoria(BaseDados base, Path arquivo) throws ArquivoInvalidoException {
        if (!Files.exists(arquivo)) {
            return;
        }
        try (LeitorCsv csv = new LeitorCsv(arquivo, StandardCharsets.UTF_8)) {
            int iSq = csv.indice("sq");
            int iAno = csv.indice("ano");
            int iCargo = csv.indice("cargo");
            int iLocal = csv.indice("local");
            int iPartido = csv.indice("partido");
            int iResultado = csv.indice("resultado");
            String[] l;
            while ((l = csv.proximaLinha()) != null) {
                Candidato c = base.buscarCandidato(LeitorCsv.campo(l, iSq));
                Integer ano = Texto.parseInteiro(LeitorCsv.campo(l, iAno));
                if (c != null && ano != null) {
                    c.adicionarCandidaturaAnterior(new CandidaturaAnterior(ano, LeitorCsv.campo(l, iCargo),
                            LeitorCsv.campo(l, iLocal), LeitorCsv.campo(l, iPartido), LeitorCsv.campo(l, iResultado)));
                }
            }
        }
    }

    private void lerExercicios(BaseDados base, Path arquivo) throws ArquivoInvalidoException {
        if (!Files.exists(arquivo)) {
            return;
        }
        try (LeitorCsv csv = new LeitorCsv(arquivo, StandardCharsets.UTF_8)) {
            int iDep = csv.indice("idDeputado");
            int iIni = csv.indice("inicio");
            int iFim = csv.indice("fim");
            String[] l;
            while ((l = csv.proximaLinha()) != null) {
                Deputado d = base.getDeputado(Texto.parseInteiro(LeitorCsv.campo(l, iDep)));
                LocalDate ini = Texto.parseData(LeitorCsv.campo(l, iIni));
                LocalDate fim = Texto.parseData(LeitorCsv.campo(l, iFim));
                if (d != null && ini != null && fim != null) {
                    d.adicionarExercicio(ini, fim);
                }
            }
        }
    }

    private void lerVotacoes(BaseDados base, Path arquivo) throws ArquivoInvalidoException {
        if (!Files.exists(arquivo)) {
            return;
        }
        try (LeitorCsv csv = new LeitorCsv(arquivo, StandardCharsets.UTF_8)) {
            int iId = csv.indice("id");
            int iData = csv.indice("data");
            int iDesc = csv.indice("descricao");
            int iProp = csv.indiceOpcional("proposicao");
            int iEmenta = csv.indiceOpcional("ementa");
            String[] l;
            while ((l = csv.proximaLinha()) != null) {
                Votacao v = new Votacao(LeitorCsv.campo(l, iId), Texto.parseData(LeitorCsv.campo(l, iData)),
                        LeitorCsv.campo(l, iDesc));
                v.setProposicao(LeitorCsv.campo(l, iProp), LeitorCsv.campo(l, iEmenta));
                base.adicionarVotacao(v);
            }
        }
    }

    private void lerVotos(BaseDados base, Path arquivo) throws ArquivoInvalidoException {
        if (!Files.exists(arquivo)) {
            return;
        }
        try (LeitorCsv csv = new LeitorCsv(arquivo, StandardCharsets.UTF_8)) {
            int iVot = csv.indice("idVotacao");
            int iDep = csv.indice("idDeputado");
            int iVoto = csv.indice("voto");
            String[] l;
            while ((l = csv.proximaLinha()) != null) {
                Integer id = Texto.parseInteiro(LeitorCsv.campo(l, iDep));
                if (id != null) {
                    base.adicionarVoto(id, LeitorCsv.campo(l, iVot), LeitorCsv.campo(l, iVoto));
                }
            }
        }
    }

    private void lerDespesas(BaseDados base, Path arquivo) throws ArquivoInvalidoException {
        if (!Files.exists(arquivo)) {
            return;
        }
        try (LeitorCsv csv = new LeitorCsv(arquivo, StandardCharsets.UTF_8)) {
            int iDep = csv.indice("idDeputado");
            int iAno = csv.indice("ano");
            int iMes = csv.indice("mes");
            int iCat = csv.indice("categoria");
            int iValor = csv.indice("valor");
            String[] l;
            while ((l = csv.proximaLinha()) != null) {
                Integer id = Texto.parseInteiro(LeitorCsv.campo(l, iDep));
                Integer ano = Texto.parseInteiro(LeitorCsv.campo(l, iAno));
                Integer mes = Texto.parseInteiro(LeitorCsv.campo(l, iMes));
                Double valor = Texto.parseDecimal(LeitorCsv.campo(l, iValor));
                if (id != null && ano != null && mes != null && valor != null) {
                    base.adicionarDespesa(new Despesa(id, ano, mes, LeitorCsv.campo(l, iCat), valor));
                }
            }
        }
    }

    private void lerProposicoes(BaseDados base, Path arquivo) throws ArquivoInvalidoException {
        if (!Files.exists(arquivo)) {
            return;
        }
        try (LeitorCsv csv = new LeitorCsv(arquivo, StandardCharsets.UTF_8)) {
            int iDep = csv.indice("idDeputado");
            int iId = csv.indice("idProposicao");
            int iTipo = csv.indice("siglaTipo");
            int iNum = csv.indice("numero");
            int iAno = csv.indice("ano");
            int iSit = csv.indice("situacao");
            int iAprov = csv.indice("aprovada");
            int iEmenta = csv.indice("ementa");
            String[] l;
            while ((l = csv.proximaLinha()) != null) {
                Integer id = Texto.parseInteiro(LeitorCsv.campo(l, iDep));
                Integer ano = Texto.parseInteiro(LeitorCsv.campo(l, iAno));
                if (id == null) {
                    continue;
                }
                base.adicionarProposicao(new Proposicao(id, LeitorCsv.campo(l, iId), LeitorCsv.campo(l, iTipo),
                        LeitorCsv.campo(l, iNum), ano == null ? 0 : ano, LeitorCsv.campo(l, iSit),
                        Boolean.parseBoolean(LeitorCsv.campo(l, iAprov)), LeitorCsv.campo(l, iEmenta)));
            }
        }
    }

    /** Lê as posições que o usuário marcou (arquivo opcional). Ignora votações que não existem na base. */
    public void carregarPosicoes(BaseDados base) throws ArquivoInvalidoException {
        for (PosicaoUsuario p : lerPosicoes(base.getDiretorio())) {
            if (base.getVotacao(p.getIdVotacao()) != null) {
                base.definirPosicao(p);
            }
        }
    }

    /** Posições gravadas em posicoes.csv no diretório; lista vazia se o arquivo não existir. */
    public List<PosicaoUsuario> lerPosicoes(Path dir) throws ArquivoInvalidoException {
        List<PosicaoUsuario> posicoes = new ArrayList<>();
        Path arquivo = dir.resolve(POSICOES);
        if (!Files.exists(arquivo)) {
            return posicoes;
        }
        try (LeitorCsv csv = new LeitorCsv(arquivo, StandardCharsets.UTF_8)) {
            int iVot = csv.indice("idVotacao");
            int iVoto = csv.indice("voto");
            int iTema = csv.indiceOpcional("tema");
            String[] l;
            while ((l = csv.proximaLinha()) != null) {
                try {
                    posicoes.add(new PosicaoUsuario(LeitorCsv.campo(l, iVot), LeitorCsv.campo(l, iVoto),
                            LeitorCsv.campo(l, iTema)));
                } catch (IllegalArgumentException e) {
                    // linha editada à mão com valor inválido: ignora só essa posição
                }
            }
        }
        return posicoes;
    }

    // ------------------------------------------------------------------ gravação

    public void salvar(BaseDados base, Path dir) throws DadosException {
        base.setDiretorio(dir);
        salvarMetadados(base.getMetadados(), dir.resolve(META));

        try (EscritorCsv csv = new EscritorCsv(dir.resolve(DEPUTADOS),
                "id", "nomeParlamentar", "nomeCivil", "partido", "uf", "dataNascimento", "genero", "urlFoto")) {
            for (Deputado d : base.getDeputados()) {
                csv.escrever(d.getId(), d.getNomeParlamentar(), d.getNome(), d.getPartido(), d.getUf(),
                        d.getDataNascimento(), d.getGenero(), d.getUrlFoto());
            }
        }
        try (EscritorCsv csv = new EscritorCsv(dir.resolve(EXERCICIOS), "idDeputado", "inicio", "fim")) {
            for (Deputado d : base.getDeputados()) {
                for (LocalDate[] p : d.getExercicios()) {
                    csv.escrever(d.getId(), p[0], p[1]);
                }
            }
        }
        try (EscritorCsv csv = new EscritorCsv(dir.resolve(CANDIDATOS), "sq", "nome", "nomeUrna", "numero", "partido",
                "uf", "cargo", "dataNascimento", "genero", "grauInstrucao", "corRaca", "ocupacao", "situacao",
                "patrimonio", "patrimonioAnterior", "idDeputado", "criterioVinculo", "detalheSituacao", "reeleicao")) {
            for (Candidato c : base.getCandidatos()) {
                csv.escrever(c.getSq(), c.getNome(), c.getNomeUrna(), c.getNumero(), c.getPartido(), c.getUf(),
                        c.getCargo(), c.getDataNascimento(), c.getGenero(), c.getGrauInstrucao().name(),
                        c.getCorRaca(), c.getOcupacao(), c.getSituacao(), c.getPatrimonio(),
                        c.getPatrimonioAnterior(), c.getIdDeputado(), c.getCriterioVinculo(), c.getDetalheSituacao(),
                        c.getReeleicao());
            }
        }
        try (EscritorCsv csv = new EscritorCsv(dir.resolve(TRAJETORIA), "sq", "ano", "cargo", "local", "partido",
                "resultado")) {
            for (Candidato c : base.getCandidatos()) {
                for (CandidaturaAnterior t : c.getTrajetoria()) {
                    csv.escrever(c.getSq(), t.getAno(), t.getCargo(), t.getLocal(), t.getPartido(), t.getResultado());
                }
            }
        }
        try (EscritorCsv csv = new EscritorCsv(dir.resolve(VOTACOES), "id", "data", "descricao", "proposicao",
                "ementa")) {
            for (Votacao v : base.getVotacoesOrdenadas()) {
                csv.escrever(v.getId(), v.getData(), v.getDescricao(), v.getProposicao(), v.getEmenta());
            }
        }
        try (EscritorCsv csv = new EscritorCsv(dir.resolve(VOTOS), "idVotacao", "idDeputado", "voto")) {
            for (Map.Entry<Integer, Map<String, String>> dep : base.getTodosVotos().entrySet()) {
                for (Map.Entry<String, String> v : dep.getValue().entrySet()) {
                    csv.escrever(v.getKey(), dep.getKey(), v.getValue());
                }
            }
        }
        try (EscritorCsv csv = new EscritorCsv(dir.resolve(DESPESAS), "idDeputado", "ano", "mes", "categoria", "valor")) {
            for (List<Despesa> lista : base.getTodasDespesas().values()) {
                for (Despesa d : lista) {
                    csv.escrever(d.getIdDeputado(), d.getAno(), d.getMes(), d.getCategoria(),
                            String.format(java.util.Locale.ROOT, "%.2f", d.getValor()));
                }
            }
        }
        try (EscritorCsv csv = new EscritorCsv(dir.resolve(PROPOSICOES), "idDeputado", "idProposicao", "siglaTipo",
                "numero", "ano", "situacao", "aprovada", "ementa")) {
            for (List<Proposicao> lista : base.getTodasProposicoes().values()) {
                for (Proposicao p : lista) {
                    csv.escrever(p.getIdDeputado(), p.getId(), p.getSiglaTipo(), p.getNumero(), p.getAno(),
                            p.getSituacao(), p.isAprovada(), p.getEmenta());
                }
            }
        }
        salvarPosicoes(base);
    }

    public void salvarPosicoes(BaseDados base) throws ArquivoInvalidoException {
        try (EscritorCsv csv = new EscritorCsv(base.getDiretorio().resolve(POSICOES), "idVotacao", "voto", "tema")) {
            for (PosicaoUsuario p : base.getPosicoes()) {
                csv.escrever(p.getIdVotacao(), p.getVoto(), p.getTema());
            }
        }
    }

    private void salvarMetadados(Metadados m, Path arquivo) throws ArquivoInvalidoException {
        Properties p = new Properties();
        p.setProperty("uf", m.getUf());
        p.setProperty("anoEleicao", String.valueOf(m.getAnoEleicao()));
        p.setProperty("anoAnterior", String.valueOf(m.getAnoAnterior()));
        LocalDate data = m.getDataEleicao();
        p.setProperty("dataEleicao", data == null ? "" : data.toString());
        p.setProperty("demonstracao", String.valueOf(m.isDemonstracao()));
        p.setProperty("descricao", m.getDescricao() == null ? "" : m.getDescricao());
        p.setProperty("geradoEm", m.getGeradoEm() == null ? "" : m.getGeradoEm());
        try {
            Files.createDirectories(arquivo.getParent());
            try (Writer w = Files.newBufferedWriter(arquivo, StandardCharsets.UTF_8)) {
                p.store(w, "Metadados da base processada");
            }
        } catch (IOException e) {
            throw new ArquivoInvalidoException("Não foi possível gravar " + arquivo + ": " + e.getMessage(), e);
        }
    }
}
