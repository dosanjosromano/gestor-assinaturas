package br.com.gestorAssinaturas.application.service;


import br.com.gestorAssinaturas.application.port.in.useCase.ExcluirUsuarioUseCase;
import br.com.gestorAssinaturas.application.port.out.UsuarioRepositoryPort;
import br.com.gestorAssinaturas.domain.exception.UsuarioNaoEncontradoException;
import br.com.gestorAssinaturas.domain.model.Usuario;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class ExcluirUsuarioService implements ExcluirUsuarioUseCase {

    private final UsuarioRepositoryPort usuarioRepositoryPort;

    public ExcluirUsuarioService(UsuarioRepositoryPort usuarioRepositoryPort) {
        this.usuarioRepositoryPort = usuarioRepositoryPort;
    }

    @Override
    public void excluir(UUID id) {
        Usuario usuario = usuarioRepositoryPort.buscarPorId(id)
                .orElseThrow(() -> new UsuarioNaoEncontradoException("Usuário %s não encontrado".formatted(id)));
        usuario.excluir();
        usuarioRepositoryPort.salvar(usuario);
    }
}
