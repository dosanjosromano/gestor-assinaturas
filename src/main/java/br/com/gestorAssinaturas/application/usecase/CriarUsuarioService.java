package br.com.gestorAssinaturas.application.usecase;

import br.com.gestorAssinaturas.application.port.in.CriarUsuarioCommand;
import br.com.gestorAssinaturas.application.port.in.CriarUsuarioUseCase;
import br.com.gestorAssinaturas.application.port.out.UsuarioRepositoryPort;
import br.com.gestorAssinaturas.domain.exception.EmailJaCadastradoException;
import br.com.gestorAssinaturas.domain.model.Usuario;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class CriarUsuarioService implements CriarUsuarioUseCase {

    private final UsuarioRepositoryPort usuarioRepositoryPort;

    public CriarUsuarioService(UsuarioRepositoryPort usuarioRepositoryPort) {
        this.usuarioRepositoryPort = usuarioRepositoryPort;
    }

    @Override
    public Usuario criar(CriarUsuarioCommand comando) {
        Usuario usuario = new Usuario(UUID.randomUUID(), comando.nome(), comando.email());
        try {
            return usuarioRepositoryPort.salvar(usuario);
        } catch (DataIntegrityViolationException ex) {
            throw new EmailJaCadastradoException("Email %s já cadastrado".formatted(comando.email()));
        }
    }
}
