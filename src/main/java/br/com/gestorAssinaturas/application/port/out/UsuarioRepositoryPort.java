package br.com.gestorAssinaturas.application.port.out;

import br.com.gestorAssinaturas.domain.model.Usuario;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UsuarioRepositoryPort {

    Usuario salvar(Usuario usuario);

    List<Usuario> listarTodos();

    Optional<Usuario> buscarPorId(UUID id);
}