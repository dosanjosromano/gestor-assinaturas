package br.com.gestorAssinaturas.application.service;


import br.com.gestorAssinaturas.application.port.in.useCase.CancelarAssinaturaUseCase;
import br.com.gestorAssinaturas.application.port.out.AssinaturaRepositoryPort;
import br.com.gestorAssinaturas.application.port.out.CacheIndisponivelException;
import br.com.gestorAssinaturas.application.port.out.CachePort;
import br.com.gestorAssinaturas.domain.exception.AssinaturaNaoEncontradaException;
import br.com.gestorAssinaturas.domain.model.Assinatura;
import br.com.gestorAssinaturas.domain.model.StatusAssinatura;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
public class CancelarAssinaturaService implements CancelarAssinaturaUseCase {

    private static final String NOME_CLASSE = CancelarAssinaturaService.class.getSimpleName();
    private static final String PREFIXO_CHAVE_CACHE = "assinatura:ativa:";

    private final AssinaturaRepositoryPort assinaturaRepositoryPort;
    private final CachePort cachePort;

    public CancelarAssinaturaService(AssinaturaRepositoryPort assinaturaRepositoryPort, CachePort cachePort) {
        this.assinaturaRepositoryPort = assinaturaRepositoryPort;
        this.cachePort = cachePort;
    }

    @Override
    public Assinatura cancelar(UUID assinaturaId, UUID usuarioId) {
        log.info("[inicia] cancelar - {} - assinaturaId={} usuarioId={}", NOME_CLASSE, assinaturaId, usuarioId);
        Assinatura assinatura = assinaturaRepositoryPort.buscarPorId(assinaturaId)
                .filter(a -> a.getUsuarioId().equals(usuarioId))
                .orElseThrow(() -> new AssinaturaNaoEncontradaException(
                        "Assinatura %s não encontrada".formatted(assinaturaId)));

        if (assinatura.getStatus() == StatusAssinatura.CANCELADA) {
            return assinatura;
        }

        assinatura.cancelar();
        assinaturaRepositoryPort.salvar(assinatura);

        try {
            cachePort.evict(PREFIXO_CHAVE_CACHE + usuarioId);
        } catch (CacheIndisponivelException e) {
            log.warn("[cache indisponível] evict pós-cancelamento não aplicado - usuarioId={} motivo={}",
                    usuarioId, e.getMessage());
        }

        log.info("[finaliza] cancelar - {} - assinaturaId={} usuarioId={}", NOME_CLASSE, assinaturaId, usuarioId);
        return assinatura;
    }
}

