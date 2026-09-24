package br.unit.eleicao.preparo;

import br.unit.eleicao.modelo.CargoPublico;
import br.unit.eleicao.modelo.Candidato;
import br.unit.eleicao.modelo.CandidaturaAnterior;
import br.unit.eleicao.modelo.Cargo;
import br.unit.eleicao.modelo.ContratoPublico;
import br.unit.eleicao.modelo.Elegibilidade;
import br.unit.eleicao.modelo.GrauInstrucao;
import br.unit.eleicao.modelo.Metadados;
import br.unit.eleicao.modelo.Sancao;
import br.unit.eleicao.modelo.SerieMandato;
import br.unit.eleicao.util.Texto;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;

/**
 * "Em resumo": poucas frases curtas, em linguagem do dia a dia, com o essencial da ficha. Cada frase diz
 * um fato e vem com um sinal (ok, informação ou atenção). Nada aqui é nota ou recomendação de voto.
 */
public class ResumoCidadao {

    /** Uma frase do resumo. */
    public static final class Frase {
        private final Avaliacao avaliacao;
        private final String texto;

        Frase(Avaliacao avaliacao, String texto) {
            this.avaliacao = avaliacao;
            this.texto = texto;
        }

        public Avaliacao getAvaliacao() {
            return avaliacao;
        }

        public String getTexto() {
            return texto;
        }
    }

    public List<Frase> montar(Candidato c, Metadados meta) {
        List<Frase> f = new ArrayList<>();
        situacao(c, f);
        experiencia(c, meta, f);
        formacao(c, f);
        governou(c, f);
        alertas(c, meta, f);
        dinheiro(c, meta, f);
        empresas(c, f);
        plano(c, meta, f);
        return f;
    }

    private void situacao(Candidato c, List<Frase> f) {
        Elegibilidade e = c.getElegibilidade();
        if (e == Elegibilidade.INAPTA) {
            f.add(new Frase(Avaliacao.NEGATIVO, "A Justiça Eleitoral barrou esta candidatura: votos nela não serão contados."));
        } else if (e == Elegibilidade.SUB_JUDICE) {
            f.add(new Frase(Avaliacao.ATENCAO, "O registro foi negado, mas há recurso: o nome fica na urna até a decisão final."));
        } else if (e == Elegibilidade.EM_ANALISE) {
            f.add(new Frase(Avaliacao.NEUTRO, "A Justiça Eleitoral ainda está analisando o registro desta candidatura."));
        }
    }

    private void experiencia(Candidato c, Metadados meta, List<Frase> f) {
        int ano = meta.getAnoEleicao();
        CandidaturaAnterior atual = c.getMandatoAtual(ano);
        CandidaturaAnterior ultimoEleito = null;
        for (CandidaturaAnterior t : c.getTrajetoria()) {
            if (t.isEleito() && t != atual) {
                ultimoEleito = t;
                break;
            }
        }
        if (atual != null) {
            f.add(new Frase(Avaliacao.NEUTRO, "Hoje é " + atual.getCargoLegivel().toLowerCase() + onde(atual)
                    + " (eleito(a) em " + atual.getAno() + ")."
                    + (ultimoEleito != null ? " Antes foi " + ultimoEleito.getCargoLegivel().toLowerCase() + onde(ultimoEleito)
                    + "." : "")));
        } else if (ultimoEleito != null) {
            f.add(new Frase(Avaliacao.NEUTRO, "Já foi " + ultimoEleito.getCargoLegivel().toLowerCase() + onde(ultimoEleito)
                    + " de " + (ultimoEleito.getAno() + 1) + " a " + ultimoEleito.getFimMandato() + "."));
        } else if (!c.getTrajetoria().isEmpty()) {
            int n = c.getTrajetoria().size();
            f.add(new Frase(Avaliacao.NEUTRO, "Já disputou " + n + (n == 1 ? " eleição" : " eleições") + " desde "
                    + (ano - 8) + ", sem ser eleito(a)."));
        } else {
            f.add(new Frase(Avaliacao.NEUTRO, "Nunca teve mandato: é a primeira eleição que disputa desde " + (ano - 8) + "."));
        }
        for (CargoPublico p : c.getCargosPublicos()) {
            String funcao = p.getFuncao().toLowerCase();
            if (!funcao.isEmpty() && !funcao.contains("deputad") && !funcao.contains("senador")
                    && !funcao.contains("vereador") && !funcao.contains("prefeit") && !funcao.contains("governador")) {
                f.add(new Frase(Avaliacao.NEUTRO, "Ocupou cargo público de gestão: " + funcao
                        + (p.getOrgao().isEmpty() ? "" : " (" + p.getOrgao() + ")") + "."));
                break;
            }
        }
    }

    private static String onde(CandidaturaAnterior t) {
        return t.getLocal().isEmpty() || t.getTipoCargo() == Cargo.PRESIDENTE ? "" : " (" + t.getLocal() + ")";
    }

    private void formacao(Candidato c, List<Frase> f) {
        GrauInstrucao g = c.getGrauInstrucao();
        if (g == GrauInstrucao.SUPERIOR_COMPLETO) {
            f.add(new Frase(Avaliacao.POSITIVO, "Tem ensino superior completo."));
        } else if (g != GrauInstrucao.NAO_INFORMADO) {
            f.add(new Frase(Avaliacao.NEUTRO, "Escolaridade declarada: " + g.getDescricao().toLowerCase() + "."));
        }
    }

    /** Até 2 indicadores de cada lugar governado, os que têm comparação com a referência. */
    private void governou(Candidato c, List<Frase> f) {
        Map<String, List<SerieMandato>> porMandato = new LinkedHashMap<>();
        for (SerieMandato s : c.getSeriesMandato()) {
            porMandato.computeIfAbsent(s.getMandato(), k -> new ArrayList<>()).add(s);
        }
        for (List<SerieMandato> series : porMandato.values()) {
            List<String> partes = new ArrayList<>();
            int melhor = 0;
            int pior = 0;
            for (SerieMandato s : series) {
                Boolean m = s.melhorQueReferencia();
                if (m == null) {
                    continue;
                }
                if (m) {
                    melhor++;
                } else {
                    pior++;
                }
                if (partes.size() < 2 && (s.getTitulo().startsWith("PIB por pessoa") || s.getTitulo().startsWith("Empregos")
                        || s.getTitulo().startsWith("IDEB") || s.getTitulo().startsWith("Desemprego"))) {
                    partes.add(s.getResumo().replaceFirst(" de \\d{4} a \\d{4}$", ""));
                }
            }
            SerieMandato primeira = series.get(0);
            String lugar = primeira.getEnte();
            if (melhor + pior > 0) {
                f.add(new Frase(Avaliacao.NEUTRO, "Quando governou (" + lugar + ", " + (primeira.getAnoEleicao() + 1) + "–"
                        + primeira.getFimMandato() + "): " + indicadores(melhor, "melhor", "melhores") + " e "
                        + indicadores(pior, "pior", "piores") + " que a média de comparação" + (partes.isEmpty() ? "." : ". " + String.join("; ", partes) + ".")));
            } else if (primeira.getCargo() == Cargo.PRESIDENTE) {
                f.add(new Frase(Avaliacao.NEUTRO, "Governou o país de " + (primeira.getAnoEleicao() + 1) + " a "
                        + primeira.getFimMandato() + ": veja crescimento, inflação, emprego e leis na seção sobre o governo."));
            }
        }
    }

    private static String indicadores(int n, String singular, String plural) {
        return n + (n == 1 ? " indicador " + singular : " indicadores " + plural);
    }

    private void alertas(Candidato c, Metadados meta, List<Frase> f) {
        List<String> itens = new ArrayList<>();
        int contas = c.getContasIrregulares().size();
        if (contas > 0) {
            itens.add(contas + (contas == 1 ? " processo" : " processos") + " de contas irregulares no TCU");
        }
        int sancoes = 0;
        int expulsoes = 0;
        for (Sancao s : c.getSancoes()) {
            if ("CEAF".equals(s.getCadastro())) {
                expulsoes++;
            } else {
                sancoes++;
            }
        }
        if (sancoes > 0) {
            itens.add(sancoes + (sancoes == 1 ? " punição" : " punições") + " na CGU (dela ou de empresa dela)");
        }
        if (expulsoes > 0) {
            itens.add("expulsão do serviço público federal");
        }
        int cassacoes = 0;
        for (CandidaturaAnterior t : c.getTrajetoria()) {
            cassacoes += t.getMotivoCassacao().isEmpty() ? 0 : 1;
        }
        if (cassacoes > 0) {
            itens.add(cassacoes + (cassacoes == 1 ? " cassação" : " cassações") + " em eleições anteriores");
        }
        if (!itens.isEmpty()) {
            f.add(new Frase(Avaliacao.ATENCAO, "Atenção: " + String.join(", ", itens) + ". Veja os detalhes abaixo."));
        } else if (meta.isVerificada("tcu") && meta.isVerificada(Metadados.FONTE_SANCOES)) {
            f.add(new Frase(Avaliacao.POSITIVO, "Nada consta nas listas do TCU e da CGU que consultamos."));
        }
    }

    private void dinheiro(Candidato c, Metadados meta, List<Frase> f) {
        SortedMap<Integer, Double> serie = c.getSeriePatrimonio(meta.getAnoEleicao());
        if (c.getPatrimonio() != null) {
            String texto = "Declarou " + moedaCurta(c.getPatrimonio()) + " em bens";
            if (serie.size() > 1) {
                int primeiro = serie.firstKey();
                texto += " (em " + primeiro + " eram " + moedaCurta(serie.get(primeiro)) + ")";
            }
            f.add(new Frase(Avaliacao.NEUTRO, texto + "."));
        }
        if (c.getFinanciamento() != null && c.getFinanciamento().getTotal() > 0) {
            double publico = 0;
            for (Map.Entry<String, Double> e : c.getFinanciamento().getPorOrigem().entrySet()) {
                if (e.getKey().contains("dinheiro público")) {
                    publico += e.getValue();
                }
            }
            long pct = Math.round(100 * publico / c.getFinanciamento().getTotal());
            f.add(new Frase(Avaliacao.NEUTRO, "A campanha já arrecadou " + moedaCurta(c.getFinanciamento().getTotal())
                    + "; " + pct + "% é dinheiro público (fundo eleitoral e partidário)."));
        }
    }

    private void empresas(Candidato c, List<Frase> f) {
        int n = c.getEmpresas().size();
        if (n == 0 && c.getContratos().isEmpty()) {
            return;
        }
        String texto = n > 0 ? "É sócio(a) de " + n + (n == 1 ? " empresa" : " empresas") : "";
        if (!c.getContratos().isEmpty()) {
            double total = 0;
            for (ContratoPublico k : c.getContratos()) {
                total += k.getValor();
            }
            texto += (texto.isEmpty() ? "Empresas ligadas a esta pessoa têm " : "; elas têm ") + c.getContratos().size()
                    + (c.getContratos().size() == 1 ? " contrato" : " contratos") + " com o governo federal ("
                    + moedaCurta(total) + ")";
        }
        f.add(new Frase(c.getContratos().isEmpty() ? Avaliacao.NEUTRO : Avaliacao.ATENCAO, texto + "."));
    }

    private void plano(Candidato c, Metadados meta, List<Frase> f) {
        if (!c.getPlanosGoverno().isEmpty()) {
            f.add(new Frase(Avaliacao.POSITIVO, "Registrou plano de governo no TSE: dá para ler as propostas completas."));
        } else if (c.getTipoCargo().isExecutivo() && c.getTipoCargo().isPrincipal()
                && meta.isVerificada(Metadados.FONTE_PLANOS)) {
            f.add(new Frase(Avaliacao.ATENCAO, "Não encontramos plano de governo nos arquivos do TSE (é obrigatório para este cargo)."));
        }
    }

    /** "R$ 1,2 milhão", "R$ 350 mil", "R$ 900". */
    public static String moedaCurta(double v) {
        double a = Math.abs(v);
        if (a >= 1e9) {
            return "R$ " + Texto.decimal(v / 1e9, 1) + (a < 2e9 ? " bilhão" : " bilhões");
        }
        if (a >= 1e6) {
            return "R$ " + Texto.decimal(v / 1e6, 1) + (a < 2e6 ? " milhão" : " milhões");
        }
        if (a >= 1e3) {
            return "R$ " + Texto.decimal(v / 1e3, 0) + " mil";
        }
        return "R$ " + Texto.decimal(v, 0);
    }
}
