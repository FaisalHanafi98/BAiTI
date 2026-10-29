package my.baiti.testing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class NativePostgresConfigTest {

  @Test
  void completeNativeConfigurationIsAcceptedWithoutStartingADatabase() {
    var config = NativePostgresConfig.from(validEnvironment());

    assertThat(config.jdbcUrl()).isEqualTo("jdbc:postgresql://localhost:5432/baiti_test_f0");
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "BAITI_TEST_DB_MODE",
        "TEST_DB_URL",
        "TEST_DB_APP_USER",
        "TEST_DB_APP_PASSWORD",
        "TEST_DB_MIGRATOR_USER",
        "TEST_DB_MIGRATOR_PASSWORD",
        "TEST_DB_OWNER_TOKEN"
      })
  void missingSettingsAreRejectedInsteadOfUsingDevelopmentDefaults(String missing) {
    var environment = new HashMap<>(validEnvironment());
    environment.remove(missing);
    environment.put("DB_URL", "jdbc:postgresql://localhost:5432/baiti");

    assertThatIllegalArgumentException()
        .isThrownBy(() -> NativePostgresConfig.from(environment))
        .withMessageContaining(missing);
  }

  @ParameterizedTest
  @ValueSource(strings = {"docker", "auto", "container", ""})
  void nonNativeModesCannotStartAContainer(String mode) {
    var environment = new HashMap<>(validEnvironment());
    environment.put("BAITI_TEST_DB_MODE", mode);

    assertThatIllegalArgumentException()
        .isThrownBy(() -> NativePostgresConfig.from(environment))
        .withMessageContaining("BAITI_TEST_DB_MODE");
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "jdbc:postgresql://localhost:5432/baiti",
        "jdbc:postgresql://localhost:5432/postgres",
        "jdbc:postgresql://localhost:5432/template1",
        "jdbc:h2:mem:baiti_test_f0",
        "jdbc:postgresql:baiti_test_f0",
        "jdbc:postgresql://localhost:5432/baiti_test_f0?user=postgres",
        "jdbc:postgresql://localhost:5432/baiti_test_f0?options=-c%20role=postgres",
        "jdbc:postgresql://user:synthetic-secret@localhost:5432/baiti_test_f0",
        "jdbc:postgresql://localhost:0/baiti_test_f0",
        "jdbc:postgresql://localhost:65536/baiti_test_f0",
        "jdbc:postgresql://localhost:5432/baiti_test_f0#secret",
        "jdbc:postgresql://localhost:5432/baiti_test_%66",
        "jdbc:postgresql://localhost:5432/baiti_test_f0/other"
      })
  void unsafeJdbcTargetsAreRejectedBeforeConnection(String url) {
    var environment = new HashMap<>(validEnvironment());
    environment.put("TEST_DB_URL", url);

    assertThatIllegalArgumentException()
        .isThrownBy(() -> NativePostgresConfig.from(environment))
        .withMessageContaining("TEST_DB_URL")
        .withMessageNotContaining("synthetic-secret");
  }

  @Test
  void runtimeRoleCannotBeReplacedByTheMigrator() {
    var environment = new HashMap<>(validEnvironment());
    environment.put("TEST_DB_APP_USER", "baiti_migrator");

    assertThatIllegalArgumentException()
        .isThrownBy(() -> NativePostgresConfig.from(environment))
        .withMessageContaining("TEST_DB_APP_USER");
  }

  @Test
  void migrationRoleCannotBeReplacedByASuperuser() {
    var environment = new HashMap<>(validEnvironment());
    environment.put("TEST_DB_MIGRATOR_USER", "postgres");

    assertThatIllegalArgumentException()
        .isThrownBy(() -> NativePostgresConfig.from(environment))
        .withMessageContaining("TEST_DB_MIGRATOR_USER");
  }

  @Test
  void ownershipTokenMustBeAnExplicitCanonicalUuid() {
    var environment = new HashMap<>(validEnvironment());
    environment.put("TEST_DB_OWNER_TOKEN", "synthetic-invalid-secret");

    assertThatIllegalArgumentException()
        .isThrownBy(() -> NativePostgresConfig.from(environment))
        .withMessageContaining("TEST_DB_OWNER_TOKEN")
        .withMessageNotContaining("synthetic-invalid-secret");
  }

  @Test
  void nativePropertiesKeepApplicationAndMigrationConnectionsSeparate() {
    var config = NativePostgresConfig.from(validEnvironment());
    Map<String, Supplier<Object>> properties = new HashMap<>();

    config.register(properties::put);

    assertThat(properties.get("spring.datasource.url").get()).isEqualTo(config.jdbcUrl());
    assertThat(properties.get("spring.flyway.url").get()).isEqualTo(config.jdbcUrl());
    assertThat(properties.get("spring.datasource.username").get()).isEqualTo("baiti_app");
    assertThat(properties.get("spring.flyway.user").get()).isEqualTo("baiti_migrator");
    assertThat(properties.get("spring.datasource.password").get())
        .isEqualTo(validEnvironment().get("TEST_DB_APP_PASSWORD"));
    assertThat(properties.get("spring.flyway.password").get())
        .isEqualTo(validEnvironment().get("TEST_DB_MIGRATOR_PASSWORD"));
    assertThat(config.toString())
        .doesNotContain("synthetic-app-password", "synthetic-migrator-password", "9cc7c390");
  }

  private static Map<String, String> validEnvironment() {
    return Map.of(
        "BAITI_TEST_DB_MODE", "native",
        "TEST_DB_URL", "jdbc:postgresql://localhost:5432/baiti_test_f0",
        "TEST_DB_APP_USER", "baiti_app",
        "TEST_DB_APP_PASSWORD", "synthetic-app-password",
        "TEST_DB_MIGRATOR_USER", "baiti_migrator",
        "TEST_DB_MIGRATOR_PASSWORD", "synthetic-migrator-password",
        "TEST_DB_OWNER_TOKEN", "9cc7c390-f8d9-48c8-81a5-ed2b14b8dc82");
  }
}
