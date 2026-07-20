package br.com.gestorAssinaturas.application.port.out;

import java.math.BigDecimal;

public interface GatewayPagamentoPort {

    ResultadoPagamento cobrar(BigDecimal valor, String idempotencyKey);
}