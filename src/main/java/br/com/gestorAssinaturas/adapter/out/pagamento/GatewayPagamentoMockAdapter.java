package br.com.gestorAssinaturas.adapter.out.pagamento;

import br.com.gestorAssinaturas.application.port.out.GatewayPagamentoPort;
import br.com.gestorAssinaturas.application.port.out.ResultadoPagamento;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class GatewayPagamentoMockAdapter implements GatewayPagamentoPort {

    private final double taxaAprovacao;
    private final double taxaRecusa;

    public GatewayPagamentoMockAdapter(
            @Value("${app.gateway-pagamento.mock.taxa-aprovacao:0.8}") double taxaAprovacao,
            @Value("${app.gateway-pagamento.mock.taxa-recusa:0.15}") double taxaRecusa) {
        this.taxaAprovacao = taxaAprovacao;
        this.taxaRecusa = taxaRecusa;
    }

    @Override
    public ResultadoPagamento cobrar(BigDecimal valor, String idempotencyKey) {
        double sorteio = ThreadLocalRandom.current().nextDouble();
        if (sorteio < taxaAprovacao) {
            return ResultadoPagamento.APROVADO;
        }
        if (sorteio < taxaAprovacao + taxaRecusa) {
            return ResultadoPagamento.RECUSADO;
        }
        return ResultadoPagamento.ERRO_TECNICO;
    }
}
