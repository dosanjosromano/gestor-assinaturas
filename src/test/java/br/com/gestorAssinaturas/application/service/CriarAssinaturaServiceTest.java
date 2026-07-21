package br.com.gestorAssinaturas.application.service;

import br.com.gestorAssinaturas.application.port.in.AssinaturaResultado;
import br.com.gestorAssinaturas.application.port.in.CriarAssinaturaCommand;
import br.com.gestorAssinaturas.application.port.out.AssinaturaRepositoryPort;
import br.com.gestorAssinaturas.application.port.out.GatewayPagamentoPort;
import br.com.gestorAssinaturas.application.port.out.ResultadoPagamento;
import br.com.gestorAssinaturas.application.port.out.TentativaPagamentoRepositoryPort;
import br.com.gestorAssinaturas.application.port.out.UsuarioRepositoryPort;
import br.com.gestorAssinaturas.domain.exception.AssinaturaJaAtivaException;
import br.com.gestorAssinaturas.domain.exception.PagamentoRecusadoException;
import br.com.gestorAssinaturas.domain.exception.UsuarioInativoException;
import br.com.gestorAssinaturas.domain.exception.UsuarioNaoEncontradoException;
import br.com.gestorAssinaturas.domain.model.Assinatura;
import br.com.gestorAssinaturas.domain.model.Plano;
import br.com.gestorAssinaturas.domain.model.StatusAssinatura;
import br.com.gestorAssinaturas.domain.model.StatusTentativa;
import br.com.gestorAssinaturas.domain.model.TentativaPagamento;
import br.com.gestorAssinaturas.domain.model.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionOperations;

import java.math.BigDecimal;
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
class CriarAssinaturaServiceTest {

    @Mock
    private AssinaturaRepositoryPort assinaturaRepositoryPort;

    @Mock
    private TentativaPagamentoRepositoryPort tentativaPagamentoRepositoryPort;

    @Mock
    private GatewayPagamentoPort gatewayPagamentoPort;

    @Mock
    private UsuarioRepositoryPort usuarioRepositoryPort;

    private CriarAssinaturaService service;

    @BeforeEach
    void setUp() {
        service = new CriarAssinaturaService(
                assinaturaRepositoryPort,
                tentativaPagamentoRepositoryPort,
                gatewayPagamentoPort,
                usuarioRepositoryPort,
                TransactionOperations.withoutTransaction());
    }

    private CriarAssinaturaCommand comando() {
        UUID usuarioId = UUID.randomUUID();
        when(usuarioRepositoryPort.buscarPorId(usuarioId))
                .thenReturn(Optional.of(new Usuario(usuarioId, "Fulano", "fulano@teste.com")));
        return new CriarAssinaturaCommand(usuarioId, Plano.BASICO);
    }

    @Test
    void pagamentoAprovadoAtivaAssinatura() {
        when(assinaturaRepositoryPort.existeAtivaOuPendentePara(any())).thenReturn(false);
        when(gatewayPagamentoPort.cobrar(any(BigDecimal.class), anyString())).thenReturn(ResultadoPagamento.APROVADO);

        AssinaturaResultado resultado = service.criar(comando());

        assertThat(resultado.status()).isEqualTo(StatusAssinatura.ATIVA);
        assertThat(resultado.statusTentativaPagamento()).isEqualTo(StatusTentativa.APROVADA);
        assertThat(resultado.dataInicio()).isNotNull();
        assertThat(resultado.dataExpiracao()).isEqualTo(resultado.dataInicio().plusMonths(1));
    }

    @Test
    void pagamentoRecusadoPersisteFalhaPagamentoELancaExcecao() {
        when(assinaturaRepositoryPort.existeAtivaOuPendentePara(any())).thenReturn(false);
        when(gatewayPagamentoPort.cobrar(any(BigDecimal.class), anyString())).thenReturn(ResultadoPagamento.RECUSADO);

        assertThatThrownBy(() -> service.criar(comando()))
                .isInstanceOf(PagamentoRecusadoException.class);

        ArgumentCaptor<Assinatura> assinaturaCaptor = ArgumentCaptor.forClass(Assinatura.class);
        verify(assinaturaRepositoryPort, times(2)).salvar(assinaturaCaptor.capture());
        assertThat(assinaturaCaptor.getValue().getStatus()).isEqualTo(StatusAssinatura.FALHA_PAGAMENTO);

        ArgumentCaptor<TentativaPagamento> tentativaCaptor = ArgumentCaptor.forClass(TentativaPagamento.class);
        verify(tentativaPagamentoRepositoryPort, times(2)).salvar(tentativaCaptor.capture());
        assertThat(tentativaCaptor.getValue().getStatus()).isEqualTo(StatusTentativa.RECUSADA);
    }

    @Test
    void erroTecnicoMantemAguardandoPagamentoComTentativaIndeterminada() {
        when(assinaturaRepositoryPort.existeAtivaOuPendentePara(any())).thenReturn(false);
        when(gatewayPagamentoPort.cobrar(any(BigDecimal.class), anyString()))
                .thenReturn(ResultadoPagamento.ERRO_TECNICO);

        AssinaturaResultado resultado = service.criar(comando());

        assertThat(resultado.status()).isEqualTo(StatusAssinatura.AGUARDANDO_PAGAMENTO);
        assertThat(resultado.statusTentativaPagamento()).isEqualTo(StatusTentativa.INDETERMINADA);
    }

    @Test
    void usuarioComAssinaturaAtivaOuPendenteLancaExcecaoSemPersistirNada() {
        when(assinaturaRepositoryPort.existeAtivaOuPendentePara(any())).thenReturn(true);

        assertThatThrownBy(() -> service.criar(comando()))
                .isInstanceOf(AssinaturaJaAtivaException.class);

        verify(assinaturaRepositoryPort, never()).salvar(any());
        verify(tentativaPagamentoRepositoryPort, never()).salvar(any());
        verify(gatewayPagamentoPort, never()).cobrar(any(), anyString());
    }

    @Test
    void usuarioInexistenteLancaExcecaoSemChamarGateway() {
        UUID usuarioId = UUID.randomUUID();
        when(usuarioRepositoryPort.buscarPorId(usuarioId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.criar(new CriarAssinaturaCommand(usuarioId, Plano.BASICO)))
                .isInstanceOf(UsuarioNaoEncontradoException.class);

        verify(gatewayPagamentoPort, never()).cobrar(any(), anyString());
        verify(assinaturaRepositoryPort, never()).salvar(any());
    }

    @Test
    void usuarioInativoLancaExcecaoSemChamarGateway() {
        UUID usuarioId = UUID.randomUUID();
        Usuario usuarioInativo = new Usuario(usuarioId, "Fulano", "fulano@teste.com");
        usuarioInativo.excluir();
        when(usuarioRepositoryPort.buscarPorId(usuarioId)).thenReturn(Optional.of(usuarioInativo));

        assertThatThrownBy(() -> service.criar(new CriarAssinaturaCommand(usuarioId, Plano.BASICO)))
                .isInstanceOf(UsuarioInativoException.class);

        verify(gatewayPagamentoPort, never()).cobrar(any(), anyString());
        verify(assinaturaRepositoryPort, never()).salvar(any());
    }
}
