package my.baiti.testing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import com.zaxxer.hikari.HikariDataSource;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import java.sql.SQLException;
import java.util.Locale;
import java.util.UUID;
import my.baiti.config.JpaConfig;
import my.baiti.security.SuperAdmin;
import my.baiti.tenancy.Tenant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest(showSql = false)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Import({
  JpaConfig.class,
  FoundationTenantConfiguration.class,
  PrivilegedEntityMappingIT.PrivilegedMappingConfiguration.class
})
class PrivilegedEntityMappingIT extends NativePostgresTestSupport {

  private final EntityManager entities;

  @Autowired
  PrivilegedEntityMappingIT(EntityManager entities) {
    this.entities = entities;
  }

  @BeforeEach
  void verifyTheExplicitPrivilegedMappingPool() {
    assertThat(entities.createNativeQuery("SELECT current_user", String.class).getSingleResult())
        .isEqualTo("baiti_migrator");
    assertThat(
            ((Number)
                    entities
                        .createNativeQuery(
                            """
                SELECT count(*) FROM pg_roles WHERE rolname = current_user
                 AND NOT rolsuper AND NOT rolbypassrls
                """)
                        .getSingleResult())
                .longValue())
        .isEqualTo(1);
  }

  @ParameterizedTest
  @ValueSource(strings = {"tenant", "admin"})
  void globalEntitiesRoundTripWithAuditingAndEveryStatus(String kind) {
    Object row = globalRow(kind, "f0-" + UUID.randomUUID());
    entities.persist(row);
    entities.flush();
    UUID id = row instanceof Tenant tenant ? tenant.getId() : ((SuperAdmin) row).getId();
    Class<?> type = row.getClass();
    entities.clear();
    Object restored = entities.find(type, id);
    assertThat(restored).isNotNull();
    if (restored instanceof Tenant tenant) {
      assertThat(tenant.getCreatedAt()).isNotNull();
      assertThat(tenant.getUpdatedAt()).isNotNull();
      for (Tenant.Status status : Tenant.Status.values()) {
        tenant.setStatus(status);
        entities.flush();
        entities.refresh(tenant);
        assertThat(tenant.getStatus()).isEqualTo(status);
        assertThat(
                entities
                    .createNativeQuery("SELECT status FROM tenants WHERE id = :id", String.class)
                    .setParameter("id", id)
                    .getSingleResult())
            .isEqualTo(status.name());
      }
    } else {
      var admin = (SuperAdmin) restored;
      assertThat(admin.getCreatedAt()).isNotNull();
      assertThat(admin.getUpdatedAt()).isNotNull();
      for (SuperAdmin.Status status : SuperAdmin.Status.values()) {
        admin.setStatus(status);
        entities.flush();
        entities.refresh(admin);
        assertThat(admin.getStatus()).isEqualTo(status);
        assertThat(
                entities
                    .createNativeQuery(
                        "SELECT status FROM super_admins WHERE id = :id", String.class)
                    .setParameter("id", id)
                    .getSingleResult())
            .isEqualTo(status.name());
      }
    }
  }

  @ParameterizedTest
  @ValueSource(strings = {"tenant", "admin"})
  void globalBusinessKeysAreCaseInsensitive(String kind) {
    String key = "f0-" + UUID.randomUUID();
    entities.persist(globalRow(kind, key));
    entities.flush();
    Throwable failure =
        catchThrowable(
            () -> {
              entities.persist(globalRow(kind, key.toUpperCase(Locale.ROOT)));
              entities.flush();
            });
    assertThat(failure).isInstanceOf(PersistenceException.class);
    while (failure.getCause() != null) {
      failure = failure.getCause();
    }
    assertThat(failure).isInstanceOf(SQLException.class);
    assertThat(((SQLException) failure).getSQLState()).isEqualTo("23505");
    assertThat(failure.getMessage())
        .contains(kind.equals("tenant") ? "tenants_slug_key" : "super_admins_email_key");
  }

  private static Object globalRow(String kind, String key) {
    if (kind.equals("tenant")) {
      var tenant = new Tenant();
      tenant.setSlug(key);
      tenant.setName("Synthetic privileged mapping tenant");
      return tenant;
    }
    var admin = new SuperAdmin();
    admin.setEmail(key + "@example.invalid");
    admin.setName("Synthetic privileged mapping administrator");
    admin.setPasswordHash("synthetic-not-a-login-hash");
    return admin;
  }

  @TestConfiguration(proxyBeanMethods = false)
  static class PrivilegedMappingConfiguration {

    @Bean(destroyMethod = "close")
    @Primary
    HikariDataSource platformMappingTestDataSource(
        @Value("${spring.flyway.url}") String url,
        @Value("${spring.flyway.user}") String user,
        @Value("${spring.flyway.password}") String password) {
      var pool = new HikariDataSource();
      pool.setJdbcUrl(url);
      pool.setUsername(user);
      pool.setPassword(password);
      pool.setPoolName("baiti-privileged-mapping-test");
      pool.setMaximumPoolSize(2);
      pool.setMinimumIdle(0);
      pool.setConnectionTimeout(5000);
      pool.addDataSourceProperty("connectTimeout", 5);
      pool.addDataSourceProperty("socketTimeout", 10);
      return pool;
    }
  }
}
