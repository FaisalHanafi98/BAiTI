package my.baiti.testing;

import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import org.springframework.test.context.DynamicPropertyRegistry;

final class NativePostgresConfig {

  private final String jdbcUrl;
  private final String appPassword;
  private final String migratorPassword;
  private final String ownerToken;

  private NativePostgresConfig(
      String jdbcUrl, String appPassword, String migratorPassword, String ownerToken) {
    this.jdbcUrl = jdbcUrl;
    this.appPassword = appPassword;
    this.migratorPassword = migratorPassword;
    this.ownerToken = ownerToken;
  }

  static NativePostgresConfig from(Map<String, String> environment) {
    for (String key :
        new String[] {
          "BAITI_TEST_DB_MODE",
          "TEST_DB_URL",
          "TEST_DB_APP_USER",
          "TEST_DB_APP_PASSWORD",
          "TEST_DB_MIGRATOR_USER",
          "TEST_DB_MIGRATOR_PASSWORD",
          "TEST_DB_OWNER_TOKEN"
        }) {
      if (environment.get(key) == null || environment.get(key).isBlank()) {
        throw new IllegalArgumentException("Missing required test setting: " + key);
      }
    }
    if (!"native".equals(environment.get("BAITI_TEST_DB_MODE"))) {
      throw new IllegalArgumentException("BAITI_TEST_DB_MODE must be native; Docker is deferred");
    }
    String url = environment.get("TEST_DB_URL");
    validateUrl(url);
    if (!"baiti_app".equals(environment.get("TEST_DB_APP_USER"))) {
      throw new IllegalArgumentException("TEST_DB_APP_USER must be baiti_app");
    }
    if (!"baiti_migrator".equals(environment.get("TEST_DB_MIGRATOR_USER"))) {
      throw new IllegalArgumentException("TEST_DB_MIGRATOR_USER must be baiti_migrator");
    }
    String token = environment.get("TEST_DB_OWNER_TOKEN");
    try {
      if (!UUID.fromString(token).toString().equals(token)) {
        throw new IllegalArgumentException();
      }
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException("TEST_DB_OWNER_TOKEN must be a canonical UUID");
    }
    return new NativePostgresConfig(
        url,
        environment.get("TEST_DB_APP_PASSWORD"),
        environment.get("TEST_DB_MIGRATOR_PASSWORD"),
        token);
  }

  private static void validateUrl(String url) {
    try {
      if (!url.startsWith("jdbc:postgresql://")) {
        throw new URISyntaxException("", "Invalid test target");
      }
      URI target = new URI(url.substring(5));
      if (target.getHost() == null
          || target.getPort() < 1
          || target.getPort() > 65535
          || target.getRawUserInfo() != null
          || target.getRawQuery() != null
          || target.getRawFragment() != null
          || target.getRawPath() == null
          || !target.getRawPath().matches("/baiti_test_[a-z0-9_]{1,52}")) {
        throw new URISyntaxException("", "Invalid test target");
      }
    } catch (URISyntaxException e) {
      throw new IllegalArgumentException(
          "TEST_DB_URL must name an explicit host, port and baiti_test_* database without URL options or credentials");
    }
  }

  String jdbcUrl() {
    return jdbcUrl;
  }

  String databaseName() {
    return URI.create(jdbcUrl.substring(5)).getPath().substring(1);
  }

  String ownershipMarker() {
    return "baiti-test:" + ownerToken;
  }

  Connection openAppConnection() throws SQLException {
    return connect("baiti_app", appPassword);
  }

  Connection openMigrationConnection() throws SQLException {
    return connect("baiti_migrator", migratorPassword);
  }

  private Connection connect(String user, String password) throws SQLException {
    var properties = new Properties();
    properties.setProperty("user", user);
    properties.setProperty("password", password);
    properties.setProperty("connectTimeout", "5");
    properties.setProperty("socketTimeout", "5");
    properties.setProperty("ApplicationName", "baiti-native-test-preflight");
    return DriverManager.getConnection(jdbcUrl, properties);
  }

  void register(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", this::jdbcUrl);
    registry.add("spring.datasource.username", () -> "baiti_app");
    registry.add("spring.datasource.password", () -> appPassword);
    registry.add("spring.flyway.url", this::jdbcUrl);
    registry.add("spring.flyway.user", () -> "baiti_migrator");
    registry.add("spring.flyway.password", () -> migratorPassword);
  }
}
