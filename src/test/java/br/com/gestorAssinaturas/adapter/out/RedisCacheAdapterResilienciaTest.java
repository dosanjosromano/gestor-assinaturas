package br.com.gestorAssinaturas.adapter.out;

import br.com.gestorAssinaturas.adapter.out.cache.RedisCacheAdapter;
import br.com.gestorAssinaturas.application.port.out.CacheIndisponivelException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers
class RedisCacheAdapterResilienciaTest {

    @Container
    static final GenericContainer<?> REDIS =
            new GenericContainer<>(DockerImageName.parse("redis:7-alpine")).withExposedPorts(6379);

    private LettuceConnectionFactory connectionFactory;
    private RedisCacheAdapter cacheAdapter;

    @BeforeEach
    void setUp() {
        LettuceClientConfiguration clientConfiguration = LettuceClientConfiguration.builder()
                .commandTimeout(Duration.ofSeconds(1))
                .build();
        connectionFactory = new LettuceConnectionFactory(
                new RedisStandaloneConfiguration(REDIS.getHost(), REDIS.getMappedPort(6379)), clientConfiguration);
        connectionFactory.afterPropertiesSet();
        StringRedisTemplate redisTemplate = new StringRedisTemplate(connectionFactory);
        redisTemplate.afterPropertiesSet();
        cacheAdapter = new RedisCacheAdapter(redisTemplate, new ObjectMapper());
    }

    @AfterEach
    void tearDown() {
        unpauseSeNecessario();
        connectionFactory.destroy();
    }

    @Test
    void buscarComRedisIndisponivelRetornaVazioSemPropagarExcecao() {
        REDIS.getDockerClient().pauseContainerCmd(REDIS.getContainerId()).exec();

        Optional<String> resultado = cacheAdapter.buscar("assinatura:ativa:qualquer", String.class);

        assertThat(resultado).isEmpty();
    }

    @Test
    void salvarComRedisIndisponivelNaoPropagaExcecao() {
        REDIS.getDockerClient().pauseContainerCmd(REDIS.getContainerId()).exec();

        cacheAdapter.salvar("assinatura:ativa:qualquer", "valor", Duration.ofMinutes(5));
    }

    @Test
    void evictComRedisIndisponivelLancaCacheIndisponivelException() {
        REDIS.getDockerClient().pauseContainerCmd(REDIS.getContainerId()).exec();

        assertThatThrownBy(() -> cacheAdapter.evict("assinatura:ativa:qualquer"))
                .isInstanceOf(CacheIndisponivelException.class);
    }

    private void unpauseSeNecessario() {
        boolean pausado = REDIS.getDockerClient()
                .inspectContainerCmd(REDIS.getContainerId())
                .exec()
                .getState()
                .getPaused();
        if (Boolean.TRUE.equals(pausado)) {
            REDIS.getDockerClient().unpauseContainerCmd(REDIS.getContainerId()).exec();
        }
    }
}

