package com.walletwise.budget;

import com.walletwise.ledger.ExpenseRecordedEvent;
import com.walletwise.notification.Notification;
import com.walletwise.notification.NotificationRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BudgetAlertService {
  private final BudgetRepository budgets;
  private final BudgetService budgetService;
  private final NotificationRepository notifications;
  private final Clock clock;

  public BudgetAlertService(
      BudgetRepository budgets,
      BudgetService budgetService,
      NotificationRepository notifications,
      Clock clock) {
    this.budgets = budgets;
    this.budgetService = budgetService;
    this.notifications = notifications;
    this.clock = clock;
  }

  @EventListener
  @Transactional
  public void onExpense(ExpenseRecordedEvent event) {
    LocalDate period = YearMonth.from(event.occurredAt().atZone(java.time.ZoneOffset.UTC)).atDay(1);
    budgets
        .findAllByOwnerIdAndCategoryIdAndPeriodStart(event.ownerId(), event.categoryId(), period)
        .forEach(this::evaluate);
  }

  @Scheduled(cron = "0 5 0 * * *", zone = "UTC")
  @Transactional
  public void scheduledCurrentMonthEvaluation() {
    runCurrentMonth();
  }

  @Transactional
  public int runCurrentMonth() {
    List<Budget> current = budgets.findAllByPeriodStart(YearMonth.now(clock).atDay(1));
    Map<UUID, BigDecimal> spending = budgetService.spending(current);
    current.forEach(budget -> evaluate(budget, spending.get(budget.getId())));
    return current.size();
  }

  private void evaluate(Budget budget) {
    evaluate(budget, budgetService.spent(budget));
  }

  private void evaluate(Budget budget, BigDecimal spent) {
    BigDecimal limit = budget.getLimitAmount();
    Notification.Type type;
    String title;
    String message;
    int comparison = spent.compareTo(limit);
    if (comparison > 0) {
      type = Notification.Type.BUDGET_EXCEEDED;
      title = "Budget exceeded";
      message = budget.getCategory().getName() + " spending is over its monthly budget.";
    } else if (comparison == 0) {
      type = Notification.Type.BUDGET_REACHED;
      title = "Budget limit reached";
      message = budget.getCategory().getName() + " spending has reached its monthly budget.";
    } else {
      BigDecimal threshold =
          limit
              .multiply(BigDecimal.valueOf(budget.getAlertThresholdPercent()))
              .divide(BigDecimal.valueOf(100));
      if (spent.compareTo(threshold) < 0) return;
      type = Notification.Type.BUDGET_APPROACHING;
      title = "Budget approaching limit";
      message =
          budget.getCategory().getName()
              + " spending has reached "
              + budget.getAlertThresholdPercent()
              + "% of its monthly budget.";
    }
    notifications.insertBudgetNotification(
        UUID.randomUUID(),
        budget.getOwner().getId(),
        type.name(),
        title,
        message,
        budget.getId(),
        Instant.now(clock));
  }
}
