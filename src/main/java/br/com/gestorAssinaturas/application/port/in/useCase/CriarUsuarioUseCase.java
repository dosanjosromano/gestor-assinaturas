package br.com.gestorAssinaturas.application.port.in.useCase;

import br.com.gestorAssinaturas.application.port.in.CriarUsuarioCommand;
import br.com.gestorAssinaturas.domain.model.Usuario;

public interface CriarUsuarioUseCase {

    Usuario criar(CriarUsuarioCommand comando);
}