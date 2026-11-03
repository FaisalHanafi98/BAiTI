package my.baiti.config;

import com.zaxxer.hikari.HikariDataSource;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.concurrent.atomic.AtomicBoolean;
import my.baiti.tenancy.TenantContext;
import org.springframework.jdbc.datasource.AbstractDataSource;

public final class TenantAwareDataSource extends AbstractDataSource {

  private final HikariDataSource pool;

  public TenantAwareDataSource(HikariDataSource pool) {
    this.pool = pool;
  }

  @Override
  public Connection getConnection() throws SQLException {
    Connection connection = pool.getConnection();
    try {
      var tenant = TenantContext.current();
      if (tenant.isPresent()) {
        try (var statement =
            connection.prepareStatement("SELECT set_config('app.current_tenant', ?, false)")) {
          statement.setString(1, tenant.orElseThrow().toString());
          statement.execute();
        }
      } else {
        reset(connection);
      }
      return guard(connection);
    } catch (SQLException failure) {
      discard(connection, failure);
      throw failure;
    }
  }

  @Override
  public Connection getConnection(String username, String password) throws SQLException {
    throw new SQLFeatureNotSupportedException("Per-call database credentials are not supported");
  }

  private Connection guard(Connection connection) {
    var closed = new AtomicBoolean();
    return (Connection)
        Proxy.newProxyInstance(
            Connection.class.getClassLoader(),
            new Class<?>[] {Connection.class},
            (proxy, method, args) -> {
              String name = method.getName();
              if (name.equals("close")) {
                if (closed.compareAndSet(false, true)) {
                  release(connection);
                }
                return null;
              }
              if (name.equals("isClosed")) {
                return closed.get() || connection.isClosed();
              }
              if (name.equals("toString")) {
                return "Tenant-aware database connection";
              }
              if (name.equals("equals")) {
                return proxy == args[0];
              }
              if (name.equals("hashCode")) {
                return System.identityHashCode(proxy);
              }
              if (closed.get()) {
                throw new SQLException("Connection is closed", "08003");
              }
              if (name.equals("unwrap")) {
                if (((Class<?>) args[0]).isInstance(proxy)) {
                  return proxy;
                }
                throw new SQLException("Physical connection access is not supported");
              }
              if (name.equals("isWrapperFor")) {
                return ((Class<?>) args[0]).isInstance(proxy);
              }
              try {
                return method.invoke(connection, args);
              } catch (InvocationTargetException failure) {
                throw failure.getCause();
              }
            });
  }

  private void release(Connection connection) throws SQLException {
    try {
      // RESET inside an unfinished transaction could be rolled back by pool cleanup.
      if (!connection.getAutoCommit()) {
        connection.rollback();
        connection.setAutoCommit(true);
      }
      reset(connection);
    } catch (SQLException failure) {
      discard(connection, failure);
      throw failure;
    }
    connection.close();
  }

  private static void reset(Connection connection) throws SQLException {
    try (var statement = connection.prepareStatement("RESET app.current_tenant")) {
      statement.execute();
    }
  }

  private void discard(Connection connection, SQLException failure) {
    pool.evictConnection(connection);
    try {
      connection.close();
    } catch (SQLException closeFailure) {
      failure.addSuppressed(closeFailure);
    }
  }
}
