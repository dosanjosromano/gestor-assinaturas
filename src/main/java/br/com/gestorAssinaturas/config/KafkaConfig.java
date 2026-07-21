package br.com.gestorAssinaturas.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.Map;

@Configuration
public class KafkaConfig {

    public static final String TOPICO_RENOVACAO_SOLICITADA = "renovacao-solicitada";

    @Bean
    public NewTopic topicoRenovacaoSolicitada() {
        return TopicBuilder.name(TOPICO_RENOVACAO_SOLICITADA)
                .partitions(6)
                .replicas(1)
                .build();
    }

    @Bean
    public ProducerFactory<String, Object> renovacaoProducerFactory(KafkaProperties kafkaProperties) {
        Map<String, Object> propriedades = kafkaProperties.buildProducerProperties(null);
        propriedades.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        propriedades.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        return new DefaultKafkaProducerFactory<>(propriedades);
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate(ProducerFactory<String, Object> renovacaoProducerFactory) {
        return new KafkaTemplate<>(renovacaoProducerFactory);
    }
}
