package br.com.gestorAssinaturas.application.port.in;

import br.com.gestorAssinaturas.domain.model.Plano;

import java.util.UUID;

public record CriarAssinaturaCommand(UUID usuarioId, Plano plano) {
}