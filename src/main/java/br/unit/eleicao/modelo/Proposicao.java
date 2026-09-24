package br.unit.eleicao.modelo;

/** Proposição legislativa em que o deputado consta como autor proponente. */
public class Proposicao {

    private final int idDeputado;
    private final String id;
    private final String siglaTipo;
    private final String numero;
    private final int ano;
    private final String situacao;
    private final boolean aprovada;
    private final String ementa;

    public Proposicao(int idDeputado, String id, String siglaTipo, String numero, int ano, String situacao,
                      boolean aprovada, String ementa) {
        this.idDeputado = idDeputado;
        this.id = id;
        this.siglaTipo = siglaTipo;
        this.numero = numero;
        this.ano = ano;
        this.situacao = situacao;
        this.aprovada = aprovada;
        this.ementa = ementa;
    }

    public String getIdentificacao() {
        return siglaTipo + " " + numero + "/" + ano;
    }

    public String getUrlCamara() {
        return "https://www.camara.leg.br/propostas-legislativas/" + id;
    }

    public int getIdDeputado() {
        return idDeputado;
    }

    public String getId() {
        return id;
    }

    public String getSiglaTipo() {
        return siglaTipo;
    }

    public String getNumero() {
        return numero;
    }

    public int getAno() {
        return ano;
    }

    public String getSituacao() {
        return situacao;
    }

    public boolean isAprovada() {
        return aprovada;
    }

    public String getEmenta() {
        return ementa;
    }
}
