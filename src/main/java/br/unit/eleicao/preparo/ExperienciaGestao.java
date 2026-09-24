package br.unit.eleicao.preparo;

import br.unit.eleicao.modelo.CargoPublico;
import br.unit.eleicao.modelo.Candidato;
import br.unit.eleicao.modelo.Metadados;

/**
 * Funções públicas de destaque fora das urnas: ministro(a), secretário(a) de estado, dirigente de
 * estatal, cargos de confiança de alto nível (lista de Pessoas Expostas Politicamente da CGU, que cobre
 * quem está na função ou saiu há menos de 5 anos).
 */
public class ExperienciaGestao extends CriterioPreparo {

    @Override
    public String getGrupo() {
        return PREPARO;
    }

    @Override
    public String getTitulo() {
        return "Cargos públicos de gestão";
    }

    @Override
    public ItemPreparo avaliar(Candidato c, Metadados meta) {
        String detalhe = "Funções como ministro(a), secretário(a) de estado, presidente de estatal ou cargo de confiança "
                + "de alto nível, exercidas agora ou nos últimos 5 anos. Mandatos eletivos aparecem no item anterior.";
        StringBuilder lista = new StringBuilder();
        int n = 0;
        for (CargoPublico p : c.getCargosPublicos()) {
            String f = p.getFuncao().toLowerCase();
            if (f.contains("deputad") || f.contains("senador") || f.contains("vereador") || f.contains("prefeit")
                    || f.contains("governador") || f.contains("presidente da república")) {
                continue; // mandatos eletivos já contam em "Experiência em cargos eletivos"
            }
            n++;
            if (n <= 3) {
                lista.append(lista.length() == 0 ? "" : "; ").append(capitalizar(p.getFuncao()))
                        .append(p.getOrgao().isEmpty() ? "" : " (" + p.getOrgao() + ")");
            }
        }
        if (n > 0) {
            return item(Avaliacao.POSITIVO, lista + (n > 3 ? " e mais " + (n - 3) : ""), detalhe, "CGU");
        }
        if (!meta.isVerificada(Metadados.FONTE_CARGOS_PUBLICOS)) {
            return item(Avaliacao.SEM_DADOS, "Lista de cargos públicos não consultada", detalhe, "CGU");
        }
        return item(Avaliacao.NEUTRO, "Nenhum cargo de gestão de destaque nos últimos 5 anos", detalhe, "CGU");
    }

    private static String capitalizar(String s) {
        String t = s.toLowerCase();
        return t.isEmpty() ? t : Character.toUpperCase(t.charAt(0)) + t.substring(1);
    }
}
