package br.com.gestorAssinaturas.application.port.in;

import java.util.UUID;

public record AtualizarUsuarioCommand(UUID id, String nome, String email) {
}
