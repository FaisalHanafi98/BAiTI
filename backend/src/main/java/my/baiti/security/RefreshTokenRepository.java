package my.baiti.security;

import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select t from RefreshToken t where t.tokenHash = :hash")
  Optional<RefreshToken> findForUpdate(@Param("hash") String hash);

  @Modifying(flushAutomatically = true, clearAutomatically = true)
  @Query(
      "update RefreshToken t set t.revokedAt = :now, t.updatedAt = :now where t.familyId = :family and t.revokedAt is null")
  int revokeFamily(@Param("family") UUID family, @Param("now") Instant now);

  @Modifying(flushAutomatically = true, clearAutomatically = true)
  @Query(
      "update RefreshToken t set t.revokedAt = :now, t.updatedAt = :now where t.user.id = :user and t.revokedAt is null")
  int revokeUser(@Param("user") UUID user, @Param("now") Instant now);

  Optional<RefreshToken> findByTokenHash(String tokenHash);

  void deleteByExpiresAtBefore(Instant expiryThreshold);
}
