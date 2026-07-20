package br.com.gestorAssinaturas.domain.model;

import br.com.gestorAssinaturas.domain.exception.EstadoInvalidoException;
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

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "assinatura")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Assinatura {

    private static final int MAX_FALHAS_RENOVACAO_CONSECUTIVAS = 3;

    @Id
    private UUID id;

    @Column(name = "usuario_id", nullable = false)
    private UUID usuarioId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Plano plano;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StatusAssinatura status;

    @Column(name = "data_inicio")
    private LocalDate dataInicio;

    @Column(name = "data_expiracao")
    private LocalDate dataExpiracao;

    @Column(name = "data_cancelamento")
    private LocalDate dataCancelamento;

    @Column(name = "falhas_renovacao_consecutivas", nullable = false)
    private int falhasRenovacaoConsecutivas;

    public Assinatura(@NonNull UUID id, @NonNull UUID usuarioId, @NonNull Plano plano) {
        this.id = id;
        this.usuarioId = usuarioId;
        this.plano = plano;
        this.status = StatusAssinatura.AGUARDANDO_PAGAMENTO;
    }

    public void confirmarPagamento() {
        exigirStatus(StatusAssinatura.AGUARDANDO_PAGAMENTO);
        LocalDate hoje = LocalDate.now();
        this.dataInicio = hoje;
        this.dataExpiracao = hoje.plusMonths(1);
        this.status = StatusAssinatura.ATIVA;
    }

    public void marcarFalhaPagamentoInicial() {
        exigirStatus(StatusAssinatura.AGUARDANDO_PAGAMENTO);
        this.status = StatusAssinatura.FALHA_PAGAMENTO;
    }

    public void renovarCiclo() {
        exigirStatus(StatusAssinatura.ATIVA);
        this.dataExpiracao = this.dataExpiracao.plusMonths(1);
        this.falhasRenovacaoConsecutivas = 0;
    }

    public void registrarFalhaRenovacao() {
        exigirStatus(StatusAssinatura.ATIVA);
        this.falhasRenovacaoConsecutivas++;
        if (this.falhasRenovacaoConsecutivas >= MAX_FALHAS_RENOVACAO_CONSECUTIVAS) {
            this.status = StatusAssinatura.SUSPENSA;
        }
    }

    public boolean podeSerCancelada() {
        return this.status == StatusAssinatura.ATIVA;
    }

    public void cancelar() {
        if (!podeSerCancelada()) {
            throw new EstadoInvalidoException(
                    "Assinatura %s não pode ser cancelada a partir do status %s".formatted(id, status));
        }
        this.status = StatusAssinatura.CANCELADA;
        this.dataCancelamento = LocalDate.now();
    }

    private void exigirStatus(StatusAssinatura esperado) {
        if (this.status != esperado) {
            throw new EstadoInvalidoException(
                    "Assinatura %s esperava status %s mas está em %s".formatted(id, esperado, status));
        }
    }
}
