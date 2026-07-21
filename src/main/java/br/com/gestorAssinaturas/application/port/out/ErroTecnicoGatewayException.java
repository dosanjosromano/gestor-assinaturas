package br.com.gestorAssinaturas.application.port.out;

public class ErroTecnicoGatewayException extends RuntimeException {

    public ErroTecnicoGatewayException(String message) {
        super(message);
    }
}
