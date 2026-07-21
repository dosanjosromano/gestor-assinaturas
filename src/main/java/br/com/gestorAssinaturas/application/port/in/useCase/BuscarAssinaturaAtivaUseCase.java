package br.com.gestorAssinaturas.application.port.in.useCase;

import br.com.gestorAssinaturas.application.port.in.AssinaturaAtivaResultado;

import java.util.UUID;

public interface BuscarAssinaturaAtivaUseCase {

    AssinaturaAtivaResultado buscarAtiva(UUID usuarioId);
}
