package br.com.gestorAssinaturas.application.service;

import br.com.gestorAssinaturas.application.port.out.AssinaturaRepositoryPort;
import br.com.gestorAssinaturas.application.port.out.CachePort;
import br.com.gestorAssinaturas.domain.exception.AssinaturaNaoEncontradaException;
import br.com.gestorAssinaturas.domain.exception.EstadoInvalidoException;
import br.com.gestorAssinaturas.domain.model.Assinatura;
import br.com.gestorAssinaturas.domain.model.Plano;
import br.com.gestorAssinaturas.domain.model.StatusAssinatura;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CancelarAssinaturaServiceTest {

    @Mock
    private AssinaturaRepositoryPort assinaturaRepositoryPort;

    @Mock
    private CachePort cachePort;

    private CancelarAssinaturaService service;

    private UUID usuarioId;

    @BeforeEach
    void setUp() {
        service = new CancelarAssinaturaService(assinaturaRepositoryPort, cachePort);
        usuarioId = UUID.randomUUID();
    }

    private Assinatura assinaturaAtiva() {
        Assinatura assinatura = new Assinatura(UUID.randomUUID(), usuarioId, Plano.BASICO);
        assinatura.confirmarPagamento();
        return assinatura;
    }

    @Test
    void cancelarAPartirDeAtivaFunciona() {
        Assinatura assinatura = assinaturaAtiva();
        when(assinaturaRepositoryPort.buscarPorId(assinatura.getId())).thenReturn(Optional.of(assinatura));

        Assinatura resultado = service.cancelar(assinatura.getId(), usuarioId);

        assertThat(resultado.getStatus()).isEqualTo(StatusAssinatura.CANCELADA);
        assertThat(resultado.getDataExpiracao()).isEqualTo(assinatura.getDataExpiracao());
        verify(assinaturaRepositoryPort).salvar(assinatura);
        verify(cachePort).evict("assinatura:ativa:" + usuarioId);
    }

    @Test
    void cancelarAPartirDeAguardandoPagamentoLanca409() {
        Assinatura assinatura = new Assinatura(UUID.randomUUID(), usuarioId, Plano.BASICO);
        when(assinaturaRepositoryPort.buscarPorId(assinatura.getId())).thenReturn(Optional.of(assinatura));

        assertThatThrownBy(() -> service.cancelar(assinatura.getId(), usuarioId))
                .isInstanceOf(EstadoInvalidoException.class);

        verify(assinaturaRepositoryPort, never()).salvar(any());
        verify(cachePort, never()).evict(any());
    }

    @Test
    void cancelarAPartirDeSuspensaLanca409() {
        Assinatura assinatura = assinaturaAtiva();
        assinatura.registrarFalhaRenovacao();
        assinatura.registrarFalhaRenovacao();
        assinatura.registrarFalhaRenovacao();
        when(assinaturaRepositoryPort.buscarPorId(assinatura.getId())).thenReturn(Optional.of(assinatura));

        assertThatThrownBy(() -> service.cancelar(assinatura.getId(), usuarioId))
                .isInstanceOf(EstadoInvalidoException.class);

        verify(assinaturaRepositoryPort, never()).salvar(any());
        verify(cachePort, never()).evict(any());
    }

    @Test
    void cancelarUmaJaCanceladaEIdempotente() {
        Assinatura assinatura = assinaturaAtiva();
        assinatura.cancelar();
        when(assinaturaRepositoryPort.buscarPorId(assinatura.getId())).thenReturn(Optional.of(assinatura));

        Assinatura resultado = service.cancelar(assinatura.getId(), usuarioId);

        assertThat(resultado.getStatus()).isEqualTo(StatusAssinatura.CANCELADA);
        verify(assinaturaRepositoryPort, never()).salvar(any());
        verify(cachePort, never()).evict(any());
    }

    @Test
    void usuarioIdDiferenteDoDonoLancaAssinaturaNaoEncontrada() {
        Assinatura assinatura = assinaturaAtiva();
        when(assinaturaRepositoryPort.buscarPorId(assinatura.getId())).thenReturn(Optional.of(assinatura));

        assertThatThrownBy(() -> service.cancelar(assinatura.getId(), UUID.randomUUID()))
                .isInstanceOf(AssinaturaNaoEncontradaException.class);

        verify(assinaturaRepositoryPort, never()).salvar(any());
        verify(cachePort, never()).evict(any());
    }
}
