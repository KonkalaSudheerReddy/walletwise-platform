package com.walletwise.auth;

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
  Optional<RefreshToken> findByTokenHash(String tokenHash);

  @Modifying
  @Query(
      "update RefreshToken t set t.revokedAt = :now where t.familyId = :familyId and t.revokedAt is null")
  int revokeFamily(@Param("familyId") UUID familyId, @Param("now") Instant now);

  @Modifying
  @Query(
      "update RefreshToken t set t.revokedAt = :now where t.user.id = :userId and t.revokedAt is null")
  int revokeAllForUser(@Param("userId") UUID userId, @Param("now") Instant now);
}
