package br.unit.eleicao.coleta;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HistoricoDeputadosTest {

    /** Formato da resposta de /api/v2/deputados/{id}/historico (campos conferidos em projeto que usa a API). */
    private static final String JSON = "{\"dados\":["
            + "{\"id\":1,\"idLegislatura\":57,\"dataHora\":\"2022-12-19T10:00\",\"situacao\":\"Exerc\\u00edcio\",\"descricaoStatus\":\"Diplomação\"},"
            + "{\"id\":1,\"idLegislatura\":57,\"dataHora\":\"2023-02-01T15:00\",\"situacao\":\"Exercício\",\"descricaoStatus\":\"Posse\"},"
            + "{\"id\":1,\"idLegislatura\":57,\"dataHora\":\"2024-03-01T00:00\",\"situacao\":\"Afastado\",\"descricaoStatus\":\"Licença\"},"
            + "{\"id\":1,\"idLegislatura\":57,\"dataHora\":\"2024-07-01T00:00\",\"situacao\":\"Exercício\",\"descricaoStatus\":\"Reassunção\"},"
            + "{\"id\":1,\"idLegislatura\":56,\"dataHora\":\"2023-05-01T00:00\",\"situacao\":\"Afastado\",\"descricaoStatus\":\"outra legislatura\"},"
            + "{\"id\":1,\"idLegislatura\":57,\"dataHora\":\"2024-08-01T00:00\",\"situacao\":null,\"descricaoStatus\":\"sem situação\"}"
            + "],\"links\":[]}";

    @Test
    void montaPeriodosDeExercicio() {
        List<LocalDate[]> p = HistoricoDeputados.periodos(JSON, "57", LocalDate.of(2023, 2, 1), LocalDate.of(2026, 9, 1));
        assertEquals(2, p.size());
        assertEquals(LocalDate.of(2023, 2, 1), p.get(0)[0]);
        assertEquals(LocalDate.of(2024, 3, 1), p.get(0)[1]);
        assertEquals(LocalDate.of(2024, 7, 1), p.get(1)[0]);
        assertEquals(LocalDate.of(2026, 9, 1), p.get(1)[1]);
    }

    @Test
    void campoDecodificaEscapes() {
        assertEquals("Exercício", HistoricoDeputados.campo("{\"situacao\":\"Exerc\\u00edcio\"}", "situacao"));
        assertEquals("57", HistoricoDeputados.campo("{\"idLegislatura\": 57}", "idLegislatura"));
        assertEquals("", HistoricoDeputados.campo("{\"situacao\":null}", "situacao"));
    }
}
