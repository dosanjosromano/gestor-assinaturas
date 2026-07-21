package br.com.gestorAssinaturas.adapter.in;

import br.com.gestorAssinaturas.adapter.in.web.controller.request.CriarAssinaturaRequest;
import br.com.gestorAssinaturas.adapter.in.web.controller.request.CriarUsuarioRequest;
import br.com.gestorAssinaturas.adapter.in.web.controller.response.AssinaturaResponse;
import br.com.gestorAssinaturas.adapter.in.web.controller.response.UsuarioResponse;
import br.com.gestorAssinaturas.application.port.out.AssinaturaRepositoryPort;
import br.com.gestorAssinaturas.application.port.out.TentativaPagamentoRepositoryPort;
import br.com.gestorAssinaturas.domain.model.Assinatura;
import br.com.gestorAssinaturas.domain.model.Plano;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class UsuarioControllerIntegrationTest {

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

    @Autowired
    private AssinaturaRepositoryPort assinaturaRepositoryPort;

    @Autowired
    private TentativaPagamentoRepositoryPort tentativaPagamentoRepositoryPort;

    @Test
    void usuarioExcluidoSomeDasBuscasMasHistoricoDeAssinaturaPermaneceIntacto() {
        UUID id = criarUsuario("Fulano", "fulano-" + UUID.randomUUID() + "@teste.com").id();

        ResponseEntity<AssinaturaResponse> assinaturaCriada = restTemplate.postForEntity(
                "/assinaturas", new CriarAssinaturaRequest(id, Plano.BASICO), AssinaturaResponse.class);
        assertThat(assinaturaCriada.getStatusCode().value()).isEqualTo(201);
        UUID assinaturaId = assinaturaCriada.getBody().id();

        ResponseEntity<Void> exclusao = restTemplate.exchange("/usuarios/{id}", HttpMethod.DELETE, null, Void.class, id);
        assertThat(exclusao.getStatusCode().value()).isEqualTo(204);

        ResponseEntity<String> buscaPorId = restTemplate.getForEntity("/usuarios/{id}", String.class, id);
        assertThat(buscaPorId.getStatusCode().value()).isEqualTo(404);

        ResponseEntity<UsuarioResponse[]> listagem = restTemplate.getForEntity("/usuarios", UsuarioResponse[].class);
        assertThat(List.of(listagem.getBody())).extracting(UsuarioResponse::id).doesNotContain(id);

        Assinatura assinaturaNoBanco = assinaturaRepositoryPort.buscarPorId(assinaturaId).orElseThrow();
        assertThat(assinaturaNoBanco.getUsuarioId()).isEqualTo(id);
        assertThat(tentativaPagamentoRepositoryPort.buscarPorIdempotencyKey(assinaturaId + ":INICIAL:1")).isPresent();
    }

    @Test
    void exclusaoDeUsuarioEIdempotente() {
        UUID id = criarUsuario("Ciclano", "ciclano-" + UUID.randomUUID() + "@teste.com").id();

        ResponseEntity<Void> primeira = restTemplate.exchange("/usuarios/{id}", HttpMethod.DELETE, null, Void.class, id);
        ResponseEntity<Void> segunda = restTemplate.exchange("/usuarios/{id}", HttpMethod.DELETE, null, Void.class, id);

        assertThat(primeira.getStatusCode().value()).isEqualTo(204);
        assertThat(segunda.getStatusCode().value()).isEqualTo(204);
    }

    @Test
    void recadastroComEmailDeUsuarioInativoERejeitado() {
        String email = "reservado-" + UUID.randomUUID() + "@teste.com";
        UUID id = criarUsuario("Beltrano", email).id();
        restTemplate.exchange("/usuarios/{id}", HttpMethod.DELETE, null, Void.class, id);

        ResponseEntity<String> novoCadastro = restTemplate.postForEntity(
                "/usuarios", new CriarUsuarioRequest("Beltrano Segundo", email), String.class);

        assertThat(novoCadastro.getStatusCode().value()).isEqualTo(409);
    }

    private UsuarioResponse criarUsuario(String nome, String email) {
        ResponseEntity<UsuarioResponse> resposta = restTemplate.postForEntity(
                "/usuarios", new CriarUsuarioRequest(nome, email), UsuarioResponse.class);
        assertThat(resposta.getStatusCode().value()).isEqualTo(201);
        return resposta.getBody();
    }
}
