package br.com.gestorAssinaturas.application.port.in.useCase;

import br.com.gestorAssinaturas.domain.model.Assinatura;

import java.util.UUID;

public interface CancelarAssinaturaUseCase {

    Assinatura cancelar(UUID assinaturaId, UUID usuarioId);
}
