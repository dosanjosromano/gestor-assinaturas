package br.com.gestorAssinaturas.adapter.in.messaging;


import br.com.gestorAssinaturas.application.port.in.useCase.ProcessarRenovacaoUseCase;
import br.com.gestorAssinaturas.application.port.out.RenovacaoSolicitadaEvent;
import br.com.gestorAssinaturas.config.KafkaConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile("worker")
public class RenovacaoEventConsumer {

    private final ProcessarRenovacaoUseCase processarRenovacaoUseCase;

    public RenovacaoEventConsumer(ProcessarRenovacaoUseCase processarRenovacaoUseCase) {
        this.processarRenovacaoUseCase = processarRenovacaoUseCase;
    }


    @RetryableTopic(
            attempts = "4",
            backoff = @Backoff(delay = 30_000, multiplier = 2.0, maxDelay = 480_000),
            retryTopicSuffix = "-retry",
            dltTopicSuffix = "-dlq",
            autoCreateTopics = "true")
    @KafkaListener(topics = KafkaConfig.TOPICO_RENOVACAO_SOLICITADA, groupId = "${spring.kafka.consumer.group-id}")
    public void ouvir(RenovacaoSolicitadaEvent evento, Acknowledgment acknowledgment) {
        log.info("[recebido] evento de renovação - assinaturaId={}", evento.assinaturaId());
        processarRenovacaoUseCase.processar(evento.assinaturaId());
        acknowledgment.acknowledge();
    }

    @DltHandler
    public void tratarDlq(RenovacaoSolicitadaEvent evento, Acknowledgment acknowledgment) {
        log.error("[dlq] renovação da assinatura {} esgotou as tentativas técnicas - tentativa permanece "
                        + "INDETERMINADA até reconciliação futura (fora do escopo desta mudança)",
                evento.assinaturaId());
        acknowledgment.acknowledge();
    }
}
