package br.com.gestorAssinaturas.application.port.in;

import br.com.gestorAssinaturas.domain.model.Usuario;

public interface CriarUsuarioUseCase {

    Usuario criar(CriarUsuarioCommand comando);
}