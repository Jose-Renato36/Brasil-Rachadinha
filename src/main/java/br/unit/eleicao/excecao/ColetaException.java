package br.unit.eleicao.excecao;

/** Falha ao baixar dados de uma fonte pública (rede, HTTP diferente de 200, interrupção). */
public class ColetaException extends DadosException {

    public ColetaException(String mensagem) {
        super(mensagem);
    }

    public ColetaException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
