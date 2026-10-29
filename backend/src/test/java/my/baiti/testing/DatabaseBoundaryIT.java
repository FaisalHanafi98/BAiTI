package my.baiti.testing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class DatabaseBoundaryIT {

  private static NativePostgresConfig config;

  @BeforeAll
  static void prepareVerifiedTarget() {
    config = NativePostgresMigrations.migrateVerifiedTarget();
  }

  @Test
  void clearingTenantContextReturnsNoRowsAfterAPositiveControl() throws Exception {
    try (var connection = config.openAppConnection()) {
      assertThat(visibleTenants(connection)).isZero();
      setTenant(connection, FoundationTenantConfiguration.TENANT.toString());
      assertThat(visibleTenants(connection)).isEqualTo(1);
      try (var reset = connection.prepareStatement("RESET app.current_tenant")) {
        reset.execute();
      }
      assertThat(visibleTenants(connection)).isZero();
    }
  }

  @ParameterizedTest
  @ValueSource(strings = {"", " ", "malformed-tenant", "' OR true --"})
  void invalidTenantSettingsReadNoRows(String tenant) throws Exception {
    try (var connection = config.openAppConnection()) {
      setTenant(connection, tenant);
      assertThat(visibleTenants(connection)).isZero();
    }
  }

  @Test
  void scopedApplicationInsertAndReadSucceedsWithoutOwnerPrivileges() throws Exception {
    try (var connection = config.openAppConnection()) {
      connection.setAutoCommit(false);
      try {
        setTenant(connection, FoundationTenantConfiguration.TENANT.toString());
        UUID id = UUID.randomUUID();
        insertBlock(connection, id, FoundationTenantConfiguration.TENANT);
        try (var statement =
            connection.prepareStatement("SELECT tenant_id FROM blocks WHERE id = ?")) {
          statement.setObject(1, id);
          try (var result = statement.executeQuery()) {
            assertThat(result.next()).isTrue();
            assertThat(result.getObject(1, UUID.class))
                .isEqualTo(FoundationTenantConfiguration.TENANT);
          }
        }
      } finally {
        connection.rollback();
      }
    }
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "malformed-tenant", "04b270a3-35dc-4bbd-b709-0942f6f7ef93"})
  void invalidOrForeignContextRejectsWritesToTenantA(String tenant) throws Exception {
    try (var connection = config.openAppConnection()) {
      connection.setAutoCommit(false);
      try {
        setTenant(connection, tenant);
        SQLException failure =
            assertThrows(
                SQLException.class,
                () ->
                    insertBlock(
                        connection, UUID.randomUUID(), FoundationTenantConfiguration.TENANT));
        assertThat(failure.getSQLState()).isEqualTo("42501");
      } finally {
        connection.rollback();
      }
    }
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "UPDATE payments SET receipt_no = 'F0_MUTATION'",
        "DELETE FROM payments",
        "UPDATE activity_logs SET action = 'F0_MUTATION'",
        "DELETE FROM activity_logs"
      })
  void applicationCannotMutateImmutableHistory(String operation) throws Exception {
    try (var connection = config.openAppConnection()) {
      connection.setAutoCommit(false);
      try {
        setTenant(connection, FoundationTenantConfiguration.TENANT.toString());
        try (var statement = connection.prepareStatement(operation + " WHERE id = ?")) {
          statement.setObject(1, UUID.randomUUID());
          SQLException failure = assertThrows(SQLException.class, statement::executeUpdate);
          assertThat(failure.getSQLState()).isEqualTo("42501");
        }
      } finally {
        connection.rollback();
      }
    }
  }

  private static void setTenant(Connection connection, String tenant) throws SQLException {
    try (var statement =
        connection.prepareStatement("SELECT set_config('app.current_tenant', ?, false)")) {
      statement.setString(1, tenant);
      statement.execute();
    }
  }

  private static long visibleTenants(Connection connection) throws SQLException {
    try (var statement = connection.prepareStatement("SELECT count(*) FROM tenants");
        var result = statement.executeQuery()) {
      assertThat(result.next()).isTrue();
      return result.getLong(1);
    }
  }

  private static void insertBlock(Connection connection, UUID id, UUID tenant) throws SQLException {
    try (var statement =
        connection.prepareStatement("INSERT INTO blocks (id, tenant_id, name) VALUES (?, ?, ?)")) {
      statement.setObject(1, id);
      statement.setObject(2, tenant);
      statement.setString(3, "F0_" + id);
      statement.executeUpdate();
    }
  }
}
