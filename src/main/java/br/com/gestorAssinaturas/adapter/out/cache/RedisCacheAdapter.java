package br.com.gestorAssinaturas.adapter.out.cache;

import br.com.gestorAssinaturas.application.port.out.CacheIndisponivelException;
import br.com.gestorAssinaturas.application.port.out.CachePort;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

@Slf4j
@Component
public class RedisCacheAdapter implements CachePort {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public RedisCacheAdapter(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public <T> Optional<T> buscar(String chave, Class<T> tipo) {
        String valor;
        try {
            valor = redisTemplate.opsForValue().get(chave);
        } catch (DataAccessException e) {
            log.warn("[cache indisponível] buscar - chave={} motivo={}", chave, e.getMessage());
            return Optional.empty();
        }
        if (valor == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(valor, tipo));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Falha ao desserializar valor do cache para a chave %s".formatted(chave), e);
        }
    }

    @Override
    public void salvar(String chave, Object valor, Duration ttl) {
        String json;
        try {
            json = objectMapper.writeValueAsString(valor);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Falha ao serializar valor para o cache, chave %s".formatted(chave), e);
        }
        try {
            redisTemplate.opsForValue().set(chave, json, ttl);
        } catch (DataAccessException e) {
            log.warn("[cache indisponível] salvar - chave={} motivo={}", chave, e.getMessage());
        }
    }

    @Override
    public void evict(String chave) {
        try {
            redisTemplate.delete(chave);
        } catch (DataAccessException e) {
            log.warn("[cache indisponível] evict - chave={} motivo={}", chave, e.getMessage());
            throw new CacheIndisponivelException(
                    "Falha ao invalidar chave %s: Redis indisponível".formatted(chave), e);
        }
    }
}
