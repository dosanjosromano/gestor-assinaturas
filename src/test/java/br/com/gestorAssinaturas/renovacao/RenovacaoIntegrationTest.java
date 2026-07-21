package br.com.gestorAssinaturas.renovacao;


import br.com.gestorAssinaturas.application.port.in.useCase.ProcessarRenovacaoUseCase;
import br.com.gestorAssinaturas.application.port.out.AssinaturaRepositoryPort;
import br.com.gestorAssinaturas.domain.model.Assinatura;
import br.com.gestorAssinaturas.domain.model.Plano;
import br.com.gestorAssinaturas.domain.model.StatusAssinatura;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest
class RenovacaoIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        // Gateway determinístico: sempre recusa, para os cenários abaixo não dependerem de sorteio.
        registry.add("app.gateway-pagamento.mock.taxa-aprovacao", () -> "0.0");
        registry.add("app.gateway-pagamento.mock.taxa-recusa", () -> "1.0");
    }

    @Autowired
    private AssinaturaRepositoryPort assinaturaRepositoryPort;

    @Autowired
    private ProcessarRenovacaoUseCase processarRenovacaoUseCase;

    private Assinatura salvarAssinaturaAtiva(LocalDate dataExpiracao, int falhas) {
        Assinatura assinatura = new Assinatura(UUID.randomUUID(), UUID.randomUUID(), Plano.BASICO);
        assinatura.confirmarPagamento();
        for (int i = 0; i < falhas; i++) {
            assinatura.registrarFalhaRenovacao();
        }
        assinaturaRepositoryPort.salvar(assinatura);
        forcarDataExpiracao(assinatura, dataExpiracao);
        return assinatura;
    }

    private void forcarDataExpiracao(Assinatura assinatura, LocalDate dataExpiracao) {
        // dataExpiracao só avança via renovarCiclo(); para testar elegibilidade precisamos de
        // assinaturas já vencidas na criação, o que exige ajustar o campo via reflexão de teste.
        try {
            var campo = Assinatura.class.getDeclaredField("dataExpiracao");
            campo.setAccessible(true);
            campo.set(assinatura, dataExpiracao);
            assinaturaRepositoryPort.salvar(assinatura);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    @Test
    void buscaElegiveisRetornaApenasAtivasVencidasComMenosDeTresFalhas() {
        LocalDate hoje = LocalDate.now();

        Assinatura elegivel = salvarAssinaturaAtiva(hoje, 1);
        Assinatura naoVencida = salvarAssinaturaAtiva(hoje.plusDays(1), 0);

        Assinatura suspensa = new Assinatura(UUID.randomUUID(), UUID.randomUUID(), Plano.BASICO);
        suspensa.confirmarPagamento();
        suspensa.registrarFalhaRenovacao();
        suspensa.registrarFalhaRenovacao();
        suspensa.registrarFalhaRenovacao();
        assinaturaRepositoryPort.salvar(suspensa);
        forcarDataExpiracao(suspensa, hoje.minusDays(1));

        List<UUID> elegiveis = assinaturaRepositoryPort.buscarElegiveisParaRenovacao(hoje).stream()
                .map(Assinatura::getId)
                .toList();

        assertThat(elegiveis).contains(elegivel.getId());
        assertThat(elegiveis).doesNotContain(naoVencida.getId());
        assertThat(elegiveis).doesNotContain(suspensa.getId());
        assertThat(suspensa.getStatus()).isEqualTo(StatusAssinatura.SUSPENSA);
    }

    @Test
    void recusaMantemDataExpiracaoEAssinaturaContinuaElegivelNoDiaSeguinte() {
        LocalDate hoje = LocalDate.now();
        Assinatura assinatura = salvarAssinaturaAtiva(hoje, 0);

        assertThat(assinaturaRepositoryPort.buscarElegiveisParaRenovacao(hoje))
                .extracting(Assinatura::getId)
                .contains(assinatura.getId());

        processarRenovacaoUseCase.processar(assinatura.getId());

        Assinatura apósRecusa = assinaturaRepositoryPort.buscarPorId(assinatura.getId()).orElseThrow();
        assertThat(apósRecusa.getDataExpiracao()).isEqualTo(hoje);
        assertThat(apósRecusa.getFalhasRenovacaoConsecutivas()).isEqualTo(1);
        assertThat(apósRecusa.getStatus()).isEqualTo(StatusAssinatura.ATIVA);

        List<UUID> elegiveisDiaSeguinte = assinaturaRepositoryPort.buscarElegiveisParaRenovacao(hoje.plusDays(1)).stream()
                .map(Assinatura::getId)
                .toList();
        assertThat(elegiveisDiaSeguinte).contains(assinatura.getId());
    }
}
