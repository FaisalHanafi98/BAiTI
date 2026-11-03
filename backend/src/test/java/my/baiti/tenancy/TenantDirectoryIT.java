package my.baiti.tenancy;

import static org.assertj.core.api.Assertions.assertThat;

import com.zaxxer.hikari.HikariDataSource;
import java.util.UUID;
import my.baiti.testing.NativePostgresTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest
@ActiveProfiles("test")
class TenantDirectoryIT extends NativePostgresTestSupport {

  @Autowired private TenantLookup lookup;
  @Autowired private TenantCatalog catalog;
  @Autowired private JdbcTemplate app;

  @Autowired
  @Qualifier("platformJdbcTemplate")
  private JdbcTemplate platform;

  @Autowired
  @Qualifier("platformConnectionPool")
  private HikariDataSource platformPool;

  @Test
  void catalogueDiscoveryIncludesSuspendedTenantsWithoutExposingAnAppRoleListing() {
    new TransactionTemplate(new DataSourceTransactionManager(platformPool))
        .executeWithoutResult(
            status -> {
              status.setRollbackOnly();
              var suspended = UUID.randomUUID();
              platform.update(
                  "INSERT INTO tenants (id, slug, name, status) VALUES (?, ?, ?, 'SUSPENDED')",
                  suspended,
                  "catalog-" + suspended,
                  "Synthetic catalogue discovery");
              assertThat(catalog.listTenantIds())
                  .contains(UUID.fromString("ad8cdb3d-885a-4f0d-b678-2331ce582805"), suspended);
              assertThat(app.queryForObject("SELECT count(*) FROM tenants", Integer.class))
                  .isZero();
            });
  }

  @Test
  void directoryResolvesOnlyActiveCommunitiesThroughTheExplicitOwnerPool() {
    assertThat(app.queryForObject("SELECT current_user", String.class)).isEqualTo("baiti_app");
    assertThat(platform.queryForObject("SELECT current_user", String.class))
        .isEqualTo("baiti_migrator");
    assertThat(
            platform.queryForObject(
                "SELECT NOT rolsuper AND NOT rolbypassrls FROM pg_roles"
                    + " WHERE rolname = current_user",
                Boolean.class))
        .isTrue();
    assertThat(lookup.findActiveTenant("baiti-foundation-a"))
        .contains(UUID.fromString("ad8cdb3d-885a-4f0d-b678-2331ce582805"));
    assertThat(lookup.findActiveTenant("synthetic-unknown-community")).isEmpty();
    assertThat(app.queryForObject("SELECT count(*) FROM tenants", Integer.class)).isZero();
    assertThat(platform.queryForObject("SELECT count(*) FROM blocks", Integer.class)).isZero();
  }

  @Test
  void suspendedCommunityIsIndistinguishableFromAnUnknownSlug() {
    new TransactionTemplate(new DataSourceTransactionManager(platformPool))
        .executeWithoutResult(
            status -> {
              status.setRollbackOnly();
              String slug = "synthetic-suspended-" + UUID.randomUUID().toString().substring(0, 8);
              platform.update(
                  "INSERT INTO tenants (id, slug, name, status) VALUES (?, ?, ?, 'SUSPENDED')",
                  UUID.randomUUID(),
                  slug,
                  "Synthetic suspended community");
              assertThat(lookup.findActiveTenant(slug)).isEmpty();
            });
  }
}
