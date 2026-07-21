package br.com.gestorAssinaturas.domain.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "usuario")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Usuario {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false, unique = true)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusUsuario status;

    @Column(name = "desativado_em")
    private LocalDateTime desativadoEm;

    public Usuario(@NonNull UUID id, @NonNull String nome, @NonNull String email) {
        this.id = id;
        this.nome = nome;
        this.email = email;
        this.status = StatusUsuario.ATIVO;
    }

    public void atualizarDados(String nome, String email) {
        if (nome != null) {
            this.nome = nome;
        }
        if (email != null) {
            this.email = email;
        }
    }

    public void excluir() {
        if (this.status == StatusUsuario.INATIVO) {
            return;
        }
        this.status = StatusUsuario.INATIVO;
        this.desativadoEm = LocalDateTime.now();
    }
}
