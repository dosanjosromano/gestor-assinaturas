package br.com.gestorAssinaturas.domain.model;

import lombok.Getter;

import java.math.BigDecimal;

@Getter
public enum Plano {

    BASICO(new BigDecimal("19.90")),
    PREMIUM(new BigDecimal("39.90")),
    FAMILIA(new BigDecimal("59.90"));

    private final BigDecimal preco;

    Plano(BigDecimal preco) {
        this.preco = preco;
    }
}
