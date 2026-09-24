package br.unit.eleicao.coleta.mandato;

import br.unit.eleicao.util.Texto;

import java.util.HashMap;
import java.util.Map;

/** Códigos IBGE das UFs e o nome de cada uma (para achar o estado nas planilhas do INEP). */
public final class CodigosIbge {

    public static final Map<String, String> CODIGO_UF = Map.ofEntries(
            Map.entry("RO", "11"), Map.entry("AC", "12"), Map.entry("AM", "13"), Map.entry("RR", "14"),
            Map.entry("PA", "15"), Map.entry("AP", "16"), Map.entry("TO", "17"), Map.entry("MA", "21"),
            Map.entry("PI", "22"), Map.entry("CE", "23"), Map.entry("RN", "24"), Map.entry("PB", "25"),
            Map.entry("PE", "26"), Map.entry("AL", "27"), Map.entry("SE", "28"), Map.entry("BA", "29"),
            Map.entry("MG", "31"), Map.entry("ES", "32"), Map.entry("RJ", "33"), Map.entry("SP", "35"),
            Map.entry("PR", "41"), Map.entry("SC", "42"), Map.entry("RS", "43"), Map.entry("MS", "50"),
            Map.entry("MT", "51"), Map.entry("GO", "52"), Map.entry("DF", "53"));

    public static final Map<String, String> NOME_UF = Map.ofEntries(
            Map.entry("RO", "Rondônia"), Map.entry("AC", "Acre"), Map.entry("AM", "Amazonas"), Map.entry("RR", "Roraima"),
            Map.entry("PA", "Pará"), Map.entry("AP", "Amapá"), Map.entry("TO", "Tocantins"), Map.entry("MA", "Maranhão"),
            Map.entry("PI", "Piauí"), Map.entry("CE", "Ceará"), Map.entry("RN", "Rio Grande do Norte"),
            Map.entry("PB", "Paraíba"), Map.entry("PE", "Pernambuco"), Map.entry("AL", "Alagoas"),
            Map.entry("SE", "Sergipe"), Map.entry("BA", "Bahia"), Map.entry("MG", "Minas Gerais"),
            Map.entry("ES", "Espírito Santo"), Map.entry("RJ", "Rio de Janeiro"), Map.entry("SP", "São Paulo"),
            Map.entry("PR", "Paraná"), Map.entry("SC", "Santa Catarina"), Map.entry("RS", "Rio Grande do Sul"),
            Map.entry("MS", "Mato Grosso do Sul"), Map.entry("MT", "Mato Grosso"), Map.entry("GO", "Goiás"),
            Map.entry("DF", "Distrito Federal"));

    private static final Map<String, String> SIGLA_POR_NOME = new HashMap<>();

    static {
        for (Map.Entry<String, String> e : NOME_UF.entrySet()) {
            SIGLA_POR_NOME.put(Texto.normalizar(e.getValue()), e.getKey());
        }
        // abreviações usadas nas planilhas do INEP
        SIGLA_POR_NOME.put("R G DO NORTE", "RN");
        SIGLA_POR_NOME.put("R G DO SUL", "RS");
        SIGLA_POR_NOME.put("M G DO SUL", "MS");
    }

    private CodigosIbge() {
    }

    /** Sigla a partir do nome escrito por extenso ou abreviado; null se não for uma UF. */
    public static String siglaPorNome(String nome) {
        String n = Texto.normalizar(nome).replace(".", " ").replaceAll("\\s+", " ").strip();
        return SIGLA_POR_NOME.get(n);
    }
}
