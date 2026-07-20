package br.com.gestorAssinaturas.adapter.out.persistencia.impl;

import br.com.gestorAssinaturas.adapter.out.persistencia.TentativaPagamentoJpaRepository;
import br.com.gestorAssinaturas.application.port.out.TentativaPagamentoRepositoryPort;
import br.com.gestorAssinaturas.domain.model.TentativaPagamento;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class TentativaPagamentoRepositoryAdapter implements TentativaPagamentoRepositoryPort {

    private final TentativaPagamentoJpaRepository tentativaPagamentoJpaRepository;

    public TentativaPagamentoRepositoryAdapter(TentativaPagamentoJpaRepository tentativaPagamentoJpaRepository) {
        this.tentativaPagamentoJpaRepository = tentativaPagamentoJpaRepository;
    }

    @Override
    public TentativaPagamento salvar(TentativaPagamento tentativaPagamento) {
        return tentativaPagamentoJpaRepository.save(tentativaPagamento);
    }

    @Override
    public Optional<TentativaPagamento> buscarPorIdempotencyKey(String idempotencyKey) {
        return tentativaPagamentoJpaRepository.findByIdempotencyKey(idempotencyKey);
    }
}