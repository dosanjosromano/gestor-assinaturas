package br.com.gestorAssinaturas.renovacao;

import br.com.gestorAssinaturas.application.port.out.AssinaturaRepositoryPort;
import br.com.gestorAssinaturas.application.port.out.RenovacaoEventPublisherPort;
import br.com.gestorAssinaturas.application.port.out.RenovacaoSolicitadaEvent;
import br.com.gestorAssinaturas.config.KafkaConfig;
import br.com.gestorAssinaturas.domain.model.Assinatura;
import br.com.gestorAssinaturas.domain.model.Plano;
import br.com.gestorAssinaturas.domain.model.StatusAssinatura;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@ActiveProfiles("worker")
@EmbeddedKafka(partitions = 1, topics = {KafkaConfig.TOPICO_RENOVACAO_SOLICITADA}, brokerProperties = {
        "listeners=PLAINTEXT://localhost:0", "port=0"
})
@SpringBootTest
class RenovacaoKafkaIntegrationTest {

    private static final String GRUPO_CONSUMIDOR = "assinaturas-worker";

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("app.gateway-pagamento.mock.taxa-aprovacao", () -> "1.0");
        registry.add("app.gateway-pagamento.mock.taxa-recusa", () -> "0.0");
        registry.add("spring.kafka.consumer.group-id", () -> GRUPO_CONSUMIDOR);
    }

    @Autowired
    private RenovacaoEventPublisherPort renovacaoEventPublisherPort;

    @Autowired
    private AssinaturaRepositoryPort assinaturaRepositoryPort;

    @org.springframework.beans.factory.annotation.Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Test
    void eventoPublicadoEConsumidoRenovaAssinaturaEAvancaOffsetSoAposSucesso() throws Exception {
        Assinatura assinatura = new Assinatura(UUID.randomUUID(), UUID.randomUUID(), Plano.BASICO);
        assinatura.confirmarPagamento();
        assinaturaRepositoryPort.salvar(assinatura);

        renovacaoEventPublisherPort.publicar(new RenovacaoSolicitadaEvent(assinatura.getId()));

        Assinatura renovada = aguardarRenovacao(assinatura.getId());
        assertThat(renovada.getStatus()).isEqualTo(StatusAssinatura.ATIVA);
        assertThat(renovada.getDataExpiracao()).isEqualTo(LocalDate.now().plusMonths(2));
        assertThat(renovada.getFalhasRenovacaoConsecutivas()).isZero();

        long offsetComprometido = offsetComprometido();
        assertThat(offsetComprometido).isEqualTo(1L);
    }

    private Assinatura aguardarRenovacao(UUID assinaturaId) throws InterruptedException {
        for (int tentativa = 0; tentativa < 4; tentativa++) {
            Optional<Assinatura> atual = assinaturaRepositoryPort.buscarPorId(assinaturaId);
            if (atual.isPresent() && atual.get().getFalhasRenovacaoConsecutivas() == 0
                    && atual.get().getDataExpiracao().isAfter(LocalDate.now().plusMonths(1))) {
                return atual.get();
            }
            Thread.sleep(200);
        }
        throw new IllegalStateException("Assinatura não foi renovada a tempo pelo consumer");
    }

    private long offsetComprometido() {
        Map<String, Object> configuracao = Map.of(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        try (AdminClient adminClient = AdminClient.create(configuracao)) {
            TopicPartition particao = new TopicPartition(KafkaConfig.TOPICO_RENOVACAO_SOLICITADA, 0);
            var offsets = adminClient.listConsumerGroupOffsets(GRUPO_CONSUMIDOR)
                    .partitionsToOffsetAndMetadata()
                    .get(10, java.util.concurrent.TimeUnit.SECONDS);
            return offsets.getOrDefault(particao, null) != null
                    ? offsets.get(particao).offset()
                    : -1L;
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
