package br.unit.eleicao.ui.grafico;

import java.awt.BasicStroke;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;

/** Gráfico de barras verticais, com uma ou mais séries agrupadas por categoria. */
public class GraficoBarras extends GraficoBase {

    private List<String> categorias = new ArrayList<>();
    private final List<String> nomesSeries = new ArrayList<>();
    private final List<double[]> series = new ArrayList<>();
    private String unidade = "";
    private Double maximoFixo;
    /** Retângulos desenhados (para o tooltip saber sobre qual barra está o mouse). */
    private final List<Rectangle> areasBarras = new ArrayList<>();
    private final List<String> textosBarras = new ArrayList<>();

    public void setDados(List<String> categorias, List<String> nomesSeries, List<double[]> valores, String unidade) {
        this.categorias = new ArrayList<>(categorias);
        this.nomesSeries.clear();
        this.nomesSeries.addAll(nomesSeries);
        this.series.clear();
        this.series.addAll(valores);
        this.unidade = unidade == null ? "" : unidade;
        repaint();
    }

    /** Uma série só. */
    public void setDados(List<String> categorias, double[] valores, String unidade) {
        setDados(categorias, List.of(""), List.of(valores), unidade);
    }

    /** Fixa o topo do eixo (ex.: 100 para notas de 0 a 100). */
    public void setMaximoFixo(Double maximoFixo) {
        this.maximoFixo = maximoFixo;
    }

    @Override
    protected boolean vazio() {
        if (categorias.isEmpty() || series.isEmpty()) {
            return true;
        }
        for (double[] s : series) {
            for (double v : s) {
                if (v != 0) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    protected void desenharConteudo(Graphics2D g, Rectangle area) {
        areasBarras.clear();
        textosBarras.clear();
        g.setFont(FONTE_PEQUENA);
        FontMetrics fm = g.getFontMetrics();
        boolean legenda = series.size() > 1;
        int alturaLegenda = legenda ? fm.getHeight() + 8 : 0;
        int margemEsq = 52;
        int alturaRotulos = fm.getHeight() * 2 + 6;
        Rectangle plot = new Rectangle(area.x + margemEsq, area.y + alturaLegenda,
                area.width - margemEsq - 4, area.height - alturaRotulos - alturaLegenda);

        double max = 0;
        for (double[] s : series) {
            for (double v : s) {
                if (!Double.isNaN(v)) {
                    max = Math.max(max, v);
                }
            }
        }
        max = maximoFixo != null ? maximoFixo : tetoRedondo(max);

        // grade e eixo Y
        for (int i = 0; i <= 4; i++) {
            double v = max * i / 4;
            int y = plot.y + plot.height - (int) Math.round(plot.height * i / 4.0);
            g.setColor(Paleta.GRADE);
            g.drawLine(plot.x, y, plot.x + plot.width, y);
            g.setColor(Paleta.TEXTO_SECUNDARIO);
            String rot = formatarEixo(v);
            g.drawString(rot, plot.x - 6 - fm.stringWidth(rot), y + fm.getAscent() / 2 - 1);
        }

        int n = categorias.size();
        double larguraGrupo = plot.width / (double) n;
        int nSeries = series.size();
        double larguraBarra = Math.min(46, (larguraGrupo * 0.72) / nSeries);
        for (int c = 0; c < n; c++) {
            double inicioGrupo = plot.x + c * larguraGrupo + (larguraGrupo - larguraBarra * nSeries) / 2;
            for (int s = 0; s < nSeries; s++) {
                double v = series.get(s)[c];
                int x = (int) Math.round(inicioGrupo + s * larguraBarra) + 1;
                int w = Math.max(2, (int) Math.round(larguraBarra) - 2); // 2px de respiro entre barras
                String serie = nomesSeries.get(s).isEmpty() ? "" : nomesSeries.get(s) + " · ";
                areasBarras.add(new Rectangle(x - 1, plot.y, w + 2, plot.height));
                if (Double.isNaN(v)) {
                    // "sem dados" é diferente de zero: marca com texto, sem barra
                    g.setColor(Paleta.TEXTO_SECUNDARIO);
                    g.drawString("s/d", x + (w - fm.stringWidth("s/d")) / 2, plot.y + plot.height - 3);
                    textosBarras.add(serie + categorias.get(c) + ": sem dados");
                    continue;
                }
                // valor zero ainda ganha um traço de 2px, para não parecer ausência de dado
                int h = Math.max(2, (int) Math.round(plot.height * Math.max(0, v) / max));
                int y = plot.y + plot.height - h;
                g.setColor(Paleta.serie(nSeries == 1 ? 0 : s));
                if (h > 0) {
                    // topo arredondado, base reta apoiada no eixo
                    g.fill(new RoundRectangle2D.Double(x, y, w, Math.min(h, 8), 4, 4));
                    if (h > 4) {
                        g.fillRect(x, y + 4, w, h - 4);
                    }
                }
                textosBarras.add(serie + categorias.get(c) + ": " + formatarEixo(v) + unidade);
                if (nSeries == 1 && n <= 12 && h >= 0) {
                    g.setColor(Paleta.TEXTO_SECUNDARIO);
                    String rot = formatarEixo(v);
                    g.drawString(rot, x + (w - fm.stringWidth(rot)) / 2, y - 3);
                }
            }
            // rótulo da categoria (até duas linhas)
            g.setColor(Paleta.TEXTO);
            String cat = categorias.get(c);
            int larg = (int) larguraGrupo - 4;
            int cx = (int) (plot.x + c * larguraGrupo + larguraGrupo / 2);
            String[] linhas = quebrar(fm, cat, larg);
            for (int i = 0; i < linhas.length; i++) {
                g.drawString(linhas[i], cx - fm.stringWidth(linhas[i]) / 2,
                        plot.y + plot.height + fm.getAscent() + 4 + i * fm.getHeight());
            }
        }
        g.setColor(Paleta.EIXO);
        g.setStroke(new BasicStroke(1f));
        g.drawLine(plot.x, plot.y + plot.height, plot.x + plot.width, plot.y + plot.height);

        if (legenda) {
            int x = plot.x;
            for (int s = 0; s < nSeries; s++) {
                g.setColor(Paleta.serie(s));
                g.fillRoundRect(x, area.y + 2, 10, 10, 3, 3);
                g.setColor(Paleta.TEXTO);
                g.drawString(nomesSeries.get(s), x + 14, area.y + 11);
                x += 24 + fm.stringWidth(nomesSeries.get(s));
            }
        }
    }

    private static String[] quebrar(FontMetrics fm, String texto, int largura) {
        if (fm.stringWidth(texto) <= largura) {
            return new String[]{texto};
        }
        int corte = texto.lastIndexOf(' ', texto.length() / 2 + 4);
        if (corte <= 0) {
            return new String[]{encurtar(fm, texto, largura)};
        }
        return new String[]{encurtar(fm, texto.substring(0, corte), largura),
            encurtar(fm, texto.substring(corte + 1), largura)};
    }

    @Override
    public String getToolTipText(MouseEvent e) {
        for (int i = 0; i < areasBarras.size(); i++) {
            if (areasBarras.get(i).contains(e.getPoint())) {
                return textosBarras.get(i);
            }
        }
        return null;
    }
}
