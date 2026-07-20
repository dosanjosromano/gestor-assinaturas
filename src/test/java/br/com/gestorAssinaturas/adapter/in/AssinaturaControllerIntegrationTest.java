package br.com.gestorAssinaturas.adapter.in;

import br.com.gestorAssinaturas.adapter.in.web.controller.request.CriarAssinaturaRequest;
import br.com.gestorAssinaturas.adapter.in.web.controller.response.AssinaturaResponse;
import br.com.gestorAssinaturas.domain.model.Plano;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AssinaturaControllerIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("app.gateway-pagamento.mock.taxa-aprovacao", () -> "1.0");
        registry.add("app.gateway-pagamento.mock.taxa-recusa", () -> "0.0");
    }

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void segundaCriacaoParaMesmoUsuarioRetorna409() {
        UUID usuarioId = UUID.randomUUID();
        CriarAssinaturaRequest request = new CriarAssinaturaRequest(usuarioId, Plano.BASICO);

        ResponseEntity<AssinaturaResponse> primeira =
                restTemplate.postForEntity("/assinaturas", request, AssinaturaResponse.class);
        assertThat(primeira.getStatusCode().value()).isEqualTo(201);

        ResponseEntity<String> segunda = restTemplate.postForEntity("/assinaturas", request, String.class);
        assertThat(segunda.getStatusCode().value()).isEqualTo(409);
    }

    @Test
    void chamadasConcorrentesParaMesmoUsuarioResultamEmApenasUmaCriacaoBemSucedida() throws InterruptedException {
        UUID usuarioId = UUID.randomUUID();
        CriarAssinaturaRequest request = new CriarAssinaturaRequest(usuarioId, Plano.BASICO);

        ExecutorService executor = Executors.newFixedThreadPool(5);
        try {
            List<CompletableFuture<Integer>> chamadas = IntStream.range(0, 5)
                    .mapToObj(i -> CompletableFuture.supplyAsync(
                            () -> restTemplate.postForEntity("/assinaturas", request, String.class)
                                    .getStatusCode()
                                    .value(),
                            executor))
                    .toList();

            List<Integer> resultados = chamadas.stream().map(CompletableFuture::join).toList();

            long sucesso = resultados.stream().filter(status -> status == 201).count();
            long conflitos = resultados.stream().filter(status -> status == 409).count();

            assertThat(sucesso).isEqualTo(1);
            assertThat(conflitos).isEqualTo(4);
        } finally {
            executor.shutdown();
        }
    }
}
