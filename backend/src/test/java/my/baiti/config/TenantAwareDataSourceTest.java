package my.baiti.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.UUID;
import my.baiti.tenancy.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class TenantAwareDataSourceTest {

  private final HikariDataSource pool = mock(HikariDataSource.class);
  private final Connection physical = mock(Connection.class);
  private final TenantAwareDataSource source = new TenantAwareDataSource(pool);

  @AfterEach
  void clearContext() {
    TenantContext.clear();
  }

  @Test
  void failedBindingEvictsTheConnectionAndNeverHandsItToTheCaller() throws Exception {
    TenantContext.set(UUID.randomUUID());
    when(pool.getConnection()).thenReturn(physical);
    var failure = new SQLException("Synthetic binding failure");
    when(physical.prepareStatement("SELECT set_config('app.current_tenant', ?, false)"))
        .thenThrow(failure);

    SQLException observed = assertThrows(SQLException.class, source::getConnection);
    assertThat((Throwable) observed).isSameAs(failure);
    verify(pool).evictConnection(physical);
    verify(physical).close();
  }

  @Test
  void failedReturnResetEvictsInsteadOfRecyclingTheConnection() throws Exception {
    when(pool.getConnection()).thenReturn(physical);
    when(physical.getAutoCommit()).thenReturn(true);
    var statement = mock(PreparedStatement.class);
    when(physical.prepareStatement("RESET app.current_tenant")).thenReturn(statement);
    var failure = new SQLException("Synthetic reset failure");
    when(statement.execute()).thenReturn(false).thenThrow(failure);
    var connection = source.getConnection();

    SQLException observed = assertThrows(SQLException.class, connection::close);
    assertThat((Throwable) observed).isSameAs(failure);
    assertThat(connection.isClosed()).isTrue();
    verify(pool).evictConnection(physical);
    verify(physical).close();
    connection.close();
  }

  @Test
  void rollbackFailureIsNotFollowedByAutoCommitOrPoolReuse() throws Exception {
    when(pool.getConnection()).thenReturn(physical);
    var statement = mock(PreparedStatement.class);
    when(physical.prepareStatement("RESET app.current_tenant")).thenReturn(statement);
    var connection = source.getConnection();
    var failure = new SQLException("Synthetic rollback failure");
    org.mockito.Mockito.doThrow(failure).when(physical).rollback();

    SQLException observed = assertThrows(SQLException.class, connection::close);
    assertThat((Throwable) observed).isSameAs(failure);
    verify(pool).evictConnection(physical);
    verify(physical, org.mockito.Mockito.never()).setAutoCommit(true);
    verify(physical).close();
  }

  @Test
  void perCallCredentialsCannotSelectAnotherDatabaseRole() {
    assertThrows(
        SQLFeatureNotSupportedException.class,
        () -> source.getConnection("synthetic-other-role", "synthetic-placeholder"));
    verifyNoInteractions(pool);
  }
}
