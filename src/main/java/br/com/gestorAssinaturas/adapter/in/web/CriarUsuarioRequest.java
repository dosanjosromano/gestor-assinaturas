package br.com.gestorAssinaturas.adapter.in.web;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CriarUsuarioRequest(
        @NotBlank String nome,
        @NotBlank @Email String email) {
}