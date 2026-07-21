package br.com.gestorAssinaturas.application.port.in.useCase;

import br.com.gestorAssinaturas.domain.model.Usuario;

import java.util.List;
import java.util.UUID;

public interface BuscaUsuariosUseCase {

    List<Usuario> listarTodos();

    Usuario buscarPorId(UUID id);
}
