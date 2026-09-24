package br.unit.eleicao.preparo;

import br.unit.eleicao.modelo.Candidato;
import br.unit.eleicao.modelo.CandidaturaAnterior;
import br.unit.eleicao.modelo.GrauInstrucao;
import br.unit.eleicao.modelo.Metadados;
import br.unit.eleicao.modelo.Sancao;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuadroPreparoTest {

    private final Metadados meta = new Metadados("ZZ", 2026, 2022, LocalDate.of(2026, 10, 4));

    private static Candidato candidato(String cargo, LocalDate nascimento) {
        Candidato c = new Candidato("1", "ANA", "ANA", nascimento, "FEMININO");
        c.setCargo(cargo);
        c.setSituacao("APTO");
        c.setDetalheSituacao("DEFERIDO");
        return c;
    }

    @Test
    void idadeMinimaContadaNaPosse() {
        // senador exige 35 anos em 1º/2/2027
        assertEquals(Avaliacao.POSITIVO, new IdadeMinima().avaliar(candidato("SENADOR", LocalDate.of(1992, 2, 1)), meta).getAvaliacao());
        assertEquals(Avaliacao.NEGATIVO, new IdadeMinima().avaliar(candidato("SENADOR", LocalDate.of(1992, 2, 2)), meta).getAvaliacao());
        // governador: 30 anos em 6/1/2027
        assertEquals(Avaliacao.POSITIVO, new IdadeMinima().avaliar(candidato("GOVERNADOR", LocalDate.of(1997, 1, 6)), meta).getAvaliacao());
        assertEquals(Avaliacao.SEM_DADOS, new IdadeMinima().avaliar(candidato("VEREADOR", null), meta).getAvaliacao());
    }

    @Test
    void experienciaSomaAnosPorPoderEReconheceMesmaFuncao() {
        Candidato c = candidato("GOVERNADOR", LocalDate.of(1970, 1, 1));
        c.adicionarCandidaturaAnterior(new CandidaturaAnterior(2020, "PREFEITO", "CIDADE/ZZ", "PXA", "Eleito(a)"));
        c.adicionarCandidaturaAnterior(new CandidaturaAnterior(2018, "DEPUTADO ESTADUAL", "ZZ", "PXA", "Eleito(a)"));
        c.adicionarCandidaturaAnterior(new CandidaturaAnterior(2024, "PREFEITO", "CIDADE/ZZ", "PXA", "Não eleito(a)"));
        ItemPreparo exp = new ExperienciaEletiva().avaliar(c, meta);
        assertEquals("8 anos de mandato (4 anos no Executivo, 4 anos no Legislativo)", exp.getResumo());
        ItemPreparo funcao = new ExperienciaMesmaFuncao().avaliar(c, meta);
        assertEquals(Avaliacao.POSITIVO, funcao.getAvaliacao());
        assertTrue(funcao.getResumo().startsWith("Função parecida: prefeito"));
    }

    @Test
    void estreanteNaoRecebeAvaliacaoNegativa() {
        Candidato c = candidato("DEPUTADO FEDERAL", LocalDate.of(1990, 1, 1));
        c.setGrauInstrucao(GrauInstrucao.MEDIO_COMPLETO);
        List<ItemPreparo> itens = new QuadroPreparo().avaliar(c, meta);
        assertTrue(itens.stream().noneMatch(i -> i.getAvaliacao() == Avaliacao.NEGATIVO));
        assertNull(new AlertaCassacao().avaliar(c, meta), "sem candidaturas anteriores não há o que conferir");
    }

    @Test
    void alertasDistinguemNaoConsultadoDeNadaConsta() {
        Candidato c = candidato("DEPUTADO FEDERAL", LocalDate.of(1980, 1, 1));
        assertEquals(Avaliacao.SEM_DADOS, new AlertaSancoes().avaliar(c, meta).getAvaliacao());
        meta.marcarVerificada(Metadados.FONTE_SANCOES);
        assertEquals(Avaliacao.POSITIVO, new AlertaSancoes().avaliar(c, meta).getAvaliacao());
        c.adicionarSancao(new Sancao("CEIS", "EMPRESA", "Suspensão", "ÓRGÃO", "", "", true));
        ItemPreparo i = new AlertaSancoes().avaliar(c, meta);
        assertEquals(Avaliacao.ATENCAO, i.getAvaliacao());
        assertEquals("1 em empresa(s) de que é sócia", i.getResumo());
    }
}
