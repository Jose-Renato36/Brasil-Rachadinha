package br.unit.eleicao.web;

import br.unit.eleicao.coleta.GeradorDadosDemo;
import br.unit.eleicao.coleta.PipelineColeta;
import br.unit.eleicao.excecao.DadosException;
import br.unit.eleicao.indicador.AlinhamentoPauta;
import br.unit.eleicao.indicador.CatalogoIndicadores;
import br.unit.eleicao.indicador.Indicador;
import br.unit.eleicao.indicador.ProducaoLegislativa;
import br.unit.eleicao.indicador.ResultadoIndicador;
import br.unit.eleicao.modelo.BaseDados;
import br.unit.eleicao.modelo.Candidato;
import br.unit.eleicao.modelo.CandidaturaAnterior;
import br.unit.eleicao.modelo.Cargo;
import br.unit.eleicao.modelo.ContaIrregular;
import br.unit.eleicao.modelo.ResumoMandato;
import br.unit.eleicao.coleta.MandatoAnteriorCamara;
import br.unit.eleicao.modelo.Deputado;
import br.unit.eleicao.modelo.Elegibilidade;
import br.unit.eleicao.modelo.GrauInstrucao;
import br.unit.eleicao.modelo.Metadados;
import br.unit.eleicao.modelo.PosicaoUsuario;
import br.unit.eleicao.modelo.Proposicao;
import br.unit.eleicao.modelo.Votacao;
import br.unit.eleicao.persistencia.RepositorioArquivos;
import br.unit.eleicao.ranking.ConfiguracaoRanking;
import br.unit.eleicao.ranking.ItemRanking;
import br.unit.eleicao.ranking.MatrizIndicadores;
import br.unit.eleicao.ranking.MetodoRanking;
import br.unit.eleicao.ranking.SomaPonderada;
import br.unit.eleicao.ranking.Topsis;
import br.unit.eleicao.util.Estatistica;
import br.unit.eleicao.util.Texto;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Regras da API: recebe parâmetros simples (texto) e devolve mapas e listas que viram JSON.
 * Não sabe nada de HTTP; o {@link ServidorWeb} só faz a ponte. Os métodos são synchronized
 * porque o servidor atende vários pedidos ao mesmo tempo e a base carregada é compartilhada.
 */
public class ServicoApi {

    private final Path raiz;
    private final List<Indicador> indicadores = CatalogoIndicadores.todos();
    private final RepositorioArquivos repositorio = new RepositorioArquivos();
    private BaseDados base;
    private MatrizIndicadores matriz;
    private String nomeBase;

    private final List<String> logColeta = Collections.synchronizedList(new ArrayList<>());
    private volatile boolean coletando;
    private volatile String erroColeta;

    public ServicoApi(Path raiz) {
        this.raiz = raiz;
    }

    // ------------------------------------------------------------------ base

    /** Abre "demo" (gerada na hora se não existir) ou a UF já processada (ex.: "SE"). */
    public synchronized Map<String, Object> abrir(String nome) throws DadosException {
        Path pasta;
        if ("demo".equalsIgnoreCase(nome)) {
            pasta = raiz.resolve("demo");
            if (!RepositorioArquivos.contemBase(pasta)) {
                new GeradorDadosDemo().gerarESalvar(pasta);
            }
        } else {
            pasta = new PipelineColeta(raiz, s -> { }).pastaProcessada(nome);
        }
        base = repositorio.carregar(pasta);
        matriz = new MatrizIndicadores(base, indicadores);
        nomeBase = "demo".equalsIgnoreCase(nome) ? "demo" : nome.toUpperCase();
        return estado();
    }

    private void exigirBase() throws DadosException {
        if (base == null) {
            throw new DadosException("Nenhuma base carregada. Abra a demonstração ou colete uma UF na aba Dados.");
        }
    }

    public synchronized Map<String, Object> estado() {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("carregada", base != null);
        r.put("basesDisponiveis", basesDisponiveis());
        List<Object> inds = new ArrayList<>();
        for (Indicador ind : indicadores) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("codigo", ind.getCodigo());
            m.put("nome", ind.getNome());
            m.put("titulo", ind.getTituloSimples());
            m.put("pergunta", ind.getPergunta());
            m.put("descricao", ind.getDescricao());
            m.put("fonte", ind.getFonte());
            m.put("sentido", ind.getSentidoPadrao().toString());
            inds.add(m);
        }
        r.put("indicadores", inds);
        if (base != null) {
            Metadados meta = base.getMetadados();
            r.put("base", nomeBase);
            r.put("uf", meta.getUf());
            r.put("demo", meta.isDemonstracao());
            r.put("descricao", meta.getDescricao());
            r.put("geradoEm", meta.getGeradoEm());
            r.put("tcuVerificado", meta.isTcuVerificado());
            List<Integer> anosTraj = new ArrayList<>();
            for (int a : br.unit.eleicao.coleta.ProcessadorTrajetoria.anosAnteriores(meta.getAnoEleicao())) {
                anosTraj.add(a);
            }
            r.put("anosTrajetoria", anosTraj);
            r.put("anoEleicao", meta.getAnoEleicao());
            r.put("anoAnterior", meta.getAnoAnterior());
            r.put("candidatos", base.getCandidatos().size());
            r.put("comMandato", base.getCandidatos().stream().filter(Candidato::temMandatoNaCamara).count());
            r.put("deputados", base.getDeputados().size());
            r.put("votacoes", base.getTotalVotacoes());
            r.put("posicoes", base.getPosicoes().size());
            Map<Cargo, Integer> contagem = new java.util.TreeMap<>();
            for (Candidato c : base.getCandidatos()) {
                contagem.merge(c.getTipoCargo(), 1, Integer::sum);
            }
            List<Object> cargos = new ArrayList<>();
            for (Map.Entry<Cargo, Integer> e : contagem.entrySet()) {
                Map<String, Object> cm = new LinkedHashMap<>();
                cm.put("codigo", e.getKey().name());
                cm.put("rotulo", e.getKey().getRotulo());
                cm.put("orgao", e.getKey().getOrgao());
                cm.put("principal", e.getKey().isPrincipal());
                cm.put("total", e.getValue());
                cargos.add(cm);
            }
            r.put("cargos", cargos);
            r.put("nacional", br.unit.eleicao.coleta.ArquivosBrutos.isNacional(meta.getUf()));
            // UF -> cargo -> total (para o seletor de estado e os cartões de cargo)
            Map<String, Map<String, Integer>> porUf = new java.util.TreeMap<>();
            for (Candidato c : base.getCandidatos()) {
                porUf.computeIfAbsent(c.getUf() == null ? "?" : c.getUf(), k -> new LinkedHashMap<>())
                        .merge(c.getTipoCargo().name(), 1, Integer::sum);
            }
            r.put("cargosPorUf", porUf);
        }
        return r;
    }

    private List<String> basesDisponiveis() {
        List<String> lista = new ArrayList<>();
        lista.add("demo");
        Path processados = raiz.resolve("processados");
        if (Files.isDirectory(processados)) {
            try (Stream<Path> pastas = Files.list(processados)) {
                pastas.filter(RepositorioArquivos::contemBase).map(p -> p.getFileName().toString()).sorted()
                        .forEach(lista::add);
            } catch (IOException e) {
                // pasta ilegível: oferece só a demonstração
            }
        }
        return lista;
    }

    // ------------------------------------------------------------------ ranking

    public synchronized Map<String, Object> ranking(Map<String, String> p) throws DadosException {
        exigirBase();
        ConfiguracaoRanking config = new ConfiguracaoRanking();
        for (Indicador ind : indicadores) {
            int peso = inteiro(p.get("peso." + ind.getCodigo()), AlinhamentoPauta.CODIGO.equals(ind.getCodigo()) ? 0 : 5);
            config.setPeso(ind.getCodigo(), Math.max(0, Math.min(ConfiguracaoRanking.PESO_MAXIMO, peso)));
            config.setInvertido(ind.getCodigo(), "1".equals(p.get("inv." + ind.getCodigo())));
        }
        config.setOcultarInaptos(!"1".equals(p.get("inaptos")));
        config.setCoberturaMinima(Math.max(1, inteiro(p.get("cobertura"), 1)));
        config.setCargo(cargo(p.get("cargo")));
        config.setUf(uf(p.get("uf")));
        MetodoRanking metodo = "topsis".equals(p.get("metodo")) ? new Topsis() : new SomaPonderada();

        List<Object> itens = new ArrayList<>();
        for (ItemRanking item : metodo.classificar(matriz, config)) {
            Map<String, Object> m = resumoCandidato(item.getCandidato());
            m.put("posicao", item.getPosicao());
            m.put("pontuacao", item.getPontuacao());
            m.put("cobertura", item.getIndicadoresUsados());
            m.put("coberturaPedida", item.getIndicadoresPedidos());
            m.put("indicadores", valoresIndicadores(item.getCandidato(), item.getNotas()));
            itens.add(m);
        }
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("metodo", metodo.getNome());
        r.put("explicacaoMetodo", metodo.getDescricao());
        r.put("itens", itens);
        return r;
    }

    /** Valor bruto, texto e nota normalizada (0 a 1, quando usada) de cada indicador. */
    private Map<String, Object> valoresIndicadores(Candidato c, Map<String, Double> notas) {
        Map<String, Object> r = new LinkedHashMap<>();
        for (Indicador ind : indicadores) {
            ResultadoIndicador res = matriz.get(ind.getCodigo(), c);
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("temDados", res != null && res.temDados());
            m.put("valor", res != null && res.temDados() ? res.getValor() : null);
            m.put("texto", ind.formatar(res));
            m.put("resumo", ind.resumir(res));
            m.put("detalhe", res == null ? "" : res.getDetalhe());
            m.put("nota", notas == null ? null : notas.get(ind.getCodigo()));
            r.put(ind.getCodigo(), m);
        }
        return r;
    }

    private Map<String, Object> resumoCandidato(Candidato c) {
        Map<String, Object> m = new LinkedHashMap<>();
        Deputado d = base.getDeputadoDe(c);
        m.put("sq", c.getSq());
        m.put("nome", c.getNomeExibicao());
        m.put("nomeCivil", c.getNome());
        m.put("partido", c.getPartido());
        m.put("uf", c.getUf());
        m.put("numero", c.getNumero());
        m.put("foto", d == null ? null : d.getUrlFoto());
        m.put("mandato", d != null);
        m.put("reeleicao", c.getReeleicao());
        m.put("vinculoDuvidoso", c.isVinculoDuvidoso());
        CandidaturaAnterior atual = c.getMandatoAtual(base.getMetadados().getAnoEleicao());
        m.put("mandatoAtual", atual == null ? null : cargoLegivel(atual));
        m.put("jaEleito", c.jaFoiEleito());
        m.put("cargo", c.getTipoCargo().name());
        m.put("cargoTexto", c.getTipoCargo().getRotulo());
        m.put("cargoPrincipal", c.getTipoCargo().isPrincipal());
        String[] exp = experiencia(c);
        m.put("experiencia", exp[0]);
        m.put("experienciaTexto", exp[1]);
        m.put("contasIrregulares", c.getContasIrregulares().size());
        List<Object> linha = new ArrayList<>();
        for (CandidaturaAnterior t : c.getTrajetoria()) {
            Map<String, Object> tm = new LinkedHashMap<>();
            tm.put("ano", t.getAno());
            tm.put("cargoTexto", t.getCargoLegivel());
            tm.put("resultado", t.getResultado());
            tm.put("eleito", t.isEleito());
            linha.add(tm);
        }
        m.put("linhaDoTempo", linha);
        Elegibilidade e = c.getElegibilidade();
        m.put("elegibilidade", e.name());
        m.put("elegibilidadeTexto", e.getRotulo());
        m.put("elegibilidadeExplicacao", e.getExplicacao());
        return m;
    }

    /** Ex.: "Vereador(a) em ARACAJU, eleito(a) em 2024". */
    private static String cargoLegivel(CandidaturaAnterior c) {
        return c.getCargoLegivel() + (c.getLocal().isEmpty() ? "" : " em " + c.getLocal()) + ", eleito(a) em " + c.getAno();
    }

    // ------------------------------------------------------------------ candidatos

    /**
     * Cartões filtrados e paginados no servidor (no Brasil inteiro são dezenas de milhares de candidaturas).
     * Parâmetros: uf, cargo, experiencia, busca, ordem (nome|numero|partido|experiencia), pagina, porPagina.
     */
    public synchronized Map<String, Object> candidatos(Map<String, String> p) throws DadosException {
        exigirBase();
        String termo = Texto.normalizar(p.get("busca"));
        String exp = p.getOrDefault("experiencia", "TODOS");
        Map<String, Integer> contagem = new LinkedHashMap<>();
        List<Candidato> filtrados = new ArrayList<>();
        Map<String, String> expDe = new java.util.HashMap<>();
        for (Candidato c : candidatosDo(p.get("cargo"), p.get("uf"))) {
            if (!termo.isEmpty() && !Texto.normalizar(c.getNomeExibicao() + " " + c.getNome() + " " + c.getPartido()
                    + " " + c.getNumero()).contains(termo)) {
                continue;
            }
            String e = experiencia(c)[0];
            expDe.put(c.getSq(), e);
            contagem.merge("TODOS", 1, Integer::sum);
            contagem.merge(e, 1, Integer::sum);
            if ("TODOS".equals(exp) || exp.equals(e)) {
                filtrados.add(c);
            }
        }
        java.util.Comparator<Candidato> porNome = java.util.Comparator.comparing(
                c -> Texto.normalizar(c.getNomeExibicao()));
        List<String> ordemExp = List.of("MANDATO_ATUAL", "JA_ELEITO", "JA_CONCORREU", "NOVO");
        switch (p.getOrDefault("ordem", "nome")) {
            case "numero":
                filtrados.sort(java.util.Comparator.comparing((Candidato c) -> String.valueOf(c.getNumero()).length())
                        .thenComparing(c -> String.valueOf(c.getNumero())));
                break;
            case "partido":
                filtrados.sort(java.util.Comparator.comparing((Candidato c) -> String.valueOf(c.getPartido()))
                        .thenComparing(porNome));
                break;
            case "experiencia":
                filtrados.sort(java.util.Comparator.comparing((Candidato c) -> ordemExp.indexOf(expDe.get(c.getSq())))
                        .thenComparing(porNome));
                break;
            default:
                filtrados.sort(porNome);
        }
        int porPagina = Math.max(1, Math.min(200, inteiro(p.get("porPagina"), 48)));
        int pagina = Math.max(1, inteiro(p.get("pagina"), 1));
        int inicio = (pagina - 1) * porPagina;
        List<Object> itens = new ArrayList<>();
        for (int i = inicio; i < Math.min(filtrados.size(), inicio + porPagina); i++) {
            itens.add(resumoCandidato(filtrados.get(i)));
        }
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("total", filtrados.size());
        r.put("contagem", contagem);
        r.put("pagina", pagina);
        r.put("temMais", inicio + porPagina < filtrados.size());
        r.put("itens", itens);
        return r;
    }

    /** Busca rápida por nome, nome de urna ou número (para a caixa de busca e a comparação). */
    public synchronized List<Object> busca(String texto, String sigla) throws DadosException {
        exigirBase();
        String termo = Texto.normalizar(texto);
        List<Object> r = new ArrayList<>();
        if (termo.length() < 2) {
            return r;
        }
        String ufPreferida = uf(sigla);
        List<Candidato> achados = new ArrayList<>();
        for (Candidato c : base.getCandidatos()) {
            if (Texto.normalizar(c.getNomeExibicao() + " " + c.getNome()).contains(termo)
                    || String.valueOf(c.getNumero()).startsWith(termo)) {
                achados.add(c);
            }
        }
        // primeiro a UF escolhida, depois cargos mais altos, depois nome
        achados.sort(java.util.Comparator.comparing((Candidato c) -> ufPreferida != null && !ufPreferida.equals(c.getUf()))
                .thenComparing(c -> c.getTipoCargo().getOrdem())
                .thenComparing(c -> Texto.normalizar(c.getNomeExibicao())));
        for (int i = 0; i < Math.min(10, achados.size()); i++) {
            r.add(resumoCandidato(achados.get(i)));
        }
        return r;
    }

    public synchronized Map<String, Object> candidato(String sq) throws DadosException {
        exigirBase();
        Candidato c = base.buscarCandidato(sq);
        if (c == null) {
            throw new DadosException("Candidatura não encontrada: " + sq);
        }
        Map<String, Object> m = resumoCandidato(c);
        Metadados meta = base.getMetadados();
        m.put("idade", c.getIdade(meta.getDataEleicao()));
        m.put("escolaridade", c.getGrauInstrucao().getDescricao());
        m.put("ocupacao", c.getOcupacao());
        m.put("genero", c.getGenero());
        m.put("corRaca", c.getCorRaca());
        m.put("situacao", c.getSituacao());
        m.put("detalheSituacao", c.getDetalheSituacao());
        m.put("patrimonio", c.getPatrimonio());
        m.put("patrimonioAnterior", c.getPatrimonioAnterior());
        m.put("anoEleicao", meta.getAnoEleicao());
        m.put("anoAnterior", meta.getAnoAnterior());
        m.put("indicadores", valoresIndicadores(c, null));
        List<Object> trajetoria = new ArrayList<>();
        for (CandidaturaAnterior t : c.getTrajetoria()) {
            Map<String, Object> tm = new LinkedHashMap<>();
            tm.put("ano", t.getAno());
            tm.put("cargo", t.getCargo());
            tm.put("cargoTexto", t.getCargoLegivel());
            tm.put("tipo", t.getTipoCargo().name());
            tm.put("local", t.getLocal());
            tm.put("partido", t.getPartido());
            tm.put("resultado", t.getResultado());
            tm.put("eleito", t.isEleito());
            tm.put("fimMandato", t.getFimMandato());
            trajetoria.add(tm);
        }
        m.put("trajetoria", trajetoria);
        List<Object> atuacao = new ArrayList<>();
        Deputado atualCamara = base.getDeputadoDe(c);
        if (atualCamara != null) {
            atuacao.add(resumoJson(MandatoAnteriorCamara.resumir(atualCamara, base,
                    (meta.getAnoEleicao() - 3) + "–hoje")));
        }
        for (ResumoMandato r : c.getAtuacao()) {
            atuacao.add(resumoJson(r));
        }
        m.put("atuacao", atuacao);
        List<Object> contas = new ArrayList<>();
        for (ContaIrregular ci : c.getContasIrregulares()) {
            Map<String, Object> cm = new LinkedHashMap<>();
            cm.put("processo", ci.getProcesso());
            cm.put("deliberacao", ci.getDeliberacao());
            cm.put("transito", ci.getDataTransito());
            cm.put("local", ci.getLocal());
            cm.put("criterio", ci.getCriterio());
            contas.add(cm);
        }
        m.put("contas", contas);
        int[] anos = br.unit.eleicao.coleta.ProcessadorTrajetoria.anosAnteriores(meta.getAnoEleicao());
        m.put("trajetoriaDe", anos[0]);
        m.put("trajetoriaAte", anos[anos.length - 1]);
        Deputado d = base.getDeputadoDe(c);
        if (d != null) {
            Map<String, Object> dep = new LinkedHashMap<>();
            dep.put("id", d.getId());
            dep.put("nome", d.getNomeExibicao());
            dep.put("partido", d.getPartido());
            dep.put("uf", d.getUf());
            dep.put("url", d.getUrlCamara());
            dep.put("criterioVinculo", c.getCriterioVinculo());
            m.put("deputado", dep);
            List<Proposicao> props = base.getProposicoesDe(d.getId());
            m.put("totalAprovadas", ProducaoLegislativa.contarAprovadas(props));
            List<Object> aprovadas = new ArrayList<>();
            Set<String> vistos = new HashSet<>();
            for (Proposicao p : props) {
                if (p.isAprovada() && vistos.add(p.getId())) {
                    Map<String, Object> pm = new LinkedHashMap<>();
                    pm.put("identificacao", p.getIdentificacao());
                    pm.put("ementa", p.getEmenta());
                    pm.put("situacao", p.getSituacao());
                    pm.put("url", p.getUrlCamara());
                    aprovadas.add(pm);
                }
            }
            m.put("aprovadas", aprovadas);
        }
        return m;
    }

    private static Map<String, Object> resumoJson(ResumoMandato r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("casa", r.getCasa());
        m.put("cargo", r.getCargo());
        m.put("periodo", r.getPeriodo());
        m.put("url", r.getUrl());
        m.put("presenca", r.getPresenca());
        m.put("detalhePresenca", r.getDetalhePresenca());
        m.put("projetos", r.getProjetos());
        m.put("aprovados", r.getAprovados());
        m.put("observacaoProjetos", r.getObservacaoProjetos());
        m.put("gastoMensal", r.getGastoMensal());
        m.put("destaques", r.getDestaques());
        return m;
    }

    // ------------------------------------------------------------------ análise de perfil

    public synchronized Map<String, Object> distribuicao(String tipo, String cargo, String sigla) throws DadosException {
        exigirBase();
        List<Candidato> grupo = candidatosDo(cargo, sigla);
        Map<String, Integer> contagem = new LinkedHashMap<>();
        Function<Candidato, String> classe;
        String titulo;
        switch (tipo == null ? "" : tipo) {
            case "idade":
                titulo = "Faixa etária";
                for (String f : new String[]{"até 29", "30-39", "40-49", "50-59", "60-69", "70+"}) {
                    contagem.put(f, 0);
                }
                classe = c -> faixaEtaria(c.getIdade(base.getMetadados().getDataEleicao()));
                break;
            case "patrimonio":
                titulo = "Bens declarados";
                for (String f : new String[]{"até 100 mil", "100-500 mil", "500 mil-1 mi", "1-5 mi", "acima de 5 mi"}) {
                    contagem.put(f, 0);
                }
                classe = c -> faixaPatrimonio(c.getPatrimonio());
                break;
            case "genero":
                titulo = "Gênero";
                classe = c -> Texto.vazio(c.getGenero()) ? "Não informado" : c.getGenero();
                break;
            case "corRaca":
                titulo = "Cor/raça";
                classe = c -> Texto.vazio(c.getCorRaca()) ? "Não informado" : c.getCorRaca();
                break;
            case "uf":
                titulo = "Estado";
                classe = c -> String.valueOf(c.getUf());
                break;
            case "partido":
                titulo = "Partido";
                classe = Candidato::getPartido;
                break;
            case "cargo":
                titulo = "Cargo disputado";
                classe = c -> c.getTipoCargo().getRotulo();
                break;
            case "mandato":
                titulo = "Já é deputado(a) federal?";
                classe = c -> c.temMandatoNaCamara() ? "Sim (tem dados da Câmara)" : "Não";
                break;
            case "experiencia":
                titulo = "Experiência em eleições anteriores";
                for (String f : new String[]{"Tem mandato hoje", "Já foi eleito(a) antes", "Já concorreu, nunca eleito(a)",
                    "Primeira eleição (desde " + ProcessadorTrajetoriaAnos.inicio(base) + ")"}) {
                    contagem.put(f, 0);
                }
                classe = c -> {
                    switch (experiencia(c)[0]) {
                        case "MANDATO_ATUAL":
                            return "Tem mandato hoje";
                        case "JA_ELEITO":
                            return "Já foi eleito(a) antes";
                        case "JA_CONCORREU":
                            return "Já concorreu, nunca eleito(a)";
                        default:
                            return "Primeira eleição (desde " + ProcessadorTrajetoriaAnos.inicio(base) + ")";
                    }
                };
                break;
            case "elegibilidade":
                titulo = "Situação da candidatura";
                classe = c -> c.getElegibilidade().getRotulo();
                break;
            default:
                titulo = "Escolaridade";
                for (GrauInstrucao g : GrauInstrucao.values()) {
                    if (g != GrauInstrucao.NAO_INFORMADO) {
                        contagem.put(g.getDescricao(), 0);
                    }
                }
                classe = c -> c.getGrauInstrucao().getDescricao();
        }
        for (Candidato c : grupo) {
            contagem.merge(classe.apply(c), 1, Integer::sum);
        }
        List<Object> barras = new ArrayList<>();
        for (Map.Entry<String, Integer> e : contagem.entrySet()) {
            Map<String, Object> b = new LinkedHashMap<>();
            b.put("rotulo", e.getKey());
            b.put("valor", e.getValue());
            barras.add(b);
        }
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("titulo", titulo);
        r.put("total", grupo.size());
        r.put("barras", barras);
        return r;
    }

    /** Primeiro ano da janela de trajetória, para os rótulos. */
    private static final class ProcessadorTrajetoriaAnos {
        static int inicio(BaseDados base) {
            return br.unit.eleicao.coleta.ProcessadorTrajetoria.anosAnteriores(base.getMetadados().getAnoEleicao())[0];
        }
    }

    private static String faixaEtaria(Integer idade) {
        if (idade == null) {
            return "Não informado";
        }
        if (idade < 30) {
            return "até 29";
        }
        if (idade >= 70) {
            return "70+";
        }
        int d = idade / 10 * 10;
        return d + "-" + (d + 9);
    }

    private static String faixaPatrimonio(Double v) {
        if (v == null) {
            return "Sem declaração";
        }
        if (v <= 100_000) {
            return "até 100 mil";
        }
        if (v <= 500_000) {
            return "100-500 mil";
        }
        if (v <= 1_000_000) {
            return "500 mil-1 mi";
        }
        return v <= 5_000_000 ? "1-5 mi" : "acima de 5 mi";
    }

    /** Variáveis numéricas disponíveis para correlação: código -> nome. */
    public synchronized Map<String, String> variaveis() {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("idade", "Idade");
        m.put("escolaridade", "Escolaridade (0 = lê e escreve, 6 = superior completo)");
        m.put("patrimonio", "Bens declarados (R$ mil)");
        for (Indicador ind : indicadores) {
            m.put(ind.getCodigo(), ind.getNome());
        }
        return m;
    }

    private Double valorVariavel(String codigo, Candidato c) {
        switch (codigo) {
            case "idade":
                Integer i = c.getIdade(base.getMetadados().getDataEleicao());
                return i == null ? null : i.doubleValue();
            case "escolaridade":
                return c.getGrauInstrucao() == GrauInstrucao.NAO_INFORMADO ? null : (double) c.getGrauInstrucao().getNivel();
            case "patrimonio":
                return c.getPatrimonio() == null ? null : c.getPatrimonio() / 1000;
            default:
                ResultadoIndicador r = matriz.get(codigo, c);
                return r != null && r.temDados() ? r.getValor() : null;
        }
    }

    public synchronized Map<String, Object> correlacao(String x, String y, String cargo, String sigla)
            throws DadosException {
        exigirBase();
        Map<String, String> nomes = variaveis();
        if (!nomes.containsKey(x) || !nomes.containsKey(y)) {
            throw new DadosException("Variável desconhecida: " + x + " / " + y);
        }
        List<Object> pontos = new ArrayList<>();
        List<Double> xs = new ArrayList<>();
        List<Double> ys = new ArrayList<>();
        for (Candidato c : candidatosDo(cargo, sigla)) {
            Double vx = valorVariavel(x, c);
            Double vy = valorVariavel(y, c);
            if (vx != null && vy != null) {
                xs.add(vx);
                ys.add(vy);
                Map<String, Object> p = new LinkedHashMap<>();
                p.put("x", vx);
                p.put("y", vy);
                p.put("nome", c.getNomeExibicao());
                pontos.add(p);
            }
        }
        double[] ax = xs.stream().mapToDouble(Double::doubleValue).toArray();
        double[] ay = ys.stream().mapToDouble(Double::doubleValue).toArray();
        double r = Estatistica.pearson(ax, ay);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("nomeX", nomes.get(x));
        m.put("nomeY", nomes.get(y));
        m.put("pontos", pontos);
        m.put("n", ax.length);
        m.put("r", r);
        m.put("interpretacao", Estatistica.interpretarCorrelacao(r));
        if (ax.length >= 3) {
            double[] ab = Estatistica.regressaoLinear(ax, ay);
            m.put("reta", List.of(ab[0], ab[1]));
        }
        return m;
    }

    // ------------------------------------------------------------------ pautas

    public synchronized Map<String, Object> votacoes(String busca, boolean soPrincipais, int limite)
            throws DadosException {
        exigirBase();
        String termo = Texto.normalizar(busca);
        List<Votacao> todas = base.getVotacoesOrdenadas();
        List<Object> lista = new ArrayList<>();
        int total = 0;
        for (int i = todas.size() - 1; i >= 0; i--) { // mais recentes primeiro
            Votacao v = todas.get(i);
            PosicaoUsuario p = base.getPosicao(v.getId());
            boolean corresponde = termo.isEmpty() || Texto.normalizar(v.getTitulo() + " " + v.getDescricao()).contains(termo);
            if (!corresponde || (soPrincipais && !v.isPrincipal() && p == null)) {
                continue;
            }
            total++;
            if (lista.size() < limite) {
                lista.add(votacaoJson(v, p));
            }
        }
        List<Object> minhas = new ArrayList<>();
        for (PosicaoUsuario p : base.getPosicoes()) {
            Votacao v = base.getVotacao(p.getIdVotacao());
            if (v != null) {
                minhas.add(votacaoJson(v, p));
            }
        }
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("total", total);
        r.put("votacoes", lista);
        r.put("minhas", minhas);
        return r;
    }

    private Map<String, Object> votacaoJson(Votacao v, PosicaoUsuario p) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", v.getId());
        m.put("data", Texto.formatarData(v.getData()));
        m.put("proposicao", v.getProposicao());
        m.put("ementa", v.getEmenta());
        m.put("descricao", v.getDescricao());
        m.put("principal", v.isPrincipal());
        m.put("meuVoto", p == null ? null : p.getVoto());
        m.put("tema", p == null ? "" : p.getTema());
        return m;
    }

    /** Marca (Sim/Não) ou remove (voto vazio) a posição do usuário numa votação, e grava em disco. */
    public synchronized Map<String, Object> definirPosicao(String idVotacao, String voto, String tema)
            throws DadosException {
        exigirBase();
        if (base.getVotacao(idVotacao) == null) {
            throw new DadosException("Votação não encontrada: " + idVotacao);
        }
        if (Texto.vazio(voto)) {
            base.removerPosicao(idVotacao);
        } else {
            try {
                base.definirPosicao(new PosicaoUsuario(idVotacao, voto, tema));
            } catch (IllegalArgumentException e) {
                throw new DadosException(e.getMessage());
            }
        }
        repositorio.salvarPosicoes(base);
        matriz.recalcular(AlinhamentoPauta.CODIGO);
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("posicoes", base.getPosicoes().size());
        return r;
    }

    // ------------------------------------------------------------------ coleta

    /** Inicia a coleta numa thread separada; o andamento é consultado por {@link #statusColeta()}. */
    public Map<String, Object> iniciarColeta(String uf, int ano, boolean baixar) throws DadosException {
        if (coletando) {
            throw new DadosException("Já existe uma coleta em andamento.");
        }
        if (uf == null || !uf.matches("[A-Za-z]{2}")) {
            throw new DadosException("UF inválida: " + uf);
        }
        coletando = true;
        erroColeta = null;
        logColeta.clear();
        Thread t = new Thread(() -> {
            try {
                new PipelineColeta(raiz, logColeta::add).executar(uf, ano, baixar);
                logColeta.add("Concluído. Abrindo a base de " + uf.toUpperCase() + "...");
                abrir(uf);
            } catch (DadosException | RuntimeException e) {
                erroColeta = e.getMessage();
                logColeta.add("ERRO: " + e.getMessage());
            } finally {
                coletando = false;
            }
        }, "coleta-" + uf);
        t.setDaemon(true);
        t.start();
        return statusColeta();
    }

    public Map<String, Object> statusColeta() {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("coletando", coletando);
        r.put("erro", erroColeta);
        synchronized (logColeta) {
            r.put("log", new ArrayList<>(logColeta));
        }
        return r;
    }

    /** Converte o código do cargo vindo da tela (ex.: "GOVERNADOR"); null = todos. */
    private static Cargo cargo(String codigo) throws DadosException {
        if (Texto.vazio(codigo) || "TODOS".equalsIgnoreCase(codigo)) {
            return null;
        }
        try {
            return Cargo.valueOf(codigo.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new DadosException("Cargo desconhecido: " + codigo);
        }
    }

    /** UF vinda da tela; null = todas. */
    private static String uf(String sigla) {
        return Texto.vazio(sigla) || "TODOS".equalsIgnoreCase(sigla) || "BR".equalsIgnoreCase(sigla) ? null
                : sigla.toUpperCase();
    }

    private List<Candidato> candidatosDo(String codigoCargo, String sigla) throws DadosException {
        ConfiguracaoRanking filtro = new ConfiguracaoRanking();
        filtro.setCargo(cargo(codigoCargo));
        filtro.setUf(uf(sigla));
        List<Candidato> lista = new ArrayList<>();
        for (Candidato c : base.getCandidatos()) {
            if (filtro.aceita(c)) {
                lista.add(c);
            }
        }
        return lista;
    }

    /** Experiência política em uma palavra-chave (para filtros) e em texto (para a tela). */
    private String[] experiencia(Candidato c) {
        int ano = base.getMetadados().getAnoEleicao();
        if (c.temMandatoNaCamara()) {
            return new String[]{"MANDATO_ATUAL", "Deputado(a) federal no mandato atual"};
        }
        CandidaturaAnterior atual = c.getMandatoAtual(ano);
        if (atual != null) {
            return new String[]{"MANDATO_ATUAL", atual.getCargoLegivel()
                    + (atual.getLocal().isEmpty() ? "" : " em " + atual.getLocal()) + " desde " + (atual.getAno() + 1)};
        }
        if (c.jaFoiEleito() || !c.getAtuacao().isEmpty()) {
            for (CandidaturaAnterior t : c.getTrajetoria()) {
                if (t.isEleito()) {
                    return new String[]{"JA_ELEITO", "Já foi " + t.getCargoLegivel().toLowerCase()
                            + " (" + (t.getAno() + 1) + "–" + t.getFimMandato() + ")"};
                }
            }
            return new String[]{"JA_ELEITO", "Já exerceu mandato"};
        }
        if (!c.getTrajetoria().isEmpty()) {
            return new String[]{"JA_CONCORREU", "Já concorreu, sem ser eleito(a)"};
        }
        int[] anos = br.unit.eleicao.coleta.ProcessadorTrajetoria.anosAnteriores(ano);
        return new String[]{"NOVO", "Primeira candidatura desde " + anos[0]};
    }

    private static int inteiro(String s, int padrao) {
        Integer i = Texto.parseInteiro(s);
        return i == null ? padrao : i;
    }
}
