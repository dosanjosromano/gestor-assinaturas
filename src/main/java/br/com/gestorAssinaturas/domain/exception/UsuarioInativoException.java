package br.com.gestorAssinaturas.domain.exception;

public class UsuarioInativoException extends RuntimeException {

    public UsuarioInativoException(String message) {
        super(message);
    }
}
