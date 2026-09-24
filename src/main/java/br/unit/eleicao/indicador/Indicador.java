package br.unit.eleicao.indicador;

import br.unit.eleicao.modelo.BaseDados;
import br.unit.eleicao.modelo.Candidato;
import br.unit.eleicao.modelo.Deputado;
import br.unit.eleicao.util.Texto;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Classe abstrata de todos os indicadores. Cada subclasse implementa seu próprio cálculo
 * (polimorfismo): o ranking trata todos da mesma forma, sem saber qual é qual.
 */
public abstract class Indicador {

    private final String codigo;
    private final String nome;
    private final String fonte;
    private final String descricao;
    private final Sentido sentidoPadrao;

    protected Indicador(String codigo, String nome, String fonte, String descricao, Sentido sentidoPadrao) {
        this.codigo = codigo;
        this.nome = nome;
        this.fonte = fonte;
        this.descricao = descricao;
        this.sentidoPadrao = sentidoPadrao;
    }

    /** Calcula o indicador para um candidato. */
    public abstract ResultadoIndicador calcular(Candidato candidato, BaseDados base);

    /**
     * Calcula para todos os candidatos. A implementação padrão chama {@link #calcular} um a um;
     * indicadores que dependem do grupo inteiro (ex.: z-score) sobrescrevem este método.
     */
    public Map<String, ResultadoIndicador> calcularTodos(List<Candidato> candidatos, BaseDados base) {
        Map<String, ResultadoIndicador> resultado = new LinkedHashMap<>();
        for (Candidato c : candidatos) {
            resultado.put(c.getSq(), calcular(c, base));
        }
        return resultado;
    }

    /** Formata o valor bruto para exibição. Subclasses sobrescrevem para usar a unidade certa. */
    public String formatar(double valor) {
        return Texto.decimal(valor, 2);
    }

    public String formatar(ResultadoIndicador r) {
        return r != null && r.temDados() ? formatar(r.getValor()) : "sem dados";
    }

    /** Deputado vinculado ao candidato, ou null para quem não teve mandato na legislatura. */
    protected Deputado deputadoDe(Candidato c, BaseDados base) {
        return base.getDeputadoDe(c);
    }

    protected static ResultadoIndicador semMandato() {
        return ResultadoIndicador.semDados("Sem mandato de deputado federal na legislatura analisada");
    }

    public String getCodigo() {
        return codigo;
    }

    public String getNome() {
        return nome;
    }

    public String getFonte() {
        return fonte;
    }

    public String getDescricao() {
        return descricao;
    }

    public Sentido getSentidoPadrao() {
        return sentidoPadrao;
    }

    @Override
    public String toString() {
        return nome;
    }
}
