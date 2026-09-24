package br.unit.eleicao.ranking;

import br.unit.eleicao.indicador.Indicador;
import br.unit.eleicao.indicador.Sentido;

import java.util.HashMap;
import java.util.Map;

/** Escolhas do usuário: peso de cada indicador (0 a 10), sentido invertido ou não e filtro de elegibilidade. */
public class ConfiguracaoRanking {

    public static final int PESO_MAXIMO = 10;

    private final Map<String, Integer> pesos = new HashMap<>();
    private final Map<String, Boolean> invertidos = new HashMap<>();
    private boolean ocultarInaptos = true;
    private int coberturaMinima = 1;
    private br.unit.eleicao.modelo.Cargo cargo;
    private String uf;

    public void setPeso(String codigo, int peso) {
        if (peso < 0 || peso > PESO_MAXIMO) {
            throw new IllegalArgumentException("Peso deve estar entre 0 e " + PESO_MAXIMO + ": " + peso);
        }
        pesos.put(codigo, peso);
    }

    public int getPeso(String codigo) {
        return pesos.getOrDefault(codigo, 0);
    }

    public void setInvertido(String codigo, boolean invertido) {
        invertidos.put(codigo, invertido);
    }

    public boolean isInvertido(String codigo) {
        return invertidos.getOrDefault(codigo, false);
    }

    public Sentido sentidoEfetivo(Indicador ind) {
        return isInvertido(ind.getCodigo()) ? ind.getSentidoPadrao().inverso() : ind.getSentidoPadrao();
    }

    /**
     * Quantos indicadores com dados um candidato precisa ter para receber pontuação.
     * Com 1, um estreante pode ser pontuado só pelo patrimônio; valores maiores evitam isso,
     * mas tiram os estreantes do ranking. A escolha é do usuário.
     */
    public int getCoberturaMinima() {
        return coberturaMinima;
    }

    public void setCoberturaMinima(int coberturaMinima) {
        if (coberturaMinima < 1) {
            throw new IllegalArgumentException("Cobertura mínima deve ser pelo menos 1: " + coberturaMinima);
        }
        this.coberturaMinima = coberturaMinima;
    }

    /** Só compara candidaturas deste cargo (null = todas). Governador se compara com governador. */
    public br.unit.eleicao.modelo.Cargo getCargo() {
        return cargo;
    }

    public void setCargo(br.unit.eleicao.modelo.Cargo cargo) {
        this.cargo = cargo;
    }

    /** Só compara candidaturas desta UF (null = todas; presidente é sempre nacional). */
    public String getUf() {
        return uf;
    }

    public void setUf(String uf) {
        this.uf = uf;
    }

    /** true se a candidatura entra na comparação pelo cargo e pela UF escolhidos. */
    public boolean aceita(br.unit.eleicao.modelo.Candidato c) {
        if (cargo != null && c.getTipoCargo() != cargo) {
            return false;
        }
        boolean nacional = c.getTipoCargo() == br.unit.eleicao.modelo.Cargo.PRESIDENTE
                || c.getTipoCargo() == br.unit.eleicao.modelo.Cargo.VICE_PRESIDENTE;
        return uf == null || nacional || uf.equalsIgnoreCase(c.getUf());
    }

    public boolean isOcultarInaptos() {
        return ocultarInaptos;
    }

    public void setOcultarInaptos(boolean ocultarInaptos) {
        this.ocultarInaptos = ocultarInaptos;
    }
}
