package my.baiti.tenancy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.UUID;
import javax.sql.DataSource;
import my.baiti.testing.FoundationTenantConfiguration;
import my.baiti.testing.NativePostgresTestSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(properties = "spring.datasource.hikari.maximum-pool-size=1")
@ActiveProfiles("test")
@Import(FoundationTenantConfiguration.class)
class TenantConnectionIT extends NativePostgresTestSupport {

  private static final UUID A = FoundationTenantConfiguration.TENANT;
  private static final UUID B = UUID.fromString("04b270a3-35dc-4bbd-b709-0942f6f7ef93");

  @Autowired private DataSource dataSource;
  @Autowired private JdbcTemplate jdbc;

  @AfterEach
  void clearContext() {
    TenantContext.clear();
  }

  @Test
  void contextScopesRawJdbcReadsToTheResolvedTenant() {
    TenantContext.set(A);
    assertThat(jdbc.queryForList("SELECT id FROM tenants", UUID.class)).containsExactly(A);
    TenantContext.set(B);
    assertThat(jdbc.queryForList("SELECT id FROM tenants", UUID.class)).containsExactly(B);
  }

  @Test
  void reusedPhysicalConnectionHasNoTenantAfterReturn() throws Exception {
    TenantContext.set(A);
    int backend;
    try (var connection = dataSource.getConnection()) {
      backend = scalar(connection, "SELECT pg_backend_pid()");
      assertThat(scalar(connection, "SELECT count(*) FROM tenants")).isEqualTo(1);
      assertThat(connection.unwrap(Connection.class)).isSameAs(connection);
    }
    TenantContext.clear();
    try (var connection = dataSource.getConnection()) {
      assertThat(scalar(connection, "SELECT pg_backend_pid()")).isEqualTo(backend);
      assertThat(scalar(connection, "SELECT count(*) FROM tenants")).isZero();
      assertThat(
              scalar(
                  connection,
                  "SELECT count(*) FROM pg_roles WHERE rolname = current_user"
                      + " AND rolname = 'baiti_app' AND NOT rolsuper AND NOT rolbypassrls"))
          .isEqualTo(1);
    }
  }

  @Test
  void missingContextRejectsRawJdbcInsertion() throws Exception {
    try (var connection = dataSource.getConnection()) {
      connection.setAutoCommit(false);
      var failure =
          assertThrows(SQLException.class, () -> insertBlock(connection, UUID.randomUUID(), A));
      assertThat(failure.getSQLState()).isEqualTo("42501");
    }
  }

  @Test
  void tenantACannotReadOrInsertTenantBThroughRawJdbc() throws Exception {
    TenantContext.set(A);
    try (var connection = dataSource.getConnection()) {
      connection.setAutoCommit(false);
      assertThat(scalar(connection, "SELECT count(*) FROM tenants WHERE id = '" + B + "'"))
          .isZero();
      var failure =
          assertThrows(SQLException.class, () -> insertBlock(connection, UUID.randomUUID(), B));
      assertThat(failure.getSQLState()).isEqualTo("42501");
    }
  }

  @Test
  void returnRollsBackUnfinishedWritesBeforeClearingTheSession() throws Exception {
    TenantContext.set(A);
    var id = UUID.randomUUID();
    int backend;
    try (var connection = dataSource.getConnection()) {
      backend = scalar(connection, "SELECT pg_backend_pid()");
      connection.setAutoCommit(false);
      insertBlock(connection, id, A);
      assertThat(scalar(connection, "SELECT count(*) FROM blocks WHERE id = '" + id + "'"))
          .isEqualTo(1);
    }
    TenantContext.clear();
    try (var connection = dataSource.getConnection()) {
      assertThat(scalar(connection, "SELECT pg_backend_pid()")).isEqualTo(backend);
      assertThat(scalar(connection, "SELECT count(*) FROM tenants")).isZero();
    }
    TenantContext.set(A);
    assertThat(jdbc.queryForObject("SELECT count(*) FROM blocks WHERE id = ?", Integer.class, id))
        .isZero();
  }

  @Test
  void returnRecoversAnAbortedTransactionAndClearsItsTenant() throws Exception {
    TenantContext.set(A);
    int backend;
    try (var connection = dataSource.getConnection()) {
      backend = scalar(connection, "SELECT pg_backend_pid()");
      connection.setAutoCommit(false);
      var failure = assertThrows(SQLException.class, () -> scalar(connection, "SELECT 1 / 0"));
      assertThat(failure.getSQLState()).isEqualTo("22012");
    }
    TenantContext.clear();
    try (var connection = dataSource.getConnection()) {
      assertThat(scalar(connection, "SELECT pg_backend_pid()")).isEqualTo(backend);
      assertThat(scalar(connection, "SELECT count(*) FROM tenants")).isZero();
    }
  }

  private static int scalar(Connection connection, String sql) throws SQLException {
    try (var statement = connection.prepareStatement(sql);
        var result = statement.executeQuery()) {
      assertThat(result.next()).isTrue();
      return result.getInt(1);
    }
  }

  private static void insertBlock(Connection connection, UUID id, UUID tenant) throws SQLException {
    try (var statement =
        connection.prepareStatement("INSERT INTO blocks (id, tenant_id, name) VALUES (?, ?, ?)")) {
      statement.setObject(1, id);
      statement.setObject(2, tenant);
      statement.setString(3, "Synthetic pool probe");
      statement.executeUpdate();
    }
  }
}
