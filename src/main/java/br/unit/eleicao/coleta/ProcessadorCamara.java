package br.unit.eleicao.coleta;

import br.unit.eleicao.excecao.ArquivoInvalidoException;
import br.unit.eleicao.modelo.BaseDados;
import br.unit.eleicao.modelo.Deputado;
import br.unit.eleicao.modelo.Despesa;
import br.unit.eleicao.modelo.Proposicao;
import br.unit.eleicao.modelo.Votacao;
import br.unit.eleicao.util.LeitorCsv;
import br.unit.eleicao.util.Texto;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Consumer;

/**
 * Lê os arquivos em lote da Câmara (já baixados) e guarda na base só o que é da UF escolhida:
 * deputados, votações nominais do Plenário, votos, despesas de cota e proposições de autoria.
 * Colunas conferidas: veja docs/FONTES.md.
 */
public class ProcessadorCamara {

    /** Tipos de proposição considerados "produção legislativa". */
    public static final Set<String> TIPOS = Set.of("PL", "PLP", "PEC", "PDL", "PRC");

    private final Path pasta;
    private final String uf;
    private final int[] anos;
    private final int legislatura;
    private final LocalDate inicioLegislatura;
    private final Consumer<String> log;

    private final Map<Integer, Deputado> deputados = new LinkedHashMap<>();
    private final Map<Integer, String> ultimoVoto = new HashMap<>();
    private final Map<Integer, String> cpfPorDeputado = new HashMap<>();

    public ProcessadorCamara(Path pasta, String uf, int[] anos, int legislatura, LocalDate inicioLegislatura,
                             Consumer<String> log) {
        this.pasta = pasta;
        this.uf = uf.toUpperCase();
        this.anos = anos.clone();
        this.legislatura = legislatura;
        this.inicioLegislatura = inicioLegislatura;
        this.log = log;
    }

    public void processar(BaseDados base) throws ArquivoInvalidoException {
        for (int ano : anos) {
            Map<String, Votacao> plenario = lerVotacoesPlenario(ano);
            lerProposicoesDasVotacoes(ano, plenario);
            lerVotos(base, ano, plenario);
            lerCota(base, ano);
        }
        completarCadastro();
        for (int ano : anos) {
            lerProposicoes(base, ano);
        }
        for (Deputado d : deputados.values()) {
            base.adicionarDeputado(d);
        }
        log.accept("Câmara: " + deputados.size() + " deputados de " + uf + ", " + base.getTotalVotacoes()
                + " votações nominais do Plenário.");
    }

    /** CPF de cada deputado (usado apenas para cruzar com o TSE; não é gravado nos arquivos processados). */
    public Map<Integer, String> getCpfPorDeputado() {
        return cpfPorDeputado;
    }

    public Map<Integer, Deputado> getDeputados() {
        return deputados;
    }

    private Path local(String url) {
        return pasta.resolve(FontesDados.nomeLocal(url));
    }

    private boolean disponivel(Path arquivo) {
        if (Files.exists(arquivo)) {
            return true;
        }
        log.accept("  aviso: " + arquivo.getFileName() + " ausente - parte ignorada");
        return false;
    }

    private Deputado deputado(int id, String nome) {
        return deputados.computeIfAbsent(id, k -> {
            Deputado d = new Deputado(k, nome, null, null, null);
            d.setUf(uf);
            return d;
        });
    }

    /** Votações do Plenário ocorridas na legislatura. Só entram na base as que tiverem voto da UF. */
    private Map<String, Votacao> lerVotacoesPlenario(int ano) throws ArquivoInvalidoException {
        Map<String, Votacao> ids = new HashMap<>();
        Path arquivo = local(FontesDados.votacoes(ano));
        if (!disponivel(arquivo)) {
            return ids;
        }
        try (LeitorCsv csv = ArquivosBrutos.abrir(arquivo, StandardCharsets.UTF_8, ".csv")) {
            int iId = csv.indice("id");
            int iData = csv.indice("data", "dataHoraRegistro");
            int iOrgao = csv.indice("siglaOrgao");
            int iDesc = csv.indiceOpcional("descricao");
            String[] l;
            while ((l = csv.proximaLinha()) != null) {
                LocalDate data = Texto.parseData(LeitorCsv.campo(l, iData));
                if ("PLEN".equalsIgnoreCase(LeitorCsv.campo(l, iOrgao)) && data != null
                        && !data.isBefore(inicioLegislatura)) {
                    String id = LeitorCsv.campo(l, iId);
                    ids.put(id, new Votacao(id, data, LeitorCsv.campo(l, iDesc)));
                }
            }
        }
        return ids;
    }

    /** votacoesProposicoes: dá à votação o nome da proposição (ex.: "PL 1087/2025") e a ementa. */
    private void lerProposicoesDasVotacoes(int ano, Map<String, Votacao> plenario) throws ArquivoInvalidoException {
        Path arquivo = local(FontesDados.votacoesProposicoes(ano));
        if (plenario.isEmpty() || !disponivel(arquivo)) {
            return;
        }
        try (LeitorCsv csv = ArquivosBrutos.abrir(arquivo, StandardCharsets.UTF_8, ".csv")) {
            int iVot = csv.indice("idVotacao");
            int iTitulo = csv.indiceOpcional("proposicao_titulo");
            int iEmenta = csv.indiceOpcional("proposicao_ementa");
            String[] l;
            while ((l = csv.proximaLinha()) != null) {
                Votacao v = plenario.get(LeitorCsv.campo(l, iVot));
                if (v != null && v.getProposicao().isEmpty()) {
                    v.setProposicao(LeitorCsv.campo(l, iTitulo), LeitorCsv.campo(l, iEmenta));
                }
            }
        }
    }

    private void lerVotos(BaseDados base, int ano, Map<String, Votacao> plenario) throws ArquivoInvalidoException {
        Path arquivo = local(FontesDados.votos(ano));
        if (plenario.isEmpty() || !disponivel(arquivo)) {
            return;
        }
        String leg = String.valueOf(legislatura);
        try (LeitorCsv csv = ArquivosBrutos.abrir(arquivo, StandardCharsets.UTF_8, ".csv")) {
            int iVot = csv.indice("idVotacao");
            int iVoto = csv.indice("voto");
            int iDep = csv.indice("deputado_id");
            int iNome = csv.indice("deputado_nome");
            int iPartido = csv.indice("deputado_siglaPartido");
            int iUf = csv.indice("deputado_siglaUf");
            int iLeg = csv.indiceOpcional("deputado_idLegislatura");
            int iFoto = csv.indiceOpcional("deputado_urlFoto");
            int iHora = csv.indiceOpcional("dataHoraVoto");
            String[] l;
            while ((l = csv.proximaLinha()) != null) {
                if (!uf.equalsIgnoreCase(LeitorCsv.campo(l, iUf))) {
                    continue;
                }
                if (iLeg >= 0 && !leg.equals(LeitorCsv.campo(l, iLeg))) {
                    continue;
                }
                Votacao votacao = plenario.get(LeitorCsv.campo(l, iVot));
                Integer id = Texto.parseInteiro(LeitorCsv.campo(l, iDep));
                if (id == null || votacao == null) {
                    continue;
                }
                Deputado d = deputado(id, LeitorCsv.campo(l, iNome));
                // o voto mais recente define partido e foto atuais
                String hora = LeitorCsv.campo(l, iHora);
                if (hora.compareTo(ultimoVoto.getOrDefault(id, "")) >= 0) {
                    ultimoVoto.put(id, hora);
                    d.setPartido(LeitorCsv.campo(l, iPartido));
                    String foto = LeitorCsv.campo(l, iFoto);
                    if (!foto.isEmpty()) {
                        d.setUrlFoto(foto);
                    }
                }
                if (base.getVotacao(votacao.getId()) == null) {
                    base.adicionarVotacao(votacao);
                }
                base.adicionarVoto(id, votacao.getId(), LeitorCsv.campo(l, iVoto));
            }
        }
    }

    private void lerCota(BaseDados base, int ano) throws ArquivoInvalidoException {
        Path arquivo = local(FontesDados.cota(ano));
        if (!disponivel(arquivo)) {
            return;
        }
        // soma por deputado + mês + categoria para o arquivo processado ficar pequeno
        Map<String, Double> somas = new TreeMap<>();
        try (LeitorCsv csv = ArquivosBrutos.abrir(arquivo, StandardCharsets.UTF_8, ".csv")) {
            int iUf = csv.indice("sgUF");
            int iId = csv.indice("ideCadastro");
            int iNome = csv.indice("txNomeParlamentar");
            int iCpf = csv.indiceOpcional("cpf");
            int iPartido = csv.indiceOpcional("sgPartido");
            int iMes = csv.indice("numMes");
            int iAno = csv.indice("numAno");
            int iCat = csv.indice("txtDescricao");
            int iValor = csv.indice("vlrLiquido");
            String[] l;
            while ((l = csv.proximaLinha()) != null) {
                if (!uf.equalsIgnoreCase(LeitorCsv.campo(l, iUf))) {
                    continue;
                }
                // lideranças partidárias também usam a cota e vêm sem ideCadastro: não são deputados
                Integer id = Texto.parseInteiro(LeitorCsv.campo(l, iId));
                Integer mes = Texto.parseInteiro(LeitorCsv.campo(l, iMes));
                Integer anoDesp = Texto.parseInteiro(LeitorCsv.campo(l, iAno));
                Double valor = Texto.parseDecimal(LeitorCsv.campo(l, iValor));
                if (id == null || mes == null || anoDesp == null || valor == null || mes < 1 || mes > 12) {
                    continue;
                }
                // janeiro de 2023 ainda é da legislatura anterior
                if (LocalDate.of(anoDesp, mes, 1).isBefore(inicioLegislatura.withDayOfMonth(1))) {
                    continue;
                }
                Deputado d = deputado(id, LeitorCsv.campo(l, iNome));
                if (Texto.vazio(d.getPartido())) {
                    d.setPartido(LeitorCsv.campo(l, iPartido));
                }
                String cpf = Texto.somenteDigitos(LeitorCsv.campo(l, iCpf));
                if (cpf.length() == 11) {
                    cpfPorDeputado.putIfAbsent(id, cpf);
                }
                String chave = id + ";" + anoDesp + ";" + mes + ";" + LeitorCsv.campo(l, iCat);
                somas.merge(chave, valor, Double::sum);
            }
        }
        for (Map.Entry<String, Double> e : somas.entrySet()) {
            String[] p = e.getKey().split(";", 4);
            base.adicionarDespesa(new Despesa(Integer.parseInt(p[0]), Integer.parseInt(p[1]), Integer.parseInt(p[2]),
                    p[3], e.getValue()));
        }
    }

    /** Completa nome civil, nascimento, sexo e CPF a partir do cadastro geral de deputados. */
    private void completarCadastro() throws ArquivoInvalidoException {
        Path arquivo = local(FontesDados.deputados());
        if (!disponivel(arquivo)) {
            return;
        }
        try (LeitorCsv csv = ArquivosBrutos.abrir(arquivo, StandardCharsets.UTF_8, ".csv")) {
            int iUri = csv.indice("uri");
            int iNome = csv.indiceOpcional("nome");
            int iCivil = csv.indice("nomeCivil");
            int iCpf = csv.indiceOpcional("cpf");
            int iSexo = csv.indiceOpcional("siglaSexo");
            int iNasc = csv.indiceOpcional("dataNascimento");
            String[] l;
            while ((l = csv.proximaLinha()) != null) {
                Integer id = idDaUri(LeitorCsv.campo(l, iUri));
                Deputado d = id == null ? null : deputados.get(id);
                if (d == null) {
                    continue;
                }
                d.setNome(LeitorCsv.campo(l, iCivil));
                if (Texto.vazio(d.getNomeParlamentar())) {
                    d.setNomeParlamentar(LeitorCsv.campo(l, iNome));
                }
                d.setDataNascimento(Texto.parseData(LeitorCsv.campo(l, iNasc)));
                String sexo = LeitorCsv.campo(l, iSexo);
                d.setGenero("F".equalsIgnoreCase(sexo) ? "FEMININO" : "M".equalsIgnoreCase(sexo) ? "MASCULINO" : "");
                String cpf = Texto.somenteDigitos(LeitorCsv.campo(l, iCpf));
                if (cpf.length() == 11) {
                    cpfPorDeputado.put(id, cpf);
                }
            }
        }
    }

    private void lerProposicoes(BaseDados base, int ano) throws ArquivoInvalidoException {
        Path arqAutores = local(FontesDados.autores(ano));
        Path arqProp = local(FontesDados.proposicoes(ano));
        if (!disponivel(arqAutores) || !disponivel(arqProp)) {
            return;
        }
        Map<String, Set<Integer>> autoresPorProposicao = new HashMap<>();
        try (LeitorCsv csv = ArquivosBrutos.abrir(arqAutores, StandardCharsets.UTF_8, ".csv")) {
            int iProp = csv.indice("idProposicao");
            int iDep = csv.indiceOpcional("idDeputadoAutor");
            int iUri = csv.indiceOpcional("uriAutor");
            int iProponente = csv.indiceOpcional("proponente");
            String[] l;
            while ((l = csv.proximaLinha()) != null) {
                Integer id = Texto.parseInteiro(LeitorCsv.campo(l, iDep));
                if (id == null) {
                    id = idDaUri(LeitorCsv.campo(l, iUri));
                }
                boolean proponente = iProponente < 0 || "1".equals(LeitorCsv.campo(l, iProponente));
                if (id != null && proponente && deputados.containsKey(id)) {
                    autoresPorProposicao.computeIfAbsent(LeitorCsv.campo(l, iProp), k -> new HashSet<>()).add(id);
                }
            }
        }
        try (LeitorCsv csv = ArquivosBrutos.abrir(arqProp, StandardCharsets.UTF_8, ".csv")) {
            int iId = csv.indice("id");
            int iTipo = csv.indice("siglaTipo");
            int iNum = csv.indice("numero");
            int iAno = csv.indice("ano");
            int iEmenta = csv.indiceOpcional("ementa");
            int iApresentacao = csv.indiceOpcional("dataApresentacao");
            int iSit = csv.indiceOpcional("ultimoStatus_descricaoSituacao");
            String[] l;
            while ((l = csv.proximaLinha()) != null) {
                String id = LeitorCsv.campo(l, iId);
                Set<Integer> autores = autoresPorProposicao.get(id);
                String tipo = LeitorCsv.campo(l, iTipo).toUpperCase();
                if (autores == null || !TIPOS.contains(tipo)) {
                    continue;
                }
                LocalDate apresentacao = Texto.parseData(LeitorCsv.campo(l, iApresentacao));
                if (apresentacao != null && apresentacao.isBefore(inicioLegislatura)) {
                    continue;
                }
                String situacao = LeitorCsv.campo(l, iSit);
                Integer anoProp = Texto.parseInteiro(LeitorCsv.campo(l, iAno));
                for (Integer idDep : autores) {
                    base.adicionarProposicao(new Proposicao(idDep, id, tipo, LeitorCsv.campo(l, iNum),
                            anoProp == null ? ano : anoProp, situacao, situacaoAprovada(situacao),
                            LeitorCsv.campo(l, iEmenta)));
                }
            }
        }
    }

    /**
     * Critério de "aprovada": virou norma jurídica, ou foi aprovada na Câmara e enviada ao Senado/sanção.
     * Critério simples e explícito, descrito na página de metodologia.
     */
    public static boolean situacaoAprovada(String situacao) {
        String s = Texto.normalizar(situacao);
        return s.contains("TRANSFORMAD") && s.contains("NORMA")
                || s.contains("APRECIACAO PELO SENADO")
                || s.contains("REMETIDA AO SENADO")
                || s.contains("ENVIADA AO SENADO")
                || s.contains("AGUARDANDO SANCAO")
                || s.contains("AGUARDANDO PROMULGACAO");
    }

    /** Extrai o id numérico do final de uma URI como ".../deputados/204554". */
    static Integer idDaUri(String uri) {
        if (Texto.vazio(uri) || !uri.contains("/deputados/")) {
            return null;
        }
        return Texto.parseInteiro(uri.substring(uri.lastIndexOf('/') + 1));
    }
}
