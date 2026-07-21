package br.com.gestorAssinaturas.application.port.out;

import java.time.Duration;
import java.util.Optional;

public interface CachePort {

    <T> Optional<T> buscar(String chave, Class<T> tipo);

    void salvar(String chave, Object valor, Duration ttl);

    void evict(String chave);
}
