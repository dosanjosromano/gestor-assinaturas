package br.com.gestorAssinaturas.application.port.in.useCase;

import br.com.gestorAssinaturas.application.port.in.AtualizarUsuarioCommand;
import br.com.gestorAssinaturas.domain.model.Usuario;

public interface AtualizarUsuarioUseCase {

    Usuario atualizar(AtualizarUsuarioCommand comando);
}