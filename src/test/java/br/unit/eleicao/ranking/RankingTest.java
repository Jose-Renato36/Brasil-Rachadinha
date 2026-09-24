package br.unit.eleicao.ranking;

import br.unit.eleicao.BaseTeste;
import br.unit.eleicao.indicador.Assiduidade;
import br.unit.eleicao.indicador.Indicador;
import br.unit.eleicao.indicador.ResultadoIndicador;
import br.unit.eleicao.indicador.Sentido;
import br.unit.eleicao.indicador.VariacaoPatrimonio;
import br.unit.eleicao.modelo.BaseDados;
import br.unit.eleicao.modelo.Candidato;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RankingTest {

    private BaseDados base;
    private List<Indicador> indicadores;

    /**
     * A: assiduidade 100%, patrimônio +100%
     * B: assiduidade 50%,  patrimônio +0%
     * C: estreante (sem assiduidade), patrimônio +50%
     * D: sem nenhum dado
     */
    @BeforeEach
    void montar() {
        base = BaseTeste.nova();
        BaseTeste.votacoes(base, 4);
        BaseTeste.deputado(base, 1);
        BaseTeste.deputado(base, 2);
        for (int i = 1; i <= 4; i++) {
            base.adicionarVoto(1, "V" + i, "Sim");
        }
        base.adicionarVoto(2, "V1", "Sim");
        base.adicionarVoto(2, "V4", "Sim");
        patrimonio(BaseTeste.candidato(base, "A", 1), 200, 100);
        patrimonio(BaseTeste.candidato(base, "B", 2), 100, 100);
        patrimonio(BaseTeste.candidato(base, "C", null), 150, 100);
        BaseTeste.candidato(base, "D", null);
        indicadores = List.of(new Assiduidade(), new VariacaoPatrimonio());
    }

    private static void patrimonio(Candidato c, double atual, double anterior) {
        c.setPatrimonio(atual);
        c.setPatrimonioAnterior(anterior);
    }

    private ConfiguracaoRanking config(int pesoAssiduidade, int pesoPatrimonio) {
        ConfiguracaoRanking cfg = new ConfiguracaoRanking();
        cfg.setPeso(Assiduidade.CODIGO, pesoAssiduidade);
        cfg.setPeso(VariacaoPatrimonio.CODIGO, pesoPatrimonio);
        return cfg;
    }

    private static Map<String, ItemRanking> porSq(List<ItemRanking> itens) {
        Map<String, ItemRanking> m = new HashMap<>();
        for (ItemRanking i : itens) {
            m.put(i.getCandidato().getSq(), i);
        }
        return m;
    }

    @Test
    void normalizacaoMinMaxRespeitaSentido() {
        Map<String, ResultadoIndicador> r = new HashMap<>();
        r.put("x", ResultadoIndicador.com(10, ""));
        r.put("y", ResultadoIndicador.com(20, ""));
        r.put("z", ResultadoIndicador.semDados(""));
        Map<String, Double> maior = Normalizador.minMax(r, Sentido.MAIOR_MELHOR);
        assertEquals(0.0, maior.get("x"), 1e-9);
        assertEquals(1.0, maior.get("y"), 1e-9);
        assertFalse(maior.containsKey("z"));
        assertEquals(1.0, Normalizador.minMax(r, Sentido.MENOR_MELHOR).get("x"), 1e-9);
    }

    @Test
    void somaPonderadaIgnoraIndicadorSemDadosEOrdena() {
        MatrizIndicadores matriz = new MatrizIndicadores(base, indicadores);
        Map<String, ItemRanking> r = porSq(new SomaPonderada().classificar(matriz, config(5, 5)));
        // assiduidade: A=1, B=0 ; patrimônio (menor melhor): A=0, B=1, C=0,5
        assertEquals(50.0, r.get("A").getPontuacao(), 1e-9);
        assertEquals(50.0, r.get("B").getPontuacao(), 1e-9);
        assertEquals(50.0, r.get("C").getPontuacao(), 1e-9);
        assertEquals("1/2", r.get("C").getCobertura());
        assertNull(r.get("D").getPontuacao());
        assertEquals(0, r.get("D").getPosicao());
    }

    @Test
    void coberturaMinimaTiraQuemTemPoucosIndicadores() {
        MatrizIndicadores matriz = new MatrizIndicadores(base, indicadores);
        ConfiguracaoRanking cfg = config(5, 5);
        cfg.setCoberturaMinima(2);
        Map<String, ItemRanking> r = porSq(new SomaPonderada().classificar(matriz, cfg));
        assertTrue(r.get("A").temPontuacao());
        assertFalse(r.get("C").temPontuacao(), "estreante só com patrimônio");
    }

    @Test
    void pesosMudamOrdem() {
        MatrizIndicadores matriz = new MatrizIndicadores(base, indicadores);
        List<ItemRanking> itens = new SomaPonderada().classificar(matriz, config(10, 1));
        assertEquals("A", itens.get(0).getCandidato().getSq());
        assertEquals("D", itens.get(itens.size() - 1).getCandidato().getSq(), "sem dados sempre no fim");
    }

    @Test
    void inverterSentido() {
        MatrizIndicadores matriz = new MatrizIndicadores(base, indicadores);
        ConfiguracaoRanking cfg = config(0, 10);
        cfg.setInvertido(VariacaoPatrimonio.CODIGO, true);
        assertEquals("A", new SomaPonderada().classificar(matriz, cfg).get(0).getCandidato().getSq());
    }

    @Test
    void topsisSoClassificaQuemTemTodosOsIndicadores() {
        MatrizIndicadores matriz = new MatrizIndicadores(base, indicadores);
        Map<String, ItemRanking> r = porSq(new Topsis().classificar(matriz, config(5, 5)));
        assertTrue(r.get("A").temPontuacao());
        assertTrue(r.get("B").temPontuacao());
        assertFalse(r.get("C").temPontuacao());
        // A e B são simétricos (um é ideal onde o outro é anti-ideal)
        assertEquals(50.0, r.get("A").getPontuacao(), 1e-9);
    }

    @Test
    void elegibilidadeEhFiltro() {
        base.buscarCandidato("A").setSituacao("INAPTO (INDEFERIDO)");
        MatrizIndicadores matriz = new MatrizIndicadores(base, indicadores);
        ConfiguracaoRanking cfg = config(5, 5);
        assertEquals(3, new SomaPonderada().classificar(matriz, cfg).size());
        cfg.setOcultarInaptos(false);
        assertEquals(4, new SomaPonderada().classificar(matriz, cfg).size());
    }
}
