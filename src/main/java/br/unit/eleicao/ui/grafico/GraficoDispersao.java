package br.unit.eleicao.ui.grafico;

import br.unit.eleicao.util.Estatistica;

import java.awt.BasicStroke;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.util.ArrayList;
import java.util.List;

/** Gráfico de dispersão (x × y) com reta de tendência de mínimos quadrados e tooltip por ponto. */
public class GraficoDispersao extends GraficoBase {

    private double[] x = new double[0];
    private double[] y = new double[0];
    private List<String> rotulos = new ArrayList<>();
    private String nomeX = "";
    private String nomeY = "";
    private final List<Point> pontosTela = new ArrayList<>();

    public void setDados(double[] x, double[] y, List<String> rotulos, String nomeX, String nomeY) {
        this.x = x.clone();
        this.y = y.clone();
        this.rotulos = new ArrayList<>(rotulos);
        this.nomeX = nomeX;
        this.nomeY = nomeY;
        repaint();
    }

    @Override
    protected boolean vazio() {
        return x.length < 2;
    }

    @Override
    protected void desenharConteudo(Graphics2D g, Rectangle area) {
        pontosTela.clear();
        g.setFont(FONTE_PEQUENA);
        FontMetrics fm = g.getFontMetrics();
        int margemEsq = 58;
        int margemInf = fm.getHeight() * 2 + 10;
        Rectangle plot = new Rectangle(area.x + margemEsq, area.y + 4, area.width - margemEsq - 10,
                area.height - margemInf - 4);

        double minX = Estatistica.minimo(x);
        double maxX = Estatistica.maximo(x);
        double minY = Estatistica.minimo(y);
        double maxY = Estatistica.maximo(y);
        double folgaX = maxX == minX ? 1 : (maxX - minX) * 0.06;
        double folgaY = maxY == minY ? 1 : (maxY - minY) * 0.08;
        minX -= folgaX;
        maxX += folgaX;
        minY -= folgaY;
        maxY += folgaY;

        for (int i = 0; i <= 4; i++) {
            double vy = minY + (maxY - minY) * i / 4;
            int py = plot.y + plot.height - (int) Math.round(plot.height * i / 4.0);
            g.setColor(Paleta.GRADE);
            g.drawLine(plot.x, py, plot.x + plot.width, py);
            g.setColor(Paleta.TEXTO_SECUNDARIO);
            String rot = formatarEixo(arred(vy));
            g.drawString(rot, plot.x - 6 - fm.stringWidth(rot), py + fm.getAscent() / 2 - 1);

            double vx = minX + (maxX - minX) * i / 4;
            int px = plot.x + (int) Math.round(plot.width * i / 4.0);
            String rx = formatarEixo(arred(vx));
            g.drawString(rx, px - fm.stringWidth(rx) / 2, plot.y + plot.height + fm.getAscent() + 4);
        }
        g.setColor(Paleta.EIXO);
        g.drawLine(plot.x, plot.y + plot.height, plot.x + plot.width, plot.y + plot.height);
        g.drawLine(plot.x, plot.y, plot.x, plot.y + plot.height);

        g.setColor(Paleta.TEXTO);
        g.drawString(nomeX, plot.x + (plot.width - fm.stringWidth(nomeX)) / 2,
                plot.y + plot.height + fm.getHeight() * 2 + 6);
        Graphics2D gv = (Graphics2D) g.create();
        gv.rotate(-Math.PI / 2);
        gv.drawString(nomeY, -(plot.y + (plot.height + fm.stringWidth(nomeY)) / 2), area.x + fm.getAscent());
        gv.dispose();

        // reta de tendência
        if (x.length >= 3) {
            double[] ab = Estatistica.regressaoLinear(x, y);
            double x1 = minX + folgaX;
            double x2 = maxX - folgaX;
            g.setColor(Paleta.TEXTO_SECUNDARIO);
            g.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f, new float[]{6, 5}, 0));
            g.drawLine(telaX(x1, minX, maxX, plot), telaY(ab[0] + ab[1] * x1, minY, maxY, plot),
                    telaX(x2, minX, maxX, plot), telaY(ab[0] + ab[1] * x2, minY, maxY, plot));
        }

        g.setStroke(new BasicStroke(2f));
        for (int i = 0; i < x.length; i++) {
            int px = telaX(x[i], minX, maxX, plot);
            int py = telaY(y[i], minY, maxY, plot);
            pontosTela.add(new Point(px, py));
            Ellipse2D ponto = new Ellipse2D.Double(px - 5, py - 5, 10, 10);
            g.setColor(Paleta.serie(0));
            g.fill(ponto);
            g.setColor(Paleta.FUNDO); // anel da cor do fundo separa pontos sobrepostos
            g.draw(ponto);
        }
    }

    private static double arred(double v) {
        return Math.abs(v) >= 10 ? Math.round(v) : Math.round(v * 10) / 10.0;
    }

    private static int telaX(double v, double min, double max, Rectangle p) {
        return p.x + (int) Math.round((v - min) / (max - min) * p.width);
    }

    private static int telaY(double v, double min, double max, Rectangle p) {
        return p.y + p.height - (int) Math.round((v - min) / (max - min) * p.height);
    }

    @Override
    public String getToolTipText(MouseEvent e) {
        for (int i = 0; i < pontosTela.size(); i++) {
            if (pontosTela.get(i).distance(e.getPoint()) <= 9) {
                return rotulos.get(i) + " - " + nomeX + ": " + formatarEixo(arred(x[i])) + "; " + nomeY + ": "
                        + formatarEixo(arred(y[i]));
            }
        }
        return null;
    }
}
