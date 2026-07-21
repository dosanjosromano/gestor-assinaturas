package br.com.gestorAssinaturas.application.port.out;

public class CacheIndisponivelException extends RuntimeException {

    public CacheIndisponivelException(String message, Throwable cause) {
        super(message, cause);
    }
}

