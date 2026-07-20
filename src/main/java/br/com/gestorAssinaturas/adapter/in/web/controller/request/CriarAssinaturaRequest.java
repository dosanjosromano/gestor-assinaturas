package br.com.gestorAssinaturas.adapter.in.web.controller.request;

import br.com.gestorAssinaturas.domain.model.Plano;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CriarAssinaturaRequest(
        @NotNull UUID usuarioId,
        @NotNull Plano plano) {
}
