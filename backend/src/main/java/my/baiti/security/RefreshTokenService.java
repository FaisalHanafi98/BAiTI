package my.baiti.security;

import jakarta.persistence.EntityManager;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;
import my.baiti.people.User;
import my.baiti.people.UserAuthentication;
import my.baiti.people.UserIdentity;
import my.baiti.tenancy.TenantContext;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class RefreshTokenService {

  private final TokenLifetimes lifetimes;
  private final RefreshTokenRepository tokens;
  private final EntityManager entityManager;
  private final Clock clock;
  private final UserAuthentication users;
  private final TransactionTemplate transactions;
  private final SecureRandom random = new SecureRandom();

  public RefreshTokenService(
      RefreshTokenRepository tokens,
      EntityManager entityManager,
      Clock clock,
      UserAuthentication users,
      PlatformTransactionManager transactionManager,
      TokenLifetimes lifetimes) {
    this.tokens = tokens;
    this.entityManager = entityManager;
    this.clock = clock;
    this.users = users;
    this.lifetimes = lifetimes;
    this.transactions = new TransactionTemplate(transactionManager);
    this.transactions.setPropagationBehavior(
        org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW);
  }

  @Transactional
  public String create(UserIdentity user) {
    return persist(user, UUID.randomUUID());
  }

  public Rotation rotate(String raw) {
    if (TenantContext.current().isEmpty() || raw == null || !raw.matches("[A-Za-z0-9_-]{43}")) {
      throw new BadCredentialsException("Authentication failed");
    }
    Rotation result =
        transactions.execute(
            status -> {
              var found = tokens.findForUpdate(hash(raw));
              if (found.isEmpty()) {
                return null;
              }
              var token = found.orElseThrow();
              var now = clock.instant();
              if (token.getRotatedAt() != null) {
                tokens.revokeFamily(token.getFamilyId(), now);
                return null;
              }
              if (token.getRevokedAt() != null || !token.getExpiresAt().isAfter(now)) {
                return null;
              }
              var user = users.findActiveIdentity(token.getUser().getId());
              if (user.isEmpty()) {
                tokens.revokeUser(token.getUser().getId(), now);
                return null;
              }
              token.setRotatedAt(now);
              return new Rotation(
                  user.orElseThrow(), persist(user.orElseThrow(), token.getFamilyId()));
            });
    // Throw only after the revocation transaction commits; a replay must not roll it back.
    if (result == null) {
      throw new BadCredentialsException("Authentication failed");
    }
    return result;
  }

  @Transactional
  public void revokeUser(UUID user) {
    tokens.revokeUser(user, clock.instant());
  }

  private String persist(UserIdentity user, UUID family) {
    if (!TenantContext.current().filter(user.tenantId()::equals).isPresent()) {
      throw new BadCredentialsException("Authentication failed");
    }
    byte[] bytes = new byte[32];
    random.nextBytes(bytes);
    String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    var token = new RefreshToken();
    token.setUser(entityManager.getReference(User.class, user.userId()));
    token.setTokenHash(hash(raw));
    token.setFamilyId(family);
    token.setExpiresAt(clock.instant().plus(lifetimes.refresh()));
    tokens.saveAndFlush(token);
    return raw;
  }

  static String hash(String token) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException unavailable) {
      throw new IllegalStateException("SHA-256 is required");
    }
  }

  public record Rotation(UserIdentity user, String raw) {
    @Override
    public String toString() {
      return "Rotation[redacted]";
    }
  }
}
