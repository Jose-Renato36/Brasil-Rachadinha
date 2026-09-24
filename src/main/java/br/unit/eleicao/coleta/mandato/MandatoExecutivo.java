package br.unit.eleicao.coleta.mandato;

import br.unit.eleicao.modelo.Cargo;
import br.unit.eleicao.modelo.SerieMandato;

/**
 * Um mandato no Executivo que a pessoa conquistou (prefeitura, governo do estado ou Presidência),
 * com os códigos necessários para consultar as fontes: código IBGE do município ou da UF.
 */
public class MandatoExecutivo {

    private final Cargo cargo;
    private final String ente;
    private final String codigoIbge;
    private final String uf;
    private final int anoEleicao;
    private final int fimMandato;
    private final int ultimoAnoFechado;

    /**
     * @param codigoIbge 7 dígitos (município), 2 dígitos (UF) ou "1" (Brasil)
     * @param ultimoAnoFechado último ano com dados anuais possíveis (normalmente o ano anterior à coleta)
     */
    public MandatoExecutivo(Cargo cargo, String ente, String codigoIbge, String uf, int anoEleicao, int fimMandato,
                            int ultimoAnoFechado) {
        this.cargo = cargo;
        this.ente = ente;
        this.codigoIbge = codigoIbge;
        this.uf = uf;
        this.anoEleicao = anoEleicao;
        this.fimMandato = fimMandato;
        this.ultimoAnoFechado = ultimoAnoFechado;
    }

    public boolean isMunicipal() {
        return cargo == Cargo.PREFEITO;
    }

    public boolean isEstadual() {
        return cargo == Cargo.GOVERNADOR;
    }

    public boolean isFederal() {
        return cargo == Cargo.PRESIDENTE;
    }

    /** Primeiro ano consultado: o anterior à eleição, para mostrar a tendência que a pessoa encontrou. */
    public int getPrimeiroAno() {
        return anoEleicao - 1;
    }

    /** Último ano consultado: fim do mandato ou último ano encerrado, o que vier antes. */
    public int getUltimoAno() {
        return Math.min(fimMandato, ultimoAnoFechado);
    }

    public boolean contem(int ano) {
        return ano >= getPrimeiroAno() && ano <= getUltimoAno();
    }

    public String getChave() {
        return SerieMandato.descreverMandato(cargo, ente, anoEleicao, fimMandato);
    }

    public Cargo getCargo() {
        return cargo;
    }

    public String getEnte() {
        return ente;
    }

    public String getCodigoIbge() {
        return codigoIbge;
    }

    public String getUf() {
        return uf;
    }

    public int getAnoEleicao() {
        return anoEleicao;
    }

    public int getFimMandato() {
        return fimMandato;
    }
}
