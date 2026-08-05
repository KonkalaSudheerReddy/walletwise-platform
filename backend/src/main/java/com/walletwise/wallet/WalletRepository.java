package com.walletwise.wallet;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WalletRepository extends JpaRepository<Wallet, UUID> {
  List<Wallet> findAllByOwnerIdOrderByCreatedAtDesc(UUID ownerId);

  List<Wallet> findAllByOwnerIdAndArchivedFalseOrderByCreatedAtDesc(UUID ownerId);

  Optional<Wallet> findByIdAndOwnerId(UUID id, UUID ownerId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select w from Wallet w where w.id = :id and w.owner.id = :ownerId")
  Optional<Wallet> findOwnedByIdForUpdate(@Param("id") UUID id, @Param("ownerId") UUID ownerId);
}
