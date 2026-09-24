package br.unit.eleicao.util;

import java.text.Normalizer;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;

/** Funções utilitárias de texto, números e datas (métodos estáticos, classe não instanciável). */
public final class Texto {

    public static final Locale PT_BR = Locale.forLanguageTag("pt-BR");
    private static final DateTimeFormatter DATA_BR = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private Texto() {
    }

    public static boolean vazio(String s) {
        return s == null || s.isBlank();
    }

    /** Remove acentos, pontuação e espaços repetidos; devolve em maiúsculas. Usado para comparar nomes. */
    public static String normalizar(String s) {
        if (s == null) {
            return "";
        }
        String semAcento = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return semAcento.toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9 ]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    /** O TSE usa marcadores como #NULO#, #NE# e -1/-3/-4 para "não informado". */
    public static String limparTse(String s) {
        if (s == null) {
            return "";
        }
        String t = s.strip();
        if (t.startsWith("#") && t.endsWith("#")) {
            return "";
        }
        if (t.equals("-1") || t.equals("-3") || t.equals("-4")) {
            return "";
        }
        return t;
    }

    public static String somenteDigitos(String s) {
        return s == null ? "" : s.replaceAll("\\D", "");
    }

    /**
     * Converte "1.234,56" (formato brasileiro) ou "1234.56" em número.
     * Devolve null quando o texto não é um número: entrada vazia é caso comum, não exceção.
     */
    public static Double parseDecimal(String s) {
        if (vazio(s)) {
            return null;
        }
        String t = s.strip();
        if (t.contains(",")) {
            t = t.replace(".", "").replace(",", ".");
        }
        try {
            return Double.parseDouble(t);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static Integer parseInteiro(String s) {
        if (vazio(s)) {
            return null;
        }
        try {
            return Integer.parseInt(s.strip());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** Aceita "dd/MM/yyyy", "yyyy-MM-dd" e "yyyy-MM-ddTHH:mm:ss". */
    public static LocalDate parseData(String s) {
        if (vazio(s)) {
            return null;
        }
        String t = s.strip();
        try {
            if (t.length() >= 10 && t.charAt(2) == '/') {
                return LocalDate.parse(t.substring(0, 10), DATA_BR);
            }
            if (t.length() >= 10 && t.charAt(4) == '-') {
                return LocalDate.parse(t.substring(0, 10));
            }
        } catch (DateTimeParseException e) {
            return null;
        }
        return null;
    }

    public static String formatarData(LocalDate data) {
        return data == null ? "—" : data.format(DATA_BR);
    }

    public static String moeda(double valor) {
        return NumberFormat.getCurrencyInstance(PT_BR).format(valor);
    }

    public static String decimal(double valor, int casas) {
        return String.format(PT_BR, "%." + casas + "f", valor);
    }

    public static String percentual(double valor) {
        return String.format(PT_BR, "%.1f%%", valor);
    }

    /** Escapa texto para ser exibido dentro de HTML (JEditorPane). */
    public static String html(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
