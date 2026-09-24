package br.unit.eleicao.indicador;

import br.unit.eleicao.BaseTeste;
import br.unit.eleicao.modelo.BaseDados;
import br.unit.eleicao.modelo.Candidato;
import br.unit.eleicao.modelo.PosicaoUsuario;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IndicadoresTest {

    @Test
    void assiduidadeUsaPeriodoEntrePrimeiroEUltimoVoto() {
        BaseDados base = BaseTeste.nova();
        BaseTeste.votacoes(base, 10);
        BaseTeste.deputado(base, 1);
        Candidato c = BaseTeste.candidato(base, "A", 1);
        // votou em V3, V5 e V8: período V3..V8 tem 6 votações -> 3/6 = 50%
        base.adicionarVoto(1, "V3", "Sim");
        base.adicionarVoto(1, "V5", "Não");
        base.adicionarVoto(1, "V8", "Abstenção");
        ResultadoIndicador r = new Assiduidade().calcular(c, base);
        assertEquals(50.0, r.getValor(), 1e-9);
    }

    @Test
    void semMandatoEhSemDadosENaoZero() {
        BaseDados base = BaseTeste.nova();
        Candidato estreante = BaseTeste.candidato(base, "E", null);
        for (Indicador ind : List.of(new Assiduidade(), new GastoCota(), new ProducaoLegislativa())) {
            ResultadoIndicador r = ind.calcular(estreante, base);
            assertFalse(r.temDados(), ind.getNome());
            assertEquals("sem dados", ind.formatar(r));
        }
    }

    @Test
    void gastoCotaEmZScoreContraOsParesDaUf() {
        BaseDados base = BaseTeste.nova();
        // médias mensais 10, 20, 30 -> média 20, desvio 10
        for (int id = 1; id <= 3; id++) {
            BaseTeste.deputado(base, id);
            BaseTeste.despesaMensal(base, id, id * 10.0, 4);
        }
        Candidato c3 = BaseTeste.candidato(base, "C3", 3);
        Candidato c1 = BaseTeste.candidato(base, "C1", 1);
        Map<String, ResultadoIndicador> r = new GastoCota().calcularTodos(List.of(c3, c1), base);
        assertEquals(1.0, r.get("C3").getValor(), 1e-9);
        assertEquals(-1.0, r.get("C1").getValor(), 1e-9);
    }

    @Test
    void mediaMensalDivideSoPelosMesesComGasto() {
        BaseDados base = BaseTeste.nova();
        BaseTeste.despesaMensal(base, 1, 100, 3);
        assertEquals(100.0, GastoCota.mediaMensal(base.getDespesasDe(1)), 1e-9);
    }

    @Test
    void producaoContaProposicoesDistintas() {
        BaseDados base = BaseTeste.nova();
        BaseTeste.deputado(base, 1);
        Candidato c = BaseTeste.candidato(base, "A", 1);
        BaseTeste.proposicao(base, 1, "P1", true);
        BaseTeste.proposicao(base, 1, "P2", false);
        BaseTeste.proposicao(base, 1, "P2", false);
        ResultadoIndicador r = new ProducaoLegislativa().calcular(c, base);
        assertEquals(2.0, r.getValor(), 1e-9);
        assertTrue(r.getDetalhe().contains("1 aprovadas"));
    }

    @Test
    void variacaoPatrimonio() {
        BaseDados base = BaseTeste.nova();
        Candidato c = BaseTeste.candidato(base, "A", null);
        c.setPatrimonio(150_000.0);
        c.setPatrimonioAnterior(100_000.0);
        assertEquals(50.0, new VariacaoPatrimonio().calcular(c, base).getValor(), 1e-9);

        c.setPatrimonioAnterior(0.0); // divisão por zero vira "sem dados", não infinito
        assertFalse(new VariacaoPatrimonio().calcular(c, base).temDados());
        c.setPatrimonioAnterior(null);
        assertFalse(new VariacaoPatrimonio().calcular(c, base).temDados());
    }

    @Test
    void alinhamentoConsideraSoVotosSimOuNao() {
        BaseDados base = BaseTeste.nova();
        BaseTeste.votacoes(base, 4);
        BaseTeste.deputado(base, 1);
        Candidato c = BaseTeste.candidato(base, "A", 1);
        AlinhamentoPauta alinhamento = new AlinhamentoPauta();
        assertFalse(alinhamento.calcular(c, base).temDados(), "sem posições marcadas");

        base.adicionarVoto(1, "V1", "Sim");
        base.adicionarVoto(1, "V2", "Não");
        base.adicionarVoto(1, "V3", "Obstrução");
        base.definirPosicao(new PosicaoUsuario("V1", "Sim", ""));
        base.definirPosicao(new PosicaoUsuario("V2", "Sim", ""));
        base.definirPosicao(new PosicaoUsuario("V3", "Não", ""));
        base.definirPosicao(new PosicaoUsuario("V4", "Não", "")); // ausente
        // comparáveis: V1 (coincide) e V2 (não) -> 50%
        assertEquals(50.0, alinhamento.calcular(c, base).getValor(), 1e-9);
    }
}
