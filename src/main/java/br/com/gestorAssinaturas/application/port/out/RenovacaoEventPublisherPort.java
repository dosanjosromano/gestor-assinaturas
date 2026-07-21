package br.com.gestorAssinaturas.application.port.out;

public interface RenovacaoEventPublisherPort {

    void publicar(RenovacaoSolicitadaEvent evento);
}
