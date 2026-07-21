package br.com.gestorAssinaturas.application.service;

import br.com.gestorAssinaturas.application.port.out.AssinaturaRepositoryPort;
import br.com.gestorAssinaturas.application.port.out.CachePort;
import br.com.gestorAssinaturas.application.port.out.ErroTecnicoGatewayException;
import br.com.gestorAssinaturas.application.port.out.GatewayPagamentoPort;
import br.com.gestorAssinaturas.application.port.out.ResultadoPagamento;
import br.com.gestorAssinaturas.application.port.out.TentativaPagamentoRepositoryPort;
import br.com.gestorAssinaturas.domain.model.Assinatura;
import br.com.gestorAssinaturas.domain.model.Plano;
import br.com.gestorAssinaturas.domain.model.StatusAssinatura;
import br.com.gestorAssinaturas.domain.model.StatusTentativa;
import br.com.gestorAssinaturas.domain.model.TentativaPagamento;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionOperations;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProcessarRenovacaoServiceTest {

    @Mock
    private AssinaturaRepositoryPort assinaturaRepositoryPort;

    @Mock
    private TentativaPagamentoRepositoryPort tentativaPagamentoRepositoryPort;

    @Mock
    private GatewayPagamentoPort gatewayPagamentoPort;

    @Mock
    private CachePort cachePort;

    private ProcessarRenovacaoService service;

    @BeforeEach
    void setUp() {
        service = new ProcessarRenovacaoService(
                assinaturaRepositoryPort,
                tentativaPagamentoRepositoryPort,
                gatewayPagamentoPort,
                cachePort,
                TransactionOperations.withoutTransaction());
    }

    private Assinatura assinaturaAtivaComFalhas(int falhas) {
        Assinatura assinatura = new Assinatura(UUID.randomUUID(), UUID.randomUUID(), Plano.BASICO);
        assinatura.confirmarPagamento();
        for (int i = 0; i < falhas; i++) {
            assinatura.registrarFalhaRenovacao();
        }
        return assinatura;
    }

    @Test
    void renovacaoAprovadaAvancaExpiracaoZeraFalhasEInvalidaCache() {
        Assinatura assinatura = assinaturaAtivaComFalhas(1);
        LocalDate expiracaoAnterior = assinatura.getDataExpiracao();
        when(assinaturaRepositoryPort.buscarPorId(assinatura.getId())).thenReturn(Optional.of(assinatura));
        when(tentativaPagamentoRepositoryPort.buscarPorIdempotencyKey(anyString())).thenReturn(Optional.empty());
        when(gatewayPagamentoPort.cobrar(any(BigDecimal.class), anyString())).thenReturn(ResultadoPagamento.APROVADO);

        service.processar(assinatura.getId());

        assertThat(assinatura.getStatus()).isEqualTo(StatusAssinatura.ATIVA);
        assertThat(assinatura.getFalhasRenovacaoConsecutivas()).isZero();
        assertThat(assinatura.getDataExpiracao()).isEqualTo(expiracaoAnterior.plusMonths(1));

        ArgumentCaptor<TentativaPagamento> tentativaCaptor = ArgumentCaptor.forClass(TentativaPagamento.class);
        verify(tentativaPagamentoRepositoryPort, times(2)).salvar(tentativaCaptor.capture());
        assertThat(tentativaCaptor.getValue().getStatus()).isEqualTo(StatusTentativa.APROVADA);

        verify(cachePort).evict("assinatura:ativa:" + assinatura.getUsuarioId());
    }

    @Test
    void renovacaoRecusadaDentroDoLimiteMantemAtivaSemInvalidarCache() {
        Assinatura assinatura = assinaturaAtivaComFalhas(1);
        LocalDate expiracaoAnterior = assinatura.getDataExpiracao();
        when(assinaturaRepositoryPort.buscarPorId(assinatura.getId())).thenReturn(Optional.of(assinatura));
        when(tentativaPagamentoRepositoryPort.buscarPorIdempotencyKey(anyString())).thenReturn(Optional.empty());
        when(gatewayPagamentoPort.cobrar(any(BigDecimal.class), anyString())).thenReturn(ResultadoPagamento.RECUSADO);

        service.processar(assinatura.getId());

        assertThat(assinatura.getStatus()).isEqualTo(StatusAssinatura.ATIVA);
        assertThat(assinatura.getFalhasRenovacaoConsecutivas()).isEqualTo(2);
        assertThat(assinatura.getDataExpiracao()).isEqualTo(expiracaoAnterior);

        verify(cachePort, never()).evict(anyString());
    }

    @Test
    void terceiraFalhaConsecutivaSuspendeAssinaturaEInvalidaCache() {
        Assinatura assinatura = assinaturaAtivaComFalhas(2);
        when(assinaturaRepositoryPort.buscarPorId(assinatura.getId())).thenReturn(Optional.of(assinatura));
        when(tentativaPagamentoRepositoryPort.buscarPorIdempotencyKey(anyString())).thenReturn(Optional.empty());
        when(gatewayPagamentoPort.cobrar(any(BigDecimal.class), anyString())).thenReturn(ResultadoPagamento.RECUSADO);

        service.processar(assinatura.getId());

        assertThat(assinatura.getStatus()).isEqualTo(StatusAssinatura.SUSPENSA);
        assertThat(assinatura.getFalhasRenovacaoConsecutivas()).isEqualTo(3);

        verify(cachePort).evict("assinatura:ativa:" + assinatura.getUsuarioId());
    }

    @Test
    void erroTecnicoNaoAlteraContadorELancaExcecaoParaAcionarRetryTecnico() {
        Assinatura assinatura = assinaturaAtivaComFalhas(1);
        when(assinaturaRepositoryPort.buscarPorId(assinatura.getId())).thenReturn(Optional.of(assinatura));
        when(tentativaPagamentoRepositoryPort.buscarPorIdempotencyKey(anyString())).thenReturn(Optional.empty());
        when(gatewayPagamentoPort.cobrar(any(BigDecimal.class), anyString())).thenReturn(ResultadoPagamento.ERRO_TECNICO);

        assertThatThrownBy(() -> service.processar(assinatura.getId()))
                .isInstanceOf(ErroTecnicoGatewayException.class);

        assertThat(assinatura.getStatus()).isEqualTo(StatusAssinatura.ATIVA);
        assertThat(assinatura.getFalhasRenovacaoConsecutivas()).isEqualTo(1);

        ArgumentCaptor<TentativaPagamento> tentativaCaptor = ArgumentCaptor.forClass(TentativaPagamento.class);
        verify(tentativaPagamentoRepositoryPort, times(2)).salvar(tentativaCaptor.capture());
        assertThat(tentativaCaptor.getValue().getStatus()).isEqualTo(StatusTentativa.INDETERMINADA);

        verify(cachePort, never()).evict(anyString());
    }

    @Test
    void assinaturaNaoAtivaIgnoraEventoSemChamarGateway() {
        Assinatura assinatura = new Assinatura(UUID.randomUUID(), UUID.randomUUID(), Plano.BASICO);
        assinatura.marcarFalhaPagamentoInicial();
        when(assinaturaRepositoryPort.buscarPorId(assinatura.getId())).thenReturn(Optional.of(assinatura));

        service.processar(assinatura.getId());

        verify(gatewayPagamentoPort, never()).cobrar(any(), anyString());
        verify(tentativaPagamentoRepositoryPort, never()).salvar(any());
    }

    @Test
    void tentativaJaAprovadaParaMesmaChaveEIgnoradaIdempotentemente() {
        Assinatura assinatura = assinaturaAtivaComFalhas(0);
        TentativaPagamento tentativaJaAprovada = TentativaPagamento.iniciar(
                UUID.randomUUID(), assinatura.getId(), br.com.gestorAssinaturas.domain.model.TipoTentativa.RENOVACAO, 1);
        tentativaJaAprovada.aprovar();
        when(assinaturaRepositoryPort.buscarPorId(assinatura.getId())).thenReturn(Optional.of(assinatura));
        when(tentativaPagamentoRepositoryPort.buscarPorIdempotencyKey(anyString())).thenReturn(Optional.of(tentativaJaAprovada));

        service.processar(assinatura.getId());

        verify(gatewayPagamentoPort, never()).cobrar(any(), anyString());
        verify(assinaturaRepositoryPort, never()).salvar(any());
    }
}

