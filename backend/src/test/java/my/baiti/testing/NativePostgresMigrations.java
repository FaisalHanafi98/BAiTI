package my.baiti.testing;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import org.flywaydb.core.Flyway;

final class NativePostgresMigrations {

  static NativePostgresConfig migrateVerifiedTarget() {
    var config = NativePostgresConfig.from(System.getenv());
    NativePostgresPreflight.verify(config);
    Map<String, Supplier<Object>> properties = new HashMap<>();
    config.register(properties::put);
    Flyway.configure()
        .dataSource(
            config.jdbcUrl(),
            (String) properties.get("spring.flyway.user").get(),
            (String) properties.get("spring.flyway.password").get())
        .locations("classpath:db/migration")
        .cleanDisabled(true)
        .load()
        .migrate();
    return config;
  }
}
