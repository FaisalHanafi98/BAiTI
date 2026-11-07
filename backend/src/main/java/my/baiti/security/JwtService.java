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
import org.springframework.security.oauth2.jwt.Jwt;
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
  private static final String PLATFORM_AUDIENCE = "baiti:platform";
  private final NimbusJwtEncoder encoder;
  private final NimbusJwtDecoder decoder;
  private final NimbusJwtDecoder platformDecoder;
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
    decoder = decoder(key, AUDIENCE, "tenant_access");
    platformDecoder = decoder(key, PLATFORM_AUDIENCE, "platform_access");
  }

  private NimbusJwtDecoder decoder(SecretKeySpec key, String audience, String purpose) {
    var result = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
    var timestamps = new JwtTimestampValidator(Duration.ZERO);
    timestamps.setClock(clock);
    result.setJwtValidator(
        new DelegatingOAuth2TokenValidator<>(
            timestamps,
            new JwtIssuerValidator(ISSUER),
            new JwtClaimValidator<List<String>>(
                "aud", value -> value != null && value.contains(audience)),
            new JwtClaimValidator<String>("token_use", purpose::equals)));
    return result;
  }

  public String issue(TenantPrincipal principal) {
    var claims =
        claims(principal.userId(), AUDIENCE, "tenant_access")
            .claim("userId", principal.userId().toString())
            .claim("tenantId", principal.tenantId().toString())
            .claim("roles", principal.roles().stream().sorted().toList())
            .claim("permissions", principal.permissions().stream().sorted().toList())
            .build();
    return encode(claims);
  }

  public String issuePlatform(PlatformPrincipal principal) {
    return encode(claims(principal.adminId(), PLATFORM_AUDIENCE, "platform_access").build());
  }

  private JwtClaimsSet.Builder claims(UUID subject, String audience, String purpose) {
    var now = clock.instant();
    return JwtClaimsSet.builder()
        .issuer(ISSUER)
        .subject(subject.toString())
        .audience(List.of(audience))
        .issuedAt(now)
        .notBefore(now)
        .expiresAt(now.plus(accessLifetime))
        .id(UUID.randomUUID().toString())
        .claim("token_use", purpose);
  }

  private String encode(JwtClaimsSet claims) {
    return encoder
        .encode(
            JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).type("JWT").build(), claims))
        .getTokenValue();
  }

  public TenantPrincipal verify(String token) {
    try {
      var jwt = verified(token, decoder);
      Map<String, Object> claims = jwt.getClaims();
      var user = UUID.fromString(requiredString(claims, "userId"));
      if (!user.toString().equals(jwt.getSubject())) {
        throw new IllegalArgumentException();
      }
      return new TenantPrincipal(
          user,
          UUID.fromString(requiredString(claims, "tenantId")),
          strings(claims, "roles"),
          strings(claims, "permissions"));
    } catch (JwtException | IllegalArgumentException | ClassCastException invalid) {
      throw new BadCredentialsException("Authentication failed");
    }
  }

  public PlatformPrincipal verifyPlatform(String token) {
    try {
      var jwt = verified(token, platformDecoder);
      var subject = requiredString(jwt.getClaims(), "sub");
      var id = UUID.fromString(subject);
      if (!id.toString().equals(subject) || jwt.hasClaim("tenantId")) {
        throw new IllegalArgumentException();
      }
      return new PlatformPrincipal(id);
    } catch (JwtException | IllegalArgumentException | ClassCastException invalid) {
      throw new BadCredentialsException("Authentication failed");
    }
  }

  private Jwt verified(String token, NimbusJwtDecoder verifier) {
    if (token == null || token.length() > 8192) {
      throw new IllegalArgumentException();
    }
    var jwt = verifier.decode(token);
    var now = clock.instant();
    if (jwt.getIssuedAt() == null
        || jwt.getExpiresAt() == null
        || jwt.getNotBefore() == null
        || jwt.getIssuedAt().isAfter(now)
        || !jwt.getExpiresAt().isAfter(now)
        || !jwt.getExpiresAt().isAfter(jwt.getIssuedAt())
        || Duration.between(jwt.getIssuedAt(), jwt.getExpiresAt()).compareTo(ACCESS_LIFETIME) > 0) {
      throw new IllegalArgumentException();
    }
    UUID.fromString(requiredString(jwt.getClaims(), "jti"));
    return jwt;
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
