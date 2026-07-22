package br.com.gestorAssinaturas.adapter.in;

import br.com.gestorAssinaturas.adapter.in.web.controller.request.CriarAssinaturaRequest;
import br.com.gestorAssinaturas.adapter.in.web.controller.request.CriarUsuarioRequest;
import br.com.gestorAssinaturas.adapter.in.web.controller.response.UsuarioResponse;
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

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AssinaturaControllerFalhaTecnicaIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("app.gateway-pagamento.mock.taxa-aprovacao", () -> "0.0");
        registry.add("app.gateway-pagamento.mock.taxa-recusa", () -> "0.0");
    }

    @Autowired
    private TestRestTemplate restTemplate;

    private UUID criarUsuario() {
        CriarUsuarioRequest request = new CriarUsuarioRequest("Fulano de Tal", "fulano-" + UUID.randomUUID() + "@teste.com");
        ResponseEntity<UsuarioResponse> resposta =
                restTemplate.postForEntity("/usuarios", request, UsuarioResponse.class);
        return resposta.getBody().id();
    }

    @Test
    void erroTecnicoRetorna503EPermiteNovaTentativa() {
        UUID usuarioId = criarUsuario();
        CriarAssinaturaRequest request = new CriarAssinaturaRequest(usuarioId, Plano.BASICO);

        ResponseEntity<String> primeira = restTemplate.postForEntity("/assinaturas", request, String.class);
        assertThat(primeira.getStatusCode().value()).isEqualTo(503);

        ResponseEntity<String> segunda = restTemplate.postForEntity("/assinaturas", request, String.class);
        assertThat(segunda.getStatusCode().value()).isEqualTo(503);
    }
}
