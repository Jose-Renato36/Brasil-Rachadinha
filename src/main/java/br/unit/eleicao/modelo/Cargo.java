package br.unit.eleicao.modelo;

import br.unit.eleicao.util.Texto;

/**
 * Cargos eletivos, com nome em linguagem comum. "Principal" = o voto é dado diretamente para ele
 * (vice e suplentes vêm na mesma chapa do titular).
 */
public enum Cargo {
    PRESIDENTE("Presidente", "Presidência", true, 0),
    VICE_PRESIDENTE("Vice-presidente", "Vice-presidência", false, 1),
    GOVERNADOR("Governador(a)", "Governo do estado", true, 2),
    VICE_GOVERNADOR("Vice-governador(a)", "Vice-governo", false, 3),
    SENADOR("Senador(a)", "Senado", true, 4),
    SUPLENTE_SENADOR("Suplente de senador(a)", "Suplência no Senado", false, 5),
    DEPUTADO_FEDERAL("Deputado(a) federal", "Câmara dos Deputados", true, 6),
    DEPUTADO_ESTADUAL("Deputado(a) estadual", "Assembleia Legislativa", true, 7),
    DEPUTADO_DISTRITAL("Deputado(a) distrital", "Câmara Legislativa do DF", true, 8),
    PREFEITO("Prefeito(a)", "Prefeitura", true, 9),
    VICE_PREFEITO("Vice-prefeito(a)", "Vice-prefeitura", false, 10),
    VEREADOR("Vereador(a)", "Câmara Municipal", true, 11),
    OUTRO("Outro cargo", "Outro", false, 12);

    private final String rotulo;
    private final String orgao;
    private final boolean principal;
    private final int ordem;

    Cargo(String rotulo, String orgao, boolean principal, int ordem) {
        this.rotulo = rotulo;
        this.orgao = orgao;
        this.principal = principal;
        this.ordem = ordem;
    }

    /** Converte o DS_CARGO do TSE (ex.: "1º SUPLENTE", "VICE-GOVERNADOR", "DEPUTADO FEDERAL"). */
    public static Cargo de(String dsCargo) {
        String c = Texto.normalizar(dsCargo);
        switch (c) {
            case "PRESIDENTE":
                return PRESIDENTE;
            case "VICE PRESIDENTE":
                return VICE_PRESIDENTE;
            case "GOVERNADOR":
                return GOVERNADOR;
            case "VICE GOVERNADOR":
                return VICE_GOVERNADOR;
            case "SENADOR":
                return SENADOR;
            case "DEPUTADO FEDERAL":
                return DEPUTADO_FEDERAL;
            case "DEPUTADO ESTADUAL":
                return DEPUTADO_ESTADUAL;
            case "DEPUTADO DISTRITAL":
                return DEPUTADO_DISTRITAL;
            case "PREFEITO":
                return PREFEITO;
            case "VICE PREFEITO":
                return VICE_PREFEITO;
            case "VEREADOR":
                return VEREADOR;
            default:
                return c.contains("SUPLENTE") ? SUPLENTE_SENADOR : OUTRO;
        }
    }

    public boolean isMunicipal() {
        return this == PREFEITO || this == VICE_PREFEITO || this == VEREADOR;
    }

    /** Duração do mandato em anos (senador tem 8). */
    public int getDuracao() {
        return this == SENADOR || this == SUPLENTE_SENADOR ? 8 : 4;
    }

    public String getRotulo() {
        return rotulo;
    }

    public String getOrgao() {
        return orgao;
    }

    public boolean isPrincipal() {
        return principal;
    }

    public int getOrdem() {
        return ordem;
    }
}
