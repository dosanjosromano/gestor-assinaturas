package br.com.gestorAssinaturas.application.port.in;

import br.com.gestorAssinaturas.domain.model.StatusAssinatura;
import br.com.gestorAssinaturas.domain.model.StatusTentativa;

import java.time.LocalDate;
import java.util.UUID;

public record AssinaturaResultado(
        UUID assinaturaId,
        StatusAssinatura status,
        StatusTentativa statusTentativaPagamento,
        LocalDate dataInicio,
        LocalDate dataExpiracao) {
}
