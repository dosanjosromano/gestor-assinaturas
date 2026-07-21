package br.com.gestorAssinaturas.adapter.in.web.controller.request;

import jakarta.validation.constraints.Email;

public record AtualizarUsuarioRequest(String nome, @Email String email) {
}
