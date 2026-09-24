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

    /** O TSE usa marcadores como #NULO, #NE (às vezes com # no fim) e -1/-3/-4 para "não informado". */
    public static String limparTse(String s) {
        if (s == null) {
            return "";
        }
        String t = s.strip();
        if (t.startsWith("#")) {
            return "";
        }
        if (t.equals("-1") || t.equals("-3") || t.equals("-4")) {
            return "";
        }
        return t;
    }

    /**
     * Compara um CPF completo com o CPF publicado por uma fonte, que pode vir mascarado
     * (ex.: "***.456.789-**" no Portal da Transparência, "***456789**" na Receita).
     * Exige pelo menos 6 dígitos visíveis, todos iguais aos da mesma posição.
     */
    public static boolean cpfCompativel(String cpfCompleto, String publicado) {
        if (cpfCompleto == null || cpfCompleto.length() != 11 || publicado == null) {
            return false;
        }
        String p = publicado.replace(".", "").replace("-", "").replace(" ", "");
        if (p.length() != 11) {
            return false;
        }
        int visiveis = 0;
        for (int i = 0; i < 11; i++) {
            char c = p.charAt(i);
            if (Character.isDigit(c)) {
                if (c != cpfCompleto.charAt(i)) {
                    return false;
                }
                visiveis++;
            }
        }
        return visiveis >= 6;
    }

    /** Dígitos centrais que as fontes mascaradas deixam visíveis (posições 4 a 9 do CPF). */
    public static String meioCpf(String cpfCompleto) {
        return cpfCompleto == null || cpfCompleto.length() != 11 ? "" : cpfCompleto.substring(3, 9);
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
