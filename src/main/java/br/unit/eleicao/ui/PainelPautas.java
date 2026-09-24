package br.unit.eleicao.ui;

import br.unit.eleicao.excecao.DadosException;
import br.unit.eleicao.modelo.BaseDados;
import br.unit.eleicao.modelo.PosicaoUsuario;
import br.unit.eleicao.modelo.Votacao;
import br.unit.eleicao.persistencia.RepositorioArquivos;
import br.unit.eleicao.util.Texto;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.AbstractTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.List;

/**
 * Aba onde o usuário diz como votaria em votações que ele mesmo escolhe. O sistema não define
 * o que é "certo" em pauta nenhuma: o alinhamento só compara os votos do deputado com os do usuário.
 */
public class PainelPautas extends JPanel implements PainelDados {

    private final ContextoApp contexto;
    private final JTextField busca = new JTextField(30);
    private final JTextField tema = new JTextField(16);
    private final ModeloVotacoes modeloVotacoes = new ModeloVotacoes();
    private final ModeloPosicoes modeloPosicoes = new ModeloPosicoes();
    private final JTable tabelaVotacoes = new JTable(modeloVotacoes);
    private final JTable tabelaPosicoes = new JTable(modeloPosicoes);
    private final JLabel status = new JLabel(" ");

    public PainelPautas(ContextoApp contexto) {
        super(new BorderLayout(8, 8));
        this.contexto = contexto;
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        add(new JLabel("<html><b>Suas posições.</b> Procure votações do Plenário, marque como você votaria e o "
                + "indicador <i>Alinhamento por pauta</i> passa a comparar os deputados com você. "
                + "Nenhuma posição é considerada certa ou errada pelo sistema.</html>"), BorderLayout.NORTH);

        JPanel esquerda = new JPanel(new BorderLayout(4, 4));
        JPanel filtro = new JPanel(new FlowLayout(FlowLayout.LEFT));
        filtro.add(new JLabel("Buscar:"));
        filtro.add(busca);
        esquerda.add(filtro, BorderLayout.NORTH);
        tabelaVotacoes.setRowHeight(22);
        tabelaVotacoes.getColumnModel().getColumn(0).setMaxWidth(90);
        tabelaVotacoes.getColumnModel().getColumn(2).setMaxWidth(90);
        tabelaPosicoes.getColumnModel().getColumn(0).setPreferredWidth(320);
        tabelaPosicoes.getColumnModel().getColumn(1).setMaxWidth(80);
        esquerda.add(new JScrollPane(tabelaVotacoes), BorderLayout.CENTER);
        JPanel acoes = new JPanel(new FlowLayout(FlowLayout.LEFT));
        acoes.add(new JLabel("Tema (opcional):"));
        acoes.add(tema);
        JButton sim = new JButton("Eu votaria SIM");
        JButton nao = new JButton("Eu votaria NÃO");
        sim.addActionListener(e -> marcar(PosicaoUsuario.SIM));
        nao.addActionListener(e -> marcar(PosicaoUsuario.NAO));
        acoes.add(sim);
        acoes.add(nao);
        esquerda.add(acoes, BorderLayout.SOUTH);

        JPanel direita = new JPanel(new BorderLayout(4, 4));
        direita.add(new JLabel("Minhas posições"), BorderLayout.NORTH);
        tabelaPosicoes.setRowHeight(22);
        direita.add(new JScrollPane(tabelaPosicoes), BorderLayout.CENTER);
        JPanel acoesDir = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton remover = new JButton("Remover selecionadas");
        remover.addActionListener(e -> remover());
        acoesDir.add(remover);
        direita.add(acoesDir, BorderLayout.SOUTH);

        JSplitPane divisao = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, esquerda, direita);
        divisao.setResizeWeight(0.6);
        add(divisao, BorderLayout.CENTER);
        add(status, BorderLayout.SOUTH);

        busca.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                filtrar();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                filtrar();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                filtrar();
            }
        });
    }

    @Override
    public void dadosAtualizados(ContextoApp ctx) {
        filtrar();
        modeloPosicoes.recarregar();
    }

    private void filtrar() {
        if (!contexto.temBase()) {
            return;
        }
        String termo = Texto.normalizar(busca.getText());
        List<Votacao> lista = new ArrayList<>();
        List<Votacao> todas = contexto.getBase().getVotacoesOrdenadas();
        for (int i = todas.size() - 1; i >= 0; i--) { // mais recentes primeiro
            Votacao v = todas.get(i);
            if (termo.isEmpty() || Texto.normalizar(v.getDescricao()).contains(termo)
                    || Texto.normalizar(v.getId()).contains(termo)) {
                lista.add(v);
            }
        }
        modeloVotacoes.setVotacoes(lista);
        status.setText(lista.size() + " votações encontradas; " + contexto.getBase().getPosicoes().size()
                + " posições marcadas.");
    }

    private void marcar(String voto) {
        int[] linhas = tabelaVotacoes.getSelectedRows();
        if (linhas.length == 0) {
            JOptionPane.showMessageDialog(this, "Selecione uma ou mais votações na lista.");
            return;
        }
        BaseDados base = contexto.getBase();
        for (int linha : linhas) {
            Votacao v = modeloVotacoes.votacoes.get(linha);
            base.definirPosicao(new PosicaoUsuario(v.getId(), voto, tema.getText().strip()));
        }
        contexto.posicoesAlteradas();
        salvar();
    }

    private void remover() {
        int[] linhas = tabelaPosicoes.getSelectedRows();
        List<String> ids = new ArrayList<>();
        for (int linha : linhas) {
            ids.add(modeloPosicoes.posicoes.get(linha).getIdVotacao());
        }
        for (String id : ids) {
            contexto.getBase().removerPosicao(id);
        }
        if (!ids.isEmpty()) {
            contexto.posicoesAlteradas();
            salvar();
        }
    }

    private void salvar() {
        try {
            new RepositorioArquivos().salvarPosicoes(contexto.getBase());
            status.setText("Posições salvas automaticamente em " + contexto.getBase().getDiretorio().resolve(RepositorioArquivos.POSICOES));
        } catch (DadosException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Erro ao salvar", JOptionPane.ERROR_MESSAGE);
        }
    }

    private class ModeloVotacoes extends AbstractTableModel {
        private List<Votacao> votacoes = new ArrayList<>();

        void setVotacoes(List<Votacao> votacoes) {
            this.votacoes = votacoes;
            fireTableDataChanged();
        }

        @Override
        public int getRowCount() {
            return votacoes.size();
        }

        @Override
        public int getColumnCount() {
            return 3;
        }

        @Override
        public String getColumnName(int c) {
            return new String[]{"Data", "Descrição", "Sua posição"}[c];
        }

        @Override
        public Object getValueAt(int l, int c) {
            Votacao v = votacoes.get(l);
            if (c == 0) {
                return Texto.formatarData(v.getData());
            }
            if (c == 1) {
                return v.getDescricao().isEmpty() ? v.getId() : v.getDescricao();
            }
            PosicaoUsuario p = contexto.getBase().getPosicao(v.getId());
            return p == null ? "" : p.getVoto();
        }
    }

    private class ModeloPosicoes extends AbstractTableModel {
        private List<PosicaoUsuario> posicoes = new ArrayList<>();

        void recarregar() {
            posicoes = new ArrayList<>(contexto.getBase().getPosicoes());
            fireTableDataChanged();
        }

        @Override
        public int getRowCount() {
            return posicoes.size();
        }

        @Override
        public int getColumnCount() {
            return 3;
        }

        @Override
        public String getColumnName(int c) {
            return new String[]{"Votação", "Meu voto", "Tema"}[c];
        }

        @Override
        public boolean isCellEditable(int l, int c) {
            return c == 2;
        }

        @Override
        public void setValueAt(Object valor, int l, int c) {
            posicoes.get(l).setTema(String.valueOf(valor));
            salvar();
        }

        @Override
        public Object getValueAt(int l, int c) {
            PosicaoUsuario p = posicoes.get(l);
            if (c == 0) {
                Votacao v = contexto.getBase().getVotacao(p.getIdVotacao());
                return v == null ? p.getIdVotacao() : Texto.formatarData(v.getData()) + " - " + v.getDescricao();
            }
            return c == 1 ? p.getVoto() : p.getTema();
        }
    }
}
