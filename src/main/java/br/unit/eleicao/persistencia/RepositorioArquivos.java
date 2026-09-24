package br.unit.eleicao.persistencia;

import br.unit.eleicao.excecao.ArquivoInvalidoException;
import br.unit.eleicao.excecao.DadosException;
import br.unit.eleicao.modelo.BaseDados;
import br.unit.eleicao.modelo.Candidato;
import br.unit.eleicao.modelo.CandidaturaAnterior;
import br.unit.eleicao.modelo.ContaIrregular;
import br.unit.eleicao.modelo.Financiamento;
import br.unit.eleicao.modelo.IndicadorFiscal;
import br.unit.eleicao.modelo.ResumoEmendas;
import br.unit.eleicao.modelo.Sancao;
import br.unit.eleicao.modelo.VinculoEmpresa;
import br.unit.eleicao.modelo.VinculoServidor;
import br.unit.eleicao.modelo.ResumoMandato;
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
    public static final String ATUACAO = "atuacao.csv";
    public static final String CONTAS = "contas_irregulares.csv";
    public static final String FINANCIAMENTO = "financiamento.csv";
    public static final String EMENDAS = "emendas.csv";
    public static final String EMPRESAS = "empresas.csv";
    public static final String SANCOES = "sancoes.csv";
    public static final String SERVIDOR = "servidor_federal.csv";
    public static final String GESTAO_FISCAL = "gestao_fiscal.csv";
    private static final String SEPARADOR_DESTAQUES = " || ";
    /** Quantos locais/áreas das emendas são guardados por parlamentar (os de maior valor pago). */
    private static final int MAIORES_EMENDAS = 8;

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
        lerAtuacao(base, dir.resolve(ATUACAO));
        lerContas(base, dir.resolve(CONTAS));
        lerFinanciamento(base, dir.resolve(FINANCIAMENTO));
        lerEmendas(base, dir.resolve(EMENDAS));
        lerEmpresas(base, dir.resolve(EMPRESAS));
        lerSancoes(base, dir.resolve(SANCOES));
        lerServidor(base, dir.resolve(SERVIDOR));
        lerGestaoFiscal(base, dir.resolve(GESTAO_FISCAL));
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
        m.setTcuVerificado(Boolean.parseBoolean(p.getProperty("tcuVerificado", "false")));
        for (String fonte : p.getProperty("fontesVerificadas", "").split(",")) {
            if (!fonte.isBlank()) {
                m.marcarVerificada(fonte.strip());
            }
        }
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
            int iSqOrigem = csv.indiceOpcional("sqOrigem");
            int iUe = csv.indiceOpcional("codigoUe");
            int iPat = csv.indiceOpcional("patrimonio");
            int iCass = csv.indiceOpcional("motivoCassacao");
            String[] l;
            while ((l = csv.proximaLinha()) != null) {
                Candidato c = base.buscarCandidato(LeitorCsv.campo(l, iSq));
                Integer ano = Texto.parseInteiro(LeitorCsv.campo(l, iAno));
                if (c != null && ano != null) {
                    CandidaturaAnterior t = new CandidaturaAnterior(ano, LeitorCsv.campo(l, iCargo),
                            LeitorCsv.campo(l, iLocal), LeitorCsv.campo(l, iPartido), LeitorCsv.campo(l, iResultado));
                    t.setSqOrigem(LeitorCsv.campo(l, iSqOrigem));
                    t.setCodigoUe(LeitorCsv.campo(l, iUe));
                    t.setPatrimonio(Texto.parseDecimal(LeitorCsv.campo(l, iPat)));
                    t.setMotivoCassacao(LeitorCsv.campo(l, iCass));
                    c.adicionarCandidaturaAnterior(t);
                }
            }
        }
    }

    private void lerAtuacao(BaseDados base, Path arquivo) throws ArquivoInvalidoException {
        if (!Files.exists(arquivo)) {
            return;
        }
        try (LeitorCsv csv = new LeitorCsv(arquivo, StandardCharsets.UTF_8)) {
            int iSq = csv.indice("sq");
            int iCasa = csv.indice("casa");
            int iCargo = csv.indice("cargo");
            int iPeriodo = csv.indice("periodo");
            int iUrl = csv.indice("url");
            int iPres = csv.indice("presenca");
            int iDetPres = csv.indice("detalhePresenca");
            int iProj = csv.indice("projetos");
            int iAprov = csv.indice("aprovados");
            int iObs = csv.indice("observacaoProjetos");
            int iGasto = csv.indice("gastoMensal");
            int iDest = csv.indice("destaques");
            String[] l;
            while ((l = csv.proximaLinha()) != null) {
                Candidato c = base.buscarCandidato(LeitorCsv.campo(l, iSq));
                if (c == null) {
                    continue;
                }
                ResumoMandato r = new ResumoMandato(LeitorCsv.campo(l, iCasa), LeitorCsv.campo(l, iCargo),
                        LeitorCsv.campo(l, iPeriodo), LeitorCsv.campo(l, iUrl));
                r.setPresenca(Texto.parseDecimal(LeitorCsv.campo(l, iPres)), LeitorCsv.campo(l, iDetPres));
                Integer proj = Texto.parseInteiro(LeitorCsv.campo(l, iProj));
                Integer aprov = Texto.parseInteiro(LeitorCsv.campo(l, iAprov));
                r.setProjetos(proj == null ? 0 : proj, aprov == null ? 0 : aprov);
                r.setObservacaoProjetos(LeitorCsv.campo(l, iObs));
                r.setGastoMensal(Texto.parseDecimal(LeitorCsv.campo(l, iGasto)));
                for (String d : LeitorCsv.campo(l, iDest).split(java.util.regex.Pattern.quote(SEPARADOR_DESTAQUES))) {
                    if (!d.isBlank()) {
                        r.adicionarDestaque(d);
                    }
                }
                c.adicionarAtuacao(r);
            }
        }
    }

    private void lerContas(BaseDados base, Path arquivo) throws ArquivoInvalidoException {
        if (!Files.exists(arquivo)) {
            return;
        }
        try (LeitorCsv csv = new LeitorCsv(arquivo, StandardCharsets.UTF_8)) {
            int iSq = csv.indice("sq");
            int iProc = csv.indice("processo");
            int iDelib = csv.indice("deliberacao");
            int iTrans = csv.indice("transito");
            int iLocal = csv.indice("local");
            int iCrit = csv.indice("criterio");
            String[] l;
            while ((l = csv.proximaLinha()) != null) {
                Candidato c = base.buscarCandidato(LeitorCsv.campo(l, iSq));
                if (c != null) {
                    c.adicionarContaIrregular(new ContaIrregular(LeitorCsv.campo(l, iProc), LeitorCsv.campo(l, iDelib),
                            LeitorCsv.campo(l, iTrans), LeitorCsv.campo(l, iLocal), LeitorCsv.campo(l, iCrit)));
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

    private void lerFinanciamento(BaseDados base, Path arquivo) throws ArquivoInvalidoException {
        if (!Files.exists(arquivo)) {
            return;
        }
        try (LeitorCsv csv = new LeitorCsv(arquivo, StandardCharsets.UTF_8)) {
            int iSq = csv.indice("sq");
            int iOrigem = csv.indice("origem");
            int iValor = csv.indice("valor");
            String[] l;
            while ((l = csv.proximaLinha()) != null) {
                Candidato c = base.buscarCandidato(LeitorCsv.campo(l, iSq));
                Double v = Texto.parseDecimal(LeitorCsv.campo(l, iValor));
                if (c == null || v == null) {
                    continue;
                }
                if (c.getFinanciamento() == null) {
                    c.setFinanciamento(new Financiamento());
                }
                c.getFinanciamento().somar(LeitorCsv.campo(l, iOrigem), v);
            }
        }
    }

    private void lerEmendas(BaseDados base, Path arquivo) throws ArquivoInvalidoException {
        if (!Files.exists(arquivo)) {
            return;
        }
        try (LeitorCsv csv = new LeitorCsv(arquivo, StandardCharsets.UTF_8)) {
            int iSq = csv.indice("sq");
            int iTipo = csv.indice("tipo");
            int iChave = csv.indice("chave");
            int iPago = csv.indice("pago");
            int iEmp = csv.indice("empenhado");
            int iEsp = csv.indice("pagoEspecial");
            int iQtd = csv.indice("quantidade");
            String[] l;
            while ((l = csv.proximaLinha()) != null) {
                Candidato c = base.buscarCandidato(LeitorCsv.campo(l, iSq));
                if (c == null) {
                    continue;
                }
                String tipo = LeitorCsv.campo(l, iTipo);
                String chave = LeitorCsv.campo(l, iChave);
                double pago = valor(LeitorCsv.campo(l, iPago));
                if ("TOTAL".equals(tipo)) {
                    ResumoEmendas r = new ResumoEmendas(chave);
                    Integer qtd = Texto.parseInteiro(LeitorCsv.campo(l, iQtd));
                    r.restaurar(valor(LeitorCsv.campo(l, iEmp)), pago, valor(LeitorCsv.campo(l, iEsp)),
                            qtd == null ? 0 : qtd);
                    c.setEmendas(r);
                } else if (c.getEmendas() != null) {
                    if ("ANO".equals(tipo) && Texto.parseInteiro(chave) != null) {
                        c.getEmendas().restaurarAno(Texto.parseInteiro(chave), pago);
                    } else if ("LOCAL".equals(tipo)) {
                        c.getEmendas().restaurarLocal(chave, pago);
                    } else if ("AREA".equals(tipo)) {
                        c.getEmendas().restaurarArea(chave, pago);
                    }
                }
            }
        }
    }

    private static double valor(String s) {
        Double d = Texto.parseDecimal(s);
        return d == null ? 0 : d;
    }

    private void lerEmpresas(BaseDados base, Path arquivo) throws ArquivoInvalidoException {
        if (!Files.exists(arquivo)) {
            return;
        }
        try (LeitorCsv csv = new LeitorCsv(arquivo, StandardCharsets.UTF_8)) {
            int iSq = csv.indice("sq");
            int iCnpj = csv.indice("cnpj");
            int iRazao = csv.indice("razaoSocial");
            int iQualif = csv.indice("qualificacao");
            int iEntrada = csv.indice("dataEntrada");
            String[] l;
            while ((l = csv.proximaLinha()) != null) {
                Candidato c = base.buscarCandidato(LeitorCsv.campo(l, iSq));
                if (c != null) {
                    c.adicionarEmpresa(new VinculoEmpresa(LeitorCsv.campo(l, iCnpj), LeitorCsv.campo(l, iRazao),
                            LeitorCsv.campo(l, iQualif), LeitorCsv.campo(l, iEntrada)));
                }
            }
        }
    }

    private void lerSancoes(BaseDados base, Path arquivo) throws ArquivoInvalidoException {
        if (!Files.exists(arquivo)) {
            return;
        }
        try (LeitorCsv csv = new LeitorCsv(arquivo, StandardCharsets.UTF_8)) {
            int iSq = csv.indice("sq");
            int iCad = csv.indice("cadastro");
            int iNome = csv.indice("sancionado");
            int iCat = csv.indice("categoria");
            int iOrgao = csv.indice("orgao");
            int iIni = csv.indice("inicio");
            int iFim = csv.indice("fim");
            int iEmp = csv.indice("sobreEmpresa");
            String[] l;
            while ((l = csv.proximaLinha()) != null) {
                Candidato c = base.buscarCandidato(LeitorCsv.campo(l, iSq));
                if (c != null) {
                    c.adicionarSancao(new Sancao(LeitorCsv.campo(l, iCad), LeitorCsv.campo(l, iNome),
                            LeitorCsv.campo(l, iCat), LeitorCsv.campo(l, iOrgao), LeitorCsv.campo(l, iIni),
                            LeitorCsv.campo(l, iFim), Boolean.parseBoolean(LeitorCsv.campo(l, iEmp))));
                }
            }
        }
    }

    private void lerServidor(BaseDados base, Path arquivo) throws ArquivoInvalidoException {
        if (!Files.exists(arquivo)) {
            return;
        }
        try (LeitorCsv csv = new LeitorCsv(arquivo, StandardCharsets.UTF_8)) {
            int iSq = csv.indice("sq");
            int iCargo = csv.indice("cargo");
            int iOrgao = csv.indice("orgao");
            int iSit = csv.indice("situacao");
            int iIng = csv.indice("ingresso");
            String[] l;
            while ((l = csv.proximaLinha()) != null) {
                Candidato c = base.buscarCandidato(LeitorCsv.campo(l, iSq));
                if (c != null) {
                    c.adicionarVinculoServidor(new VinculoServidor(LeitorCsv.campo(l, iCargo),
                            LeitorCsv.campo(l, iOrgao), LeitorCsv.campo(l, iSit), LeitorCsv.campo(l, iIng)));
                }
            }
        }
    }

    private void lerGestaoFiscal(BaseDados base, Path arquivo) throws ArquivoInvalidoException {
        if (!Files.exists(arquivo)) {
            return;
        }
        try (LeitorCsv csv = new LeitorCsv(arquivo, StandardCharsets.UTF_8)) {
            int iSq = csv.indice("sq");
            int iEnte = csv.indice("ente");
            int iAno = csv.indice("ano");
            int iPessoal = csv.indice("pessoalRcl");
            int iLimite = csv.indice("limite");
            int iDurante = csv.indice("duranteMandato");
            String[] l;
            while ((l = csv.proximaLinha()) != null) {
                Candidato c = base.buscarCandidato(LeitorCsv.campo(l, iSq));
                Integer ano = Texto.parseInteiro(LeitorCsv.campo(l, iAno));
                Double pessoal = Texto.parseDecimal(LeitorCsv.campo(l, iPessoal));
                if (c != null && ano != null && pessoal != null) {
                    c.adicionarIndicadorFiscal(new IndicadorFiscal(LeitorCsv.campo(l, iEnte), ano, pessoal,
                            Texto.parseDecimal(LeitorCsv.campo(l, iLimite)),
                            Boolean.parseBoolean(LeitorCsv.campo(l, iDurante))));
                }
            }
        }
    }

    private void salvarComplementos(BaseDados base, Path dir) throws DadosException {
        try (EscritorCsv csv = new EscritorCsv(dir.resolve(FINANCIAMENTO), "sq", "origem", "valor")) {
            for (Candidato c : base.getCandidatos()) {
                if (c.getFinanciamento() != null) {
                    for (Map.Entry<String, Double> e : c.getFinanciamento().getPorOrigem().entrySet()) {
                        csv.escrever(c.getSq(), e.getKey(), dinheiro(e.getValue()));
                    }
                }
            }
        }
        try (EscritorCsv csv = new EscritorCsv(dir.resolve(EMENDAS), "sq", "tipo", "chave", "pago", "empenhado",
                "pagoEspecial", "quantidade")) {
            for (Candidato c : base.getCandidatos()) {
                ResumoEmendas r = c.getEmendas();
                if (r == null) {
                    continue;
                }
                csv.escrever(c.getSq(), "TOTAL", r.getAutor(), dinheiro(r.getPago()), dinheiro(r.getEmpenhado()),
                        dinheiro(r.getPagoTransferenciaEspecial()), r.getQuantidade());
                for (Map.Entry<Integer, Double> e : r.getPagoPorAno().entrySet()) {
                    csv.escrever(c.getSq(), "ANO", e.getKey(), dinheiro(e.getValue()), "", "", "");
                }
                for (Map.Entry<String, Double> e : ResumoEmendas.maiores(r.getPagoPorLocal(), MAIORES_EMENDAS)) {
                    csv.escrever(c.getSq(), "LOCAL", e.getKey(), dinheiro(e.getValue()), "", "", "");
                }
                for (Map.Entry<String, Double> e : ResumoEmendas.maiores(r.getPagoPorArea(), MAIORES_EMENDAS)) {
                    csv.escrever(c.getSq(), "AREA", e.getKey(), dinheiro(e.getValue()), "", "", "");
                }
            }
        }
        try (EscritorCsv csv = new EscritorCsv(dir.resolve(EMPRESAS), "sq", "cnpj", "razaoSocial", "qualificacao",
                "dataEntrada")) {
            for (Candidato c : base.getCandidatos()) {
                for (VinculoEmpresa e : c.getEmpresas()) {
                    csv.escrever(c.getSq(), e.getCnpj(), e.getRazaoSocial(), e.getQualificacao(), e.getDataEntrada());
                }
            }
        }
        try (EscritorCsv csv = new EscritorCsv(dir.resolve(SANCOES), "sq", "cadastro", "sancionado", "categoria",
                "orgao", "inicio", "fim", "sobreEmpresa")) {
            for (Candidato c : base.getCandidatos()) {
                for (Sancao s : c.getSancoes()) {
                    csv.escrever(c.getSq(), s.getCadastro(), s.getSancionado(), s.getCategoria(), s.getOrgao(),
                            s.getInicio(), s.getFim(), s.isSobreEmpresa());
                }
            }
        }
        try (EscritorCsv csv = new EscritorCsv(dir.resolve(SERVIDOR), "sq", "cargo", "orgao", "situacao", "ingresso")) {
            for (Candidato c : base.getCandidatos()) {
                for (VinculoServidor v : c.getVinculosServidor()) {
                    csv.escrever(c.getSq(), v.getCargo(), v.getOrgao(), v.getSituacao(), v.getIngresso());
                }
            }
        }
        try (EscritorCsv csv = new EscritorCsv(dir.resolve(GESTAO_FISCAL), "sq", "ente", "ano", "pessoalRcl", "limite",
                "duranteMandato")) {
            for (Candidato c : base.getCandidatos()) {
                for (IndicadorFiscal i : c.getGestaoFiscal()) {
                    csv.escrever(c.getSq(), i.getEnte(), i.getAno(), String.format(java.util.Locale.ROOT, "%.2f",
                            i.getPessoalRcl()), i.getLimite(), i.isDuranteMandato());
                }
            }
        }
    }

    private static String dinheiro(double v) {
        return String.format(java.util.Locale.ROOT, "%.2f", v);
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
                "resultado", "sqOrigem", "codigoUe", "patrimonio", "motivoCassacao")) {
            for (Candidato c : base.getCandidatos()) {
                for (CandidaturaAnterior t : c.getTrajetoria()) {
                    csv.escrever(c.getSq(), t.getAno(), t.getCargo(), t.getLocal(), t.getPartido(), t.getResultado(),
                            t.getSqOrigem(), t.getCodigoUe(), t.getPatrimonio(), t.getMotivoCassacao());
                }
            }
        }
        salvarComplementos(base, dir);
        try (EscritorCsv csv = new EscritorCsv(dir.resolve(ATUACAO), "sq", "casa", "cargo", "periodo", "url",
                "presenca", "detalhePresenca", "projetos", "aprovados", "observacaoProjetos", "gastoMensal", "destaques")) {
            for (Candidato c : base.getCandidatos()) {
                for (ResumoMandato r : c.getAtuacao()) {
                    csv.escrever(c.getSq(), r.getCasa(), r.getCargo(), r.getPeriodo(), r.getUrl(), r.getPresenca(),
                            r.getDetalhePresenca(), r.getProjetos(), r.getAprovados(), r.getObservacaoProjetos(),
                            r.getGastoMensal(), String.join(SEPARADOR_DESTAQUES, r.getDestaques()));
                }
            }
        }
        try (EscritorCsv csv = new EscritorCsv(dir.resolve(CONTAS), "sq", "processo", "deliberacao", "transito",
                "local", "criterio")) {
            for (Candidato c : base.getCandidatos()) {
                for (ContaIrregular ci : c.getContasIrregulares()) {
                    csv.escrever(c.getSq(), ci.getProcesso(), ci.getDeliberacao(), ci.getDataTransito(), ci.getLocal(),
                            ci.getCriterio());
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
        p.setProperty("tcuVerificado", String.valueOf(m.isTcuVerificado()));
        p.setProperty("fontesVerificadas", String.join(",", m.getFontesVerificadas()));
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
