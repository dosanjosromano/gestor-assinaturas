package br.com.gestorAssinaturas.application.service;

import br.com.gestorAssinaturas.application.port.in.BuscaUsuariosUseCase;
import br.com.gestorAssinaturas.application.port.out.UsuarioRepositoryPort;
import br.com.gestorAssinaturas.domain.exception.UsuarioNaoEncontradoException;
import br.com.gestorAssinaturas.domain.model.Usuario;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class BuscaUsuariosService implements BuscaUsuariosUseCase {

    private final UsuarioRepositoryPort usuarioRepositoryPort;

    public BuscaUsuariosService(UsuarioRepositoryPort usuarioRepositoryPort) {
        this.usuarioRepositoryPort = usuarioRepositoryPort;
    }

    @Override
    public List<Usuario> listarTodos() {
        return usuarioRepositoryPort.listarTodos();
    }

    @Override
    public Usuario buscarPorId(UUID id) {
        return usuarioRepositoryPort.buscarPorId(id)
                .orElseThrow(() -> new UsuarioNaoEncontradoException("Usuário %s não encontrado".formatted(id)));
    }
}
