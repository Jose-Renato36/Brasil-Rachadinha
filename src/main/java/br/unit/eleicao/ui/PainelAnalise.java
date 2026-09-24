package br.unit.eleicao.ui;

import br.unit.eleicao.indicador.Indicador;
import br.unit.eleicao.indicador.ResultadoIndicador;
import br.unit.eleicao.modelo.BaseDados;
import br.unit.eleicao.modelo.Candidato;
import br.unit.eleicao.modelo.GrauInstrucao;
import br.unit.eleicao.ui.grafico.GraficoBarras;
import br.unit.eleicao.ui.grafico.GraficoDispersao;
import br.unit.eleicao.util.Estatistica;
import br.unit.eleicao.util.Texto;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Análise de perfil: distribuições (escolaridade, idade, patrimônio, gênero, cor/raça, partido)
 * e correlações entre variáveis. Descreve o grupo de candidatos, sem julgar ninguém.
 */
public class PainelAnalise extends JPanel implements PainelDados {

    private static final String[] DISTRIBUICOES = {"Escolaridade", "Faixa etária", "Bens declarados", "Gênero",
        "Cor/raça", "Partido", "Mandato na Câmara"};

    private final ContextoApp contexto;
    private final JComboBox<String> comboDistribuicao = new JComboBox<>(DISTRIBUICOES);
    private final JComboBox<VariavelAnalise> comboX = new JComboBox<>();
    private final JComboBox<VariavelAnalise> comboY = new JComboBox<>();
    private final GraficoBarras barras = new GraficoBarras();
    private final GraficoDispersao dispersao = new GraficoDispersao();
    private final JLabel resultado = new JLabel(" ");
    private final List<VariavelAnalise> variaveis = new ArrayList<>();
    private boolean atualizando;

    public PainelAnalise(ContextoApp contexto) {
        super(new BorderLayout(8, 8));
        this.contexto = contexto;
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JPanel esquerda = new JPanel(new BorderLayout(4, 4));
        JPanel topoEsq = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topoEsq.add(new JLabel("Distribuição:"));
        topoEsq.add(comboDistribuicao);
        esquerda.add(topoEsq, BorderLayout.NORTH);
        esquerda.add(barras, BorderLayout.CENTER);

        JPanel direita = new JPanel(new BorderLayout(4, 4));
        JPanel topoDir = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topoDir.add(new JLabel("X:"));
        topoDir.add(comboX);
        topoDir.add(new JLabel("Y:"));
        topoDir.add(comboY);
        direita.add(topoDir, BorderLayout.NORTH);
        direita.add(dispersao, BorderLayout.CENTER);
        direita.add(resultado, BorderLayout.SOUTH);

        JPanel centro = new JPanel(new GridLayout(1, 2, 8, 8));
        centro.add(esquerda);
        centro.add(direita);
        add(centro, BorderLayout.CENTER);

        JButton matriz = new JButton("Matriz de correlações...");
        matriz.addActionListener(e -> mostrarMatriz());
        JPanel rodape = new JPanel(new BorderLayout());
        rodape.add(new JLabel("<html><small>Gênero e cor/raça aparecem só aqui, para descrever o grupo; nunca entram "
                + "na pontuação. Correlação não é causalidade, e com poucos candidatos o r é instável.</small></html>"),
                BorderLayout.CENTER);
        rodape.add(matriz, BorderLayout.EAST);
        add(rodape, BorderLayout.SOUTH);

        comboDistribuicao.addActionListener(e -> atualizarDistribuicao());
        comboX.addActionListener(e -> atualizarDispersao());
        comboY.addActionListener(e -> atualizarDispersao());
    }

    @Override
    public void dadosAtualizados(ContextoApp ctx) {
        BaseDados base = ctx.getBase();
        variaveis.clear();
        variaveis.add(new VariavelAnalise("Idade", c -> {
            Integer i = c.getIdade(base.getMetadados().getDataEleicao());
            return i == null ? null : i.doubleValue();
        }));
        variaveis.add(new VariavelAnalise("Escolaridade (nível 0-6)", c -> c.getGrauInstrucao() == GrauInstrucao.NAO_INFORMADO
                ? null : (double) c.getGrauInstrucao().getNivel()));
        variaveis.add(new VariavelAnalise("Bens declarados (R$ mil)",
                c -> c.getPatrimonio() == null ? null : c.getPatrimonio() / 1000));
        for (Indicador ind : ctx.getIndicadores()) {
            variaveis.add(new VariavelAnalise(ind.getNome(), c -> {
                ResultadoIndicador r = ctx.getMatriz().get(ind.getCodigo(), c);
                return r != null && r.temDados() ? r.getValor() : null;
            }));
        }
        Object x = comboX.getSelectedItem();
        Object y = comboY.getSelectedItem();
        atualizando = true;
        comboX.removeAllItems();
        comboY.removeAllItems();
        for (VariavelAnalise v : variaveis) {
            comboX.addItem(v);
            comboY.addItem(v);
        }
        selecionarPorNome(comboX, x, "Escolaridade (nível 0-6)");
        selecionarPorNome(comboY, y, "Assiduidade");
        atualizando = false;
        atualizarDistribuicao();
        atualizarDispersao();
    }

    private static void selecionarPorNome(JComboBox<VariavelAnalise> combo, Object anterior, String padrao) {
        String alvo = anterior == null ? padrao : anterior.toString();
        for (int i = 0; i < combo.getItemCount(); i++) {
            if (combo.getItemAt(i).getNome().equals(alvo)) {
                combo.setSelectedIndex(i);
                return;
            }
        }
    }

    private void atualizarDistribuicao() {
        if (!contexto.temBase()) {
            return;
        }
        BaseDados base = contexto.getBase();
        List<Candidato> candidatos = base.getCandidatos();
        String tipo = (String) comboDistribuicao.getSelectedItem();
        Map<String, Integer> contagem = new LinkedHashMap<>();
        Function<Candidato, String> classe;
        switch (tipo) {
            case "Escolaridade":
                for (GrauInstrucao g : GrauInstrucao.values()) {
                    if (g != GrauInstrucao.NAO_INFORMADO) {
                        contagem.put(g.getDescricao(), 0);
                    }
                }
                classe = c -> c.getGrauInstrucao().getDescricao();
                break;
            case "Faixa etária":
                for (String f : new String[]{"até 29", "30-39", "40-49", "50-59", "60-69", "70+"}) {
                    contagem.put(f, 0);
                }
                classe = c -> faixaEtaria(c.getIdade(base.getMetadados().getDataEleicao()));
                break;
            case "Bens declarados":
                for (String f : new String[]{"até 100 mil", "100-500 mil", "500 mil-1 mi", "1-5 mi", "acima de 5 mi"}) {
                    contagem.put(f, 0);
                }
                classe = c -> faixaPatrimonio(c.getPatrimonio());
                break;
            case "Gênero":
                classe = c -> Texto.vazio(c.getGenero()) ? "não informado" : c.getGenero();
                break;
            case "Cor/raça":
                classe = c -> Texto.vazio(c.getCorRaca()) ? "não informado" : c.getCorRaca();
                break;
            case "Partido":
                classe = Candidato::getPartido;
                break;
            default:
                classe = c -> c.temMandatoNaCamara() ? "com mandato (dados da Câmara)" : "sem mandato";
        }
        for (Candidato c : candidatos) {
            contagem.merge(classe.apply(c), 1, Integer::sum);
        }
        List<String> categorias = new ArrayList<>(contagem.keySet());
        double[] valores = new double[categorias.size()];
        for (int i = 0; i < valores.length; i++) {
            valores[i] = contagem.get(categorias.get(i));
        }
        barras.setTitulo("Candidaturas por " + tipo.toLowerCase(), candidatos.size() + " candidaturas a "
                + "deputado federal - " + base.getMetadados().getUf());
        barras.setDados(categorias, valores, " candidaturas");
    }

    private static String faixaEtaria(Integer idade) {
        if (idade == null) {
            return "não informado";
        }
        if (idade < 30) {
            return "até 29";
        }
        if (idade >= 70) {
            return "70+";
        }
        int d = idade / 10 * 10;
        return d + "-" + (d + 9);
    }

    private static String faixaPatrimonio(Double v) {
        if (v == null) {
            return "sem declaração";
        }
        if (v <= 100_000) {
            return "até 100 mil";
        }
        if (v <= 500_000) {
            return "100-500 mil";
        }
        if (v <= 1_000_000) {
            return "500 mil-1 mi";
        }
        if (v <= 5_000_000) {
            return "1-5 mi";
        }
        return "acima de 5 mi";
    }

    /** Pares (x, y) dos candidatos que têm as duas variáveis. */
    private double[][] pares(VariavelAnalise vx, VariavelAnalise vy, List<String> rotulos) {
        List<double[]> lista = new ArrayList<>();
        for (Candidato c : contexto.getBase().getCandidatos()) {
            Double x = vx.valor(c);
            Double y = vy.valor(c);
            if (x != null && y != null) {
                lista.add(new double[]{x, y});
                if (rotulos != null) {
                    rotulos.add(c.getNomeExibicao());
                }
            }
        }
        double[][] r = new double[2][lista.size()];
        for (int i = 0; i < lista.size(); i++) {
            r[0][i] = lista.get(i)[0];
            r[1][i] = lista.get(i)[1];
        }
        return r;
    }

    private void atualizarDispersao() {
        if (atualizando || !contexto.temBase() || comboX.getSelectedItem() == null || comboY.getSelectedItem() == null) {
            return;
        }
        VariavelAnalise vx = (VariavelAnalise) comboX.getSelectedItem();
        VariavelAnalise vy = (VariavelAnalise) comboY.getSelectedItem();
        List<String> rotulos = new ArrayList<>();
        double[][] p = pares(vx, vy, rotulos);
        double r = Estatistica.pearson(p[0], p[1]);
        dispersao.setTitulo(vy.getNome() + " × " + vx.getNome(), "Cada ponto é um candidato com as duas "
                + "informações; linha tracejada = tendência linear");
        dispersao.setDados(p[0], p[1], rotulos, vx.getNome(), vy.getNome());
        resultado.setText(String.format("<html>r de Pearson = <b>%s</b> (n = %d): correlação %s.%s</html>",
                Double.isNaN(r) ? "—" : Texto.decimal(r, 2), p[0].length, Estatistica.interpretarCorrelacao(r),
                p[0].length < 10 ? " <i>Amostra pequena: interprete com cautela.</i>" : ""));
    }

    private void mostrarMatriz() {
        if (!contexto.temBase()) {
            return;
        }
        int n = variaveis.size();
        Object[][] dados = new Object[n][n + 1];
        String[] colunas = new String[n + 1];
        colunas[0] = "";
        for (int i = 0; i < n; i++) {
            colunas[i + 1] = variaveis.get(i).getNome();
            dados[i][0] = variaveis.get(i).getNome();
            for (int j = 0; j < n; j++) {
                double[][] p = pares(variaveis.get(i), variaveis.get(j), null);
                double r = Estatistica.pearson(p[0], p[1]);
                dados[i][j + 1] = (Double.isNaN(r) ? "—" : Texto.decimal(r, 2)) + "  (n=" + p[0].length + ")";
            }
        }
        JTable tabela = new JTable(dados, colunas);
        tabela.setEnabled(false);
        JDialog dialogo = new JDialog(SwingUtilities.getWindowAncestor(this), "Matriz de correlações (Pearson)");
        dialogo.add(new JScrollPane(tabela));
        dialogo.setSize(new Dimension(1000, 320));
        dialogo.setLocationRelativeTo(this);
        dialogo.setVisible(true);
    }
}
