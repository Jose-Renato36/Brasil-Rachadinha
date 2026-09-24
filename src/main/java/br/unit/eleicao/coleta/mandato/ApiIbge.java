package br.unit.eleicao.coleta.mandato;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * API de agregados do IBGE (SIDRA), versão 3:
 * servicodados.ibge.gov.br/api/v3/agregados/TABELA/periodos/all/variaveis/VAR?localidades=N6[codigo].
 * Resposta: lista de variáveis, cada uma com "resultados" → "series" → "serie": {"2019": "123", ...}.
 */
public final class ApiIbge {

    private static final Pattern VARIAVEL = Pattern.compile("\\{\\s*\"id\"\\s*:\\s*\"(\\d+)\"\\s*,\\s*\"variavel\"");
    private static final Pattern SERIE = Pattern.compile("\"serie\"\\s*:\\s*\\{([^}]*)\\}");
    private static final Pattern PAR = Pattern.compile("\"(\\d+)\"\\s*:\\s*(?:\"([^\"]*)\"|null)");

    private ApiIbge() {
    }

    /** @param nivel "N6" (município), "N3" (UF) ou "N1" (Brasil) */
    public static String url(int tabela, String variaveis, String nivel, String codigo) {
        return "https://servicodados.ibge.gov.br/api/v3/agregados/" + tabela + "/periodos/all/variaveis/" + variaveis
                + "?localidades=" + nivel + "%5B" + codigo + "%5D";
    }

    /** Nível territorial do IBGE para um código (7 dígitos = município, 2 = UF, 1 = Brasil). */
    public static String nivel(String codigo) {
        return codigo.length() >= 6 ? "N6" : codigo.length() == 2 ? "N3" : "N1";
    }

    /**
     * Lê a primeira série de cada variável da resposta.
     *
     * @return variável → (período → valor); valores "-", "..." e "X" (sigilo) ficam de fora
     */
    public static Map<String, Map<String, Double>> ler(String json) {
        Map<String, Map<String, Double>> resultado = new LinkedHashMap<>();
        if (json == null) {
            return resultado;
        }
        Matcher mv = VARIAVEL.matcher(json);
        java.util.List<int[]> posicoes = new java.util.ArrayList<>();
        java.util.List<String> ids = new java.util.ArrayList<>();
        while (mv.find()) {
            posicoes.add(new int[]{mv.start()});
            ids.add(mv.group(1));
        }
        for (int i = 0; i < ids.size(); i++) {
            int inicio = posicoes.get(i)[0];
            int fim = i + 1 < ids.size() ? posicoes.get(i + 1)[0] : json.length();
            Matcher ms = SERIE.matcher(json.substring(inicio, fim));
            Map<String, Double> valores = new TreeMap<>();
            if (ms.find()) {
                Matcher mp = PAR.matcher(ms.group(1));
                while (mp.find()) {
                    Double v = numero(mp.group(2));
                    if (v != null) {
                        valores.put(mp.group(1), v);
                    }
                }
            }
            resultado.put(ids.get(i), valores);
        }
        return resultado;
    }

    static Double numero(String texto) {
        if (texto == null) {
            return null;
        }
        String t = texto.strip();
        if (t.isEmpty() || t.equals("-") || t.equals("...") || t.equalsIgnoreCase("X") || t.equals("..")) {
            return null;
        }
        try {
            return Double.valueOf(t);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** Série anual (período AAAA → valor). */
    public static Map<Integer, Double> anual(Map<String, Double> porPeriodo) {
        Map<Integer, Double> anos = new TreeMap<>();
        for (Map.Entry<String, Double> e : porPeriodo.entrySet()) {
            if (e.getKey().length() == 4) {
                anos.put(Integer.parseInt(e.getKey()), e.getValue());
            }
        }
        return anos;
    }

    /** Média anual de uma série trimestral (período AAAAQQ), só para anos com os 4 trimestres. */
    public static Map<Integer, Double> mediaAnualDeTrimestres(Map<String, Double> porPeriodo) {
        Map<Integer, double[]> soma = new TreeMap<>();
        for (Map.Entry<String, Double> e : porPeriodo.entrySet()) {
            if (e.getKey().length() == 6) {
                double[] s = soma.computeIfAbsent(Integer.parseInt(e.getKey().substring(0, 4)), k -> new double[2]);
                s[0] += e.getValue();
                s[1]++;
            }
        }
        Map<Integer, Double> anos = new TreeMap<>();
        for (Map.Entry<Integer, double[]> e : soma.entrySet()) {
            if (e.getValue()[1] == 4) {
                anos.put(e.getKey(), e.getValue()[0] / 4);
            }
        }
        return anos;
    }
}
