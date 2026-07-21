package br.com.gestorAssinaturas.application.service;

import br.com.gestorAssinaturas.application.port.in.AtualizarUsuarioCommand;

import br.com.gestorAssinaturas.application.port.in.useCase.AtualizarUsuarioUseCase;
import br.com.gestorAssinaturas.application.port.out.UsuarioRepositoryPort;
import br.com.gestorAssinaturas.domain.exception.EmailJaCadastradoException;
import br.com.gestorAssinaturas.domain.exception.UsuarioNaoEncontradoException;
import br.com.gestorAssinaturas.domain.model.StatusUsuario;
import br.com.gestorAssinaturas.domain.model.Usuario;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class AtualizarUsuarioService implements AtualizarUsuarioUseCase {

    private final UsuarioRepositoryPort usuarioRepositoryPort;

    public AtualizarUsuarioService(UsuarioRepositoryPort usuarioRepositoryPort) {
        this.usuarioRepositoryPort = usuarioRepositoryPort;
    }

    @Override
    public Usuario atualizar(AtualizarUsuarioCommand comando) {
        Usuario usuario = buscarAtivo(comando.id());
        usuario.atualizarDados(comando.nome(), comando.email());
        try {
            return usuarioRepositoryPort.salvar(usuario);
        } catch (DataIntegrityViolationException ex) {
            throw new EmailJaCadastradoException("Email %s já cadastrado".formatted(comando.email()));
        }
    }

    private Usuario buscarAtivo(UUID id) {
        Usuario usuario = usuarioRepositoryPort.buscarPorId(id)
                .orElseThrow(() -> new UsuarioNaoEncontradoException("Usuário %s não encontrado".formatted(id)));
        if (usuario.getStatus() != StatusUsuario.ATIVO) {
            throw new UsuarioNaoEncontradoException("Usuário %s não encontrado".formatted(id));
        }
        return usuario;
    }
}
