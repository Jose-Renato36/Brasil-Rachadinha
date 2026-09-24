package br.unit.eleicao.indicador;

import br.unit.eleicao.modelo.BaseDados;
import br.unit.eleicao.modelo.Candidato;
import br.unit.eleicao.modelo.Deputado;
import br.unit.eleicao.modelo.Proposicao;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Número de proposições (PL, PLP, PEC, PDL, PRC) de autoria do deputado, com as aprovadas em destaque. */
public class ProducaoLegislativa extends Indicador {

    public static final String CODIGO = "producao";

    public ProducaoLegislativa() {
        super(CODIGO, "Produção legislativa", "Câmara dos Deputados - proposições e autores",
                "Proposições apresentadas como autor na legislatura. Quantidade não é qualidade: "
                        + "veja as aprovadas no perfil.",
                Sentido.MAIOR_MELHOR);
    }

    @Override
    public ResultadoIndicador calcular(Candidato candidato, BaseDados base) {
        Deputado d = deputadoDe(candidato, base);
        if (d == null) {
            return semMandato();
        }
        List<Proposicao> lista = base.getProposicoesDe(d.getId());
        Set<String> distintas = new HashSet<>();
        Set<String> aprovadas = new HashSet<>();
        for (Proposicao p : lista) {
            distintas.add(p.getId());
            if (p.isAprovada()) {
                aprovadas.add(p.getId());
            }
        }
        return ResultadoIndicador.com(distintas.size(),
                distintas.size() + " proposições de autoria, " + aprovadas.size() + " aprovadas");
    }

    /** Quantas das proposições foram aprovadas (exibido à parte, não entra na nota). */
    public static long contarAprovadas(List<Proposicao> lista) {
        return lista.stream().filter(Proposicao::isAprovada).map(Proposicao::getId).distinct().count();
    }

    @Override
    public String formatar(double valor) {
        return String.format("%.0f", valor);
    }
}
