package br.com.gestorAssinaturas.adapter.in.web.controller.response;

import java.time.LocalDate;
import java.util.UUID;

public record AssinaturaResponse(
        UUID id,
        UUID usuarioId,
        String plano,
        String status,
        LocalDate dataInicio,
        LocalDate dataExpiracao) {
}
