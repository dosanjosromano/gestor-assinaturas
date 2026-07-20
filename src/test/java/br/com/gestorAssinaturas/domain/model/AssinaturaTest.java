package br.com.gestorAssinaturas.domain.model;

import br.com.gestorAssinaturas.domain.exception.EstadoInvalidoException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AssinaturaTest {

    private Assinatura novaAssinatura() {
        return new Assinatura(UUID.randomUUID(), UUID.randomUUID(), Plano.BASICO);
    }

    @Test
    void confirmarPagamentoAtivaAssinaturaComUmMesDeExpiracao() {
        Assinatura assinatura = novaAssinatura();

        assinatura.confirmarPagamento();

        assertThat(assinatura.getStatus()).isEqualTo(StatusAssinatura.ATIVA);
        assertThat(assinatura.getDataInicio()).isEqualTo(LocalDate.now());
        assertThat(assinatura.getDataExpiracao()).isEqualTo(LocalDate.now().plusMonths(1));
    }

    @Test
    void marcarFalhaPagamentoInicialMoveParaFalhaPagamento() {
        Assinatura assinatura = novaAssinatura();

        assinatura.marcarFalhaPagamentoInicial();

        assertThat(assinatura.getStatus()).isEqualTo(StatusAssinatura.FALHA_PAGAMENTO);
    }

    @Test
    void confirmarPagamentoForaDeAguardandoPagamentoLancaExcecao() {
        Assinatura assinatura = novaAssinatura();
        assinatura.confirmarPagamento();

        assertThatThrownBy(assinatura::confirmarPagamento)
                .isInstanceOf(EstadoInvalidoException.class);
    }

    @Test
    void renovarCicloAvancaExpiracaoEZeraFalhas() {
        Assinatura assinatura = novaAssinatura();
        assinatura.confirmarPagamento();
        LocalDate expiracaoAntes = assinatura.getDataExpiracao();
        assinatura.registrarFalhaRenovacao();

        assinatura.renovarCiclo();

        assertThat(assinatura.getStatus()).isEqualTo(StatusAssinatura.ATIVA);
        assertThat(assinatura.getDataExpiracao()).isEqualTo(expiracaoAntes.plusMonths(1));
        assertThat(assinatura.getFalhasRenovacaoConsecutivas()).isZero();
    }

    @Test
    void tresFalhasConsecutivasDeRenovacaoSuspendemAssinatura() {
        Assinatura assinatura = novaAssinatura();
        assinatura.confirmarPagamento();

        assinatura.registrarFalhaRenovacao();
        assinatura.registrarFalhaRenovacao();
        assertThat(assinatura.getStatus()).isEqualTo(StatusAssinatura.ATIVA);

        assinatura.registrarFalhaRenovacao();

        assertThat(assinatura.getStatus()).isEqualTo(StatusAssinatura.SUSPENSA);
    }

    @Test
    void cancelamentoValidoAPartirDeAtivaMantemDataExpiracao() {
        Assinatura assinatura = novaAssinatura();
        assinatura.confirmarPagamento();
        LocalDate expiracaoAntes = assinatura.getDataExpiracao();

        assinatura.cancelar();

        assertThat(assinatura.getStatus()).isEqualTo(StatusAssinatura.CANCELADA);
        assertThat(assinatura.getDataCancelamento()).isEqualTo(LocalDate.now());
        assertThat(assinatura.getDataExpiracao()).isEqualTo(expiracaoAntes);
    }

    @Test
    void transicaoInvalidaDeCancelamentoNaoAlteraNenhumCampo() {
        Assinatura assinatura = novaAssinatura();
        assinatura.confirmarPagamento();
        assinatura.registrarFalhaRenovacao();
        assinatura.registrarFalhaRenovacao();
        assinatura.registrarFalhaRenovacao();
        assertThat(assinatura.getStatus()).isEqualTo(StatusAssinatura.SUSPENSA);
        LocalDate expiracaoAntes = assinatura.getDataExpiracao();

        assertThatThrownBy(assinatura::cancelar)
                .isInstanceOf(EstadoInvalidoException.class);

        assertThat(assinatura.getStatus()).isEqualTo(StatusAssinatura.SUSPENSA);
        assertThat(assinatura.getDataCancelamento()).isNull();
        assertThat(assinatura.getDataExpiracao()).isEqualTo(expiracaoAntes);
    }

    @Test
    void podeSerCanceladaSoRetornaVerdadeiroQuandoAtiva() {
        Assinatura assinatura = novaAssinatura();
        assertThat(assinatura.podeSerCancelada()).isFalse();

        assinatura.confirmarPagamento();
        assertThat(assinatura.podeSerCancelada()).isTrue();

        assinatura.cancelar();
        assertThat(assinatura.podeSerCancelada()).isFalse();
    }
}
