package br.com.gestorAssinaturas.domain.exception;

public class FalhaTecnicaPagamentoException extends RuntimeException {

    public FalhaTecnicaPagamentoException(String message) {
        super(message);
    }
}
