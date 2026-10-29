package my.baiti.security;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

class JwtAbuseTest {

  private static final Instant NOW = Instant.parse("2026-10-29T12:00:00Z");
  private final String secret = randomSecret();
  private final JwtService service = new JwtService(secret, Clock.fixed(NOW, ZoneOffset.UTC));
  private final TenantPrincipal principal =
      new TenantPrincipal(
          UUID.randomUUID(), UUID.randomUUID(), Set.of("Admin"), Set.of("BILL_MANAGE"));

  @Test
  void anotherSigningKeyCannotAuthenticate() {
    String token =
        new JwtService(randomSecret(), Clock.fixed(NOW, ZoneOffset.UTC)).issue(principal);
    rejected(token);
  }

  @Test
  void changingTheTenantPayloadWithoutResigningIsRejected() {
    String[] pieces = service.issue(principal).split("\\.");
    String payload = new String(Base64.getUrlDecoder().decode(pieces[1]), StandardCharsets.UTF_8);
    payload = payload.replace(principal.tenantId().toString(), UUID.randomUUID().toString());
    rejected(
        pieces[0]
            + "."
            + Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(payload.getBytes(StandardCharsets.UTF_8))
            + "."
            + pieces[2]);
  }

  @Test
  void unsignedAndAlternativeAlgorithmTokensAreRejected() {
    String[] pieces = service.issue(principal).split("\\.");
    String header =
        Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString("{\"alg\":\"none\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));
    rejected(header + "." + pieces[1] + ".");
    rejected(signed(claims -> {}, MacAlgorithm.HS512));
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "exp",
        "iat",
        "nbf",
        "iss",
        "aud",
        "sub",
        "jti",
        "userId",
        "tenantId",
        "roles",
        "permissions",
        "token_use"
      })
  void requiredClaimsCannotBeOmittedEvenWithAValidSignature(String name) {
    rejected(signed(claims -> claims.remove(name), MacAlgorithm.HS256));
  }

  @ParameterizedTest
  @ValueSource(strings = {"userId", "tenantId", "jti"})
  void identityClaimsMustBeUuids(String name) {
    rejected(signed(claims -> claims.put(name, "synthetic-invalid-id"), MacAlgorithm.HS256));
  }

  @Test
  void issuerAudienceTokenPurposeAndSubjectAreChecked() {
    rejected(signed(claims -> claims.put("iss", "another-issuer"), MacAlgorithm.HS256));
    rejected(signed(claims -> claims.put("aud", List.of("another-audience")), MacAlgorithm.HS256));
    rejected(signed(claims -> claims.put("token_use", "super_admin_access"), MacAlgorithm.HS256));
    rejected(signed(claims -> claims.put("sub", UUID.randomUUID().toString()), MacAlgorithm.HS256));
  }

  @Test
  void futureAndOverlongTokenLifetimesAreRejected() {
    rejected(
        new JwtService(secret, Clock.fixed(NOW.plusSeconds(60), ZoneOffset.UTC)).issue(principal));
    rejected(signed(claims -> claims.put("exp", NOW.plusSeconds(901)), MacAlgorithm.HS256));
  }

  @Test
  void malformedAuthoritiesAndOversizedTokensAreRejected() {
    rejected(signed(claims -> claims.put("roles", "Admin"), MacAlgorithm.HS256));
    rejected(signed(claims -> claims.put("permissions", List.of(12)), MacAlgorithm.HS256));
    rejected("a".repeat(8193));
  }

  @Test
  void missingMalformedAndWeakSecretsFailWithoutEchoingTheirValue() {
    for (String invalid :
        new String[] {
          null, "synthetic invalid base64", Base64.getEncoder().encodeToString(new byte[16])
        }) {
      assertThatIllegalArgumentException()
          .isThrownBy(() -> new JwtService(invalid, Clock.systemUTC()))
          .withMessage("JWT_SECRET must be Base64 with at least 32 decoded bytes");
    }
  }

  private void rejected(String token) {
    assertThatThrownBy(() -> service.verify(token))
        .isInstanceOf(BadCredentialsException.class)
        .hasMessage("Authentication failed");
  }

  private String signed(Consumer<Map<String, Object>> change, MacAlgorithm algorithm) {
    var claims =
        JwtClaimsSet.builder()
            .issuer("baiti")
            .audience(List.of("baiti:tenant"))
            .subject(principal.userId().toString())
            .issuedAt(NOW)
            .notBefore(NOW)
            .expiresAt(NOW.plusSeconds(900))
            .id(UUID.randomUUID().toString())
            .claim("token_use", "tenant_access")
            .claim("userId", principal.userId().toString())
            .claim("tenantId", principal.tenantId().toString())
            .claim("roles", List.of("Admin"))
            .claim("permissions", List.of("BILL_MANAGE"))
            .claims(change)
            .build();
    var key = new SecretKeySpec(Base64.getDecoder().decode(secret), "HmacSHA256");
    return new NimbusJwtEncoder(new ImmutableSecret<>(key))
        .encode(JwtEncoderParameters.from(JwsHeader.with(algorithm).type("JWT").build(), claims))
        .getTokenValue();
  }

  private static String randomSecret() {
    byte[] bytes = new byte[64];
    new SecureRandom().nextBytes(bytes);
    return Base64.getEncoder().encodeToString(bytes);
  }
}
