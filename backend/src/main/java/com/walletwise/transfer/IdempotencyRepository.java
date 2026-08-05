package com.walletwise.transfer;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IdempotencyRepository extends JpaRepository<IdempotencyRecord, UUID> {
  Optional<IdempotencyRecord> findByOwnerIdAndOperationAndIdempotencyKey(
      UUID ownerId, String operation, String key);

  long deleteByExpiresAtBefore(Instant cutoff);
}
