package br.com.gestorAssinaturas.application.service;

import br.com.gestorAssinaturas.application.port.in.AssinaturaAtivaResultado;
import br.com.gestorAssinaturas.application.port.out.AssinaturaRepositoryPort;
import br.com.gestorAssinaturas.application.port.out.CachePort;
import br.com.gestorAssinaturas.domain.exception.AssinaturaNaoEncontradaException;
import br.com.gestorAssinaturas.domain.model.Assinatura;
import br.com.gestorAssinaturas.domain.model.Plano;
import br.com.gestorAssinaturas.domain.model.StatusAssinatura;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BuscarAssinaturaAtivaServiceTest {

    @Mock
    private CachePort cachePort;

    @Mock
    private AssinaturaRepositoryPort assinaturaRepositoryPort;

    private BuscarAssinaturaAtivaService service;

    @BeforeEach
    void setUp() {
        service = new BuscarAssinaturaAtivaService(cachePort, assinaturaRepositoryPort, Duration.ofMinutes(5));
    }

    @Test
    void cacheHitNaoConsultaORepositorio() {
        UUID usuarioId = UUID.randomUUID();
        AssinaturaAtivaResultado resultadoEmCache = new AssinaturaAtivaResultado(
                UUID.randomUUID(), usuarioId, Plano.BASICO, StatusAssinatura.ATIVA, null, null);
        when(cachePort.buscar(eq("assinatura:ativa:" + usuarioId), eq(AssinaturaAtivaResultado.class)))
                .thenReturn(Optional.of(resultadoEmCache));

        AssinaturaAtivaResultado resultado = service.buscarAtiva(usuarioId);

        assertThat(resultado).isEqualTo(resultadoEmCache);
        verifyNoInteractions(assinaturaRepositoryPort);
    }

    @Test
    void cacheMissConsultaRepositorioEPopulaCache() {
        UUID usuarioId = UUID.randomUUID();
        Assinatura assinatura = new Assinatura(UUID.randomUUID(), usuarioId, Plano.PREMIUM);
        assinatura.confirmarPagamento();

        when(cachePort.buscar(eq("assinatura:ativa:" + usuarioId), eq(AssinaturaAtivaResultado.class)))
                .thenReturn(Optional.empty());
        when(assinaturaRepositoryPort.buscarAtivaPorUsuario(usuarioId)).thenReturn(Optional.of(assinatura));

        AssinaturaAtivaResultado resultado = service.buscarAtiva(usuarioId);

        assertThat(resultado.assinaturaId()).isEqualTo(assinatura.getId());
        assertThat(resultado.usuarioId()).isEqualTo(usuarioId);
        assertThat(resultado.plano()).isEqualTo(Plano.PREMIUM);

        ArgumentCaptor<AssinaturaAtivaResultado> captor = ArgumentCaptor.forClass(AssinaturaAtivaResultado.class);
        verify(cachePort).salvar(eq("assinatura:ativa:" + usuarioId), captor.capture(), eq(Duration.ofMinutes(5)));
        assertThat(captor.getValue()).isEqualTo(resultado);
    }

    @Test
    void semAssinaturaAtivaLancaExcecaoSemPopularCache() {
        UUID usuarioId = UUID.randomUUID();
        when(cachePort.buscar(eq("assinatura:ativa:" + usuarioId), eq(AssinaturaAtivaResultado.class)))
                .thenReturn(Optional.empty());
        when(assinaturaRepositoryPort.buscarAtivaPorUsuario(usuarioId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscarAtiva(usuarioId))
                .isInstanceOf(AssinaturaNaoEncontradaException.class);

        verify(cachePort, never()).salvar(any(), any(), any());
    }
}
