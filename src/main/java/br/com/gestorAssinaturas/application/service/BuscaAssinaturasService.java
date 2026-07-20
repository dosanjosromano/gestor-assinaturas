package br.com.gestorAssinaturas.application.service;

import br.com.gestorAssinaturas.application.port.in.useCase.BuscaAssinaturasUseCase;
import br.com.gestorAssinaturas.application.port.out.AssinaturaRepositoryPort;
import br.com.gestorAssinaturas.domain.exception.AssinaturaNaoEncontradaException;
import br.com.gestorAssinaturas.domain.model.Assinatura;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class BuscaAssinaturasService implements BuscaAssinaturasUseCase {

    private final AssinaturaRepositoryPort assinaturaRepositoryPort;

    public BuscaAssinaturasService(AssinaturaRepositoryPort assinaturaRepositoryPort) {
        this.assinaturaRepositoryPort = assinaturaRepositoryPort;
    }

    @Override
    public Assinatura buscarPorId(UUID id) {
        return assinaturaRepositoryPort.buscarPorId(id)
                .orElseThrow(() -> new AssinaturaNaoEncontradaException("Assinatura %s não encontrada".formatted(id)));
    }
}
