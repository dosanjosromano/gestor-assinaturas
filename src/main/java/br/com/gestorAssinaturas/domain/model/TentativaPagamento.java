package br.com.gestorAssinaturas.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import java.util.UUID;

@Entity
@Table(name = "tentativa_pagamento")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TentativaPagamento {

    @Id
    private UUID id;

    @Column(name = "assinatura_id", nullable = false)
    private UUID assinaturaId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoTentativa tipo;

    @Column(name = "tentativa_numero", nullable = false)
    private int tentativaNumero;

    @Column(name = "idempotency_key", nullable = false, unique = true)
    private String idempotencyKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusTentativa status;

    private TentativaPagamento(@NonNull UUID id, @NonNull UUID assinaturaId, @NonNull TipoTentativa tipo, int tentativaNumero) {
        this.id = id;
        this.assinaturaId = assinaturaId;
        this.tipo = tipo;
        this.tentativaNumero = tentativaNumero;
        this.idempotencyKey = assinaturaId + ":" + tipo + ":" + tentativaNumero;
        this.status = StatusTentativa.INICIADA;
    }

    public static TentativaPagamento iniciar(UUID id, UUID assinaturaId, TipoTentativa tipo, int tentativaNumero) {
        return new TentativaPagamento(id, assinaturaId, tipo, tentativaNumero);
    }
}
