package br.unit.eleicao.modelo;

import br.unit.eleicao.util.Texto;

/**
 * Uma candidatura da mesma pessoa numa eleição anterior (qualquer cargo), vinda dos arquivos
 * históricos do TSE. Serve para mostrar a trajetória política; nunca entra na nota.
 */
public class CandidaturaAnterior implements Comparable<CandidaturaAnterior> {

    /** Cargos disputados nas eleições municipais (mandato começa no ano seguinte e dura 4 anos). */
    private static final String[] CARGOS_MUNICIPAIS = {"PREFEITO", "VICE PREFEITO", "VEREADOR"};

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
        switch (Texto.normalizar(cargo)) {
            case "DEPUTADO FEDERAL":
                return "Deputado(a) federal";
            case "DEPUTADO ESTADUAL":
                return "Deputado(a) estadual";
            case "DEPUTADO DISTRITAL":
                return "Deputado(a) distrital";
            case "VEREADOR":
                return "Vereador(a)";
            case "PREFEITO":
                return "Prefeito(a)";
            case "VICE PREFEITO":
                return "Vice-prefeito(a)";
            case "SENADOR":
                return "Senador(a)";
            case "GOVERNADOR":
                return "Governador(a)";
            case "VICE GOVERNADOR":
                return "Vice-governador(a)";
            case "1O SUPLENTE":
            case "1 SUPLENTE":
                return "1º suplente de senador(a)";
            case "2O SUPLENTE":
            case "2 SUPLENTE":
                return "2º suplente de senador(a)";
            default:
                return cargo.isEmpty() ? "" : cargo.charAt(0) + cargo.substring(1).toLowerCase();
        }
    }

    public boolean isEleito() {
        return resultado.startsWith("Eleito");
    }

    public boolean isMunicipal() {
        String c = Texto.normalizar(cargo);
        for (String m : CARGOS_MUNICIPAIS) {
            if (c.equals(m)) {
                return true;
            }
        }
        return false;
    }

    /** Último ano do mandato conquistado (mandatos de 4 anos; senador tem 8). 0 se não foi eleito. */
    public int getFimMandato() {
        if (!isEleito()) {
            return 0;
        }
        return ano + (Texto.normalizar(cargo).equals("SENADOR") ? 8 : 4);
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
