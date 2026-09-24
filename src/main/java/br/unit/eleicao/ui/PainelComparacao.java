package br.unit.eleicao.ui;

import br.unit.eleicao.indicador.Indicador;
import br.unit.eleicao.indicador.ResultadoIndicador;
import br.unit.eleicao.modelo.BaseDados;
import br.unit.eleicao.modelo.Candidato;
import br.unit.eleicao.ranking.Normalizador;
import br.unit.eleicao.ui.grafico.GraficoBarras;
import br.unit.eleicao.util.Texto;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Comparação lado a lado de 2 ou 3 candidatos: tabela com valores brutos e gráfico com notas normalizadas. */
public class PainelComparacao extends JPanel implements PainelDados {

    private static final String NENHUM = "— nenhum —";

    private final ContextoApp contexto;
    private final List<JComboBox<Object>> combos = new ArrayList<>();
    private final DefaultTableModel modelo = new DefaultTableModel() {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final GraficoBarras grafico = new GraficoBarras();
    private boolean atualizando;

    public PainelComparacao(ContextoApp contexto) {
        super(new BorderLayout(8, 8));
        this.contexto = contexto;
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        JPanel topo = new JPanel(new FlowLayout(FlowLayout.LEFT));
        for (int i = 0; i < 3; i++) {
            JComboBox<Object> combo = new JComboBox<>();
            combo.addActionListener(e -> {
                if (!atualizando) {
                    atualizar();
                }
            });
            combos.add(combo);
            topo.add(new JLabel((i + 1) + ":"));
            topo.add(combo);
        }
        add(topo, BorderLayout.NORTH);
        JTable tabela = new JTable(modelo);
        tabela.setRowHeight(22);
        grafico.setMaximoFixo(100.0);
        JSplitPane divisao = new JSplitPane(JSplitPane.VERTICAL_SPLIT, new JScrollPane(tabela), grafico);
        divisao.setResizeWeight(0.45);
        add(divisao, BorderLayout.CENTER);
    }

    @Override
    public void dadosAtualizados(ContextoApp ctx) {
        atualizando = true;
        List<Candidato> lista = ctx.getBase().getCandidatosOrdenadosPorNome();
        for (int i = 0; i < combos.size(); i++) {
            JComboBox<Object> combo = combos.get(i);
            Object atual = combo.getSelectedItem();
            DefaultComboBoxModel<Object> m = new DefaultComboBoxModel<>();
            if (i == 2) {
                m.addElement(NENHUM);
            }
            for (Candidato c : lista) {
                m.addElement(c);
            }
            combo.setModel(m);
            if (atual != null && (atual == NENHUM || lista.contains(atual))) {
                combo.setSelectedItem(atual);
            } else if (i < 2 && lista.size() > i) {
                combo.setSelectedIndex(i);
            }
        }
        atualizando = false;
        atualizar();
    }

    private void atualizar() {
        if (!contexto.temBase()) {
            return;
        }
        BaseDados base = contexto.getBase();
        List<Candidato> escolhidos = new ArrayList<>();
        for (JComboBox<Object> combo : combos) {
            Object o = combo.getSelectedItem();
            if (o instanceof Candidato && !escolhidos.contains(o)) {
                escolhidos.add((Candidato) o);
            }
        }
        List<String> colunas = new ArrayList<>();
        colunas.add("");
        for (Candidato c : escolhidos) {
            colunas.add(c.toString());
        }
        modelo.setDataVector(new Object[0][], colunas.toArray());
        adicionarLinha("Partido", escolhidos, Candidato::getPartido);
        adicionarLinha("Situação", escolhidos, Candidato::getSituacao);
        adicionarLinha("Idade", escolhidos, c -> {
            Integer idade = c.getIdade(base.getMetadados().getDataEleicao());
            return idade == null ? "sem dados" : idade + " anos";
        });
        adicionarLinha("Escolaridade", escolhidos, c -> c.getGrauInstrucao().getDescricao());
        adicionarLinha("Bens declarados", escolhidos,
                c -> c.getPatrimonio() == null ? "sem dados" : Texto.moeda(c.getPatrimonio()));
        adicionarLinha("Mandato na Câmara", escolhidos, c -> c.temMandatoNaCamara() ? "sim" : "não");
        for (Indicador ind : contexto.getIndicadores()) {
            adicionarLinha(ind.getNome(), escolhidos, c -> ind.formatar(contexto.getMatriz().get(ind.getCodigo(), c)));
        }

        // gráfico: notas 0-100 normalizadas entre TODOS os candidatos, no sentido padrão de cada indicador
        List<String> categorias = new ArrayList<>();
        for (Indicador ind : contexto.getIndicadores()) {
            categorias.add(ind.getNome());
        }
        List<String> nomes = new ArrayList<>();
        List<double[]> valores = new ArrayList<>();
        for (Candidato c : escolhidos) {
            nomes.add(c.getNomeExibicao());
            double[] v = new double[categorias.size()];
            for (int j = 0; j < v.length; j++) {
                Indicador ind = contexto.getIndicadores().get(j);
                Map<String, ResultadoIndicador> todos = contexto.getMatriz().getResultados(ind.getCodigo());
                Double nota = Normalizador.minMax(todos, ind.getSentidoPadrao()).get(c.getSq());
                v[j] = nota == null ? Double.NaN : nota * 100;
            }
            valores.add(v);
        }
        grafico.setTitulo("Notas normalizadas (0 = pior, 100 = melhor entre todos os candidatos)",
                "Sentido padrão de cada indicador. \"s/d\" = sem dados (não é zero).");
        grafico.setDados(categorias, nomes, valores, "");
    }

    private void adicionarLinha(String rotulo, List<Candidato> candidatos,
                                java.util.function.Function<Candidato, String> valor) {
        Object[] linha = new Object[candidatos.size() + 1];
        linha[0] = rotulo;
        for (int i = 0; i < candidatos.size(); i++) {
            linha[i + 1] = valor.apply(candidatos.get(i));
        }
        modelo.addRow(linha);
    }
}
