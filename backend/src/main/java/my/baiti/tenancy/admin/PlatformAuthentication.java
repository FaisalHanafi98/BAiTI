package my.baiti.tenancy.admin;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import my.baiti.security.LoginRateLimitException;
import my.baiti.security.LoginRateLimiter;
import my.baiti.security.PlatformPrincipal;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@UsesPrivilegedDataSource
public class PlatformAuthentication {
  private static final UUID PLATFORM_ACCOUNT_SCOPE = new UUID(0, 0);
  private final JdbcTemplate platform;
  private final PasswordEncoder passwords;
  private final LoginRateLimiter limits;
  private final Clock clock;
  private final String dummyHash;

  public PlatformAuthentication(
      @Qualifier("platformJdbcTemplate") JdbcTemplate platform,
      PasswordEncoder passwords,
      LoginRateLimiter limits,
      Clock clock) {
    this.platform = platform;
    this.passwords = passwords;
    this.limits = limits;
    this.clock = clock;
    this.dummyHash = passwords.encode(UUID.randomUUID().toString());
  }

  public PlatformPrincipal login(String email, String password) {
    if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
      throw new BadCredentialsException("Authentication failed");
    }
    if (!limits.allowAccount(PLATFORM_ACCOUNT_SCOPE, email)) {
      throw new LoginRateLimitException();
    }
    var account =
        platform
            .query(
                "SELECT id, password_hash FROM super_admins WHERE email = CAST(? AS citext) AND status = 'ACTIVE'",
                (row, index) ->
                    new Account(row.getObject("id", UUID.class), row.getString("password_hash")),
                email)
            .stream()
            .findFirst();
    if (!passwords.matches(password, account.map(Account::hash).orElse(dummyHash))
        || account.isEmpty()) {
      throw new BadCredentialsException("Authentication failed");
    }
    var operator = account.orElseThrow();
    var now = OffsetDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
    if (platform.update(
            "UPDATE super_admins SET last_login_at = ?, updated_at = ? "
                + "WHERE id = ? AND status = 'ACTIVE' AND password_hash = ?",
            now,
            now,
            operator.id(),
            operator.hash())
        != 1) {
      throw new BadCredentialsException("Authentication failed");
    }
    return new PlatformPrincipal(operator.id());
  }

  public boolean active(UUID id) {
    return Boolean.TRUE.equals(
        platform.queryForObject(
            "SELECT EXISTS (SELECT 1 FROM super_admins WHERE id = ? AND status = 'ACTIVE')",
            Boolean.class,
            id));
  }

  private record Account(UUID id, String hash) {
    @Override
    public String toString() {
      return "Account[redacted]";
    }
  }
}
