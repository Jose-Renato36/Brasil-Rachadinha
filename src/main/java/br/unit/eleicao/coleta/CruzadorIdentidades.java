package br.unit.eleicao.coleta;

import br.unit.eleicao.modelo.Candidato;
import br.unit.eleicao.modelo.Deputado;
import br.unit.eleicao.util.Texto;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Liga cada candidatura do TSE ao cadastro de deputado da Câmara, do critério mais seguro ao
 * menos seguro. Vínculos só por nome ficam marcados "CONFERIR" e vão para um relatório,
 * porque homônimos e nomes de urna diferentes são o principal risco do cruzamento.
 */
public class CruzadorIdentidades {

    public static final String MANUAL = "MANUAL";
    public static final String CPF = "CPF";
    public static final String NOME_NASCIMENTO = "NOME CIVIL + NASCIMENTO";
    public static final String NOME_CIVIL = "NOME CIVIL (CONFERIR)";
    public static final String NOME_URNA = "NOME DE URNA (CONFERIR)";

    /** Valor em vinculos_manuais.csv que força "sem vínculo". */
    public static final int SEM_VINCULO = 0;

    private final List<String> paraConferir = new ArrayList<>();

    /** Resultado de uma associação: id do deputado e critério usado. */
    public static final class Vinculo {
        private final int idDeputado;
        private final String criterio;

        Vinculo(int idDeputado, String criterio) {
            this.idDeputado = idDeputado;
            this.criterio = criterio;
        }

        public int getIdDeputado() {
            return idDeputado;
        }

        public String getCriterio() {
            return criterio;
        }
    }

    /** Liga as candidaturas ao mandato atual na Câmara (grava o vínculo no candidato). */
    public void cruzar(List<Candidato> candidatos, Collection<Deputado> deputados, Map<String, String> cpfPorSq,
                       Map<Integer, String> cpfPorDeputado, Map<String, Integer> manuais) {
        Map<String, Vinculo> achados = associar(candidatos, deputados, cpfPorSq, cpfPorDeputado, manuais);
        for (Candidato c : candidatos) {
            if (manuais.containsKey(c.getSq()) && manuais.get(c.getSq()) == SEM_VINCULO) {
                c.vincularDeputado(null, MANUAL);
            }
            Vinculo v = achados.get(c.getSq());
            if (v != null) {
                c.vincularDeputado(v.idDeputado, v.criterio);
            }
        }
    }

    /**
     * Encontra o deputado de cada candidatura, do critério mais seguro ao menos seguro, sem alterar
     * os candidatos (usado também para legislaturas anteriores).
     */
    public Map<String, Vinculo> associar(List<Candidato> candidatos, Collection<Deputado> deputados,
                                         Map<String, String> cpfPorSq, Map<Integer, String> cpfPorDeputado,
                                         Map<String, Integer> manuais) {
        Map<String, Vinculo> resultado = new HashMap<>();
        Set<Integer> usados = new HashSet<>();
        for (Candidato c : candidatos) {
            Integer manual = manuais.get(c.getSq());
            if (manual != null && manual != SEM_VINCULO) {
                resultado.put(c.getSq(), new Vinculo(manual, MANUAL));
                usados.add(manual);
            }
        }
        String[] criterios = {CPF, NOME_NASCIMENTO, NOME_CIVIL, NOME_URNA};
        for (String criterio : criterios) {
            for (Candidato c : candidatos) {
                if (resultado.containsKey(c.getSq()) || manuais.containsKey(c.getSq())) {
                    continue;
                }
                List<Deputado> achados = new ArrayList<>();
                for (Deputado d : deputados) {
                    if (!usados.contains(d.getId()) && combina(criterio, c, d, cpfPorSq, cpfPorDeputado)) {
                        achados.add(d);
                    }
                }
                if (achados.size() == 1) {
                    Deputado d = achados.get(0);
                    resultado.put(c.getSq(), new Vinculo(d.getId(), criterio));
                    usados.add(d.getId());
                    if (criterio.contains("CONFERIR")) {
                        paraConferir.add(c.getSq() + ";" + c.getNomeExibicao() + ";" + d.getId() + ";"
                                + d.getNomeExibicao() + ";" + criterio);
                    }
                } else if (achados.size() > 1) {
                    paraConferir.add(c.getSq() + ";" + c.getNomeExibicao() + ";;" + achados.size()
                            + " deputados possíveis;AMBÍGUO - " + criterio);
                }
            }
        }
        return resultado;
    }

    private static boolean combina(String criterio, Candidato c, Deputado d, Map<String, String> cpfPorSq,
                                   Map<Integer, String> cpfPorDeputado) {
        switch (criterio) {
            case CPF: {
                String a = cpfPorSq.get(c.getSq());
                return a != null && a.equals(cpfPorDeputado.get(d.getId()));
            }
            case NOME_NASCIMENTO:
                return c.getDataNascimento() != null && mesmoNome(c.getNome(), d.getNome())
                        && Objects.equals(c.getDataNascimento(), d.getDataNascimento());
            case NOME_CIVIL:
                return mesmaUf(c, d) && mesmoNome(c.getNome(), d.getNome());
            case NOME_URNA:
                return mesmaUf(c, d) && mesmoNome(c.getNomeUrna(), d.getNomeParlamentar());
            default:
                return false;
        }
    }

    /** Na versão nacional, homônimos de outros estados são comuns: critérios só por nome exigem a mesma UF. */
    private static boolean mesmaUf(Candidato c, Deputado d) {
        return c.getUf() == null || d.getUf() == null || c.getUf().equalsIgnoreCase(d.getUf());
    }

    private static boolean mesmoNome(String a, String b) {
        String na = Texto.normalizar(a);
        return !na.isEmpty() && na.equals(Texto.normalizar(b));
    }

    /** Linhas "sq;candidato;idDeputado;deputado;critério" que merecem conferência manual. */
    public List<String> getParaConferir() {
        return paraConferir;
    }
}
