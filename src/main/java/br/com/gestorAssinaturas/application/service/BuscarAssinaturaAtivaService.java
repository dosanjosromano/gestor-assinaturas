package br.com.gestorAssinaturas.application.service;

import br.com.gestorAssinaturas.application.port.in.AssinaturaAtivaResultado;
import br.com.gestorAssinaturas.application.port.in.useCase.BuscarAssinaturaAtivaUseCase;
import br.com.gestorAssinaturas.application.port.out.AssinaturaRepositoryPort;

import br.com.gestorAssinaturas.application.port.out.CachePort;
import br.com.gestorAssinaturas.domain.exception.AssinaturaNaoEncontradaException;
import br.com.gestorAssinaturas.domain.model.Assinatura;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Slf4j
@Service
public class BuscarAssinaturaAtivaService implements BuscarAssinaturaAtivaUseCase {

    private static final String NOME_CLASSE = BuscarAssinaturaAtivaService.class.getSimpleName();
    private static final String PREFIXO_CHAVE = "assinatura:ativa:";

    private final CachePort cachePort;
    private final AssinaturaRepositoryPort assinaturaRepositoryPort;
    private final Duration ttl;

    public BuscarAssinaturaAtivaService(
            CachePort cachePort,
            AssinaturaRepositoryPort assinaturaRepositoryPort,
            @Value("${app.cache.assinatura-ativa.ttl:5m}") Duration ttl) {
        this.cachePort = cachePort;
        this.assinaturaRepositoryPort = assinaturaRepositoryPort;
        this.ttl = ttl;
    }

    @Override
    public AssinaturaAtivaResultado buscarAtiva(UUID usuarioId) {
        log.info("[inicia] buscarAtiva - {}", NOME_CLASSE);
        try {
            String chave = chave(usuarioId);
            AssinaturaAtivaResultado resultado = cachePort.buscar(chave, AssinaturaAtivaResultado.class)
                    .orElseGet(() -> buscarNoBancoEPopularCache(usuarioId, chave));
            log.info("[finaliza] buscarAtiva - {}", NOME_CLASSE);
            return resultado;
        } catch (RuntimeException e) {
            log.warn("[erro] buscarAtiva - {} - usuarioId={} - motivo={}", NOME_CLASSE, usuarioId, e.getMessage());
            throw e;
        }
    }

    private AssinaturaAtivaResultado buscarNoBancoEPopularCache(UUID usuarioId, String chave) {
        log.info("[inicia] buscarNoBancoEPopularCache - {}", NOME_CLASSE);
        try {
            Assinatura assinatura = assinaturaRepositoryPort.buscarAtivaPorUsuario(usuarioId)
                    .orElseThrow(() -> new AssinaturaNaoEncontradaException(
                            "Usuário %s não possui assinatura ativa".formatted(usuarioId)));

            AssinaturaAtivaResultado resultado = new AssinaturaAtivaResultado(
                    assinatura.getId(),
                    assinatura.getUsuarioId(),
                    assinatura.getPlano(),
                    assinatura.getStatus(),
                    assinatura.getDataInicio(),
                    assinatura.getDataExpiracao());

            log.info("[persistindo] buscarNoBancoEPopularCache - {} - chave={} valor={}", NOME_CLASSE, chave, resultado);
            cachePort.salvar(chave, resultado, ttl);
            log.info("[finaliza] buscarNoBancoEPopularCache - {}", NOME_CLASSE);
            return resultado;
        } catch (RuntimeException e) {
            log.warn("[erro] buscarNoBancoEPopularCache - {} - usuarioId={} - motivo={}",
                    NOME_CLASSE, usuarioId, e.getMessage());
            throw e;
        }
    }

    private String chave(UUID usuarioId) {
        return PREFIXO_CHAVE + usuarioId;
    }
}
