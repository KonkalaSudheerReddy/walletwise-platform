package com.walletwise.analytics;

import com.walletwise.budget.BudgetDtos.BudgetResponse;
import com.walletwise.budget.BudgetService;
import com.walletwise.common.ApiException;
import com.walletwise.user.AppUser;
import com.walletwise.user.CurrentUser;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Types;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.Currency;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MonthlyAnalyticsService {
  private final NamedParameterJdbcTemplate jdbc;
  private final CurrentUser currentUser;
  private final BudgetService budgets;
  private final Clock clock;

  public MonthlyAnalyticsService(
      NamedParameterJdbcTemplate jdbc,
      CurrentUser currentUser,
      BudgetService budgets,
      Clock clock) {
    this.jdbc = jdbc;
    this.currentUser = currentUser;
    this.budgets = budgets;
    this.clock = clock;
  }

  @Transactional(readOnly = true)
  public MonthlyAnalyticsResponse monthly(String requestedMonth, String requestedCurrency) {
    AppUser user = currentUser.require();
    YearMonth month = requestedMonth == null ? YearMonth.now(clock) : parseMonth(requestedMonth);
    String currency =
        requestedCurrency == null ? user.getPreferredCurrency() : requestedCurrency.toUpperCase();
    try {
      Currency.getInstance(currency);
    } catch (IllegalArgumentException exception) {
      throw ApiException.unprocessable(
          "invalid_currency", "Currency must be a valid ISO 4217 code");
    }
    Instant start = month.atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant();
    Instant end = month.plusMonths(1).atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant();
    MapSqlParameterSource parameters = parameters(user.getId(), currency, start, end);

    Map<String, Object> totals =
        jdbc.queryForMap(
            """
                select
                    coalesce(sum(case when le.type = 'INCOME' then le.amount else 0 end), 0) as income,
                    coalesce(sum(case when le.type = 'EXPENSE' then le.amount else 0 end), 0) as expense,
                    count(*) as transaction_count
                from ledger_entries le
                join wallets w on w.id = le.wallet_id
                where le.owner_id = :ownerId and w.currency = :currency
                  and le.occurred_at >= :start and le.occurred_at < :end
                """,
            parameters);
    BigDecimal income = decimal(totals.get("income"));
    BigDecimal expense = decimal(totals.get("expense"));
    long count = ((Number) totals.get("transaction_count")).longValue();

    BigDecimal opening = balanceAt(user.getId(), currency, start);
    BigDecimal closing = balanceAt(user.getId(), currency, end);
    List<CategoryAmount> expenseByCategory = categoryBreakdown(parameters, "EXPENSE");
    List<CategoryAmount> incomeByCategory = categoryBreakdown(parameters, "INCOME");
    List<TrendPoint> trend =
        jdbc.query(
            """
                select (le.occurred_at at time zone 'UTC')::date as day, coalesce(sum(le.amount), 0) as amount
                from ledger_entries le join wallets w on w.id = le.wallet_id
                where le.owner_id = :ownerId and w.currency = :currency and le.type = 'EXPENSE'
                  and le.occurred_at >= :start and le.occurred_at < :end
                group by day order by day
                """,
            parameters,
            (rs, row) ->
                new TrendPoint(rs.getDate("day").toLocalDate(), rs.getBigDecimal("amount")));

    YearMonth previous = month.minusMonths(1);
    MapSqlParameterSource previousParameters =
        parameters(
            user.getId(),
            currency,
            previous.atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant(),
            month.atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant());
    BigDecimal previousExpense =
        decimal(
            jdbc.queryForObject(
                """
                select coalesce(sum(le.amount), 0)
                from ledger_entries le join wallets w on w.id = le.wallet_id
                where le.owner_id = :ownerId and w.currency = :currency and le.type = 'EXPENSE'
                  and le.occurred_at >= :start and le.occurred_at < :end
                """,
                previousParameters,
                BigDecimal.class));
    BigDecimal comparison =
        previousExpense.signum() == 0
            ? null
            : expense
                .subtract(previousExpense)
                .multiply(BigDecimal.valueOf(100))
                .divide(previousExpense, 2, RoundingMode.HALF_UP);

    List<BudgetResponse> budgetUtilization =
        currency.equals(user.getPreferredCurrency()) ? budgets.list(month.toString()) : List.of();
    return new MonthlyAnalyticsResponse(
        month.toString(),
        currency,
        income,
        expense,
        income.subtract(expense),
        opening,
        closing,
        count,
        previousExpense,
        comparison,
        expenseByCategory,
        incomeByCategory,
        expenseByCategory.stream().limit(5).toList(),
        trend,
        budgetUtilization,
        Instant.now(clock));
  }

  private BigDecimal balanceAt(UUID ownerId, String currency, Instant boundary) {
    MapSqlParameterSource params =
        new MapSqlParameterSource()
            .addValue("ownerId", ownerId)
            .addValue("currency", currency)
            .addValue("boundary", boundary.atOffset(ZoneOffset.UTC), Types.TIMESTAMP_WITH_TIMEZONE);
    return decimal(
        jdbc.queryForObject(
            """
                select coalesce(sum(case when le.direction = 'CREDIT' then le.amount else -le.amount end), 0)
                from ledger_entries le join wallets w on w.id = le.wallet_id
                where le.owner_id = :ownerId and w.currency = :currency and le.occurred_at < :boundary
                """,
            params,
            BigDecimal.class));
  }

  private List<CategoryAmount> categoryBreakdown(MapSqlParameterSource params, String type) {
    MapSqlParameterSource values =
        new MapSqlParameterSource(params.getValues()).addValue("type", type);
    return jdbc.query(
        """
                select c.id, c.name, coalesce(sum(le.amount), 0) as amount
                from ledger_entries le
                join wallets w on w.id = le.wallet_id
                join categories c on c.id = le.category_id
                where le.owner_id = :ownerId and w.currency = :currency and le.type = :type
                  and le.occurred_at >= :start and le.occurred_at < :end
                group by c.id, c.name order by amount desc, c.name
                """,
        values,
        (rs, row) ->
            new CategoryAmount(
                rs.getObject("id", UUID.class), rs.getString("name"), rs.getBigDecimal("amount")));
  }

  private static MapSqlParameterSource parameters(
      UUID ownerId, String currency, Instant start, Instant end) {
    return new MapSqlParameterSource()
        .addValue("ownerId", ownerId)
        .addValue("currency", currency)
        .addValue("start", start.atOffset(ZoneOffset.UTC), Types.TIMESTAMP_WITH_TIMEZONE)
        .addValue("end", end.atOffset(ZoneOffset.UTC), Types.TIMESTAMP_WITH_TIMEZONE);
  }

  private static BigDecimal decimal(Object value) {
    return value == null
        ? BigDecimal.ZERO.setScale(4)
        : new BigDecimal(value.toString()).setScale(4, RoundingMode.HALF_UP);
  }

  private static YearMonth parseMonth(String value) {
    try {
      return YearMonth.parse(value);
    } catch (DateTimeParseException exception) {
      throw ApiException.unprocessable("invalid_month", "Month must use YYYY-MM format");
    }
  }

  public record CategoryAmount(UUID categoryId, String categoryName, BigDecimal amount) {}

  public record TrendPoint(LocalDate date, BigDecimal amount) {}

  public record MonthlyAnalyticsResponse(
      String month,
      String currency,
      BigDecimal totalIncome,
      BigDecimal totalExpense,
      BigDecimal netCashFlow,
      BigDecimal openingBalance,
      BigDecimal closingBalance,
      long transactionCount,
      BigDecimal previousMonthExpense,
      BigDecimal previousMonthComparisonPercent,
      List<CategoryAmount> expenseByCategory,
      List<CategoryAmount> incomeByCategory,
      List<CategoryAmount> topExpenseCategories,
      List<TrendPoint> spendingTrend,
      List<BudgetResponse> budgetUtilization,
      Instant generatedAt) {}
}
