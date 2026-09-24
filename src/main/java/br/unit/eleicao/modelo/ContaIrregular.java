package br.unit.eleicao.modelo;

/**
 * Processo em que o TCU julgou irregulares as contas de um gestor, com decisão definitiva
 * (lista enviada ao TSE para a Lei da Ficha Limpa). Só é ligada à candidatura com CPF conferido.
 */
public class ContaIrregular {

    private final String processo;
    private final String deliberacao;
    private final String dataTransito;
    private final String local;
    private final String criterio;

    public ContaIrregular(String processo, String deliberacao, String dataTransito, String local, String criterio) {
        this.processo = processo;
        this.deliberacao = deliberacao;
        this.dataTransito = dataTransito;
        this.local = local;
        this.criterio = criterio;
    }

    public String getProcesso() {
        return processo;
    }

    public String getDeliberacao() {
        return deliberacao;
    }

    public String getDataTransito() {
        return dataTransito;
    }

    public String getLocal() {
        return local;
    }

    /** Como a ligação foi feita ("CPF" ou "CPF parcial + nome"). */
    public String getCriterio() {
        return criterio;
    }
}
