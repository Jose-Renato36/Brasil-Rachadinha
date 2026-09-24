package br.unit.eleicao;

import br.unit.eleicao.modelo.BaseDados;
import br.unit.eleicao.modelo.Candidato;
import br.unit.eleicao.modelo.Deputado;
import br.unit.eleicao.modelo.Despesa;
import br.unit.eleicao.modelo.Metadados;
import br.unit.eleicao.modelo.Proposicao;
import br.unit.eleicao.modelo.Votacao;

import java.time.LocalDate;

/** Monta bases pequenas, com números fáceis de conferir à mão, para os testes. */
public final class BaseTeste {

    private BaseTeste() {
    }

    public static BaseDados nova() {
        return new BaseDados(new Metadados("ZZ", 2026, 2022, LocalDate.of(2026, 10, 4)));
    }

    public static Deputado deputado(BaseDados base, int id) {
        Deputado d = new Deputado(id, "Dep " + id, "Deputado " + id, null, "");
        d.setUf("ZZ");
        base.adicionarDeputado(d);
        return d;
    }

    public static Candidato candidato(BaseDados base, String sq, Integer idDeputado) {
        Candidato c = new Candidato(sq, "Candidato " + sq, "Cand " + sq, LocalDate.of(1970, 1, 1), "");
        c.setPartido("PX");
        c.setSituacao("APTO");
        if (idDeputado != null) {
            c.vincularDeputado(idDeputado, "TESTE");
        }
        base.adicionarCandidato(c);
        return c;
    }

    /** n votações nominais em dias consecutivos a partir de 01/03/2023, ids V1..Vn. */
    public static void votacoes(BaseDados base, int n) {
        for (int i = 1; i <= n; i++) {
            base.adicionarVotacao(new Votacao("V" + i, LocalDate.of(2023, 3, 1).plusDays(i - 1), "Votação " + i));
        }
    }

    public static void despesaMensal(BaseDados base, int idDeputado, double valorPorMes, int meses) {
        for (int m = 1; m <= meses; m++) {
            base.adicionarDespesa(new Despesa(idDeputado, 2024, m, "X", valorPorMes));
        }
    }

    public static void proposicao(BaseDados base, int idDeputado, String id, boolean aprovada) {
        base.adicionarProposicao(new Proposicao(idDeputado, id, "PL", "1", 2024, "", aprovada, "ementa"));
    }
}
