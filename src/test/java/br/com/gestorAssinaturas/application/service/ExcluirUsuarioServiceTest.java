package br.com.gestorAssinaturas.application.service;

import br.com.gestorAssinaturas.application.port.out.UsuarioRepositoryPort;
import br.com.gestorAssinaturas.domain.exception.UsuarioNaoEncontradoException;
import br.com.gestorAssinaturas.domain.model.StatusUsuario;
import br.com.gestorAssinaturas.domain.model.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExcluirUsuarioServiceTest {

    @Mock
    private UsuarioRepositoryPort usuarioRepositoryPort;

    private ExcluirUsuarioService service;

    @BeforeEach
    void setUp() {
        service = new ExcluirUsuarioService(usuarioRepositoryPort);
    }

    @Test
    void excluiUsuarioAtivo() {
        Usuario usuario = new Usuario(UUID.randomUUID(), "Fulano", "fulano@teste.com");
        when(usuarioRepositoryPort.buscarPorId(usuario.getId())).thenReturn(Optional.of(usuario));

        service.excluir(usuario.getId());

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepositoryPort).salvar(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(StatusUsuario.INATIVO);
    }

    @Test
    void excluirUsuarioJaInativoEIdempotente() {
        Usuario usuario = new Usuario(UUID.randomUUID(), "Fulano", "fulano@teste.com");
        usuario.excluir();
        when(usuarioRepositoryPort.buscarPorId(usuario.getId())).thenReturn(Optional.of(usuario));

        service.excluir(usuario.getId());

        verify(usuarioRepositoryPort).salvar(usuario);
        assertThat(usuario.getStatus()).isEqualTo(StatusUsuario.INATIVO);
    }

    @Test
    void usuarioInexistenteLancaExcecao() {
        UUID id = UUID.randomUUID();
        when(usuarioRepositoryPort.buscarPorId(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.excluir(id))
                .isInstanceOf(UsuarioNaoEncontradoException.class);
    }
}
