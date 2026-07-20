package br.com.gestorAssinaturas.application.port.in.useCase;

import br.com.gestorAssinaturas.application.port.in.AssinaturaResultado;
import br.com.gestorAssinaturas.application.port.in.CriarAssinaturaCommand;

public interface CriarAssinaturaUseCase {

    AssinaturaResultado criar(CriarAssinaturaCommand comando);
}