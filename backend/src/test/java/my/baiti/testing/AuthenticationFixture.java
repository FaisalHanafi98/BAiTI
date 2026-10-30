package my.baiti.testing;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import javax.sql.DataSource;
import my.baiti.tenancy.TenantContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.support.TransactionTemplate;

public final class AuthenticationFixture implements AutoCloseable {

  private final DataSource source;
  private final UUID tenant;
  private final UUID user = UUID.randomUUID();
  private final UUID role = UUID.randomUUID();
  private final UUID permission = UUID.randomUUID();
  private final String email = "synthetic-" + user + "@example.invalid";

  public AuthenticationFixture(DataSource source, UUID tenant, String password) {
    this.source = source;
    this.tenant = tenant;
    scoped(
        jdbc -> {
          assertThat(jdbc.queryForObject("SELECT current_user", String.class))
              .isEqualTo("baiti_app");
          jdbc.update(
              "INSERT INTO roles (id, tenant_id, name) VALUES (?, ?, ?)",
              role,
              tenant,
              "Synthetic authentication role " + role);
          jdbc.update(
              "INSERT INTO permissions (id, tenant_id, code, module) VALUES (?, ?, ?, ?)",
              permission,
              tenant,
              "AUTH_TEST_"
                  + permission.toString().replace("-", "").toUpperCase(java.util.Locale.ROOT),
              "AUTH_TEST");
          jdbc.update(
              "INSERT INTO roles_permissions (role_id, permission_id, tenant_id) VALUES (?, ?, ?)",
              role,
              permission,
              tenant);
          jdbc.update(
              "INSERT INTO users (id, tenant_id, name, email, password_hash, role_id) VALUES (?, ?, ?, ?, ?, ?)",
              user,
              tenant,
              "Synthetic authentication user",
              email,
              new BCryptPasswordEncoder(4).encode(password),
              role);
        });
  }

  public UUID userId() {
    return user;
  }

  public String email() {
    return email;
  }

  public void scoped(java.util.function.Consumer<JdbcTemplate> operation) {
    TenantContext.set(tenant);
    try {
      new TransactionTemplate(new DataSourceTransactionManager(source))
          .executeWithoutResult(status -> operation.accept(new JdbcTemplate(source)));
    } finally {
      TenantContext.clear();
    }
  }

  @Override
  public void close() {
    scoped(
        jdbc -> {
          jdbc.update("DELETE FROM refresh_tokens WHERE user_id = ?", user);
          jdbc.update("DELETE FROM users WHERE id = ?", user);
          jdbc.update("DELETE FROM roles_permissions WHERE role_id = ?", role);
          jdbc.update("DELETE FROM roles WHERE id = ?", role);
          jdbc.update("DELETE FROM permissions WHERE id = ?", permission);
        });
  }
}
