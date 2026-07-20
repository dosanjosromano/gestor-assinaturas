package br.com.gestorAssinaturas.domain.exception;

public class PagamentoRecusadoException extends RuntimeException {

    public PagamentoRecusadoException(String message) {
        super(message);
    }
}
