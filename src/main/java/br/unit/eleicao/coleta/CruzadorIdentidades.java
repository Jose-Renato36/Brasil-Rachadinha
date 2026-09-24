package br.unit.eleicao.coleta;

import br.unit.eleicao.modelo.Candidato;
import br.unit.eleicao.modelo.Deputado;
import br.unit.eleicao.util.Texto;

import java.util.ArrayList;
import java.util.Collection;
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

    public void cruzar(List<Candidato> candidatos, Collection<Deputado> deputados, Map<String, String> cpfPorSq,
                       Map<Integer, String> cpfPorDeputado, Map<String, Integer> manuais) {
        Set<Integer> usados = new HashSet<>();
        // 1ª passada: manuais; depois critérios automáticos em ordem de confiança
        for (Candidato c : candidatos) {
            Integer manual = manuais.get(c.getSq());
            if (manual != null) {
                c.vincularDeputado(manual == SEM_VINCULO ? null : manual, MANUAL);
                if (manual != SEM_VINCULO) {
                    usados.add(manual);
                }
            }
        }
        String[] criterios = {CPF, NOME_NASCIMENTO, NOME_CIVIL, NOME_URNA};
        for (String criterio : criterios) {
            for (Candidato c : candidatos) {
                if (c.getCriterioVinculo() != null || manuais.containsKey(c.getSq())) {
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
                    c.vincularDeputado(d.getId(), criterio);
                    usados.add(d.getId());
                    if (c.isVinculoDuvidoso()) {
                        paraConferir.add(c.getSq() + ";" + c.getNomeExibicao() + ";" + d.getId() + ";"
                                + d.getNomeExibicao() + ";" + criterio);
                    }
                } else if (achados.size() > 1) {
                    paraConferir.add(c.getSq() + ";" + c.getNomeExibicao() + ";;" + achados.size()
                            + " deputados possíveis;AMBÍGUO - " + criterio);
                }
            }
        }
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
                return mesmoNome(c.getNome(), d.getNome());
            case NOME_URNA:
                return mesmoNome(c.getNomeUrna(), d.getNomeParlamentar());
            default:
                return false;
        }
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
