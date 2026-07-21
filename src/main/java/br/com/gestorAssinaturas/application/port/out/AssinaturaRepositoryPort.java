package br.com.gestorAssinaturas.application.port.out;


import br.com.gestorAssinaturas.domain.model.Assinatura;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AssinaturaRepositoryPort {

    Assinatura salvar(Assinatura assinatura);

    Optional<Assinatura> buscarPorId(UUID id);

    boolean existeAtivaOuPendentePara(UUID usuarioId);

    Optional<Assinatura> buscarAtivaPorUsuario(UUID usuarioId);

    List<Assinatura> buscarElegiveisParaRenovacao(LocalDate hoje);

}
