package br.unit.eleicao.ui;

import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import java.awt.BorderLayout;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/** Página de metodologia e limites, lida de um arquivo HTML dentro do próprio .jar. */
public class PainelMetodologia extends JPanel {

    public PainelMetodologia() {
        super(new BorderLayout());
        JEditorPane texto = new JEditorPane("text/html", carregar());
        texto.setEditable(false);
        texto.setCaretPosition(0);
        add(new JScrollPane(texto), BorderLayout.CENTER);
    }

    private static String carregar() {
        try (InputStream in = PainelMetodologia.class.getResourceAsStream("/metodologia.html")) {
            if (in == null) {
                return "<html><body>Arquivo metodologia.html não encontrado.</body></html>";
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return "<html><body>Erro ao ler a metodologia: " + e.getMessage() + "</body></html>";
        }
    }
}
