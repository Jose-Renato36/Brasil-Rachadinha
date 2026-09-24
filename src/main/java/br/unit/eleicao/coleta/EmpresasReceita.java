package br.unit.eleicao.coleta;

import br.unit.eleicao.excecao.ArquivoInvalidoException;
import br.unit.eleicao.modelo.Candidato;
import br.unit.eleicao.modelo.VinculoEmpresa;
import br.unit.eleicao.util.LeitorCsv;
import br.unit.eleicao.util.Texto;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Empresas em que o candidato aparece como sócio(a), pelos dados abertos do CNPJ (Receita Federal).
 * Os arquivos não têm cabeçalho; o CPF do sócio vem mascarado ("***456789**"), então a ligação exige
 * o nome completo igual E os 6 dígitos visíveis iguais aos do CPF informado ao TSE.
 * Arquivos esperados em dados/brutos/cnpj/: Socios0.zip ... Socios9.zip, Empresas0.zip ... e,
 * opcionalmente, Qualificacoes.zip (nomes dos papéis).
 */
public class EmpresasReceita {

    public static final String PASTA = "cnpj";
    /** Papéis mais comuns, usados se Qualificacoes.zip não estiver presente. */
    private static final Map<String, String> QUALIFICACOES_BASICAS = Map.of(
            "49", "Sócio-Administrador", "22", "Sócio", "05", "Administrador", "16", "Presidente",
            "10", "Diretor", "65", "Titular (pessoa física)", "54", "Fundador", "37", "Sócio pessoa física residente no exterior");

    private final Path pasta;
    private final Consumer<String> log;

    public EmpresasReceita(Path brutos, Consumer<String> log) {
        this.pasta = brutos.resolve(PASTA);
        this.log = log;
    }

    /** @return true se havia arquivos de sócios para ler */
    public boolean processar(List<Candidato> candidatos, Map<String, String> cpfPorSq) throws ArquivoInvalidoException {
        List<Path> socios = listar("socios");
        if (socios.isEmpty()) {
            log.accept("  aviso: sem arquivos Socios*.zip em " + pasta.toAbsolutePath() + " - empresas não verificadas");
            return false;
        }
        Map<String, Candidato> porChave = new HashMap<>();
        for (Candidato c : candidatos) {
            String cpf = cpfPorSq.get(c.getSq());
            if (cpf != null && cpf.length() == 11 && !Texto.vazio(c.getNome())) {
                porChave.put(chave(c.getNome(), Texto.meioCpf(cpf)), c);
            }
        }
        Map<String, String> qualificacoes = lerQualificacoes();
        // cnpj básico -> (candidato, vínculo sem razão social ainda)
        Map<String, List<Object[]>> pendentes = new HashMap<>();
        for (Path zip : socios) {
            lerZip(zip, l -> {
                if (l.length < 6 || !"2".equals(l[1].strip())) {
                    return; // só pessoas físicas
                }
                String meio = Texto.somenteDigitos(l[3]);
                Candidato c = porChave.get(chave(l[2], meio));
                if (c == null || !Texto.cpfCompativel(cpfPorSq.get(c.getSq()), l[3])) {
                    return;
                }
                String qualif = qualificacoes.getOrDefault(l[4].strip(), "Sócio(a)");
                pendentes.computeIfAbsent(l[0].strip(), k -> new ArrayList<>())
                        .add(new Object[]{c, qualif, formatarData(l[5])});
            });
        }
        Map<String, String> razoes = new HashMap<>();
        for (Path zip : listar("empresas")) {
            lerZip(zip, l -> {
                if (l.length > 1 && pendentes.containsKey(l[0].strip())) {
                    razoes.put(l[0].strip(), l[1].strip());
                }
            });
        }
        int n = 0;
        for (Map.Entry<String, List<Object[]>> e : pendentes.entrySet()) {
            for (Object[] v : e.getValue()) {
                ((Candidato) v[0]).adicionarEmpresa(new VinculoEmpresa(e.getKey(),
                        razoes.getOrDefault(e.getKey(), ""), (String) v[1], (String) v[2]));
                n++;
            }
        }
        log.accept("Empresas: " + n + " participações societárias ligadas a candidatos (CPF parcial + nome).");
        return true;
    }

    static String chave(String nome, String meioCpf) {
        return Texto.normalizar(nome) + "|" + meioCpf;
    }

    private static String formatarData(String aaaammdd) {
        String d = aaaammdd == null ? "" : aaaammdd.strip();
        return d.length() == 8 ? d.substring(6) + "/" + d.substring(4, 6) + "/" + d.substring(0, 4) : "";
    }

    private Map<String, String> lerQualificacoes() throws ArquivoInvalidoException {
        Map<String, String> mapa = new HashMap<>(QUALIFICACOES_BASICAS);
        for (Path zip : listar("qualificacoes")) {
            lerZip(zip, l -> {
                if (l.length >= 2) {
                    mapa.put(l[0].strip(), l[1].strip());
                }
            });
        }
        return mapa;
    }

    private List<Path> listar(String prefixo) {
        List<Path> lista = new ArrayList<>();
        if (!Files.isDirectory(pasta)) {
            return lista;
        }
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(pasta)) {
            for (Path p : ds) {
                String nome = p.getFileName().toString().toLowerCase();
                if (nome.startsWith(prefixo) && nome.endsWith(".zip")) {
                    lista.add(p);
                }
            }
        } catch (IOException e) {
            log.accept("  aviso: não foi possível listar " + pasta + ": " + e.getMessage());
        }
        lista.sort(null);
        return lista;
    }

    /** Lê todas as entradas de um zip sem cabeçalho, linha a linha. */
    private void lerZip(Path arquivo, Consumer<String[]> linha) throws ArquivoInvalidoException {
        try (ZipInputStream zip = new ZipInputStream(Files.newInputStream(arquivo))) {
            ZipEntry entry;
            Set<String> lidas = new HashSet<>();
            while ((entry = zip.getNextEntry()) != null) {
                if (entry.isDirectory() || !lidas.add(entry.getName())) {
                    continue;
                }
                LeitorCsv csv = new LeitorCsv(zip, ProcessadorTSE.LATIN1, ';', arquivo.getFileName() + "!" + entry.getName(),
                        false);
                String[] l;
                while ((l = csv.proximaLinha()) != null) {
                    linha.accept(l);
                }
                // não fecha o LeitorCsv aqui: fecharia o zip inteiro; a próxima entrada reaproveita o fluxo
            }
        } catch (IOException e) {
            throw new ArquivoInvalidoException("Erro lendo " + arquivo + ": " + e.getMessage(), e);
        }
    }
}
