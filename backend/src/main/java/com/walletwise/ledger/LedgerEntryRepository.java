package com.walletwise.ledger;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LedgerEntryRepository
    extends JpaRepository<LedgerEntry, UUID>, JpaSpecificationExecutor<LedgerEntry> {
  @EntityGraph(attributePaths = {"wallet", "category"})
  Optional<LedgerEntry> findByIdAndOwnerId(UUID id, UUID ownerId);

  boolean existsByWalletId(UUID walletId);

  @EntityGraph(attributePaths = {"wallet", "category"})
  Page<LedgerEntry> findAllByOwnerId(UUID ownerId, Pageable pageable);

  @Override
  @EntityGraph(attributePaths = {"wallet", "category"})
  Page<LedgerEntry> findAll(Specification<LedgerEntry> specification, Pageable pageable);

  @Query(
      "select coalesce(sum(e.amount), 0) from LedgerEntry e where e.owner.id = :ownerId and e.category.id = :categoryId and e.type = :type and e.wallet.currency = e.owner.preferredCurrency and e.occurredAt >= :start and e.occurredAt < :end")
  java.math.BigDecimal sumExpenseByType(
      @Param("ownerId") UUID ownerId,
      @Param("categoryId") UUID categoryId,
      @Param("type") LedgerEntry.Type type,
      @Param("start") Instant start,
      @Param("end") Instant end);

  default java.math.BigDecimal sumExpense(
      UUID ownerId, UUID categoryId, Instant start, Instant end) {
    return sumExpenseByType(ownerId, categoryId, LedgerEntry.Type.EXPENSE, start, end);
  }

  @EntityGraph(attributePaths = {"wallet", "category"})
  List<LedgerEntry> findTop10ByWalletIdAndOwnerIdOrderByOccurredAtDesc(UUID walletId, UUID ownerId);
}
