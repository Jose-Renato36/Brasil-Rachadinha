package br.unit.eleicao.ui.grafico;

import javax.swing.JPanel;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;

/**
 * Base dos gráficos desenhados com Java2D. Desenha fundo, título e mensagem de "sem dados";
 * cada subclasse sobrescreve {@link #desenharConteudo} com o seu tipo de gráfico.
 */
public abstract class GraficoBase extends JPanel {

    protected static final Font FONTE_TITULO = new Font(Font.SANS_SERIF, Font.BOLD, 14);
    protected static final Font FONTE = new Font(Font.SANS_SERIF, Font.PLAIN, 12);
    protected static final Font FONTE_PEQUENA = new Font(Font.SANS_SERIF, Font.PLAIN, 11);

    private String titulo = "";
    private String subtitulo = "";

    protected GraficoBase() {
        setBackground(Paleta.FUNDO);
        setPreferredSize(new Dimension(480, 340));
        setToolTipText(""); // ativa tooltips; o texto vem de getToolTipText(MouseEvent)
    }

    public void setTitulo(String titulo, String subtitulo) {
        this.titulo = titulo == null ? "" : titulo;
        this.subtitulo = subtitulo == null ? "" : subtitulo;
        repaint();
    }

    /** true quando não há o que desenhar. */
    protected abstract boolean vazio();

    /** Desenha o gráfico dentro da área útil (já descontado o título). */
    protected abstract void desenharConteudo(Graphics2D g, Rectangle area);

    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        int topo = 12;
        g.setColor(Paleta.TEXTO);
        g.setFont(FONTE_TITULO);
        topo += g.getFontMetrics().getAscent();
        g.drawString(titulo, 14, topo);
        if (!subtitulo.isEmpty()) {
            g.setFont(FONTE_PEQUENA);
            g.setColor(Paleta.TEXTO_SECUNDARIO);
            topo += g.getFontMetrics().getHeight() + 2;
            g.drawString(subtitulo, 14, topo);
        }
        Rectangle area = new Rectangle(14, topo + 12, getWidth() - 28, getHeight() - topo - 24);
        if (vazio()) {
            g.setFont(FONTE);
            g.setColor(Paleta.TEXTO_SECUNDARIO);
            String msg = "Sem dados suficientes para este gráfico";
            FontMetrics fm = g.getFontMetrics();
            g.drawString(msg, area.x + (area.width - fm.stringWidth(msg)) / 2, area.y + area.height / 2);
        } else if (area.width > 60 && area.height > 60) {
            desenharConteudo(g, area);
        }
        g.dispose();
    }

    /** Arredonda o máximo do eixo para um valor "redondo" (1, 2, 2,5 ou 5 × potência de 10). */
    protected static double tetoRedondo(double valor) {
        if (valor <= 0) {
            return 1;
        }
        double potencia = Math.pow(10, Math.floor(Math.log10(valor)));
        for (double m : new double[]{1, 2, 2.5, 5, 10}) {
            if (m * potencia >= valor) {
                return m * potencia;
            }
        }
        return 10 * potencia;
    }

    protected static String formatarEixo(double v) {
        double a = Math.abs(v);
        if (a >= 1_000_000) {
            return String.format("%.1f mi", v / 1_000_000).replace('.', ',');
        }
        if (a >= 10_000) {
            return String.format("%.0f mil", v / 1000);
        }
        if (a == Math.rint(a)) {
            return String.format("%.0f", v);
        }
        return String.format("%.1f", v).replace('.', ',');
    }

    protected static String encurtar(FontMetrics fm, String texto, int largura) {
        if (fm.stringWidth(texto) <= largura) {
            return texto;
        }
        String t = texto;
        while (t.length() > 1 && fm.stringWidth(t + "…") > largura) {
            t = t.substring(0, t.length() - 1);
        }
        return t + "…";
    }
}
