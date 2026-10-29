package my.baiti.testing;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.io.ClassPathResource;

class MigrationLocationsIT {

  @Test
  void missingPackagedMigrationsFailAtLocationResolution() throws Exception {
    var config = NativePostgresMigrations.migrateVerifiedTarget();
    var application =
        new YamlPropertySourceLoader()
            .load("application", new ClassPathResource("application.yml"))
            .get(0);
    boolean failOnMissing =
        Boolean.TRUE.equals(application.getProperty("spring.flyway.fail-on-missing-locations"));
    Map<String, Supplier<Object>> properties = new HashMap<>();
    config.register(properties::put);
    var missing =
        Flyway.configure()
            .dataSource(
                config.jdbcUrl(),
                (String) properties.get("spring.flyway.user").get(),
                (String) properties.get("spring.flyway.password").get())
            .locations("classpath:db/foundation-intentionally-missing")
            .failOnMissingLocations(failOnMissing)
            .cleanDisabled(true)
            .load();

    assertThatThrownBy(missing::migrate)
        .isInstanceOf(FlywayException.class)
        .hasMessageContaining("Unable to resolve location");
  }
}
