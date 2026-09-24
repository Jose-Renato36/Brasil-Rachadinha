package br.unit.eleicao.web;

import br.unit.eleicao.modelo.Candidato;
import br.unit.eleicao.modelo.CandidaturaAnterior;
import br.unit.eleicao.modelo.Financiamento;
import br.unit.eleicao.modelo.IndicadorFiscal;
import br.unit.eleicao.modelo.Metadados;
import br.unit.eleicao.modelo.ResumoEmendas;
import br.unit.eleicao.modelo.Sancao;
import br.unit.eleicao.modelo.VinculoEmpresa;
import br.unit.eleicao.modelo.VinculoServidor;
import br.unit.eleicao.preparo.ItemPreparo;
import br.unit.eleicao.preparo.QuadroPreparo;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Monta as partes da ficha do candidato que respondem "essa pessoa está preparada?" e "o que fez antes?":
 * quadro de preparo, patrimônio ao longo do tempo, financiamento, emendas, empresas, sanções,
 * serviço público e gestão fiscal. Cada parte diz também se a fonte foi consultada.
 */
public class FichaComplementar {

    private static final int MAIORES = 5;
    private final QuadroPreparo quadro = new QuadroPreparo();

    public Map<String, Object> montar(Candidato c, Metadados meta) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("preparo", preparo(c, meta));
        m.put("patrimonioSerie", seriePatrimonio(c, meta));
        m.put("financiamento", financiamento(c.getFinanciamento()));
        m.put("emendas", emendas(c.getEmendas()));
        m.put("empresas", empresas(c));
        m.put("sancoes", sancoes(c));
        m.put("servidor", servidor(c));
        m.put("gestaoFiscal", gestaoFiscal(c));
        Map<String, Object> fontes = new LinkedHashMap<>();
        for (String f : new String[]{"tcu", Metadados.FONTE_CASSACAO, Metadados.FONTE_BENS_ANTERIORES,
                Metadados.FONTE_RECEITAS, Metadados.FONTE_EMENDAS, Metadados.FONTE_EMPRESAS, Metadados.FONTE_SANCOES,
                Metadados.FONTE_SIAPE, Metadados.FONTE_SICONFI}) {
            fontes.put(f, meta.isVerificada(f));
        }
        m.put("fontes", fontes);
        return m;
    }

    private List<Object> preparo(Candidato c, Metadados meta) {
        List<Object> lista = new ArrayList<>();
        for (ItemPreparo i : quadro.avaliar(c, meta)) {
            Map<String, Object> im = new LinkedHashMap<>();
            im.put("grupo", i.getGrupo());
            im.put("titulo", i.getTitulo());
            im.put("avaliacao", i.getAvaliacao().getClasse());
            im.put("resumo", i.getResumo());
            im.put("detalhe", i.getDetalhe());
            im.put("fonte", i.getFonte());
            lista.add(im);
        }
        return lista;
    }

    private List<Object> seriePatrimonio(Candidato c, Metadados meta) {
        Map<Integer, String> cargoPorAno = new LinkedHashMap<>();
        for (CandidaturaAnterior t : c.getTrajetoria()) {
            cargoPorAno.putIfAbsent(t.getAno(), t.getCargoLegivel() + (t.isEleito() ? " (eleito/a)" : ""));
        }
        cargoPorAno.put(meta.getAnoEleicao(), c.getTipoCargo().getRotulo() + " (atual)");
        List<Object> lista = new ArrayList<>();
        for (Map.Entry<Integer, Double> e : c.getSeriePatrimonio(meta.getAnoEleicao()).entrySet()) {
            Map<String, Object> p = new LinkedHashMap<>();
            p.put("ano", e.getKey());
            p.put("valor", e.getValue());
            p.put("candidatura", cargoPorAno.getOrDefault(e.getKey(), ""));
            lista.add(p);
        }
        return lista;
    }

    private Map<String, Object> financiamento(Financiamento f) {
        if (f == null) {
            return null;
        }
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("total", f.getTotal());
        List<Object> partes = new ArrayList<>();
        for (Map.Entry<String, Double> e : ResumoEmendas.maiores(f.getPorOrigem(), 20)) {
            Map<String, Object> p = new LinkedHashMap<>();
            p.put("origem", e.getKey());
            p.put("valor", e.getValue());
            p.put("parte", f.participacao(e.getKey()));
            partes.add(p);
        }
        m.put("partes", partes);
        double publico = 0;
        for (Map.Entry<String, Double> e : f.getPorOrigem().entrySet()) {
            if (e.getKey().contains("dinheiro público")) {
                publico += e.getValue();
            }
        }
        m.put("parteDinheiroPublico", f.getTotal() <= 0 ? 0 : publico / f.getTotal());
        return m;
    }

    private Map<String, Object> emendas(ResumoEmendas r) {
        if (r == null) {
            return null;
        }
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("autor", r.getAutor());
        m.put("quantidade", r.getQuantidade());
        m.put("empenhado", r.getEmpenhado());
        m.put("pago", r.getPago());
        m.put("pagoTransferenciaEspecial", r.getPagoTransferenciaEspecial());
        List<Object> anos = new ArrayList<>();
        for (Map.Entry<Integer, Double> e : r.getPagoPorAno().entrySet()) {
            anos.add(par("ano", e.getKey(), e.getValue()));
        }
        m.put("porAno", anos);
        m.put("locais", lista(ResumoEmendas.maiores(r.getPagoPorLocal(), MAIORES)));
        m.put("areas", lista(ResumoEmendas.maiores(r.getPagoPorArea(), MAIORES)));
        return m;
    }

    private static List<Object> lista(List<Map.Entry<String, Double>> entradas) {
        List<Object> l = new ArrayList<>();
        for (Map.Entry<String, Double> e : entradas) {
            l.add(par("nome", e.getKey(), e.getValue()));
        }
        return l;
    }

    private static Map<String, Object> par(String chave, Object nome, double valor) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put(chave, nome);
        p.put("valor", valor);
        return p;
    }

    private List<Object> empresas(Candidato c) {
        List<Object> l = new ArrayList<>();
        for (VinculoEmpresa e : c.getEmpresas()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("cnpj", e.getCnpj());
            m.put("razaoSocial", e.getRazaoSocial());
            m.put("qualificacao", e.getQualificacao());
            m.put("dataEntrada", e.getDataEntrada());
            l.add(m);
        }
        return l;
    }

    private List<Object> sancoes(Candidato c) {
        List<Object> l = new ArrayList<>();
        for (Sancao s : c.getSancoes()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("cadastro", s.getCadastro());
            m.put("sancionado", s.getSancionado());
            m.put("categoria", s.getCategoria());
            m.put("orgao", s.getOrgao());
            m.put("inicio", s.getInicio());
            m.put("fim", s.getFim());
            m.put("sobreEmpresa", s.isSobreEmpresa());
            l.add(m);
        }
        return l;
    }

    private List<Object> servidor(Candidato c) {
        List<Object> l = new ArrayList<>();
        for (VinculoServidor v : c.getVinculosServidor()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("cargo", v.getCargo());
            m.put("orgao", v.getOrgao());
            m.put("situacao", v.getSituacao());
            m.put("ingresso", v.getIngresso());
            l.add(m);
        }
        return l;
    }

    private List<Object> gestaoFiscal(Candidato c) {
        List<Object> l = new ArrayList<>();
        for (IndicadorFiscal i : c.getGestaoFiscal()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("ente", i.getEnte());
            m.put("ano", i.getAno());
            m.put("pessoalRcl", i.getPessoalRcl());
            m.put("limite", i.getLimite());
            m.put("acimaDoLimite", i.isAcimaDoLimite());
            m.put("duranteMandato", i.isDuranteMandato());
            l.add(m);
        }
        return l;
    }
}
