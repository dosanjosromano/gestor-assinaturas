package br.com.gestorAssinaturas.adapter.out.persistencia;

import br.com.gestorAssinaturas.domain.model.Assinatura;
import br.com.gestorAssinaturas.domain.model.StatusAssinatura;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface AssinaturaJpaRepository extends JpaRepository<Assinatura, UUID> {

    boolean existsByUsuarioIdAndStatusIn(UUID usuarioId, Collection<StatusAssinatura> status);

    Optional<Assinatura> findByUsuarioIdAndStatus(UUID usuarioId, StatusAssinatura status);
}
