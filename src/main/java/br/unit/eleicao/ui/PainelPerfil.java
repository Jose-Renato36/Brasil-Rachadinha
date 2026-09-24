package br.unit.eleicao.ui;

import br.unit.eleicao.indicador.Indicador;
import br.unit.eleicao.indicador.ProducaoLegislativa;
import br.unit.eleicao.indicador.ResultadoIndicador;
import br.unit.eleicao.modelo.BaseDados;
import br.unit.eleicao.modelo.Candidato;
import br.unit.eleicao.modelo.Deputado;
import br.unit.eleicao.modelo.Proposicao;
import br.unit.eleicao.util.Texto;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.List;

/** Perfil de um candidato: dados cadastrais, cada indicador separado com fonte e "sem dados" explícito. */
public class PainelPerfil extends JPanel implements PainelDados {

    private final ContextoApp contexto;
    private final JComboBox<Candidato> combo = new JComboBox<>();
    private final JEditorPane conteudo = new JEditorPane("text/html", "");
    private boolean atualizandoCombo;

    public PainelPerfil(ContextoApp contexto) {
        super(new BorderLayout(8, 8));
        this.contexto = contexto;
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        JPanel topo = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topo.add(new JLabel("Candidato(a):"));
        topo.add(combo);
        add(topo, BorderLayout.NORTH);
        conteudo.setEditable(false);
        add(new JScrollPane(conteudo), BorderLayout.CENTER);
        combo.addActionListener(e -> {
            if (!atualizandoCombo) {
                exibir((Candidato) combo.getSelectedItem());
            }
        });
    }

    @Override
    public void dadosAtualizados(ContextoApp ctx) {
        Candidato atual = (Candidato) combo.getSelectedItem();
        atualizandoCombo = true;
        List<Candidato> lista = ctx.getBase().getCandidatosOrdenadosPorNome();
        combo.setModel(new DefaultComboBoxModel<>(lista.toArray(new Candidato[0])));
        if (atual != null && lista.contains(atual)) {
            combo.setSelectedItem(atual);
        }
        atualizandoCombo = false;
        exibir((Candidato) combo.getSelectedItem());
    }

    public void mostrar(Candidato c) {
        combo.setSelectedItem(c);
    }

    private void exibir(Candidato c) {
        if (c == null || !contexto.temBase()) {
            conteudo.setText("<html><body><p>Nenhum candidato.</p></body></html>");
            return;
        }
        BaseDados base = contexto.getBase();
        StringBuilder h = new StringBuilder("<html><body style='font-family:sans-serif; margin:8px'>");
        h.append("<h2>").append(Texto.html(c.getNomeExibicao())).append(" &nbsp;<small>")
                .append(Texto.html(c.getPartido())).append(" · ").append(Texto.html(c.getNumero()))
                .append("</small></h2>");
        if (c.isInapta()) {
            h.append("<p style='color:#b3261e'><b>Candidatura inapta:</b> ").append(Texto.html(c.getSituacao()))
                    .append(". Por padrão ela não aparece no ranking.</p>");
        }

        h.append("<h3>Dados cadastrais (TSE)</h3><table cellpadding='3'>");
        linha(h, "Nome civil", c.getNome());
        Integer idade = c.getIdade(base.getMetadados().getDataEleicao());
        linha(h, "Idade na eleição", idade == null ? "sem dados" : idade + " anos");
        linha(h, "Escolaridade declarada", c.getGrauInstrucao().getDescricao());
        linha(h, "Ocupação declarada", c.getOcupacao());
        linha(h, "Gênero / cor ou raça", c.getGenero() + " / " + c.getCorRaca() + "  (só na análise de perfil, nunca na nota)");
        linha(h, "Situação da candidatura", c.getSituacao());
        linha(h, "Bens declarados " + base.getMetadados().getAnoEleicao(),
                c.getPatrimonio() == null ? "sem dados" : Texto.moeda(c.getPatrimonio()));
        linha(h, "Bens declarados " + base.getMetadados().getAnoAnterior(),
                c.getPatrimonioAnterior() == null ? "sem dados" : Texto.moeda(c.getPatrimonioAnterior()));
        h.append("</table>");

        Deputado d = base.getDeputadoDe(c);
        h.append("<h3>Mandato na Câmara</h3>");
        if (d == null) {
            h.append("<p>Não encontramos mandato de deputado federal na legislatura analisada. Os indicadores da "
                    + "Câmara aparecem como <i>sem dados</i> — isso não é nota baixa (veja “viés contra "
                    + "estreantes” na Metodologia).</p>");
        } else {
            h.append("<p>").append(Texto.html(d.getNomeExibicao())).append(" (").append(Texto.html(d.getPartido()))
                    .append("-").append(Texto.html(d.getUf())).append("), id Câmara ").append(d.getId())
                    .append(". Vínculo feito por: <b>").append(Texto.html(c.getCriterioVinculo())).append("</b>");
            if (c.isVinculoDuvidoso()) {
                h.append("<br><span style='color:#b3261e'>⚠ Vínculo feito só pelo nome: confira se é a mesma pessoa "
                        + "antes de usar estes números.</span>");
            }
            h.append("<br><small>").append(Texto.html(d.getUrlCamara())).append("</small></p>");
        }

        h.append("<h3>Indicadores</h3><table border='0' cellpadding='4'>")
                .append("<tr style='background:#eeeeec'><th align='left'>Indicador</th><th>Valor</th>"
                        + "<th align='left'>Como chegamos nele</th><th align='left'>Fonte</th></tr>");
        for (Indicador ind : contexto.getIndicadores()) {
            ResultadoIndicador r = contexto.getMatriz().get(ind.getCodigo(), c);
            String valor = r != null && r.temDados() ? "<b>" + Texto.html(ind.formatar(r)) + "</b>"
                    : "<i style='color:gray'>sem dados</i>";
            h.append("<tr><td>").append(Texto.html(ind.getNome())).append("</td><td align='center'>").append(valor)
                    .append("</td><td>").append(Texto.html(r == null ? "" : r.getDetalhe())).append("</td><td><small>")
                    .append(Texto.html(ind.getFonte())).append("</small></td></tr>");
        }
        h.append("</table>");

        if (d != null) {
            List<Proposicao> props = base.getProposicoesDe(d.getId());
            long aprovadas = ProducaoLegislativa.contarAprovadas(props);
            h.append("<h3>Proposições aprovadas (").append(aprovadas).append(")</h3>");
            if (aprovadas == 0) {
                h.append("<p>Nenhuma proposição de autoria aprovada no período analisado.</p>");
            } else {
                h.append("<ul>");
                java.util.Set<String> vistos = new java.util.HashSet<>();
                for (Proposicao p : props) {
                    if (p.isAprovada() && vistos.add(p.getId())) {
                        h.append("<li><b>").append(Texto.html(p.getIdentificacao())).append("</b> — ")
                                .append(Texto.html(p.getEmenta())).append(" <small>(")
                                .append(Texto.html(p.getSituacao())).append(")</small></li>");
                    }
                }
                h.append("</ul>");
            }
        }
        h.append("<p><small>Base: ").append(Texto.html(base.getMetadados().getDescricao()))
                .append("</small></p></body></html>");
        conteudo.setText(h.toString());
        conteudo.setCaretPosition(0);
    }

    private static void linha(StringBuilder h, String rotulo, String valor) {
        h.append("<tr><td><b>").append(Texto.html(rotulo)).append("</b></td><td>")
                .append(Texto.vazio(valor) ? "<i style='color:gray'>não informado</i>" : Texto.html(valor))
                .append("</td></tr>");
    }
}
