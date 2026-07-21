package br.com.gestorAssinaturas.application.service;

import br.com.gestorAssinaturas.application.port.in.AtualizarUsuarioCommand;
import br.com.gestorAssinaturas.application.port.out.UsuarioRepositoryPort;
import br.com.gestorAssinaturas.domain.exception.EmailJaCadastradoException;
import br.com.gestorAssinaturas.domain.exception.UsuarioNaoEncontradoException;
import br.com.gestorAssinaturas.domain.model.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AtualizarUsuarioServiceTest {

    @Mock
    private UsuarioRepositoryPort usuarioRepositoryPort;

    private AtualizarUsuarioService service;

    @BeforeEach
    void setUp() {
        service = new AtualizarUsuarioService(usuarioRepositoryPort);
    }

    private Usuario usuarioAtivo() {
        return new Usuario(UUID.randomUUID(), "Fulano", "fulano@teste.com");
    }

    @Test
    void atualizaNomeEEmailJuntos() {
        Usuario usuario = usuarioAtivo();
        when(usuarioRepositoryPort.buscarPorId(usuario.getId())).thenReturn(Optional.of(usuario));
        when(usuarioRepositoryPort.salvar(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Usuario resultado = service.atualizar(new AtualizarUsuarioCommand(usuario.getId(), "Novo Nome", "novo@teste.com"));

        assertThat(resultado.getNome()).isEqualTo("Novo Nome");
        assertThat(resultado.getEmail()).isEqualTo("novo@teste.com");
    }

    @Test
    void atualizaSomenteUmDosCampos() {
        Usuario usuario = usuarioAtivo();
        when(usuarioRepositoryPort.buscarPorId(usuario.getId())).thenReturn(Optional.of(usuario));
        when(usuarioRepositoryPort.salvar(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Usuario resultado = service.atualizar(new AtualizarUsuarioCommand(usuario.getId(), "Novo Nome", null));

        assertThat(resultado.getNome()).isEqualTo("Novo Nome");
        assertThat(resultado.getEmail()).isEqualTo("fulano@teste.com");
    }

    @Test
    void emailDuplicadoLancaExcecao() {
        Usuario usuario = usuarioAtivo();
        when(usuarioRepositoryPort.buscarPorId(usuario.getId())).thenReturn(Optional.of(usuario));
        when(usuarioRepositoryPort.salvar(any())).thenThrow(new DataIntegrityViolationException("email duplicado"));

        assertThatThrownBy(() -> service.atualizar(new AtualizarUsuarioCommand(usuario.getId(), null, "outro@teste.com")))
                .isInstanceOf(EmailJaCadastradoException.class);
    }

    @Test
    void usuarioInativoNaoEEncontradoParaAtualizacao() {
        Usuario usuario = usuarioAtivo();
        usuario.excluir();
        when(usuarioRepositoryPort.buscarPorId(usuario.getId())).thenReturn(Optional.of(usuario));

        assertThatThrownBy(() -> service.atualizar(new AtualizarUsuarioCommand(usuario.getId(), "Novo Nome", null)))
                .isInstanceOf(UsuarioNaoEncontradoException.class);
    }

    @Test
    void usuarioInexistenteLancaExcecao() {
        UUID id = UUID.randomUUID();
        when(usuarioRepositoryPort.buscarPorId(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.atualizar(new AtualizarUsuarioCommand(id, "Novo Nome", null)))
                .isInstanceOf(UsuarioNaoEncontradoException.class);
    }
}
