package br.com.gestorAssinaturas.adapter.in;

import br.com.gestorAssinaturas.adapter.in.web.GlobalExceptionHandler;
import br.com.gestorAssinaturas.domain.exception.FalhaTecnicaPagamentoException;
import br.com.gestorAssinaturas.domain.exception.PagamentoRecusadoException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void falhaTecnicaPagamentoRetorna503() {
        ProblemDetail problemDetail = handler.tratarFalhaTecnicaPagamento(
                new FalhaTecnicaPagamentoException("falha técnica do gateway"));

        assertThat(problemDetail.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE.value());
        assertThat(problemDetail.getDetail()).isEqualTo("falha técnica do gateway");
    }

    @Test
    void pagamentoRecusadoRetorna402() {
        ProblemDetail problemDetail = handler.tratarPagamentoRecusado(
                new PagamentoRecusadoException("pagamento recusado"));

        assertThat(problemDetail.getStatus()).isEqualTo(HttpStatus.PAYMENT_REQUIRED.value());
        assertThat(problemDetail.getDetail()).isEqualTo("pagamento recusado");
    }
}
