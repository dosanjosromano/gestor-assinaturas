package br.com.gestorAssinaturas.application.port.in.useCase;

import br.com.gestorAssinaturas.domain.model.Assinatura;

import java.util.UUID;

public interface BuscaAssinaturasUseCase {

    Assinatura buscarPorId(UUID id);
}