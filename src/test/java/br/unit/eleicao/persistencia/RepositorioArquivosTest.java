package br.unit.eleicao.persistencia;

import br.unit.eleicao.coleta.GeradorDadosDemo;
import br.unit.eleicao.modelo.BaseDados;
import br.unit.eleicao.modelo.Candidato;
import br.unit.eleicao.modelo.PosicaoUsuario;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RepositorioArquivosTest {

    @Test
    void gravarELerDevolveAMesmaBase(@TempDir Path dir) throws Exception {
        BaseDados original = new GeradorDadosDemo().gerar();
        original.setDiretorio(dir);
        String idVotacao = original.getVotacoesOrdenadas().get(0).getId();
        original.definirPosicao(new PosicaoUsuario(idVotacao, "Não", "tema; com separador"));
        RepositorioArquivos repo = new RepositorioArquivos();
        repo.salvar(original, dir);

        BaseDados lida = repo.carregar(dir);
        assertTrue(lida.getMetadados().isDemonstracao());
        assertEquals(original.getCandidatos().size(), lida.getCandidatos().size());
        assertEquals(original.getDeputados().size(), lida.getDeputados().size());
        assertEquals(original.getTotalVotacoes(), lida.getTotalVotacoes());
        assertEquals(original.getTodosVotos().get(9000).size(), lida.getTodosVotos().get(9000).size());
        assertEquals(original.getProposicoesDe(9000).size(), lida.getProposicoesDe(9000).size());
        assertEquals("tema; com separador", lida.getPosicao(idVotacao).getTema());

        Candidato a = original.getCandidatos().get(0);
        Candidato b = lida.buscarCandidato(a.getSq());
        assertEquals(a.getNomeUrna(), b.getNomeUrna());
        assertEquals(a.getGrauInstrucao(), b.getGrauInstrucao());
        assertEquals(a.getPatrimonio(), b.getPatrimonio());
        assertEquals(a.getIdDeputado(), b.getIdDeputado());
        assertEquals(a.getDataNascimento(), b.getDataNascimento());
    }
}
