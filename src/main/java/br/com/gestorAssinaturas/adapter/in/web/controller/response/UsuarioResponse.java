package br.com.gestorAssinaturas.adapter.in.web.controller.response;

import java.util.UUID;

public record UsuarioResponse(UUID id, String nome, String email) {
}
