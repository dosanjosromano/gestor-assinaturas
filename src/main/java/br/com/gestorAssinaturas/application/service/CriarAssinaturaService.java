package br.com.gestorAssinaturas.application.service;

import br.com.gestorAssinaturas.application.port.in.AssinaturaResultado;
import br.com.gestorAssinaturas.application.port.in.CriarAssinaturaCommand;
import br.com.gestorAssinaturas.application.port.in.useCase.CriarAssinaturaUseCase;
import br.com.gestorAssinaturas.application.port.out.AssinaturaRepositoryPort;
import br.com.gestorAssinaturas.application.port.out.GatewayPagamentoPort;
import br.com.gestorAssinaturas.application.port.out.ResultadoPagamento;
import br.com.gestorAssinaturas.application.port.out.TentativaPagamentoRepositoryPort;
import br.com.gestorAssinaturas.domain.exception.AssinaturaJaAtivaException;
import br.com.gestorAssinaturas.domain.exception.PagamentoRecusadoException;
import br.com.gestorAssinaturas.domain.model.Assinatura;
import br.com.gestorAssinaturas.domain.model.TentativaPagamento;
import br.com.gestorAssinaturas.domain.model.TipoTentativa;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionOperations;

import java.util.UUID;

@Slf4j
@Service
public class CriarAssinaturaService implements CriarAssinaturaUseCase {

    private static final String NOME_CLASSE = CriarAssinaturaService.class.getSimpleName();

    private final AssinaturaRepositoryPort assinaturaRepositoryPort;
    private final TentativaPagamentoRepositoryPort tentativaPagamentoRepositoryPort;
    private final GatewayPagamentoPort gatewayPagamentoPort;
    private final TransactionOperations transactionOperations;

    public CriarAssinaturaService(
            AssinaturaRepositoryPort assinaturaRepositoryPort,
            TentativaPagamentoRepositoryPort tentativaPagamentoRepositoryPort,
            GatewayPagamentoPort gatewayPagamentoPort,
            TransactionOperations transactionOperations) {
        this.assinaturaRepositoryPort = assinaturaRepositoryPort;
        this.tentativaPagamentoRepositoryPort = tentativaPagamentoRepositoryPort;
        this.gatewayPagamentoPort = gatewayPagamentoPort;
        this.transactionOperations = transactionOperations;
    }

    @Override
    public AssinaturaResultado criar(CriarAssinaturaCommand comando) {
        log.info("[inicia] criar - {} - usuarioId={}", NOME_CLASSE, comando.usuarioId());
        if (assinaturaRepositoryPort.existeAtivaOuPendentePara(comando.usuarioId())) {
            throw new AssinaturaJaAtivaException(
                    "Usuário %s já possui assinatura ativa ou pendente".formatted(comando.usuarioId()));
        }

        Assinatura assinatura = new Assinatura(UUID.randomUUID(), comando.usuarioId(), comando.plano());
        TentativaPagamento tentativa = TentativaPagamento.iniciar(
                UUID.randomUUID(), assinatura.getId(), TipoTentativa.INICIAL, 1);

        try {
            transactionOperations.executeWithoutResult(status -> {
                assinaturaRepositoryPort.salvar(assinatura);
                tentativaPagamentoRepositoryPort.salvar(tentativa);
            });
            log.info("[finaliza] criar - {} - usuarioId={}", NOME_CLASSE, comando.usuarioId());
        } catch (DataIntegrityViolationException ex) {
            log.warn("[erro] criar - {} - usuarioId={} - motivo={}", NOME_CLASSE, comando.usuarioId(), ex.getMessage());
            throw new AssinaturaJaAtivaException(
                    "Usuário %s já possui assinatura ativa ou pendente".formatted(comando.usuarioId()));
        }

        ResultadoPagamento resultadoPagamento = gatewayPagamentoPort.cobrar(
                comando.plano().getPreco(), tentativa.getIdempotencyKey());

        transactionOperations.executeWithoutResult(status -> {
            switch (resultadoPagamento) {
                case APROVADO -> {
                    tentativa.aprovar();
                    assinatura.confirmarPagamento();
                }
                case RECUSADO -> {
                    tentativa.recusar();
                    assinatura.marcarFalhaPagamentoInicial();
                }
                case ERRO_TECNICO -> tentativa.marcarIndeterminada();
            }
            assinaturaRepositoryPort.salvar(assinatura);
            tentativaPagamentoRepositoryPort.salvar(tentativa);
        });

        if (resultadoPagamento == ResultadoPagamento.RECUSADO) {
            throw new PagamentoRecusadoException(
                    "Pagamento recusado para a assinatura %s".formatted(assinatura.getId()));
        }

        return new AssinaturaResultado(
                assinatura.getId(),
                assinatura.getStatus(),
                tentativa.getStatus(),
                assinatura.getDataInicio(),
                assinatura.getDataExpiracao());
    }
}
