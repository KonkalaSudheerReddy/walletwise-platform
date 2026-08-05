package com.walletwise.budget;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BudgetRepository extends JpaRepository<Budget, UUID> {
  boolean existsByOwnerIdAndCategoryIdAndPeriodStart(
      UUID ownerId, UUID categoryId, LocalDate periodStart);

  @EntityGraph(attributePaths = {"owner", "category"})
  Optional<Budget> findByIdAndOwnerId(UUID id, UUID ownerId);

  @EntityGraph(attributePaths = {"owner", "category"})
  List<Budget> findAllByOwnerIdOrderByPeriodStartDescCategoryNameAsc(UUID ownerId);

  @EntityGraph(attributePaths = {"owner", "category"})
  List<Budget> findAllByOwnerIdAndPeriodStartOrderByCategoryName(
      UUID ownerId, LocalDate periodStart);

  @EntityGraph(attributePaths = {"owner", "category"})
  List<Budget> findAllByOwnerIdAndCategoryIdAndPeriodStart(
      UUID ownerId, UUID categoryId, LocalDate periodStart);

  @EntityGraph(attributePaths = {"owner", "category"})
  List<Budget> findAllByPeriodStart(LocalDate periodStart);

  @Query(
      value =
          """
          select b.id as "budgetId",
                 coalesce(sum(case when w.id is not null then le.amount else 0 end), 0) as "spentAmount"
          from budgets b
          join app_users u on u.id = b.owner_id
          left join ledger_entries le
            on le.owner_id = b.owner_id
           and le.category_id = b.category_id
           and le.type = 'EXPENSE'
           and le.occurred_at >= (b.period_start::timestamp at time zone 'UTC')
           and le.occurred_at < ((b.period_start + interval '1 month')::timestamp at time zone 'UTC')
          left join wallets w on w.id = le.wallet_id and w.currency = u.preferred_currency
          where b.id in (:budgetIds)
          group by b.id
          """,
      nativeQuery = true)
  List<BudgetSpend> summarizeSpend(@Param("budgetIds") Collection<UUID> budgetIds);

  interface BudgetSpend {
    UUID getBudgetId();

    BigDecimal getSpentAmount();
  }
}
