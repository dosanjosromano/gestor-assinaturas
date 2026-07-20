package br.com.gestorAssinaturas.migration;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
class FlywayMigrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Test
    void migracoesAplicamComSucessoECriamIndiceParcialDeAssinaturaAtiva() throws SQLException {
        Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration")
                .load()
                .migrate();

        try (Connection connection = DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())) {

            assertTrue(indiceExiste(connection, "uk_assinatura_usuario_ativa_ou_pendente"),
                    "índice único parcial de assinatura ativa/pendente deveria existir");

            UUID usuarioId = UUID.randomUUID();
            inserirUsuario(connection, usuarioId);
            inserirAssinaturaAtiva(connection, usuarioId);

            assertThatThrownBy(() -> inserirAssinaturaAtiva(connection, usuarioId))
                    .isInstanceOf(SQLException.class);
        }
    }

    private boolean indiceExiste(Connection connection, String indexName) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(
                     "SELECT indexname FROM pg_indexes WHERE indexname = '" + indexName + "'")) {
            return resultSet.next();
        }
    }

    private void inserirUsuario(Connection connection, UUID usuarioId) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("""
                    INSERT INTO usuario (id, nome, email)
                    VALUES ('%s', 'Usuario Teste', '%s@teste.com')
                    """.formatted(usuarioId, usuarioId));
        }
    }

    private void inserirAssinaturaAtiva(Connection connection, UUID usuarioId) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("""
                    INSERT INTO assinatura (id, usuario_id, plano, status, data_inicio, data_expiracao)
                    VALUES ('%s', '%s', 'BASICO', 'ATIVA', now(), now() + interval '1 month')
                    """.formatted(UUID.randomUUID(), usuarioId));
        }
    }
}
