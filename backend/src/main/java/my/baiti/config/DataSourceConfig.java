package my.baiti.config;

import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.autoconfigure.DataSourceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Dual-datasource wiring for the two-role database model (blueprint 03_ARCHITECTURE §6.2, ED-4).
 *
 * <h2>appDataSource (primary, {@code baiti_app})</h2>
 *
 * HikariCP runtime pool. {@code baiti_app} is a non-owner role with {@code NOBYPASSRLS}. FORCE RLS
 * also binds the ordinary owner. {@code TenantAwareDataSource} sets/resets the tenant on
 * borrow/return. The platform path requires its separate explicit privilege boundary.
 *
 * <h2>Flyway datasource ({@code baiti_migrator})</h2>
 *
 * NOT a bean here — Spring Boot's Flyway auto-configuration creates a dedicated connection from
 * {@code spring.flyway.url/user/password} (set in the profile YAML). When {@code spring.flyway.url}
 * is present, Flyway ignores the primary DataSource and uses the migrator credentials directly.
 * {@code baiti_migrator} owns the schema and must never be the runtime pool user. FORCE RLS also
 * binds that owner; platform operations still require explicit tested policies and privilege scope.
 *
 * <p>Env contract: {@code .env.example} / {@code docs/blueprint/03_ARCHITECTURE.md §8}.
 */
@Configuration
public class DataSourceConfig {

  @Bean(destroyMethod = "close")
  @ConfigurationProperties("spring.datasource.hikari")
  public HikariDataSource appConnectionPool(DataSourceProperties dataSourceProperties) {
    return dataSourceProperties.initializeDataSourceBuilder().type(HikariDataSource.class).build();
  }

  @Bean
  @Primary
  public DataSource appDataSource(@Qualifier("appConnectionPool") HikariDataSource pool) {
    return new TenantAwareDataSource(pool);
  }
}
