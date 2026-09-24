package br.unit.eleicao.modelo;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * De onde veio o dinheiro da campanha atual, somado por origem (prestação de contas entregue ao TSE).
 * Durante a campanha os dados são parciais: o TSE publica o que já foi declarado.
 */
public class Financiamento {

    private final Map<String, Double> porOrigem = new LinkedHashMap<>();

    /** Soma um valor numa categoria já legível (ver {@link #origemLegivel}). */
    public void somar(String origem, double valor) {
        porOrigem.merge(origem, valor, Double::sum);
    }

    public double getTotal() {
        double t = 0;
        for (double v : porOrigem.values()) {
            t += v;
        }
        return t;
    }

    public boolean isVazio() {
        return porOrigem.isEmpty();
    }

    /** Parte do total (0 a 1) vinda da origem informada. */
    public double participacao(String origem) {
        double total = getTotal();
        return total <= 0 ? 0 : porOrigem.getOrDefault(origem, 0.0) / total;
    }

    public Map<String, Double> getPorOrigem() {
        return Collections.unmodifiableMap(porOrigem);
    }

    /** Agrupa os textos do TSE (DS_ORIGEM_RECEITA / DS_FONTE_RECEITA) em categorias que o eleitor entende. */
    public static String origemLegivel(String texto) {
        String t = br.unit.eleicao.util.Texto.normalizar(texto);
        if (t.contains("FUNDO ESPECIAL") || t.contains("FEFC")) {
            return "Fundo eleitoral (dinheiro público)";
        }
        if (t.contains("FUNDO PARTIDARIO")) {
            return "Fundo partidário (dinheiro público)";
        }
        if (t.contains("PROPRIOS")) {
            return "Dinheiro do próprio candidato";
        }
        if (t.contains("PESSOAS FISICAS") || t.contains("PESSOA FISICA")) {
            return "Doações de pessoas";
        }
        if (t.contains("FINANCIAMENTO COLETIVO") || t.contains("INTERNET")) {
            return "Vaquinha on-line";
        }
        if (t.contains("PARTIDO") || t.contains("OUTROS CANDIDATOS") || t.contains("CANDIDATO")) {
            return "Repasses de partidos e outros candidatos";
        }
        return t.isEmpty() ? "Não informado" : "Outras origens";
    }
}
