package br.unit.eleicao.excecao;

/** Arquivo ausente, ilegível ou fora do formato esperado (ex.: coluna obrigatória faltando). */
public class ArquivoInvalidoException extends DadosException {

    public ArquivoInvalidoException(String mensagem) {
        super(mensagem);
    }

    public ArquivoInvalidoException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
