package br.unit.eleicao.excecao;

/**
 * Exceção verificada base do sistema: qualquer problema ao obter, ler ou gravar dados.
 * Por ser "checked", o compilador obriga quem chama a tratar ou propagar.
 */
public class DadosException extends Exception {

    public DadosException(String mensagem) {
        super(mensagem);
    }

    public DadosException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
