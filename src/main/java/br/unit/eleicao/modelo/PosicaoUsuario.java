package br.unit.eleicao.modelo;

/** Como o próprio usuário diz que votaria numa votação escolhida por ele (base do alinhamento por pauta). */
public class PosicaoUsuario {

    public static final String SIM = "Sim";
    public static final String NAO = "Não";

    private final String idVotacao;
    private String voto;
    private String tema;

    public PosicaoUsuario(String idVotacao, String voto, String tema) {
        this.idVotacao = idVotacao;
        setVoto(voto);
        this.tema = tema == null ? "" : tema;
    }

    public String getIdVotacao() {
        return idVotacao;
    }

    public String getVoto() {
        return voto;
    }

    public final void setVoto(String voto) {
        if (!SIM.equalsIgnoreCase(voto) && !NAO.equalsIgnoreCase(voto) && !"Nao".equalsIgnoreCase(voto)) {
            throw new IllegalArgumentException("Posição deve ser Sim ou Não: " + voto);
        }
        this.voto = SIM.equalsIgnoreCase(voto) ? SIM : NAO;
    }

    public String getTema() {
        return tema;
    }

    public void setTema(String tema) {
        this.tema = tema == null ? "" : tema;
    }
}
