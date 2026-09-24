package br.unit.eleicao.ui;

import br.unit.eleicao.indicador.AlinhamentoPauta;
import br.unit.eleicao.indicador.Indicador;
import br.unit.eleicao.indicador.ResultadoIndicador;
import br.unit.eleicao.modelo.Candidato;
import br.unit.eleicao.ranking.ConfiguracaoRanking;
import br.unit.eleicao.ranking.ItemRanking;
import br.unit.eleicao.ranking.MetodoRanking;
import br.unit.eleicao.ranking.SomaPonderada;
import br.unit.eleicao.ranking.Topsis;
import br.unit.eleicao.util.Texto;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/** Aba de ranking: sliders de peso à esquerda, tabela atualizada em tempo real à direita. */
public class PainelRanking extends JPanel implements PainelDados {

    private final ContextoApp contexto;
    private final ConfiguracaoRanking config = new ConfiguracaoRanking();
    private final JComboBox<MetodoRanking> comboMetodo = new JComboBox<>(
            new MetodoRanking[]{new SomaPonderada(), new Topsis()});
    private final JLabel descricaoMetodo = new JLabel();
    private final ModeloTabela modelo = new ModeloTabela();
    private final JTable tabela;
    private final JLabel rodape = new JLabel();
    private final Map<String, JSlider> sliders = new HashMap<>();
    private boolean alinhamentoJaAtivado;
    private final JSpinner spinnerCobertura = new JSpinner(new SpinnerNumberModel(1, 1, 5, 1));

    public PainelRanking(ContextoApp contexto, Consumer<Candidato> abrirPerfil) {
        super(new BorderLayout(8, 8));
        this.contexto = contexto;
        this.tabela = new JTable(modelo); // só depois de "contexto": o modelo usa a lista de indicadores
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JPanel controles = new JPanel();
        controles.setLayout(new BoxLayout(controles, BoxLayout.Y_AXIS));
        JLabel titulo = new JLabel("<html><b>O que pesa mais para você?</b><br>"
                + "<small>0 = ignorar, 10 = peso máximo</small></html>");
        controles.add(titulo);
        controles.add(Box.createVerticalStrut(8));
        for (Indicador ind : contexto.getIndicadores()) {
            controles.add(criarControle(ind));
            controles.add(Box.createVerticalStrut(6));
        }
        JCheckBox ocultar = new JCheckBox("Ocultar candidaturas inaptas", config.isOcultarInaptos());
        ocultar.setToolTipText("Elegibilidade é filtro, não nota: candidaturas inaptas saem da lista, mas não "
                + "perdem pontos.");
        ocultar.addActionListener(e -> {
            config.setOcultarInaptos(ocultar.isSelected());
            atualizar();
        });
        controles.add(ocultar);
        JPanel linhaCobertura = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        linhaCobertura.add(new JLabel("Mín. de indicadores com dados: "));
        linhaCobertura.add(spinnerCobertura);
        linhaCobertura.setAlignmentX(Component.LEFT_ALIGNMENT);
        linhaCobertura.setMaximumSize(new Dimension(320, 30));
        linhaCobertura.setToolTipText("<html>Com 1, um estreante pode ser pontuado só pela variação de patrimônio "
                + "e ficar à frente de quem é avaliado em 4 indicadores.<br>Aumentar evita isso, mas tira os "
                + "estreantes do ranking. Veja \"viés contra estreantes\" na Metodologia.</html>");
        spinnerCobertura.addChangeListener(e -> {
            config.setCoberturaMinima((Integer) spinnerCobertura.getValue());
            atualizar();
        });
        controles.add(linhaCobertura);
        controles.add(Box.createVerticalStrut(8));
        JPanel linhaMetodo = new JPanel(new BorderLayout(4, 4));
        linhaMetodo.add(new JLabel("Método:"), BorderLayout.WEST);
        linhaMetodo.add(comboMetodo, BorderLayout.CENTER);
        linhaMetodo.setAlignmentX(Component.LEFT_ALIGNMENT);
        linhaMetodo.setMaximumSize(new Dimension(320, 30));
        controles.add(linhaMetodo);
        descricaoMetodo.setAlignmentX(Component.LEFT_ALIGNMENT);
        controles.add(descricaoMetodo);
        comboMetodo.addActionListener(e -> atualizar());
        controles.add(Box.createVerticalGlue());
        JScrollPane rolagem = new JScrollPane(controles, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        rolagem.setPreferredSize(new Dimension(320, 400));
        rolagem.setBorder(BorderFactory.createEmptyBorder());
        add(rolagem, BorderLayout.WEST);

        tabela.setRowHeight(22);
        tabela.setAutoCreateRowSorter(false);
        tabela.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabela.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int linha = tabela.getSelectedRow();
                if (e.getClickCount() == 2 && linha >= 0) {
                    abrirPerfil.accept(modelo.itens.get(linha).getCandidato());
                }
            }
        });
        tabela.setDefaultRenderer(Object.class, new RenderizadorRanking());
        int[] larguras = {40, 170, 60, 55, 75, 75, 140};
        for (int i = 0; i < larguras.length; i++) {
            tabela.getColumnModel().getColumn(i).setPreferredWidth(larguras[i]);
        }
        add(new JScrollPane(tabela), BorderLayout.CENTER);
        rodape.setFont(rodape.getFont().deriveFont(Font.PLAIN, 11f));
        add(rodape, BorderLayout.SOUTH);
    }

    private JPanel criarControle(Indicador ind) {
        JPanel p = new JPanel(new BorderLayout(2, 2));
        p.setBorder(BorderFactory.createTitledBorder(ind.getNome()));
        p.setToolTipText("<html>" + Texto.html(ind.getDescricao()) + "<br>Fonte: " + Texto.html(ind.getFonte())
                + "</html>");
        int pesoInicial = AlinhamentoPauta.CODIGO.equals(ind.getCodigo()) ? 0 : 5;
        config.setPeso(ind.getCodigo(), pesoInicial);
        JSlider slider = new JSlider(0, ConfiguracaoRanking.PESO_MAXIMO, pesoInicial);
        slider.setMajorTickSpacing(5);
        slider.setMinorTickSpacing(1);
        slider.setPaintTicks(true);
        slider.setPaintLabels(true);
        slider.setSnapToTicks(true);
        slider.addChangeListener(e -> {
            config.setPeso(ind.getCodigo(), slider.getValue());
            atualizar();
        });
        sliders.put(ind.getCodigo(), slider);
        JCheckBox inverter = new JCheckBox("Inverter (padrão: " + ind.getSentidoPadrao() + ")");
        inverter.setFont(inverter.getFont().deriveFont(11f));
        inverter.addActionListener(e -> {
            config.setInvertido(ind.getCodigo(), inverter.isSelected());
            atualizar();
        });
        p.add(slider, BorderLayout.CENTER);
        p.add(inverter, BorderLayout.SOUTH);
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.setMaximumSize(new Dimension(320, 110));
        return p;
    }

    @Override
    public void dadosAtualizados(ContextoApp ctx) {
        // ao marcar a primeira posição, liga o alinhamento se o usuário ainda não mexeu nele
        JSlider sAlinhamento = sliders.get(AlinhamentoPauta.CODIGO);
        if (sAlinhamento != null && sAlinhamento.getValue() == 0 && !ctx.getBase().getPosicoes().isEmpty()
                && !alinhamentoJaAtivado) {
            alinhamentoJaAtivado = true;
            sAlinhamento.setValue(5);
        }
        atualizar();
    }

    private void atualizar() {
        if (!contexto.temBase()) {
            return;
        }
        MetodoRanking metodo = (MetodoRanking) comboMetodo.getSelectedItem();
        descricaoMetodo.setText("<html><div style='width:230px'><small>" + Texto.html(metodo.getDescricao())
                + "</small></div></html>");
        List<ItemRanking> itens = metodo.classificar(contexto.getMatriz(), config);
        modelo.setItens(itens);
        long semPontuacao = itens.stream().filter(i -> !i.temPontuacao()).count();
        rodape.setText("<html>Pontuação de 0 a 100, relativa aos candidatos desta lista (não é nota absoluta). "
                + "Cobertura = indicadores com dados / indicadores com peso. " + semPontuacao
                + " sem dados suficientes aparecem no fim, sem posição. Compare pontuações só entre coberturas "
                + "parecidas. Dê dois cliques para ver o perfil.</html>");
    }

    /** Modelo da tabela: colunas fixas + uma por indicador (valor bruto formatado). */
    private class ModeloTabela extends AbstractTableModel {

        private static final int FIXAS = 7;
        private final String[] nomesFixos = {"#", "Candidato(a)", "Partido", "Nº", "Pontuação", "Cobertura",
            "Situação"};
        private List<ItemRanking> itens = new ArrayList<>();

        void setItens(List<ItemRanking> itens) {
            this.itens = itens;
            fireTableDataChanged();
        }

        @Override
        public int getRowCount() {
            return itens.size();
        }

        @Override
        public int getColumnCount() {
            return FIXAS + contexto.getIndicadores().size();
        }

        @Override
        public String getColumnName(int col) {
            return col < FIXAS ? nomesFixos[col] : contexto.getIndicadores().get(col - FIXAS).getNome();
        }

        @Override
        public Object getValueAt(int linha, int col) {
            ItemRanking item = itens.get(linha);
            Candidato c = item.getCandidato();
            switch (col) {
                case 0:
                    return item.getPosicao() == 0 ? "—" : item.getPosicao() + "º";
                case 1:
                    return c.getNomeExibicao() + (c.isVinculoDuvidoso() ? " ⚠" : "");
                case 2:
                    return c.getPartido();
                case 3:
                    return c.getNumero();
                case 4:
                    return item.temPontuacao() ? Texto.decimal(item.getPontuacao(), 1) : "sem dados";
                case 5:
                    return item.getCobertura();
                case 6:
                    return c.getSituacao();
                default:
                    Indicador ind = contexto.getIndicadores().get(col - FIXAS);
                    ResultadoIndicador r = contexto.getMatriz().get(ind.getCodigo(), c);
                    return ind.formatar(r);
            }
        }
    }

    /** Deixa "sem dados" em cinza e itálico, para não ser confundido com zero. */
    private static class RenderizadorRanking extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable t, Object valor, boolean sel, boolean foco, int l,
                                                       int c) {
            Component comp = super.getTableCellRendererComponent(t, valor, sel, foco, l, c);
            boolean semDados = "sem dados".equals(valor);
            comp.setFont(comp.getFont().deriveFont(semDados ? Font.ITALIC : Font.PLAIN));
            if (!sel) {
                comp.setForeground(semDados ? java.awt.Color.GRAY : t.getForeground());
            }
            setHorizontalAlignment(c == 1 || c == 6 ? LEFT : CENTER);
            return comp;
        }
    }
}
