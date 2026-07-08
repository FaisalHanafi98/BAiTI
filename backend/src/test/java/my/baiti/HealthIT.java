package my.baiti;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * P02 skeleton health check: the Spring context boots against a real PostgreSQL 16 container using
 * the two-role model, and {@code GET /actuator/health} responds 200 UP.
 *
 * <p>The container is a singleton (started once in a static block, reused across test classes per
 * the Testcontainers-recommended pattern and blueprint 06_TESTING_STRATEGY §1 speed budget). The
 * two roles are created on the container at start so that the app pool connects as {@code
 * baiti_app} (RLS-bound) and Flyway connects as {@code baiti_migrator} (owner) — mirroring {@code
 * infra/db-init/01_roles.sh}. No migrations exist yet (P03), so Flyway applies nothing.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
class HealthIT {

  static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>("postgres:16")
          .withDatabaseName("baiti")
          .withUsername("test")
          .withPassword("test");

  static {
    POSTGRES.start();
    bootstrapRoles();
  }

  private static void bootstrapRoles() {
    try (Connection c =
            DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        Statement s = c.createStatement()) {
      s.execute("CREATE ROLE baiti_migrator LOGIN PASSWORD 'test'");
      s.execute("CREATE ROLE baiti_app LOGIN PASSWORD 'test' NOBYPASSRLS");
      s.execute("ALTER DATABASE baiti OWNER TO baiti_migrator");
      s.execute("ALTER SCHEMA public OWNER TO baiti_migrator");
      s.execute("GRANT USAGE ON SCHEMA public TO baiti_app");
    } catch (SQLException e) {
      throw new IllegalStateException("test role bootstrap failed", e);
    }
  }

  @DynamicPropertySource
  static void registerDatasource(DynamicPropertyRegistry registry) {
    // App pool = baiti_app (RLS binds here). Flyway = baiti_migrator (owner) via spring.flyway.*.
    registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    registry.add("spring.datasource.username", () -> "baiti_app");
    registry.add("spring.datasource.password", () -> "test");
    registry.add("spring.flyway.url", POSTGRES::getJdbcUrl);
    registry.add("spring.flyway.user", () -> "baiti_migrator");
    registry.add("spring.flyway.password", () -> "test");
  }

  @LocalServerPort int port;

  @Autowired org.springframework.boot.test.web.client.TestRestTemplate rest;

  @Test
  void contextLoads() {
    // context startup itself is the assertion — a failure here means the dual-datasource wiring,
    // Flyway, or security config did not load.
  }

  @Test
  void healthEndpointIsUpAndAnonymous() {
    var response = rest.getForEntity("/actuator/health", String.class);
    assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
    assertThat(response.getBody()).contains("\"status\":\"UP\"");
  }
}
