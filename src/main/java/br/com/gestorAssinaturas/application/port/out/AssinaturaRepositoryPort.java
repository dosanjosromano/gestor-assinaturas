package br.com.gestorAssinaturas.application.port.out;


import br.com.gestorAssinaturas.domain.model.Assinatura;

import java.util.Optional;
import java.util.UUID;

public interface AssinaturaRepositoryPort {

    Assinatura salvar(Assinatura assinatura);

    Optional<Assinatura> buscarPorId(UUID id);

    boolean existeAtivaOuPendentePara(UUID usuarioId);
}
