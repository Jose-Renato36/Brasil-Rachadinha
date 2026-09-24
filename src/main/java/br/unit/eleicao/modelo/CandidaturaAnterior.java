package br.unit.eleicao.modelo;

import br.unit.eleicao.util.Texto;

/**
 * Uma candidatura da mesma pessoa numa eleição anterior (qualquer cargo), vinda dos arquivos
 * históricos do TSE. Serve para mostrar a trajetória política; nunca entra na nota.
 */
public class CandidaturaAnterior implements Comparable<CandidaturaAnterior> {

    private final int ano;
    private final String cargo;
    private final String local;
    private final String partido;
    private final String resultado;

    public CandidaturaAnterior(int ano, String cargo, String local, String partido, String resultado) {
        this.ano = ano;
        this.cargo = cargo == null ? "" : cargo;
        this.local = local == null ? "" : local;
        this.partido = partido == null ? "" : partido;
        this.resultado = resultado == null ? "" : resultado;
    }

    /** Converte DS_SIT_TOT_TURNO do TSE em texto simples. */
    public static String resultadoSimples(String dsSitTotTurno) {
        String s = Texto.normalizar(dsSitTotTurno);
        if (s.startsWith("ELEITO")) {
            return "Eleito(a)";
        }
        if (s.startsWith("SUPLENTE")) {
            return "Suplente";
        }
        if (s.startsWith("NAO ELEITO")) {
            return "Não eleito(a)";
        }
        if (s.contains("2 TURNO") || s.contains("2O TURNO")) {
            return "Foi ao 2º turno";
        }
        return "Sem resultado";
    }

    /** Nome do cargo em linguagem comum, com flexão de gênero neutra (ex.: "Deputado(a) estadual"). */
    public String getCargoLegivel() {
        Cargo c = getTipoCargo();
        return c == Cargo.OUTRO ? cargo : c.getRotulo();
    }

    public Cargo getTipoCargo() {
        return Cargo.de(cargo);
    }

    public boolean isEleito() {
        return resultado.startsWith("Eleito");
    }

    public boolean isMunicipal() {
        return getTipoCargo().isMunicipal();
    }

    /** Último ano do mandato conquistado (mandatos de 4 anos; senador tem 8). 0 se não foi eleito. */
    public int getFimMandato() {
        if (!isEleito()) {
            return 0;
        }
        return ano + getTipoCargo().getDuracao();
    }

    /** Frase para a tela, ex.: "2024 · Vereador(a) em ARACAJU · PXA · Eleito(a)". */
    public String getDescricao() {
        return ano + " · " + getCargoLegivel() + (local.isEmpty() ? "" : " (" + local + ")") + " · " + partido + " · " + resultado;
    }

    /** Mais recentes primeiro. */
    @Override
    public int compareTo(CandidaturaAnterior o) {
        return Integer.compare(o.ano, ano);
    }

    public int getAno() {
        return ano;
    }

    public String getCargo() {
        return cargo;
    }

    public String getLocal() {
        return local;
    }

    public String getPartido() {
        return partido;
    }

    public String getResultado() {
        return resultado;
    }
}
