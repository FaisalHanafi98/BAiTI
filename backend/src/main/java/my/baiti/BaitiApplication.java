package my.baiti;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * BAiTI backend entry point.
 *
 * <p>Multi-tenant condominium management platform. The application pool runs as {@code baiti_app}
 * (NOBYPASSRLS, subject to every RLS policy); Flyway runs as {@code baiti_migrator} (schema owner).
 * See {@code docs/blueprint/03_ARCHITECTURE.md} §6 and {@code DataSourceConfig}.
 */
@SpringBootApplication
public class BaitiApplication {

  public static void main(String[] args) {
    SpringApplication.run(BaitiApplication.class, args);
  }
}
