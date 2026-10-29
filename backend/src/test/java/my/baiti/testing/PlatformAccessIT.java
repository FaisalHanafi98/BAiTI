package my.baiti.testing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.sql.SQLException;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class PlatformAccessIT {

  private static NativePostgresConfig config;

  @BeforeAll
  static void prepareVerifiedTarget() {
    config = NativePostgresMigrations.migrateVerifiedTarget();
  }

  @ParameterizedTest
  @ValueSource(strings = {"app-tenants", "app-admins", "owner-tenants", "owner-admins"})
  void currentGlobalPoliciesDoNotProvideAPlatformProvisioningPath(String scenario)
      throws Exception {
    boolean owner = scenario.startsWith("owner-");
    boolean tenant = scenario.endsWith("-tenants");
    try (var connection = owner ? config.openMigrationConnection() : config.openAppConnection()) {
      connection.setAutoCommit(false);
      try {
        try (var statement = connection.prepareStatement("SELECT current_user");
            var result = statement.executeQuery()) {
          assertThat(result.next()).isTrue();
          assertThat(result.getString(1)).isEqualTo(owner ? "baiti_migrator" : "baiti_app");
        }
        String sql =
            tenant
                ? "INSERT INTO tenants (id, slug, name) VALUES (?, ?, ?)"
                : "INSERT INTO super_admins (id, email, name, password_hash) VALUES (?, ?, ?, ?)";
        try (var statement = connection.prepareStatement(sql)) {
          UUID id = UUID.randomUUID();
          statement.setObject(1, id);
          statement.setString(2, tenant ? "f0-" + id : "f0-" + id + "@example.invalid");
          statement.setString(3, "Synthetic platform access fixture");
          if (!tenant) {
            statement.setString(4, "synthetic-not-a-login-hash");
          }
          SQLException failure = assertThrows(SQLException.class, statement::executeUpdate);
          assertThat(failure.getSQLState()).isEqualTo("42501");
        }
      } finally {
        connection.rollback();
      }
    }
  }
}
