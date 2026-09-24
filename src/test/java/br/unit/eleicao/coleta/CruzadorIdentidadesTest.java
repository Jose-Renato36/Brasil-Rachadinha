package br.unit.eleicao.coleta;

import br.unit.eleicao.modelo.Candidato;
import br.unit.eleicao.modelo.Deputado;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CruzadorIdentidadesTest {

    private static final LocalDate NASC = LocalDate.of(1970, 5, 20);

    @Test
    void ordemDeConfiancaDosCriterios() {
        Deputado d1 = new Deputado(1, "Zé da Feira", "José Pereira Santos", NASC, "");
        Deputado d2 = new Deputado(2, "Maria Luz", "Maria da Luz Costa", NASC, "");
        Deputado d3 = new Deputado(3, "Doutor Paulo", "Paulo Mendes", LocalDate.of(1960, 1, 1), "");

        Candidato porCpf = new Candidato("10", "JOSE PEREIRA SANTOS", "ZÉ", NASC, "");
        Candidato porNomeNasc = new Candidato("20", "MARIA DA LUZ COSTA", "MARIA LUZ", NASC, "");
        Candidato porUrna = new Candidato("30", "PAULO ROBERTO MENDES", "DOUTOR PAULO", null, "");
        Candidato estreante = new Candidato("40", "FULANO NOVO", "FULANO", NASC, "");

        CruzadorIdentidades cruzador = new CruzadorIdentidades();
        cruzador.cruzar(List.of(porCpf, porNomeNasc, porUrna, estreante), List.of(d1, d2, d3),
                Map.of("10", "12345678901"), Map.of(1, "12345678901"), Map.of());

        assertEquals(1, porCpf.getIdDeputado());
        assertEquals(CruzadorIdentidades.CPF, porCpf.getCriterioVinculo());
        assertEquals(2, porNomeNasc.getIdDeputado());
        assertEquals(CruzadorIdentidades.NOME_NASCIMENTO, porNomeNasc.getCriterioVinculo());
        assertEquals(3, porUrna.getIdDeputado());
        assertTrue(porUrna.isVinculoDuvidoso());
        assertNull(estreante.getIdDeputado());
        assertEquals(1, cruzador.getParaConferir().size());
    }

    @Test
    void homonimosNaoSaoVinculadosAutomaticamente() {
        Deputado d1 = new Deputado(1, "João Silva", "João Silva", LocalDate.of(1960, 1, 1), "");
        Deputado d2 = new Deputado(2, "João Silva", "João Silva", LocalDate.of(1975, 1, 1), "");
        Candidato c = new Candidato("1", "JOAO SILVA", "JOAO SILVA", null, "");
        CruzadorIdentidades cruzador = new CruzadorIdentidades();
        cruzador.cruzar(List.of(c), List.of(d1, d2), Map.of(), Map.of(), Map.of());
        assertNull(c.getIdDeputado());
        assertTrue(cruzador.getParaConferir().get(0).contains("AMBÍGUO"));
    }

    @Test
    void vinculoManualTemPrioridade() {
        Deputado d1 = new Deputado(1, "A", "Ana", NASC, "");
        Candidato c = new Candidato("1", "ANA", "A", NASC, "");
        new CruzadorIdentidades().cruzar(List.of(c), List.of(d1), Map.of(), Map.of(),
                Map.of("1", CruzadorIdentidades.SEM_VINCULO));
        assertNull(c.getIdDeputado());
    }
}
