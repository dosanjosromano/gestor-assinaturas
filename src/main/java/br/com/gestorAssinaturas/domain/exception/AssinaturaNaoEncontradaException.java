package br.com.gestorAssinaturas.domain.exception;

public class AssinaturaNaoEncontradaException extends RuntimeException {

    public AssinaturaNaoEncontradaException(String message) {
        super(message);
    }
}
