package br.com.gestorAssinaturas.application.service;


import br.com.gestorAssinaturas.application.port.in.useCase.ProcessarRenovacaoUseCase;
import br.com.gestorAssinaturas.application.port.out.AssinaturaRepositoryPort;
import br.com.gestorAssinaturas.application.port.out.CacheIndisponivelException;
import br.com.gestorAssinaturas.application.port.out.CachePort;
import br.com.gestorAssinaturas.application.port.out.ErroTecnicoGatewayException;
import br.com.gestorAssinaturas.application.port.out.GatewayPagamentoPort;
import br.com.gestorAssinaturas.application.port.out.ResultadoPagamento;
import br.com.gestorAssinaturas.application.port.out.TentativaPagamentoRepositoryPort;
import br.com.gestorAssinaturas.domain.exception.AssinaturaNaoEncontradaException;
import br.com.gestorAssinaturas.domain.model.Assinatura;
import br.com.gestorAssinaturas.domain.model.StatusAssinatura;
import br.com.gestorAssinaturas.domain.model.StatusTentativa;
import br.com.gestorAssinaturas.domain.model.TentativaPagamento;
import br.com.gestorAssinaturas.domain.model.TipoTentativa;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionOperations;

import java.util.UUID;

@Slf4j
@Service
public class ProcessarRenovacaoService implements ProcessarRenovacaoUseCase {

    private static final String PREFIXO_CHAVE_CACHE = "assinatura:ativa:";

    private final AssinaturaRepositoryPort assinaturaRepositoryPort;
    private final TentativaPagamentoRepositoryPort tentativaPagamentoRepositoryPort;
    private final GatewayPagamentoPort gatewayPagamentoPort;
    private final CachePort cachePort;
    private final TransactionOperations transactionOperations;

    public ProcessarRenovacaoService(
            AssinaturaRepositoryPort assinaturaRepositoryPort,
            TentativaPagamentoRepositoryPort tentativaPagamentoRepositoryPort,
            GatewayPagamentoPort gatewayPagamentoPort,
            CachePort cachePort,
            TransactionOperations transactionOperations) {
        this.assinaturaRepositoryPort = assinaturaRepositoryPort;
        this.tentativaPagamentoRepositoryPort = tentativaPagamentoRepositoryPort;
        this.gatewayPagamentoPort = gatewayPagamentoPort;
        this.cachePort = cachePort;
        this.transactionOperations = transactionOperations;
    }

    @Override
    public void processar(UUID assinaturaId) {
        Assinatura assinatura = buscarAssinatura(assinaturaId);
        if (assinatura.getStatus() != StatusAssinatura.ATIVA) {
            log.info("[ignorado] assinatura {} não está ATIVA (status={}) - evento de renovação descartado",
                    assinaturaId, assinatura.getStatus());
            return;
        }

        int tentativaNumero = assinatura.getFalhasRenovacaoConsecutivas() + 1;
        String idempotencyKey = idempotencyKey(assinaturaId, tentativaNumero);
        TentativaPagamento tentativa = tentativaPagamentoRepositoryPort.buscarPorIdempotencyKey(idempotencyKey)
                .orElse(null);

        if (tentativaJaConcluida(tentativa)) {
            log.info("[idempotente] tentativa {} já concluída com status {} - evento de renovação ignorado",
                    idempotencyKey, tentativa.getStatus());
            return;
        }
        if (tentativa == null) {
            tentativa = criarTentativa(assinaturaId, tentativaNumero);
        }

        ResultadoPagamento resultadoPagamento = gatewayPagamentoPort.cobrar(assinatura.getPlano().getPreco(), idempotencyKey);
        aplicarResultado(assinatura, tentativa, resultadoPagamento);
        invalidarCacheSeNecessario(assinatura, resultadoPagamento);

        if (resultadoPagamento == ResultadoPagamento.ERRO_TECNICO) {
            throw new ErroTecnicoGatewayException(
                    "Erro técnico do gateway ao processar renovação da assinatura %s".formatted(assinaturaId));
        }
    }

    private Assinatura buscarAssinatura(UUID assinaturaId) {
        return assinaturaRepositoryPort.buscarPorId(assinaturaId)
                .orElseThrow(() -> new AssinaturaNaoEncontradaException(
                        "Assinatura %s não encontrada".formatted(assinaturaId)));
    }

    private String idempotencyKey(UUID assinaturaId, int tentativaNumero) {
        return assinaturaId + ":" + TipoTentativa.RENOVACAO + ":" + tentativaNumero;
    }

    private boolean tentativaJaConcluida(TentativaPagamento tentativa) {
        return tentativa != null
                && tentativa.getStatus() != StatusTentativa.INICIADA
                && tentativa.getStatus() != StatusTentativa.INDETERMINADA;
    }

    private TentativaPagamento criarTentativa(UUID assinaturaId, int tentativaNumero) {
        TentativaPagamento novaTentativa = TentativaPagamento.iniciar(
                UUID.randomUUID(), assinaturaId, TipoTentativa.RENOVACAO, tentativaNumero);
        transactionOperations.executeWithoutResult(status -> tentativaPagamentoRepositoryPort.salvar(novaTentativa));
        return novaTentativa;
    }

    private void aplicarResultado(Assinatura assinatura, TentativaPagamento tentativa, ResultadoPagamento resultadoPagamento) {
        transactionOperations.executeWithoutResult(status -> {
            switch (resultadoPagamento) {
                case APROVADO -> {
                    tentativa.aprovar();
                    assinatura.renovarCiclo();
                }
                case RECUSADO -> {
                    tentativa.recusar();
                    assinatura.registrarFalhaRenovacao();
                }
                case ERRO_TECNICO -> tentativa.marcarIndeterminada();
            }
            tentativaPagamentoRepositoryPort.salvar(tentativa);
            assinaturaRepositoryPort.salvar(assinatura);
        });
    }

    private void invalidarCacheSeNecessario(Assinatura assinatura, ResultadoPagamento resultadoPagamento) {
        boolean rendeuInvalidacaoDeCache = resultadoPagamento == ResultadoPagamento.APROVADO
                || (resultadoPagamento == ResultadoPagamento.RECUSADO && assinatura.getStatus() == StatusAssinatura.SUSPENSA);
        if (!rendeuInvalidacaoDeCache) {
            return;
        }
        try {
            cachePort.evict(PREFIXO_CHAVE_CACHE + assinatura.getUsuarioId());
        } catch (CacheIndisponivelException e) {
            log.warn("[cache indisponível] evict pós-renovação não aplicado - usuarioId={} motivo={}",
                    assinatura.getUsuarioId(), e.getMessage());
        }
    }
}

