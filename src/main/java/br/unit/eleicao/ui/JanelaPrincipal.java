package br.unit.eleicao.ui;

import br.unit.eleicao.coleta.GeradorDadosDemo;
import br.unit.eleicao.coleta.PipelineColeta;
import br.unit.eleicao.excecao.DadosException;
import br.unit.eleicao.modelo.BaseDados;
import br.unit.eleicao.modelo.Metadados;
import br.unit.eleicao.persistencia.RepositorioArquivos;
import br.unit.eleicao.util.Texto;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.nio.file.Path;
import java.util.List;

/** Janela principal: menu de dados, faixa informando a base carregada e as abas do sistema. */
public class JanelaPrincipal extends JFrame {

    private static final String[] UFS = {"AC", "AL", "AM", "AP", "BA", "CE", "DF", "ES", "GO", "MA", "MG", "MS", "MT",
        "PA", "PB", "PE", "PI", "PR", "RJ", "RN", "RO", "RR", "RS", "SC", "SE", "SP", "TO"};

    private final Path raizDados;
    private final ContextoApp contexto = new ContextoApp();
    private final JLabel faixa = new JLabel(" ");
    private final JTabbedPane abas = new JTabbedPane();
    private final PainelPerfil perfil;

    public JanelaPrincipal(Path raizDados) {
        super("Decisão Eleitoral - dados abertos");
        this.raizDados = raizDados;
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1280, 800);
        setLocationRelativeTo(null);

        perfil = new PainelPerfil(contexto);
        PainelRanking ranking = new PainelRanking(contexto, c -> {
            perfil.mostrar(c);
            abas.setSelectedComponent(perfil);
        });
        PainelComparacao comparacao = new PainelComparacao(contexto);
        PainelAnalise analise = new PainelAnalise(contexto);
        PainelPautas pautas = new PainelPautas(contexto);
        // polimorfismo: todos são PainelDados, avisados da mesma forma quando os dados mudam
        for (PainelDados p : List.of(ranking, perfil, comparacao, analise, pautas)) {
            contexto.registrar(p);
        }
        abas.addTab("Ranking", ranking);
        abas.addTab("Perfil", perfil);
        abas.addTab("Comparar", comparacao);
        abas.addTab("Análise de perfil", analise);
        abas.addTab("Pautas", pautas);
        abas.addTab("Metodologia e limites", new PainelMetodologia());

        faixa.setOpaque(true);
        faixa.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
        add(faixa, BorderLayout.NORTH);
        add(abas, BorderLayout.CENTER);
        setJMenuBar(criarMenu());
    }

    private JMenuBar criarMenu() {
        JMenuBar barra = new JMenuBar();
        JMenu dados = new JMenu("Dados");
        JMenuItem demo = new JMenuItem("Abrir demonstração (dados fictícios)");
        demo.addActionListener(e -> abrirDemonstracao());
        JMenuItem abrirUf = new JMenuItem("Abrir UF já processada...");
        abrirUf.addActionListener(e -> escolherUfProcessada());
        JMenuItem coletar = new JMenuItem("Coletar e processar UF (TSE + Câmara)...");
        coletar.addActionListener(e -> coletar());
        JMenuItem sair = new JMenuItem("Sair");
        sair.addActionListener(e -> dispose());
        dados.add(demo);
        dados.add(abrirUf);
        dados.add(coletar);
        dados.addSeparator();
        dados.add(sair);
        barra.add(dados);
        return barra;
    }

    public void abrirDemonstracao() {
        try {
            Path pasta = raizDados.resolve("demo");
            if (!RepositorioArquivos.contemBase(pasta)) {
                new GeradorDadosDemo().gerarESalvar(pasta);
            }
            carregar(pasta);
        } catch (DadosException e) {
            erro(e);
        }
    }

    public void carregar(Path pasta) {
        try {
            BaseDados base = new RepositorioArquivos().carregar(pasta);
            contexto.setBase(base);
            Metadados m = base.getMetadados();
            String texto = m.getDescricao() + " · " + base.getCandidatos().size() + " candidaturas · "
                    + base.getDeputados().size() + " deputados · " + base.getTotalVotacoes() + " votações";
            if (m.isDemonstracao()) {
                faixa.setBackground(new Color(0xFFF1CC));
                faixa.setText("<html><b>DEMONSTRAÇÃO COM DADOS FICTÍCIOS.</b> " + Texto.html(texto)
                        + ". Use Dados › Coletar para dados reais.</html>");
            } else {
                faixa.setBackground(new Color(0xE3EEFB));
                faixa.setText("<html><b>Dados públicos:</b> " + Texto.html(texto) + " · processado em "
                        + Texto.html(m.getGeradoEm()) + "</html>");
            }
        } catch (DadosException e) {
            erro(e);
        }
    }

    private void escolherUfProcessada() {
        JFileChooser chooser = new JFileChooser(raizDados.resolve("processados").toFile());
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setDialogTitle("Escolha a pasta da UF (ex.: dados/processados/SE)");
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            carregar(chooser.getSelectedFile().toPath());
        }
    }

    private void coletar() {
        JComboBox<String> uf = new JComboBox<>(UFS);
        uf.setSelectedItem("SE");
        JComboBox<Integer> ano = new JComboBox<>(new Integer[]{2026, 2022});
        JCheckBox baixar = new JCheckBox("Baixar arquivos que ainda não estão em dados/brutos", true);
        JPanel form = new JPanel(new GridLayout(0, 1, 4, 4));
        form.add(new JLabel("UF:"));
        form.add(uf);
        form.add(new JLabel("Eleição:"));
        form.add(ano);
        form.add(baixar);
        form.add(new JLabel("<html><small>O download pode levar vários minutos (arquivos de centenas de MB).<br>"
                + "Sem internet, coloque os arquivos manualmente em dados/brutos e desmarque a opção.</small></html>"));
        if (JOptionPane.showConfirmDialog(this, form, "Coletar dados", JOptionPane.OK_CANCEL_OPTION)
                != JOptionPane.OK_OPTION) {
            return;
        }
        JTextArea log = new JTextArea(22, 90);
        log.setEditable(false);
        JDialog dialogo = new JDialog(this, "Coleta em andamento", false);
        dialogo.add(new JScrollPane(log));
        dialogo.pack();
        dialogo.setLocationRelativeTo(this);
        dialogo.setVisible(true);

        String siglaUf = (String) uf.getSelectedItem();
        int anoEleicao = (Integer) ano.getSelectedItem();
        boolean fazerDownload = baixar.isSelected();
        // SwingWorker: a coleta roda fora da thread da interface, que continua respondendo
        new SwingWorker<Path, String>() {
            @Override
            protected Path doInBackground() throws Exception {
                PipelineColeta pipeline = new PipelineColeta(raizDados, this::publish);
                pipeline.executar(siglaUf, anoEleicao, fazerDownload);
                return pipeline.pastaProcessada(siglaUf);
            }

            @Override
            protected void process(List<String> linhas) {
                for (String l : linhas) {
                    log.append(l + "\n");
                }
            }

            @Override
            protected void done() {
                try {
                    Path pasta = get();
                    log.append("\nConcluído.\n");
                    carregar(pasta);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (java.util.concurrent.ExecutionException e) {
                    Throwable causa = e.getCause();
                    log.append("\nERRO: " + causa.getMessage() + "\n");
                    erro(causa);
                }
            }
        }.execute();
    }

    private void erro(Throwable e) {
        JOptionPane.showMessageDialog(this, e.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
    }

    /** Abre a janela na thread de eventos do Swing. */
    public static void iniciar(Path raizDados, Path pastaInicial) {
        SwingUtilities.invokeLater(() -> {
            JanelaPrincipal janela = new JanelaPrincipal(raizDados);
            janela.setVisible(true);
            if (pastaInicial != null) {
                janela.carregar(pastaInicial);
            } else {
                janela.abrirDemonstracao();
            }
        });
    }
}
