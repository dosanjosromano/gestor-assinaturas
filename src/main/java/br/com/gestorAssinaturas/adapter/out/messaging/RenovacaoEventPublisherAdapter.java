package br.com.gestorAssinaturas.adapter.out.messaging;

import br.com.gestorAssinaturas.application.port.out.RenovacaoEventPublisherPort;
import br.com.gestorAssinaturas.application.port.out.RenovacaoSolicitadaEvent;
import br.com.gestorAssinaturas.config.KafkaConfig;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class RenovacaoEventPublisherAdapter implements RenovacaoEventPublisherPort {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public RenovacaoEventPublisherAdapter(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public void publicar(RenovacaoSolicitadaEvent evento) {
        kafkaTemplate.send(KafkaConfig.TOPICO_RENOVACAO_SOLICITADA, evento.assinaturaId().toString(), evento);
    }
}
