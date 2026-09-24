package br.unit.eleicao.indicador;

import br.unit.eleicao.modelo.BaseDados;
import br.unit.eleicao.modelo.Candidato;
import br.unit.eleicao.modelo.Deputado;
import br.unit.eleicao.modelo.PosicaoUsuario;
import br.unit.eleicao.util.Texto;

import java.util.Collection;
import java.util.Map;

/**
 * % de votos do deputado iguais à posição que o próprio usuário marcou em cada votação.
 * Só entram votações em que o deputado votou Sim ou Não (ausência, abstenção e obstrução não contam).
 */
public class AlinhamentoPauta extends Indicador {

    public static final String CODIGO = "alinhamento";

    public AlinhamentoPauta() {
        super(CODIGO, "Alinhamento por pauta", "Câmara dos Deputados - votos nominais + suas posições",
                "% de votos Sim/Não do deputado iguais ao voto que você marcou em \"Minhas opiniões\".",
                Sentido.MAIOR_MELHOR);
    }

    @Override
    public ResultadoIndicador calcular(Candidato candidato, BaseDados base) {
        Collection<PosicaoUsuario> posicoes = base.getPosicoes();
        if (posicoes.isEmpty()) {
            return ResultadoIndicador.semDados("Você ainda não respondeu nenhuma pergunta em \"Minhas opiniões\"");
        }
        Deputado d = deputadoDe(candidato, base);
        if (d == null) {
            return semMandato();
        }
        Map<String, String> votos = base.getVotosDe(d.getId());
        int comparaveis = 0;
        int coincidentes = 0;
        for (PosicaoUsuario p : posicoes) {
            String voto = Texto.normalizar(votos.get(p.getIdVotacao()));
            if (!voto.equals("SIM") && !voto.equals("NAO")) {
                continue;
            }
            comparaveis++;
            if (voto.equals(Texto.normalizar(p.getVoto()))) {
                coincidentes++;
            }
        }
        if (comparaveis == 0) {
            return ResultadoIndicador.semDados("Não votou Sim/Não em nenhuma das " + posicoes.size()
                    + " votações que você marcou");
        }
        return ResultadoIndicador.com(100.0 * coincidentes / comparaveis,
                "Coincidiu com você em " + coincidentes + " de " + comparaveis + " votações comparáveis");
    }

    @Override
    public String getTituloSimples() {
        return "Vota como eu votaria";
    }

    @Override
    public String getPergunta() {
        return "Quanto importa que a pessoa tenha votado como você votaria? (responda em \"Minhas opiniões\")";
    }

    @Override
    public String resumir(double valor) {
        return "Votou igual a você em " + Texto.percentual(valor) + " das vezes";
    }

    @Override
    public String formatar(double valor) {
        return Texto.percentual(valor);
    }
}
