package br.com.gestorAssinaturas.adapter.in.web;

import java.util.UUID;

public record UsuarioResponse(UUID id, String nome, String email) {
}
