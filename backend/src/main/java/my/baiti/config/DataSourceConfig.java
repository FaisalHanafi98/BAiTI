package my.baiti.config;

import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Dual-datasource wiring for the two-role database model (blueprint 03_ARCHITECTURE §6.2, ED-4).
 *
 * <h2>appDataSource (primary, {@code baiti_app})</h2>
 *
 * HikariCP runtime pool. {@code baiti_app} is a non-owner role with {@code NOBYPASSRLS} — this is
 * the only connection over which RLS policies bind. Built explicitly here so that P05 can wrap it
 * with {@code TenantAwareDataSource} (set/reset {@code app.current_tenant} on borrow/return, ED-5).
 *
 * <h2>Flyway datasource ({@code baiti_migrator})</h2>
 *
 * NOT a bean here — Spring Boot's Flyway auto-configuration creates a dedicated connection from
 * {@code spring.flyway.url/user/password} (set in the profile YAML). When {@code spring.flyway.url}
 * is present, Flyway ignores the primary DataSource and uses the migrator credentials directly.
 * {@code baiti_migrator} owns the schema, so RLS does not bind it (this is why the two roles must
 * never collapse — a single-owner app role would silently void tenant isolation).
 *
 * <p>Env contract: {@code .env.example} / {@code docs/blueprint/03_ARCHITECTURE.md §8}.
 */
@Configuration
public class DataSourceConfig {

  @Bean
  @Primary
  @ConfigurationProperties("spring.datasource.hikari")
  public DataSource appDataSource(DataSourceProperties dataSourceProperties) {
    // P05: wrap the returned HikariDataSource with TenantAwareDataSource so that
    // app.current_tenant is set on borrow and RESET on return (fail-closed, ED-5/ED-6).
    return dataSourceProperties.initializeDataSourceBuilder().type(HikariDataSource.class).build();
  }
}
