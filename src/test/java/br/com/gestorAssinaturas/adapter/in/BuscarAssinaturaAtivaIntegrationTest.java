package br.com.gestorAssinaturas.adapter.in;

import br.com.gestorAssinaturas.adapter.in.web.controller.request.CriarAssinaturaRequest;
import br.com.gestorAssinaturas.adapter.in.web.controller.request.CriarUsuarioRequest;
import br.com.gestorAssinaturas.adapter.in.web.controller.response.AssinaturaResponse;
import br.com.gestorAssinaturas.adapter.in.web.controller.response.UsuarioResponse;

import br.com.gestorAssinaturas.adapter.out.persistencia.AssinaturaJpaRepository;
import br.com.gestorAssinaturas.domain.model.Assinatura;
import br.com.gestorAssinaturas.domain.model.Plano;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class BuscarAssinaturaAtivaIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Container
    static final GenericContainer<?> REDIS =
            new GenericContainer<>(DockerImageName.parse("redis:7-alpine")).withExposedPorts(6379);

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
        registry.add("app.gateway-pagamento.mock.taxa-aprovacao", () -> "1.0");
        registry.add("app.gateway-pagamento.mock.taxa-recusa", () -> "0.0");
        registry.add("app.cache.assinatura-ativa.ttl", () -> "1s");
        registry.add("spring.data.redis.timeout", () -> "1s");
    }

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private AssinaturaJpaRepository assinaturaJpaRepository;

    private UUID criarUsuario() {
        CriarUsuarioRequest request = new CriarUsuarioRequest("Fulano de Tal", "fulano-" + UUID.randomUUID() + "@teste.com");
        ResponseEntity<UsuarioResponse> resposta =
                restTemplate.postForEntity("/usuarios", request, UsuarioResponse.class);
        return resposta.getBody().id();
    }

    @Test
    void ttlExpiraEConsultaSeguinteVoltaAoBanco() throws InterruptedException {
        UUID usuarioId = criarUsuario();

        restTemplate.postForEntity(
                "/assinaturas", new CriarAssinaturaRequest(usuarioId, Plano.BASICO), AssinaturaResponse.class);

        AssinaturaResponse primeiraConsulta = restTemplate.getForObject(
                "/assinaturas/usuario/{usuarioId}", AssinaturaResponse.class, usuarioId);
        LocalDate dataExpiracaoOriginal = primeiraConsulta.dataExpiracao();

        // Avança o ciclo diretamente no banco, sem passar pelo CachePort — simula uma
        // mudança de estado que ainda não invalida o cache (não é escopo desta mudança).
        Assinatura assinatura = assinaturaJpaRepository.findById(primeiraConsulta.id()).orElseThrow();
        assinatura.renovarCiclo();
        assinaturaJpaRepository.save(assinatura);
        LocalDate dataExpiracaoRenovada = dataExpiracaoOriginal.plusMonths(1);

        Thread.sleep(1100);

        AssinaturaResponse segundaConsulta = restTemplate.getForObject(
                "/assinaturas/usuario/{usuarioId}", AssinaturaResponse.class, usuarioId);

        assertThat(segundaConsulta.dataExpiracao()).isEqualTo(dataExpiracaoRenovada);
    }

    @Test
    void redisIndisponivelDuranteConsultaCaiParaOBanco() {
        UUID usuarioId = criarUsuario();

        ResponseEntity<AssinaturaResponse> assinaturaCriada = restTemplate.postForEntity(
                "/assinaturas", new CriarAssinaturaRequest(usuarioId, Plano.BASICO), AssinaturaResponse.class);

        REDIS.getDockerClient().pauseContainerCmd(REDIS.getContainerId()).exec();
        try {
            ResponseEntity<AssinaturaResponse> resposta = restTemplate.getForEntity(
                    "/assinaturas/usuario/{usuarioId}", AssinaturaResponse.class, usuarioId);

            assertThat(resposta.getStatusCode().is2xxSuccessful()).isTrue();
            assertThat(resposta.getBody().id()).isEqualTo(assinaturaCriada.getBody().id());
        } finally {
            REDIS.getDockerClient().unpauseContainerCmd(REDIS.getContainerId()).exec();
        }
    }

    @Test
    void cancelamentoInvalidaCacheImediatamente() {
        UUID usuarioId = criarUsuario();

        ResponseEntity<AssinaturaResponse> assinaturaCriada = restTemplate.postForEntity(
                "/assinaturas", new CriarAssinaturaRequest(usuarioId, Plano.BASICO), AssinaturaResponse.class);
        UUID assinaturaId = assinaturaCriada.getBody().id();

        // Popula o cache com o status ATIVA
        ResponseEntity<AssinaturaResponse> consultaAntesDoCancelamento = restTemplate.getForEntity(
                "/assinaturas/usuario/{usuarioId}", AssinaturaResponse.class, usuarioId);
        assertThat(consultaAntesDoCancelamento.getBody().status()).isEqualTo("ATIVA");

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Usuario-Id", usuarioId.toString());
        ResponseEntity<AssinaturaResponse> cancelamento = restTemplate.exchange(
                "/assinaturas/{id}/cancelamento",
                HttpMethod.DELETE,
                new HttpEntity<>(headers),
                AssinaturaResponse.class,
                assinaturaId);
        assertThat(cancelamento.getStatusCode().is2xxSuccessful()).isTrue();
        ResponseEntity<String> consultaPosCancelamento = restTemplate.getForEntity(
                "/assinaturas/usuario/{usuarioId}", String.class, usuarioId);

        assertThat(consultaPosCancelamento.getStatusCode().value()).isEqualTo(404);
    }
}
