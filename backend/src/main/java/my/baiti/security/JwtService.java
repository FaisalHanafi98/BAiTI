package my.baiti.security;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.time.Clock;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

public final class JwtService {

  public static final Duration ACCESS_LIFETIME = Duration.ofMinutes(15);
  private static final String ISSUER = "baiti";
  private static final String AUDIENCE = "baiti:tenant";
  private final NimbusJwtEncoder encoder;
  private final NimbusJwtDecoder decoder;
  private final Clock clock;
  private final Duration accessLifetime;

  public JwtService(String base64Secret, Clock clock) {
    this(base64Secret, clock, ACCESS_LIFETIME);
  }

  public JwtService(String base64Secret, Clock clock, Duration accessLifetime) {
    new TokenLifetimes(accessLifetime, Duration.ofDays(7));
    this.accessLifetime = accessLifetime;
    byte[] secret;
    try {
      secret = Base64.getDecoder().decode(base64Secret);
    } catch (IllegalArgumentException | NullPointerException invalid) {
      throw new IllegalArgumentException(
          "JWT_SECRET must be Base64 with at least 32 decoded bytes");
    }
    if (secret.length < 32) {
      throw new IllegalArgumentException(
          "JWT_SECRET must be Base64 with at least 32 decoded bytes");
    }
    var key = new SecretKeySpec(secret, "HmacSHA256");
    this.clock = clock;
    encoder = new NimbusJwtEncoder(new ImmutableSecret<>(key));
    decoder = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
    var timestamps = new JwtTimestampValidator(Duration.ZERO);
    timestamps.setClock(clock);
    decoder.setJwtValidator(
        new DelegatingOAuth2TokenValidator<>(
            timestamps,
            new JwtIssuerValidator(ISSUER),
            new JwtClaimValidator<List<String>>(
                "aud", value -> value != null && value.contains(AUDIENCE)),
            new JwtClaimValidator<String>("token_use", "tenant_access"::equals)));
  }

  public String issue(TenantPrincipal principal) {
    var now = clock.instant();
    var claims =
        JwtClaimsSet.builder()
            .issuer(ISSUER)
            .subject(principal.userId().toString())
            .audience(List.of(AUDIENCE))
            .issuedAt(now)
            .notBefore(now)
            .expiresAt(now.plus(accessLifetime))
            .id(UUID.randomUUID().toString())
            .claim("token_use", "tenant_access")
            .claim("userId", principal.userId().toString())
            .claim("tenantId", principal.tenantId().toString())
            .claim("roles", principal.roles().stream().sorted().toList())
            .claim("permissions", principal.permissions().stream().sorted().toList())
            .build();
    return encoder
        .encode(
            JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).type("JWT").build(), claims))
        .getTokenValue();
  }

  public TenantPrincipal verify(String token) {
    try {
      if (token == null || token.length() > 8192) {
        throw new IllegalArgumentException();
      }
      var jwt = decoder.decode(token);
      Map<String, Object> claims = jwt.getClaims();
      var user = UUID.fromString(requiredString(claims, "userId"));
      if (!user.toString().equals(jwt.getSubject())
          || jwt.getIssuedAt() == null
          || jwt.getExpiresAt() == null
          || jwt.getNotBefore() == null
          || jwt.getIssuedAt().isAfter(clock.instant())
          || !jwt.getExpiresAt().isAfter(clock.instant())
          || !jwt.getExpiresAt().isAfter(jwt.getIssuedAt())
          || Duration.between(jwt.getIssuedAt(), jwt.getExpiresAt()).compareTo(ACCESS_LIFETIME)
              > 0) {
        throw new IllegalArgumentException();
      }
      UUID.fromString(requiredString(claims, "jti"));
      return new TenantPrincipal(
          user,
          UUID.fromString(requiredString(claims, "tenantId")),
          strings(claims, "roles"),
          strings(claims, "permissions"));
    } catch (JwtException | IllegalArgumentException | ClassCastException invalid) {
      throw new BadCredentialsException("Authentication failed");
    }
  }

  private static String requiredString(Map<String, Object> claims, String name) {
    if (claims.get(name) instanceof String value && !value.isBlank() && value.length() <= 128) {
      return value;
    }
    throw new IllegalArgumentException();
  }

  private static Set<String> strings(Map<String, Object> claims, String name) {
    if (!(claims.get(name) instanceof List<?> values)
        || values.size() > 256
        || values.stream()
            .anyMatch(
                value ->
                    !(value instanceof String text) || text.isBlank() || text.length() > 128)) {
      throw new IllegalArgumentException();
    }
    return values.stream().map(String.class::cast).collect(Collectors.toUnmodifiableSet());
  }
}
