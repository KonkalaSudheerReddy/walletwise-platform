package com.walletwise.budget;

import com.walletwise.audit.AuditService;
import com.walletwise.budget.BudgetDtos.BudgetResponse;
import com.walletwise.budget.BudgetDtos.CreateBudgetRequest;
import com.walletwise.budget.BudgetDtos.UpdateBudgetRequest;
import com.walletwise.category.Category;
import com.walletwise.category.CategoryRepository;
import com.walletwise.common.ApiException;
import com.walletwise.ledger.LedgerEntryRepository;
import com.walletwise.ledger.LedgerService;
import com.walletwise.user.AppUser;
import com.walletwise.user.CurrentUser;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BudgetService {
  private final BudgetRepository budgets;
  private final CategoryRepository categories;
  private final LedgerEntryRepository entries;
  private final CurrentUser currentUser;
  private final Clock clock;
  private final AuditService audit;

  public BudgetService(
      BudgetRepository budgets,
      CategoryRepository categories,
      LedgerEntryRepository entries,
      CurrentUser currentUser,
      Clock clock,
      AuditService audit) {
    this.budgets = budgets;
    this.categories = categories;
    this.entries = entries;
    this.currentUser = currentUser;
    this.clock = clock;
    this.audit = audit;
  }

  @Transactional
  public BudgetResponse create(CreateBudgetRequest request) {
    AppUser user = currentUser.require();
    Category category = expenseCategory(request.categoryId());
    LocalDate period = parseMonth(request.month()).atDay(1);
    if (budgets.existsByOwnerIdAndCategoryIdAndPeriodStart(
        user.getId(), category.getId(), period)) {
      throw ApiException.conflict(
          "budget_already_exists", "A budget already exists for this category and month");
    }
    Instant now = Instant.now(clock);
    Budget budget =
        budgets.save(
            new Budget(
                UUID.randomUUID(),
                user,
                category,
                period,
                LedgerService.money(request.limitAmount()),
                request.alertThresholdPercent() == null ? 80 : request.alertThresholdPercent(),
                now));
    audit.success(user.getId(), "BUDGET_CREATED", "BUDGET", budget.getId());
    return response(budget);
  }

  @Transactional(readOnly = true)
  public List<BudgetResponse> list(String month) {
    UUID userId = currentUser.id();
    List<Budget> result =
        month == null
            ? budgets.findAllByOwnerIdOrderByPeriodStartDescCategoryNameAsc(userId)
            : budgets.findAllByOwnerIdAndPeriodStartOrderByCategoryName(
                userId, parseMonth(month).atDay(1));
    Map<UUID, BigDecimal> spending = spending(result);
    return result.stream().map(budget -> response(budget, spending.get(budget.getId()))).toList();
  }

  @Transactional(readOnly = true)
  public BudgetResponse get(UUID id) {
    return response(findOwned(id));
  }

  @Transactional
  public BudgetResponse update(UUID id, UpdateBudgetRequest request) {
    AppUser user = currentUser.require();
    Budget budget =
        budgets
            .findByIdAndOwnerId(id, user.getId())
            .orElseThrow(() -> ApiException.notFound("Budget"));
    budget.update(
        LedgerService.money(request.limitAmount()),
        request.alertThresholdPercent(),
        Instant.now(clock));
    audit.success(user.getId(), "BUDGET_UPDATED", "BUDGET", id);
    return response(budget);
  }

  @Transactional
  public void delete(UUID id) {
    AppUser user = currentUser.require();
    Budget budget =
        budgets
            .findByIdAndOwnerId(id, user.getId())
            .orElseThrow(() -> ApiException.notFound("Budget"));
    BigDecimal spent = spent(budget);
    LocalDate currentMonth = YearMonth.now(clock).atDay(1);
    if (!budget.getPeriodStart().isAfter(currentMonth) && spent.signum() != 0) {
      throw ApiException.conflict(
          "budget_in_use", "A current or past budget with spending cannot be deleted");
    }
    budgets.delete(budget);
    audit.success(user.getId(), "BUDGET_DELETED", "BUDGET", id);
  }

  public BudgetResponse response(Budget budget) {
    return response(budget, spent(budget));
  }

  BudgetResponse response(Budget budget, BigDecimal spentAmount) {
    BigDecimal spent = spentAmount.setScale(4, RoundingMode.HALF_UP);
    BigDecimal remaining =
        budget.getLimitAmount().subtract(spent).setScale(4, RoundingMode.HALF_UP);
    BigDecimal utilization =
        spent
            .multiply(BigDecimal.valueOf(100))
            .divide(budget.getLimitAmount(), 2, RoundingMode.HALF_UP);
    return new BudgetResponse(
        budget.getId(),
        budget.getCategory().getId(),
        budget.getCategory().getName(),
        YearMonth.from(budget.getPeriodStart()).toString(),
        budget.getOwner().getPreferredCurrency(),
        budget.getLimitAmount(),
        budget.getAlertThresholdPercent(),
        spent,
        remaining,
        utilization,
        budget.getCreatedAt(),
        budget.getUpdatedAt());
  }

  public BigDecimal spent(Budget budget) {
    Instant start = budget.getPeriodStart().atStartOfDay(ZoneOffset.UTC).toInstant();
    Instant end = budget.getPeriodStart().plusMonths(1).atStartOfDay(ZoneOffset.UTC).toInstant();
    return entries.sumExpense(budget.getOwner().getId(), budget.getCategory().getId(), start, end);
  }

  public Map<UUID, BigDecimal> spending(List<Budget> budgetList) {
    if (budgetList.isEmpty()) return Map.of();
    Map<UUID, BigDecimal> result =
        budgets.summarizeSpend(budgetList.stream().map(Budget::getId).toList()).stream()
            .collect(
                Collectors.toMap(
                    BudgetRepository.BudgetSpend::getBudgetId,
                    BudgetRepository.BudgetSpend::getSpentAmount));
    return budgetList.stream()
        .map(Budget::getId)
        .collect(
            Collectors.toUnmodifiableMap(
                Function.identity(), id -> result.getOrDefault(id, BigDecimal.ZERO)));
  }

  private Budget findOwned(UUID id) {
    return budgets
        .findByIdAndOwnerId(id, currentUser.id())
        .orElseThrow(() -> ApiException.notFound("Budget"));
  }

  private Category expenseCategory(UUID id) {
    Category category =
        categories
            .findById(id)
            .filter(Category::isActive)
            .orElseThrow(() -> ApiException.notFound("Category"));
    if (category.getType() != Category.Type.EXPENSE) {
      throw ApiException.unprocessable(
          "category_type_mismatch", "Budgets require an expense category");
    }
    return category;
  }

  static YearMonth parseMonth(String month) {
    try {
      return YearMonth.parse(month);
    } catch (DateTimeParseException | NullPointerException exception) {
      throw ApiException.unprocessable("invalid_month", "Month must use YYYY-MM format");
    }
  }
}
