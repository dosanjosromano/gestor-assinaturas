package br.com.gestorAssinaturas.application.port.in.useCase;

import java.util.UUID;

public interface ProcessarRenovacaoUseCase {

    void processar(UUID assinaturaId);
}