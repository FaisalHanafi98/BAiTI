package my.baiti.testing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class PlatformAccessIT {

  private static final UUID PLATFORM_FIXTURE =
      UUID.fromString("b4da3ea7-53f5-4dce-97c7-b231bf4be102");

  private static NativePostgresConfig config;

  @BeforeAll
  static void prepareVerifiedTarget() {
    config = NativePostgresMigrations.migrateVerifiedTarget();
  }

  @ParameterizedTest
  @ValueSource(strings = {"app-tenants", "app-admins"})
  void applicationCannotProvisionGlobalRows(String scenario) throws Exception {
    boolean tenant = scenario.endsWith("-tenants");
    try (var connection = config.openAppConnection()) {
      connection.setAutoCommit(false);
      try {
        try (var statement = connection.prepareStatement("SELECT current_user");
            var result = statement.executeQuery()) {
          assertThat(result.next()).isTrue();
          assertThat(result.getString(1)).isEqualTo("baiti_app");
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

  @ParameterizedTest
  @ValueSource(strings = {"tenants", "super_admins"})
  void ordinaryMigratorCanProvisionGlobalRowsWithoutDeleteAccess(String table) throws Exception {
    try (var connection = config.openMigrationConnection()) {
      connection.setAutoCommit(false);
      try {
        UUID id = UUID.randomUUID();
        assertThatCode(() -> insertGlobal(connection, table, id)).doesNotThrowAnyException();
        assertThat(globalCount(connection, table, id)).isEqualTo(1);
        String updateSql =
            table.equals("tenants")
                ? "UPDATE tenants SET status = 'SUSPENDED' WHERE id = ?"
                : "UPDATE super_admins SET status = 'SUSPENDED' WHERE id = ?";
        try (var update = connection.prepareStatement(updateSql)) {
          update.setObject(1, id);
          assertThat(update.executeUpdate()).isEqualTo(1);
        }
        String deleteSql =
            table.equals("tenants")
                ? "DELETE FROM tenants WHERE id = ?"
                : "DELETE FROM super_admins WHERE id = ?";
        try (var delete = connection.prepareStatement(deleteSql)) {
          delete.setObject(1, id);
          assertThat(delete.executeUpdate()).isZero();
        }
        assertThat(globalCount(connection, table, id)).isEqualTo(1);
      } finally {
        connection.rollback();
      }
    }
  }

  @Test
  void applicationCannotReadAnExistingPlatformAdmin() throws Exception {
    assertPlatformFixture();
    try (var connection = config.openAppConnection()) {
      connection.setAutoCommit(false);
      try {
        setTenant(connection, FoundationTenantConfiguration.TENANT);
        assertThat(globalCount(connection, "tenants", FoundationTenantConfiguration.TENANT))
            .isEqualTo(1);
        assertThat(globalCount(connection, "super_admins", PLATFORM_FIXTURE)).isZero();
      } finally {
        connection.rollback();
      }
    }
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "UPDATE super_admins SET status = 'SUSPENDED' WHERE id = ?",
        "DELETE FROM super_admins WHERE id = ?"
      })
  void applicationCannotChangeAnExistingPlatformAdmin(String operation) throws Exception {
    assertPlatformFixture();
    try (var connection = config.openAppConnection()) {
      connection.setAutoCommit(false);
      try {
        setTenant(connection, FoundationTenantConfiguration.TENANT);
        try (var statement = connection.prepareStatement(operation)) {
          statement.setObject(1, PLATFORM_FIXTURE);
          assertThat(statement.executeUpdate()).isZero();
        }
      } finally {
        connection.rollback();
      }
    }
    assertPlatformFixture();
  }

  @Test
  void applicationCannotAssumeTheMigrationRole() throws Exception {
    try (var connection = config.openAppConnection()) {
      connection.setAutoCommit(false);
      try (var statement = connection.prepareStatement("SET LOCAL ROLE baiti_migrator")) {
        SQLException failure = assertThrows(SQLException.class, statement::execute);
        assertThat(failure.getSQLState()).isEqualTo("42501");
      } finally {
        connection.rollback();
      }
    }
  }

  @Test
  void platformPoliciesDoNotBypassTenantTableIsolation() throws Exception {
    try (var connection = config.openMigrationConnection()) {
      connection.setAutoCommit(false);
      try {
        UUID blockId = UUID.randomUUID();
        setTenant(connection, FoundationTenantConfiguration.TENANT);
        try (var insert =
            connection.prepareStatement(
                "INSERT INTO blocks (id, tenant_id, name) VALUES (?, ?, ?)")) {
          insert.setObject(1, blockId);
          insert.setObject(2, FoundationTenantConfiguration.TENANT);
          insert.setString(3, "F0_" + blockId);
          assertThat(insert.executeUpdate()).isEqualTo(1);
        }
        assertThat(visibleBlock(connection, blockId)).isEqualTo(1);
        setTenant(connection, UUID.fromString("04b270a3-35dc-4bbd-b709-0942f6f7ef93"));
        assertThat(visibleBlock(connection, blockId)).isZero();
        try (var reset = connection.prepareStatement("RESET app.current_tenant")) {
          reset.execute();
        }
        assertThat(visibleBlock(connection, blockId)).isZero();
        setTenant(connection, FoundationTenantConfiguration.TENANT);
        assertThat(visibleBlock(connection, blockId)).isEqualTo(1);
      } finally {
        connection.rollback();
      }
    }
  }

  private static void assertPlatformFixture() throws SQLException {
    try (var connection = config.openMigrationConnection();
        var statement =
            connection.prepareStatement(
                """
        SELECT count(*) FROM super_admins WHERE id = ?
         AND email = 'foundation-platform@example.invalid'
         AND name = 'Synthetic foundation platform administrator'
         AND status = 'ACTIVE'
        """)) {
      statement.setObject(1, PLATFORM_FIXTURE);
      try (var result = statement.executeQuery()) {
        assertThat(result.next()).isTrue();
        assertThat(result.getLong(1)).isEqualTo(1);
      }
    }
  }

  private static void setTenant(Connection connection, UUID tenant) throws SQLException {
    try (var statement =
        connection.prepareStatement("SELECT set_config('app.current_tenant', ?, true)")) {
      statement.setString(1, tenant.toString());
      statement.execute();
    }
  }

  private static long visibleBlock(Connection connection, UUID blockId) throws SQLException {
    try (var statement = connection.prepareStatement("SELECT count(*) FROM blocks WHERE id = ?")) {
      statement.setObject(1, blockId);
      try (var result = statement.executeQuery()) {
        assertThat(result.next()).isTrue();
        return result.getLong(1);
      }
    }
  }

  private static void insertGlobal(Connection connection, String table, UUID id)
      throws SQLException {
    boolean tenant = table.equals("tenants");
    String sql =
        tenant
            ? "INSERT INTO tenants (id, slug, name) VALUES (?, ?, ?)"
            : "INSERT INTO super_admins (id, email, name, password_hash) VALUES (?, ?, ?, ?)";
    try (var statement = connection.prepareStatement(sql)) {
      statement.setObject(1, id);
      statement.setString(2, tenant ? "f0-" + id : "f0-" + id + "@example.invalid");
      statement.setString(3, "Synthetic platform policy fixture");
      if (!tenant) {
        statement.setString(4, "synthetic-not-a-login-hash");
      }
      assertThat(statement.executeUpdate()).isEqualTo(1);
    }
  }

  private static long globalCount(Connection connection, String table, UUID id)
      throws SQLException {
    String sql =
        table.equals("tenants")
            ? "SELECT count(*) FROM tenants WHERE id = ?"
            : "SELECT count(*) FROM super_admins WHERE id = ?";
    try (var statement = connection.prepareStatement(sql)) {
      statement.setObject(1, id);
      try (var result = statement.executeQuery()) {
        assertThat(result.next()).isTrue();
        return result.getLong(1);
      }
    }
  }
}
