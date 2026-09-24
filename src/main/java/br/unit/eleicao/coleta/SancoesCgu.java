package br.unit.eleicao.coleta;

import br.unit.eleicao.excecao.ArquivoInvalidoException;
import br.unit.eleicao.modelo.Candidato;
import br.unit.eleicao.modelo.Sancao;
import br.unit.eleicao.modelo.VinculoEmpresa;
import br.unit.eleicao.util.LeitorCsv;
import br.unit.eleicao.util.Texto;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Cadastros de sanções da CGU (Portal da Transparência): CEIS (empresas e pessoas inidôneas e suspensas)
 * e CNEP (punidas pela Lei Anticorrupção). Arquivos baixados como dados/brutos/ceis.zip e cnep.zip
 * (entrada AAAAMMDD_CEIS.csv / AAAAMMDD_CNEP.csv, separador ';', Windows-1252).
 * Pessoa física: CPF publicado mascarado + nome completo. Empresa: CNPJ das empresas de que o candidato é sócio.
 */
public class SancoesCgu {

    public static final String[] CADASTROS = {"CEIS", "CNEP", "CEAF"};

    private final Path pasta;
    private final Consumer<String> log;

    public SancoesCgu(Path pasta, Consumer<String> log) {
        this.pasta = pasta;
        this.log = log;
    }

    public static Path arquivo(Path pasta, String cadastro) {
        return pasta.resolve(cadastro.toLowerCase() + ".zip");
    }

    private final java.util.Set<String> lidos = new java.util.TreeSet<>();

    /** Cadastros efetivamente lidos (CEIS, CNEP, CEAF). */
    public java.util.Set<String> getLidos() {
        return lidos;
    }

    /**
     * CEIS e CNEP: empresas e pessoas punidas. CEAF: servidores federais expulsos (demissão, destituição,
     * cassação de aposentadoria), só pessoa física.
     *
     * @return true se ao menos um dos cadastros foi lido
     */
    public boolean processar(List<Candidato> candidatos, Map<String, String> cpfPorSq) throws ArquivoInvalidoException {
        Map<String, List<Candidato>> porNome = new HashMap<>();
        Map<String, List<Candidato>> porEmpresa = new HashMap<>();
        for (Candidato c : candidatos) {
            if (!Texto.vazio(c.getNome())) {
                porNome.computeIfAbsent(Texto.normalizar(c.getNome()), k -> new ArrayList<>()).add(c);
            }
            for (VinculoEmpresa e : c.getEmpresas()) {
                porEmpresa.computeIfAbsent(e.getCnpj(), k -> new ArrayList<>()).add(c);
            }
        }
        boolean algum = false;
        int ligadas = 0;
        for (String cadastro : CADASTROS) {
            Path arquivo = arquivo(pasta, cadastro);
            if (!Files.exists(arquivo)) {
                log.accept("  aviso: " + arquivo.getFileName() + " ausente - " + cadastro + " não verificado");
                continue;
            }
            algum = true;
            lidos.add(cadastro);
            try (LeitorCsv csv = ArquivosBrutos.abrir(arquivo, EmendasParlamentares.WINDOWS_1252, "", ".csv", ';')) {
                int iTipo = csv.indiceContendo("TIPO DE PESSOA");
                int iDoc = csv.indice("CPF OU CNPJ DO SANCIONADO");
                int iNome = csv.indiceOpcional("NOME DO SANCIONADO", "NOME INFORMADO PELO ÓRGÃO SANCIONADOR");
                int iRazao = csv.indiceOpcional("RAZÃO SOCIAL - CADASTRO RECEITA");
                int iCategoria = csv.indiceOpcional("CATEGORIA DA SANÇÃO", "TIPO SANÇÃO");
                int iOrgao = csv.indiceOpcional("ÓRGÃO SANCIONADOR");
                int iInicio = csv.indiceOpcional("DATA INÍCIO SANÇÃO");
                int iFim = csv.indiceOpcional("DATA FINAL SANÇÃO");
                String[] l;
                while ((l = csv.proximaLinha()) != null) {
                    String doc = Texto.somenteDigitos(LeitorCsv.campo(l, iDoc));
                    String tipo = Texto.normalizar(LeitorCsv.campo(l, iTipo));
                    String nome = LeitorCsv.campo(l, iNome);
                    boolean empresa = !cadastro.equals("CEAF") && (tipo.startsWith("J") || doc.length() == 14);
                    List<Candidato> alvos = new ArrayList<>();
                    if (empresa && doc.length() >= 8) {
                        alvos.addAll(porEmpresa.getOrDefault(doc.substring(0, 8), List.of()));
                        String razao = LeitorCsv.campo(l, iRazao);
                        nome = razao.isEmpty() ? nome : razao;
                    } else if (!empresa) {
                        for (Candidato c : porNome.getOrDefault(Texto.normalizar(nome), List.of())) {
                            if (Texto.cpfCompativel(cpfPorSq.get(c.getSq()), LeitorCsv.campo(l, iDoc))) {
                                alvos.add(c);
                            }
                        }
                    }
                    for (Candidato c : alvos) {
                        c.adicionarSancao(new Sancao(cadastro, nome, LeitorCsv.campo(l, iCategoria),
                                LeitorCsv.campo(l, iOrgao), LeitorCsv.campo(l, iInicio), LeitorCsv.campo(l, iFim),
                                empresa));
                        ligadas++;
                    }
                }
            }
        }
        if (algum) {
            log.accept("Sanções (CGU): " + ligadas + " registro(s) ligados a candidatos ou às empresas deles.");
        }
        return algum;
    }

    /** Remove arquivos antigos que não sejam zip (o portal devolve HTML quando a data não existe). */
    static boolean pareceZip(Path arquivo) {
        try (java.io.InputStream in = Files.newInputStream(arquivo)) {
            return in.read() == 'P' && in.read() == 'K';
        } catch (IOException e) {
            return false;
        }
    }
}
