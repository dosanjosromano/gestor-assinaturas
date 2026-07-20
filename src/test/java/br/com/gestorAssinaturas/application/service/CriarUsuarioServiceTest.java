package br.com.gestorAssinaturas.application.service;

import br.com.gestorAssinaturas.application.port.in.CriarUsuarioCommand;
import br.com.gestorAssinaturas.application.port.out.UsuarioRepositoryPort;
import br.com.gestorAssinaturas.domain.exception.EmailJaCadastradoException;
import br.com.gestorAssinaturas.domain.model.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CriarUsuarioServiceTest {

    @Mock
    private UsuarioRepositoryPort usuarioRepositoryPort;

    private CriarUsuarioService service;

    @BeforeEach
    void setUp() {
        service = new CriarUsuarioService(usuarioRepositoryPort);
    }

    @Test
    void criaUsuarioComSucesso() {
        when(usuarioRepositoryPort.salvar(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Usuario usuario = service.criar(new CriarUsuarioCommand("Fulano", "fulano@teste.com"));

        assertThat(usuario.getId()).isNotNull();
        assertThat(usuario.getNome()).isEqualTo("Fulano");
        assertThat(usuario.getEmail()).isEqualTo("fulano@teste.com");
    }

    @Test
    void emailDuplicadoLancaExcecao() {
        when(usuarioRepositoryPort.salvar(any())).thenThrow(new DataIntegrityViolationException("email duplicado"));

        assertThatThrownBy(() -> service.criar(new CriarUsuarioCommand("Fulano", "fulano@teste.com")))
                .isInstanceOf(EmailJaCadastradoException.class);
    }
}
