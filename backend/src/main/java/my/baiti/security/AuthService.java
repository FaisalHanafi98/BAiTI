package my.baiti.security;

import java.nio.charset.StandardCharsets;
import java.util.Set;
import my.baiti.people.UserAuthentication;
import my.baiti.people.UserIdentity;
import my.baiti.tenancy.TenantContext;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

  private final UserAuthentication users;
  private final JwtService jwt;
  private final RefreshTokenService refresh;
  private final LoginRateLimiter limits;

  public AuthService(
      UserAuthentication users,
      JwtService jwt,
      RefreshTokenService refresh,
      LoginRateLimiter limits) {
    this.users = users;
    this.jwt = jwt;
    this.refresh = refresh;
    this.limits = limits;
  }

  @Transactional
  public LoginResult login(String email, String password) {
    if (TenantContext.current().isEmpty()
        || password.getBytes(StandardCharsets.UTF_8).length > 72) {
      throw new BadCredentialsException("Authentication failed");
    }
    if (!limits.allowAccount(TenantContext.current().orElseThrow(), email)) {
      throw new LoginRateLimitException();
    }
    var user =
        users
            .verifyPassword(email, password)
            .orElseThrow(() -> new BadCredentialsException("Authentication failed"));
    String access =
        jwt.issue(
            new TenantPrincipal(
                user.userId(), user.tenantId(), Set.of(user.role()), user.permissions()));
    return new LoginResult(access, refresh.create(user), user);
  }

  public LoginResult refresh(String raw) {
    var rotated = refresh.rotate(raw);
    var user = rotated.user();
    return new LoginResult(
        jwt.issue(
            new TenantPrincipal(
                user.userId(), user.tenantId(), Set.of(user.role()), user.permissions())),
        rotated.raw(),
        user);
  }

  public UserIdentity me(TenantPrincipal principal) {
    if (principal == null) {
      throw new BadCredentialsException("Authentication failed");
    }
    return users
        .findActiveIdentity(principal.userId())
        .orElseThrow(() -> new BadCredentialsException("Authentication failed"));
  }

  public void logout(TenantPrincipal principal) {
    refresh.revokeUser(me(principal).userId());
  }

  public record LoginResult(String accessToken, String refreshToken, UserIdentity user) {
    @Override
    public String toString() {
      return "LoginResult[redacted]";
    }
  }
}
