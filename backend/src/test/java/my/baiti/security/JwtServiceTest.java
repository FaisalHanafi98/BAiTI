package my.baiti.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;

class JwtServiceTest {

  private static final Instant NOW = Instant.parse("2026-10-29T12:00:00Z");

  @Test
  void signedAccessTokenRestoresItsTenantUserAndAuthorities() {
    var service = new JwtService(randomSecret(), Clock.fixed(NOW, ZoneOffset.UTC));
    var principal =
        new TenantPrincipal(
            UUID.randomUUID(), UUID.randomUUID(), Set.of("Admin"), Set.of("BILL_MANAGE"));
    var token = service.issue(principal);
    assertThat(service.verify(token)).isEqualTo(principal);
    assertThatThrownBy(() -> service.verify("malformed-token"))
        .isInstanceOf(BadCredentialsException.class)
        .hasMessage("Authentication failed");
  }

  static String randomSecret() {
    byte[] bytes = new byte[32];
    new SecureRandom().nextBytes(bytes);
    return Base64.getEncoder().encodeToString(bytes);
  }

  @Test
  void tokenIsExpiredAtTheExactFifteenMinuteBoundary() {
    var key = randomSecret();
    var issuer = new JwtService(key, Clock.fixed(NOW, ZoneOffset.UTC));
    var verifier = new JwtService(key, Clock.fixed(NOW.plusSeconds(900), ZoneOffset.UTC));
    var token =
        issuer.issue(
            new TenantPrincipal(
                UUID.randomUUID(), UUID.randomUUID(), Set.of("Admin"), Set.of("BILL_MANAGE")));
    assertThatThrownBy(() -> verifier.verify(token)).isInstanceOf(BadCredentialsException.class);
  }
}
