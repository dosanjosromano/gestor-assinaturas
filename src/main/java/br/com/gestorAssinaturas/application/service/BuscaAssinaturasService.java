package br.com.gestorAssinaturas.application.service;

import br.com.gestorAssinaturas.application.port.in.useCase.BuscaAssinaturasUseCase;
import br.com.gestorAssinaturas.application.port.out.AssinaturaRepositoryPort;
import br.com.gestorAssinaturas.domain.exception.AssinaturaNaoEncontradaException;
import br.com.gestorAssinaturas.domain.model.Assinatura;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
public class BuscaAssinaturasService implements BuscaAssinaturasUseCase {

    private static final String NOME_CLASSE = BuscaAssinaturasService.class.getSimpleName();
    private final AssinaturaRepositoryPort assinaturaRepositoryPort;

    public BuscaAssinaturasService(AssinaturaRepositoryPort assinaturaRepositoryPort) {
        this.assinaturaRepositoryPort = assinaturaRepositoryPort;
    }

    @Override
    public Assinatura buscarPorId(UUID id) {
        log.info("[inicia] buscarPorId - {} - id={}", NOME_CLASSE, id);
        try {
            Assinatura assinatura = assinaturaRepositoryPort.buscarPorId(id)
                    .orElseThrow(() -> new AssinaturaNaoEncontradaException("Assinatura %s não encontrada".formatted(id)));
            log.info("[finaliza] buscarPorId - {} - id={}", NOME_CLASSE, id);
            return assinatura;
        } catch (RuntimeException e) {
            log.warn("[erro] buscarPorId - {} - id={} - motivo={}", NOME_CLASSE, id, e.getMessage());
            throw e;
        }
    }
}
