package br.com.gestorAssinaturas.application.service;

import br.com.gestorAssinaturas.application.port.in.AssinaturaResultado;
import br.com.gestorAssinaturas.application.port.in.CriarAssinaturaCommand;

import br.com.gestorAssinaturas.application.port.in.useCase.CriarAssinaturaUseCase;
import br.com.gestorAssinaturas.application.port.out.AssinaturaRepositoryPort;
import br.com.gestorAssinaturas.application.port.out.GatewayPagamentoPort;
import br.com.gestorAssinaturas.application.port.out.ResultadoPagamento;
import br.com.gestorAssinaturas.application.port.out.TentativaPagamentoRepositoryPort;
import br.com.gestorAssinaturas.application.port.out.UsuarioRepositoryPort;
import br.com.gestorAssinaturas.domain.exception.AssinaturaJaAtivaException;
import br.com.gestorAssinaturas.domain.exception.FalhaTecnicaPagamentoException;
import br.com.gestorAssinaturas.domain.exception.PagamentoRecusadoException;
import br.com.gestorAssinaturas.domain.exception.UsuarioInativoException;
import br.com.gestorAssinaturas.domain.exception.UsuarioNaoEncontradoException;
import br.com.gestorAssinaturas.domain.model.Assinatura;
import br.com.gestorAssinaturas.domain.model.StatusUsuario;
import br.com.gestorAssinaturas.domain.model.TentativaPagamento;
import br.com.gestorAssinaturas.domain.model.TipoTentativa;
import br.com.gestorAssinaturas.domain.model.Usuario;
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
    private final UsuarioRepositoryPort usuarioRepositoryPort;
    private final TransactionOperations transactionOperations;

    public CriarAssinaturaService(
            AssinaturaRepositoryPort assinaturaRepositoryPort,
            TentativaPagamentoRepositoryPort tentativaPagamentoRepositoryPort,
            GatewayPagamentoPort gatewayPagamentoPort,
            UsuarioRepositoryPort usuarioRepositoryPort,
            TransactionOperations transactionOperations) {
        this.assinaturaRepositoryPort = assinaturaRepositoryPort;
        this.tentativaPagamentoRepositoryPort = tentativaPagamentoRepositoryPort;
        this.gatewayPagamentoPort = gatewayPagamentoPort;
        this.usuarioRepositoryPort = usuarioRepositoryPort;
        this.transactionOperations = transactionOperations;
    }

    @Override
    public AssinaturaResultado criar(CriarAssinaturaCommand comando) {
        log.info("[inicia] criar - {} - usuarioId={}", NOME_CLASSE, comando.usuarioId());
        try {
            AssinaturaResultado resultado = criarInternamente(comando);
            log.info("[finaliza] criar - {} - usuarioId={}", NOME_CLASSE, comando.usuarioId());
            return resultado;
        } catch (RuntimeException e) {
            log.warn("[erro] criar - {} - usuarioId={} - motivo={}", NOME_CLASSE, comando.usuarioId(), e.getMessage());
            throw e;
        }
    }

    private AssinaturaResultado criarInternamente(CriarAssinaturaCommand comando) {
        Usuario usuario = usuarioRepositoryPort.buscarPorId(comando.usuarioId())
                .orElseThrow(() -> new UsuarioNaoEncontradoException(
                        "Usuário %s não encontrado".formatted(comando.usuarioId())));
        if (usuario.getStatus() != StatusUsuario.ATIVO) {
            throw new UsuarioInativoException(
                    "Usuário %s está inativo e não pode assinar".formatted(comando.usuarioId()));
        }

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
        } catch (DataIntegrityViolationException ex) {
            throw new AssinaturaJaAtivaException(
                    "Usuário %s já possui assinatura ativa ou pendente".formatted(comando.usuarioId()));
        }

        ResultadoPagamento resultadoPagamento = gatewayPagamentoPort.cobrar(
                comando.plano().getPreco(), tentativa.getIdempotencyKey());
        log.info("[desfecho-pagamento] criar - {} - assinaturaId={} resultado={}",
                NOME_CLASSE, assinatura.getId(), resultadoPagamento);

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
                case ERRO_TECNICO -> {
                    tentativa.marcarIndeterminada();
                    assinatura.marcarFalhaPagamentoInicial();
                }
            }
            assinaturaRepositoryPort.salvar(assinatura);
            tentativaPagamentoRepositoryPort.salvar(tentativa);
        });

        if (resultadoPagamento == ResultadoPagamento.RECUSADO) {
            throw new PagamentoRecusadoException(
                    "Pagamento recusado para a assinatura %s".formatted(assinatura.getId()));
        }
        if (resultadoPagamento == ResultadoPagamento.ERRO_TECNICO) {
            throw new FalhaTecnicaPagamentoException(
                    "Falha técnica do gateway ao processar pagamento da assinatura %s".formatted(assinatura.getId()));
        }

        return new AssinaturaResultado(
                assinatura.getId(),
                assinatura.getStatus(),
                tentativa.getStatus(),
                assinatura.getDataInicio(),
                assinatura.getDataExpiracao());
    }
}
