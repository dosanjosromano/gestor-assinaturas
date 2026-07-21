package br.com.gestorAssinaturas.application.port.in;

import br.com.gestorAssinaturas.domain.model.Plano;
import br.com.gestorAssinaturas.domain.model.StatusAssinatura;

import java.time.LocalDate;
import java.util.UUID;

public record AssinaturaAtivaResultado(
        UUID assinaturaId,
        UUID usuarioId,
        Plano plano,
        StatusAssinatura status,
        LocalDate dataInicio,
        LocalDate dataExpiracao) {
}
