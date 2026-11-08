package my.baiti.config;

import jakarta.validation.Validator;
import java.util.UUID;
import my.baiti.security.PlatformPrincipal;
import my.baiti.tenancy.admin.PlatformAuthentication;
import my.baiti.tenancy.admin.TenantOnboardingService;
import my.baiti.tenancy.admin.TenantRegistration;
import my.baiti.tenancy.admin.UsesPrivilegedDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@Profile("dev & !test & !prod")
@ConditionalOnProperty(prefix = "baiti.dev.seed", name = "enabled", havingValue = "true")
@UsesPrivilegedDataSource
public class DevDataInitializer implements ApplicationRunner {
  private static final Logger log = LoggerFactory.getLogger(DevDataInitializer.class);

  private final JdbcTemplate platform;
  private final PlatformAuthentication authentication;
  private final TenantOnboardingService onboarding;
  private final Validator validator;
  private final UUID dbToken;
  private final UUID operatorId;
  private final String initialPassword;

  public DevDataInitializer(
      @Qualifier("platformJdbcTemplate") JdbcTemplate platform,
      PlatformAuthentication authentication,
      TenantOnboardingService onboarding,
      Validator validator,
      @Value("${baiti.dev.seed.db-token}") UUID dbToken,
      @Value("${baiti.dev.seed.operator-id}") UUID operatorId,
      @Value("${baiti.dev.seed.initial-password}") String initialPassword) {
    this.platform = platform;
    this.authentication = authentication;
    this.onboarding = onboarding;
    this.validator = validator;
    this.dbToken = dbToken;
    this.operatorId = operatorId;
    this.initialPassword = initialPassword;
  }

  @Override
  public void run(ApplicationArguments arguments) {
    if (dbToken == null || !ownsDevDatabase()) {
      throw new IllegalStateException(
          "Dev seeding requires a marked loopback PostgreSQL 16 database");
    }
    if (operatorId == null || !authentication.active(operatorId)) {
      throw new IllegalStateException("Dev seeding requires an active synthetic platform operator");
    }
    var operator = new PlatformPrincipal(operatorId);
    seed(operator, "baiti-dev-rumbia", "Rumbia Gardens");
    seed(operator, "baiti-dev-kenari", "Kenari Grove");
  }

  private boolean ownsDevDatabase() {
    return Boolean.TRUE.equals(
        platform.queryForObject(
            """
            SELECT EXISTS (
              SELECT 1 FROM pg_database
              WHERE datname = current_database() AND datname ~ '^baiti_dev_[a-z0-9_]+$'
                AND current_user = 'baiti_migrator' AND pg_get_userbyid(datdba) = current_user
                AND inet_server_addr() IN (inet '127.0.0.1', inet '::1')
                AND current_setting('server_version_num')::int BETWEEN 160000 AND 169999
                AND shobj_description(oid, 'pg_database') = ?
            )
            """,
            Boolean.class,
            "baiti-dev:" + dbToken));
  }

  private void seed(PlatformPrincipal operator, String slug, String name) {
    var registration =
        new TenantRegistration(
            slug, name, slug + "@example.invalid", "Synthetic Admin", initialPassword);
    if (!validator.validate(registration).isEmpty()) {
      throw new IllegalStateException("Dev seed initial password must fit the onboarding policy");
    }
    try {
      onboarding.create(operator, registration, "127.0.0.1");
      log.info("Created development community {}", slug);
    } catch (TenantOnboardingService.SlugConflict conflict) {
      var existing =
          platform.queryForObject(
              """
              SELECT EXISTS (
                SELECT 1 FROM tenants WHERE slug = CAST(? AS citext) AND name = ? AND status = 'ACTIVE'
              )
              """,
              Boolean.class,
              slug,
              name);
      if (!Boolean.TRUE.equals(existing)) {
        throw new IllegalStateException("Development seed slug is occupied: " + slug);
      }
      log.info("Development community {} already exists", slug);
    }
  }
}
