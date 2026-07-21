package br.com.gestorAssinaturas.adapter.out.persistencia.impl;

import br.com.gestorAssinaturas.adapter.out.persistencia.AssinaturaJpaRepository;
import br.com.gestorAssinaturas.application.port.out.AssinaturaRepositoryPort;
import br.com.gestorAssinaturas.domain.model.Assinatura;
import br.com.gestorAssinaturas.domain.model.StatusAssinatura;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class AssinaturaRepositoryAdapter implements AssinaturaRepositoryPort {

    private static final List<StatusAssinatura> STATUS_ATIVA_OU_PENDENTE =
            List.of(StatusAssinatura.ATIVA, StatusAssinatura.AGUARDANDO_PAGAMENTO);

    private static final int MAX_FALHAS_RENOVACAO_CONSECUTIVAS = 3;

    private final AssinaturaJpaRepository assinaturaJpaRepository;

    public AssinaturaRepositoryAdapter(AssinaturaJpaRepository assinaturaJpaRepository) {
        this.assinaturaJpaRepository = assinaturaJpaRepository;
    }

    @Override
    public Assinatura salvar(Assinatura assinatura) {
        return assinaturaJpaRepository.save(assinatura);
    }

    @Override
    public Optional<Assinatura> buscarPorId(UUID id) {
        return assinaturaJpaRepository.findById(id);
    }

    @Override
    public boolean existeAtivaOuPendentePara(UUID usuarioId) {
        return assinaturaJpaRepository.existsByUsuarioIdAndStatusIn(usuarioId, STATUS_ATIVA_OU_PENDENTE);
    }

    @Override
    public Optional<Assinatura> buscarAtivaPorUsuario(UUID usuarioId) {
        return assinaturaJpaRepository.findByUsuarioIdAndStatus(usuarioId, StatusAssinatura.ATIVA);
    }

    @Override
    public List<Assinatura> buscarElegiveisParaRenovacao(LocalDate hoje) {
        return assinaturaJpaRepository.findByStatusAndDataExpiracaoLessThanEqualAndFalhasRenovacaoConsecutivasLessThan(
                StatusAssinatura.ATIVA, hoje, MAX_FALHAS_RENOVACAO_CONSECUTIVAS);
    }
}
