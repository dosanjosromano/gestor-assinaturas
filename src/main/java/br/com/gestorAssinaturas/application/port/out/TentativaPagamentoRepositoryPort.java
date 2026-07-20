package br.com.gestorAssinaturas.application.port.out;

import br.com.gestorAssinaturas.domain.model.TentativaPagamento;

import java.util.Optional;

public interface TentativaPagamentoRepositoryPort {

    TentativaPagamento salvar(TentativaPagamento tentativaPagamento);

    Optional<TentativaPagamento> buscarPorIdempotencyKey(String idempotencyKey);
}
