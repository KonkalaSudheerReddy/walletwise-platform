package com.walletwise.transfer;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransferRepository extends JpaRepository<Transfer, UUID> {
  @EntityGraph(attributePaths = {"sourceWallet", "destinationWallet"})
  Optional<Transfer> findByIdAndOwnerId(UUID id, UUID ownerId);

  @EntityGraph(attributePaths = {"sourceWallet", "destinationWallet"})
  Page<Transfer> findAllByOwnerId(UUID ownerId, Pageable pageable);
}
