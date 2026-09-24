package br.unit.eleicao.modelo;

import br.unit.eleicao.util.Texto;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Um indicador público ano a ano do lugar que a pessoa governou (prefeitura, estado ou país), antes e
 * durante o mandato, ao lado de uma referência (o estado, para prefeituras; o Brasil, para estados).
 * Mostra o que aconteceu no período; não prova que foi por causa do governante.
 */
public class SerieMandato {

    private final String mandato;
    private final Cargo cargo;
    private final String ente;
    private final int anoEleicao;
    private final int fimMandato;
    private final TemaMandato tema;
    private final String titulo;
    private final String unidade;
    private final FormaResumo forma;
    private final Boolean maiorMelhor;
    private String referenciaNome = "";
    private String fonte = "";
    private String explicacao = "";
    private Double limite;
    private final Map<Integer, PontoSerie> pontos = new TreeMap<>();

    /**
     * @param maiorMelhor true se subir é bom (PIB), false se subir é ruim (desemprego), null se depende
     */
    public SerieMandato(String mandato, Cargo cargo, String ente, int anoEleicao, int fimMandato, TemaMandato tema,
                        String titulo, String unidade, FormaResumo forma, Boolean maiorMelhor) {
        this.mandato = mandato;
        this.cargo = cargo;
        this.ente = ente;
        this.anoEleicao = anoEleicao;
        this.fimMandato = fimMandato;
        this.tema = tema;
        this.titulo = titulo;
        this.unidade = unidade;
        this.forma = forma;
        this.maiorMelhor = maiorMelhor;
    }

    /** Texto que identifica o mandato, ex.: "Prefeitura de ARACAJU/SE · 2021–2024". */
    public static String descreverMandato(Cargo cargo, String ente, int anoEleicao, int fimMandato) {
        return ente + " · " + (anoEleicao + 1) + "–" + fimMandato;
    }

    public void adicionar(int ano, double valor, Double referencia) {
        pontos.put(ano, new PontoSerie(ano, valor, referencia));
    }

    /** Ano do mandato em que o eleito governou (o ano da eleição é o "antes"). */
    public boolean isDuranteMandato(int ano) {
        return ano > anoEleicao && ano <= fimMandato;
    }

    public boolean isVazia() {
        return pontos.isEmpty();
    }

    /** Último ano com dado até o ano da eleição (situação que a pessoa encontrou). */
    public PontoSerie getAntes() {
        PontoSerie p = null;
        for (PontoSerie x : pontos.values()) {
            if (x.getAno() <= anoEleicao) {
                p = x;
            }
        }
        return p;
    }

    /** Último ano com dado dentro do mandato. */
    public PontoSerie getUltimoDoMandato() {
        PontoSerie p = null;
        for (PontoSerie x : pontos.values()) {
            if (isDuranteMandato(x.getAno())) {
                p = x;
            }
        }
        return p;
    }

    private List<PontoSerie> doMandato() {
        List<PontoSerie> l = new ArrayList<>();
        for (PontoSerie x : pontos.values()) {
            if (isDuranteMandato(x.getAno())) {
                l.add(x);
            }
        }
        return l;
    }

    /**
     * Resultado do período conforme a forma: variação (%), diferença (pontos), média ou soma.
     *
     * @param daReferencia true para calcular a mesma medida para a referência (estado/Brasil)
     * @return null quando não há dados suficientes
     */
    public Double resultado(boolean daReferencia) {
        List<PontoSerie> durante = doMandato();
        if (durante.isEmpty()) {
            return null;
        }
        switch (forma) {
            case SOMA:
            case MEDIA: {
                double soma = 0;
                int n = 0;
                for (PontoSerie p : durante) {
                    Double v = daReferencia ? p.getReferencia() : Double.valueOf(p.getValor());
                    if (v != null) {
                        soma += v;
                        n++;
                    }
                }
                if (n == 0) {
                    return null;
                }
                return forma == FormaResumo.SOMA ? soma : soma / n;
            }
            default: {
                PontoSerie antes = getAntes();
                PontoSerie depois = getUltimoDoMandato();
                if (antes == null || depois == null) {
                    return null;
                }
                Double a = daReferencia ? antes.getReferencia() : Double.valueOf(antes.getValor());
                Double d = daReferencia ? depois.getReferencia() : Double.valueOf(depois.getValor());
                if (a == null || d == null) {
                    return null;
                }
                if (forma == FormaResumo.VARIACAO_PONTOS) {
                    return d - a;
                }
                return a == 0 ? null : 100 * (d - a) / Math.abs(a);
            }
        }
    }

    /**
     * true se o lugar governado foi melhor que a referência no período; null se não dá para dizer
     * (sem referência, sem sentido definido ou empate).
     */
    public Boolean melhorQueReferencia() {
        Double proprio = resultado(false);
        Double ref = resultado(true);
        if (proprio == null || ref == null || maiorMelhor == null || Math.abs(proprio - ref) < 1e-9) {
            return null;
        }
        return maiorMelhor ? proprio > ref : proprio < ref;
    }

    /** Frase curta para a tela, ex.: "PIB por pessoa: +18% (estado: +12%) de 2020 a 2023". */
    public String getResumo() {
        Double r = resultado(false);
        if (r == null) {
            return "Sem dados suficientes do período do mandato.";
        }
        Double ref = resultado(true);
        String periodo;
        PontoSerie antes = getAntes();
        PontoSerie depois = getUltimoDoMandato();
        if ((forma == FormaResumo.VARIACAO_PERCENTUAL || forma == FormaResumo.VARIACAO_PONTOS) && antes != null) {
            periodo = " de " + antes.getAno() + " a " + depois.getAno();
        } else {
            List<PontoSerie> d = doMandato();
            periodo = d.size() == 1 ? " em " + d.get(0).getAno()
                    : " de " + d.get(0).getAno() + " a " + d.get(d.size() - 1).getAno();
        }
        return titulo + ": " + formatar(r) + (ref == null ? "" : " (" + referenciaNome + ": " + formatar(ref) + ")")
                + periodo;
    }

    private String formatar(double v) {
        switch (forma) {
            case VARIACAO_PERCENTUAL:
                return (v >= 0 ? "+" : "") + Texto.decimal(v, 1) + "%";
            case VARIACAO_PONTOS:
                return (v >= 0 ? "+" : "") + Texto.decimal(v, 1) + ("%".equals(unidade) ? " ponto(s)" : "");
            case SOMA:
                return Texto.decimal(v, 0) + (unidade.isEmpty() ? "" : " " + unidade);
            default:
                return Texto.decimal(v, 1) + ("%".equals(unidade) ? "% ao ano" : " " + unidade);
        }
    }

    public Collection<PontoSerie> getPontos() {
        return Collections.unmodifiableCollection(pontos.values());
    }

    public String getMandato() {
        return mandato;
    }

    public Cargo getCargo() {
        return cargo;
    }

    public String getEnte() {
        return ente;
    }

    public int getAnoEleicao() {
        return anoEleicao;
    }

    public int getFimMandato() {
        return fimMandato;
    }

    public TemaMandato getTema() {
        return tema;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getUnidade() {
        return unidade;
    }

    public FormaResumo getForma() {
        return forma;
    }

    public Boolean getMaiorMelhor() {
        return maiorMelhor;
    }

    public String getReferenciaNome() {
        return referenciaNome;
    }

    public void setReferenciaNome(String referenciaNome) {
        this.referenciaNome = referenciaNome == null ? "" : referenciaNome;
    }

    public String getFonte() {
        return fonte;
    }

    public void setFonte(String fonte) {
        this.fonte = fonte == null ? "" : fonte;
    }

    public String getExplicacao() {
        return explicacao;
    }

    public void setExplicacao(String explicacao) {
        this.explicacao = explicacao == null ? "" : explicacao;
    }

    /** Limite legal (ex.: gasto com pessoal da LRF), ou null. */
    public Double getLimite() {
        return limite;
    }

    public void setLimite(Double limite) {
        this.limite = limite;
    }
}
