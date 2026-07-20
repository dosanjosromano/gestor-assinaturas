package br.com.gestorAssinaturas.adapter.out.persistencia;

import br.com.gestorAssinaturas.domain.model.TentativaPagamento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TentativaPagamentoJpaRepository extends JpaRepository<TentativaPagamento, UUID> {

    Optional<TentativaPagamento> findByIdempotencyKey(String idempotencyKey);
}