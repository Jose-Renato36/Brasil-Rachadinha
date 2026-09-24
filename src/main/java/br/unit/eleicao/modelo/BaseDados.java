package br.unit.eleicao.modelo;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Tudo o que foi carregado para uma UF, em memória, organizado em coleções
 * (listas e mapas) para consultas rápidas pelos indicadores.
 */
public class BaseDados {

    private final Metadados metadados;
    private Path diretorio;
    private final List<Candidato> candidatos = new ArrayList<>();
    private final Map<Integer, Deputado> deputados = new TreeMap<>();
    private final Map<String, Votacao> votacoes = new HashMap<>();
    /** idDeputado -> (idVotacao -> voto registrado). */
    private final Map<Integer, Map<String, String>> votos = new HashMap<>();
    private final Map<Integer, List<Despesa>> despesas = new HashMap<>();
    private final Map<Integer, List<Proposicao>> proposicoes = new HashMap<>();
    private final Map<String, PosicaoUsuario> posicoes = new LinkedHashMap<>();

    public BaseDados(Metadados metadados) {
        this.metadados = metadados;
    }

    // ---- inclusão ----

    public void adicionarCandidato(Candidato c) {
        candidatos.add(c);
    }

    public void adicionarDeputado(Deputado d) {
        deputados.put(d.getId(), d);
    }

    public void adicionarVotacao(Votacao v) {
        votacoes.put(v.getId(), v);
    }

    public void adicionarVoto(int idDeputado, String idVotacao, String voto) {
        votos.computeIfAbsent(idDeputado, k -> new HashMap<>()).put(idVotacao, voto);
    }

    public void adicionarDespesa(Despesa d) {
        despesas.computeIfAbsent(d.getIdDeputado(), k -> new ArrayList<>()).add(d);
    }

    public void adicionarProposicao(Proposicao p) {
        proposicoes.computeIfAbsent(p.getIdDeputado(), k -> new ArrayList<>()).add(p);
    }

    public void definirPosicao(PosicaoUsuario p) {
        posicoes.put(p.getIdVotacao(), p);
    }

    public void removerPosicao(String idVotacao) {
        posicoes.remove(idVotacao);
    }

    // ---- consultas ----

    public List<Candidato> getCandidatos() {
        return Collections.unmodifiableList(candidatos);
    }

    public Candidato buscarCandidato(String sq) {
        for (Candidato c : candidatos) {
            if (c.getSq().equals(sq)) {
                return c;
            }
        }
        return null;
    }

    public List<Candidato> getCandidatosOrdenadosPorNome() {
        List<Candidato> lista = new ArrayList<>(candidatos);
        lista.sort(Comparator.comparing(Candidato::getNomeExibicao, String.CASE_INSENSITIVE_ORDER));
        return lista;
    }

    public Collection<Deputado> getDeputados() {
        return Collections.unmodifiableCollection(deputados.values());
    }

    public Deputado getDeputado(Integer id) {
        return id == null ? null : deputados.get(id);
    }

    public Deputado getDeputadoDe(Candidato c) {
        return getDeputado(c.getIdDeputado());
    }

    public Votacao getVotacao(String id) {
        return votacoes.get(id);
    }

    public List<Votacao> getVotacoesOrdenadas() {
        List<Votacao> lista = new ArrayList<>(votacoes.values());
        Collections.sort(lista);
        return lista;
    }

    public int getTotalVotacoes() {
        return votacoes.size();
    }

    /** Quantas votações nominais do Plenário ocorreram no intervalo (inclusive). */
    public int contarVotacoesEntre(LocalDate inicio, LocalDate fim) {
        int total = 0;
        for (Votacao v : votacoes.values()) {
            LocalDate d = v.getData();
            if (d != null && !d.isBefore(inicio) && !d.isAfter(fim)) {
                total++;
            }
        }
        return total;
    }

    public Map<String, String> getVotosDe(int idDeputado) {
        return votos.getOrDefault(idDeputado, Collections.emptyMap());
    }

    public Map<Integer, Map<String, String>> getTodosVotos() {
        return Collections.unmodifiableMap(votos);
    }

    public List<Despesa> getDespesasDe(int idDeputado) {
        return despesas.getOrDefault(idDeputado, Collections.emptyList());
    }

    public Map<Integer, List<Despesa>> getTodasDespesas() {
        return Collections.unmodifiableMap(despesas);
    }

    public List<Proposicao> getProposicoesDe(int idDeputado) {
        return proposicoes.getOrDefault(idDeputado, Collections.emptyList());
    }

    public Map<Integer, List<Proposicao>> getTodasProposicoes() {
        return Collections.unmodifiableMap(proposicoes);
    }

    public Collection<PosicaoUsuario> getPosicoes() {
        return Collections.unmodifiableCollection(posicoes.values());
    }

    public PosicaoUsuario getPosicao(String idVotacao) {
        return posicoes.get(idVotacao);
    }

    public Metadados getMetadados() {
        return metadados;
    }

    public Path getDiretorio() {
        return diretorio;
    }

    public void setDiretorio(Path diretorio) {
        this.diretorio = diretorio;
    }
}
