package br.com.gestorAssinaturas.adapter.out.persistencia;

import br.com.gestorAssinaturas.domain.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface UsuarioJpaRepository extends JpaRepository<Usuario, UUID> {
}