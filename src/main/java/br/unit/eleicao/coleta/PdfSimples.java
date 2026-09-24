package br.unit.eleicao.coleta;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** Gera um PDF mínimo de uma página com linhas de texto (usado só nos planos de governo fictícios da demonstração). */
final class PdfSimples {

    private PdfSimples() {
    }

    static byte[] pagina(String titulo, String... linhas) {
        StringBuilder conteudo = new StringBuilder("BT /F1 18 Tf 60 780 Td (" + escapar(titulo) + ") Tj /F1 12 Tf");
        for (String l : linhas) {
            conteudo.append(" 0 -24 Td (").append(escapar(l)).append(") Tj");
        }
        conteudo.append(" ET");
        String stream = conteudo.toString();
        List<String> objetos = new ArrayList<>();
        objetos.add("<< /Type /Catalog /Pages 2 0 R >>");
        objetos.add("<< /Type /Pages /Kids [3 0 R] /Count 1 >>");
        objetos.add("<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Contents 4 0 R "
                + "/Resources << /Font << /F1 5 0 R >> >> >>");
        objetos.add("<< /Length " + stream.length() + " >>\nstream\n" + stream + "\nendstream");
        objetos.add("<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>");
        StringBuilder pdf = new StringBuilder("%PDF-1.4\n");
        List<Integer> posicoes = new ArrayList<>();
        for (int i = 0; i < objetos.size(); i++) {
            posicoes.add(pdf.length());
            pdf.append(i + 1).append(" 0 obj\n").append(objetos.get(i)).append("\nendobj\n");
        }
        int xref = pdf.length();
        pdf.append("xref\n0 ").append(objetos.size() + 1).append("\n0000000000 65535 f \n");
        for (int p : posicoes) {
            pdf.append(String.format("%010d 00000 n \n", p));
        }
        pdf.append("trailer\n<< /Size ").append(objetos.size() + 1).append(" /Root 1 0 R >>\nstartxref\n")
                .append(xref).append("\n%%EOF\n");
        return pdf.toString().getBytes(StandardCharsets.ISO_8859_1);
    }

    private static String escapar(String s) {
        return s.replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)");
    }
}
