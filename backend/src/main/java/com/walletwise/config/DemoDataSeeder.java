package com.walletwise.config;

import com.walletwise.audit.AuditLog;
import com.walletwise.audit.AuditLogRepository;
import com.walletwise.auth.AuthService;
import com.walletwise.budget.Budget;
import com.walletwise.budget.BudgetAlertService;
import com.walletwise.budget.BudgetRepository;
import com.walletwise.category.Category;
import com.walletwise.category.CategoryRepository;
import com.walletwise.ledger.LedgerEntry;
import com.walletwise.ledger.LedgerEntryRepository;
import com.walletwise.transfer.IdempotencyRecord;
import com.walletwise.transfer.IdempotencyRepository;
import com.walletwise.transfer.Transfer;
import com.walletwise.transfer.TransferRepository;
import com.walletwise.user.AppUser;
import com.walletwise.user.UserRepository;
import com.walletwise.user.UserRole;
import com.walletwise.wallet.Wallet;
import com.walletwise.wallet.WalletRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.UUID;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DemoDataSeeder implements ApplicationRunner {
  public static final String DEMO_EMAIL = "demo@walletwise.app";
  public static final String DEMO_PASSWORD = "Demo@12345";
  private static final UUID DEMO_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000101");
  private final AppProperties properties;
  private final UserRepository users;
  private final WalletRepository wallets;
  private final CategoryRepository categories;
  private final LedgerEntryRepository entries;
  private final TransferRepository transfers;
  private final IdempotencyRepository idempotency;
  private final BudgetRepository budgets;
  private final AuditLogRepository auditLogs;
  private final PasswordEncoder passwords;
  private final BudgetAlertService alerts;
  private final Clock clock;
  private final JdbcTemplate jdbc;

  public DemoDataSeeder(
      AppProperties properties,
      UserRepository users,
      WalletRepository wallets,
      CategoryRepository categories,
      LedgerEntryRepository entries,
      TransferRepository transfers,
      IdempotencyRepository idempotency,
      BudgetRepository budgets,
      AuditLogRepository auditLogs,
      PasswordEncoder passwords,
      BudgetAlertService alerts,
      Clock clock,
      JdbcTemplate jdbc) {
    this.properties = properties;
    this.users = users;
    this.wallets = wallets;
    this.categories = categories;
    this.entries = entries;
    this.transfers = transfers;
    this.idempotency = idempotency;
    this.budgets = budgets;
    this.auditLogs = auditLogs;
    this.passwords = passwords;
    this.alerts = alerts;
    this.clock = clock;
    this.jdbc = jdbc;
  }

  @Override
  @Transactional
  public void run(ApplicationArguments args) {
    seedAdminIfConfigured();
    if (!properties.demoSeedEnabled()) return;

    resetDemoData();

    Instant now = Instant.now(clock);
    YearMonth oldestMonth = YearMonth.now(clock).minusMonths(2);
    Instant createdAt =
        oldestMonth.atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant().minusSeconds(1);
    AppUser demo =
        users.save(
            new AppUser(
                DEMO_USER_ID,
                "WalletWise Demo",
                DEMO_EMAIL,
                passwords.encode(DEMO_PASSWORD),
                UserRole.USER,
                true,
                "USD",
                createdAt));
    Wallet checking =
        wallets.save(
            new Wallet(
                UUID.fromString("00000000-0000-0000-0000-000000000201"),
                demo,
                "Everyday Checking",
                Wallet.Type.BANK,
                "USD",
                createdAt));
    Wallet cash =
        wallets.save(
            new Wallet(
                UUID.fromString("00000000-0000-0000-0000-000000000202"),
                demo,
                "Cash Wallet",
                Wallet.Type.CASH,
                "USD",
                createdAt));
    Wallet savings =
        wallets.save(
            new Wallet(
                UUID.fromString("00000000-0000-0000-0000-000000000203"),
                demo,
                "Emergency Savings",
                Wallet.Type.SAVINGS,
                "USD",
                createdAt));

    Instant openingTime = createdAt.plusSeconds(1);
    opening(cash, demo, amount("200"), openingTime);
    opening(savings, demo, amount("2500"), openingTime);
    Category salary = category("salary", Category.Type.INCOME);
    Category housing = category("housing", Category.Type.EXPENSE);
    Category groceries = category("groceries", Category.Type.EXPENSE);
    Category dining = category("dining", Category.Type.EXPENSE);
    Category utilities = category("utilities", Category.Type.EXPENSE);

    for (int offset = -2; offset <= 0; offset++) {
      YearMonth month = YearMonth.now(clock).plusMonths(offset);
      record(
          checking,
          demo,
          LedgerEntry.Type.INCOME,
          LedgerEntry.Direction.CREDIT,
          amount("5200"),
          salary,
          "Monthly salary",
          occurred(month, 1, 9, now),
          null);
      record(
          checking,
          demo,
          LedgerEntry.Type.EXPENSE,
          LedgerEntry.Direction.DEBIT,
          amount("1600"),
          housing,
          "Rent",
          occurred(month, 3, 10, now),
          null);
      record(
          checking,
          demo,
          LedgerEntry.Type.EXPENSE,
          LedgerEntry.Direction.DEBIT,
          amount(offset == 0 ? "480" : "430"),
          groceries,
          "Groceries and household supplies",
          occurred(month, 10, 18, now),
          null);
      record(
          checking,
          demo,
          LedgerEntry.Type.EXPENSE,
          LedgerEntry.Direction.DEBIT,
          amount(offset == 0 ? "260" : "210"),
          dining,
          "Dining and coffee",
          occurred(month, 15, 20, now),
          null);
      record(
          checking,
          demo,
          LedgerEntry.Type.EXPENSE,
          LedgerEntry.Direction.DEBIT,
          amount("185"),
          utilities,
          "Utilities",
          occurred(month, 20, 12, now),
          null);
    }

    Instant transferTime = now.minusSeconds(60);
    String transferKey = "demo-seed-transfer-v1";
    Transfer transfer =
        transfers.save(
            new Transfer(
                UUID.fromString("00000000-0000-0000-0000-000000000301"),
                demo,
                checking,
                savings,
                amount("750"),
                "USD",
                "Monthly savings",
                transferKey,
                transferTime));
    checking.debit(amount("750"), transferTime);
    savings.credit(amount("750"), transferTime);
    record(
        checking,
        demo,
        LedgerEntry.Type.TRANSFER_OUT,
        LedgerEntry.Direction.DEBIT,
        amount("750"),
        null,
        "Monthly savings",
        transferTime,
        transfer.getId(),
        false);
    record(
        savings,
        demo,
        LedgerEntry.Type.TRANSFER_IN,
        LedgerEntry.Direction.CREDIT,
        amount("750"),
        null,
        "Monthly savings",
        transferTime,
        transfer.getId(),
        false);
    transfer.complete(transferTime);
    IdempotencyRecord seedIdempotency =
        idempotency.save(
            new IdempotencyRecord(
                UUID.fromString("00000000-0000-0000-0000-000000000302"),
                demo.getId(),
                "CREATE_TRANSFER",
                transferKey,
                AuthService.hash("demo-seed"),
                now,
                now.plusSeconds(30L * 86400)));
    seedIdempotency.complete(201, transfer.getId());

    YearMonth current = YearMonth.now(clock);
    budgets.save(
        new Budget(
            UUID.fromString("00000000-0000-0000-0000-000000000401"),
            demo,
            groceries,
            current.atDay(1),
            amount("600"),
            80,
            now));
    budgets.save(
        new Budget(
            UUID.fromString("00000000-0000-0000-0000-000000000402"),
            demo,
            dining,
            current.atDay(1),
            amount("350"),
            80,
            now));
    budgets.save(
        new Budget(
            UUID.fromString("00000000-0000-0000-0000-000000000403"),
            demo,
            groceries,
            current.minusMonths(1).atDay(1),
            amount("550"),
            80,
            now));
    auditLogs.save(
        new AuditLog(
            UUID.randomUUID(),
            demo.getId(),
            "DEMO_DATA_SEEDED",
            "USER",
            demo.getId(),
            "SUCCESS",
            now,
            "demo-seed",
            null,
            null,
            "{}"));
    entries.flush();
    budgets.flush();
    alerts.runCurrentMonth();
  }

  private void seedAdminIfConfigured() {
    String email = properties.adminEmail();
    String password = properties.adminPassword();
    if (email == null || email.isBlank() || password == null || password.isBlank()) return;
    String normalized = AuthService.normalizeEmail(email);
    if (users.existsByEmailNormalized(normalized)) return;
    if (password.length() < 12)
      throw new IllegalStateException("APP_ADMIN_PASSWORD must be at least 12 characters");
    Instant now = Instant.now(clock);
    users.save(
        new AppUser(
            UUID.randomUUID(),
            "WalletWise Administrator",
            normalized,
            passwords.encode(password),
            UserRole.ADMIN,
            true,
            "USD",
            now));
  }

  private void resetDemoData() {
    jdbc.update("delete from notifications where owner_id = ?", DEMO_USER_ID);
    jdbc.update("delete from audit_logs where actor_user_id = ?", DEMO_USER_ID);
    jdbc.update("delete from refresh_tokens where user_id = ?", DEMO_USER_ID);
    jdbc.update("delete from idempotency_records where owner_id = ?", DEMO_USER_ID);
    jdbc.update("delete from ledger_entries where owner_id = ?", DEMO_USER_ID);
    jdbc.update("delete from transfers where owner_id = ?", DEMO_USER_ID);
    jdbc.update("delete from budgets where owner_id = ?", DEMO_USER_ID);
    jdbc.update("delete from wallets where owner_id = ?", DEMO_USER_ID);
    jdbc.update("delete from app_users where id = ?", DEMO_USER_ID);
  }

  private Category category(String normalizedName, Category.Type type) {
    return categories.findByNormalizedNameAndType(normalizedName, type).orElseThrow();
  }

  private void opening(Wallet wallet, AppUser user, BigDecimal value, Instant occurredAt) {
    wallet.credit(value, occurredAt);
    record(
        wallet,
        user,
        LedgerEntry.Type.OPENING_BALANCE,
        LedgerEntry.Direction.CREDIT,
        value,
        null,
        "Opening balance",
        occurredAt,
        null,
        false);
  }

  private void record(
      Wallet wallet,
      AppUser user,
      LedgerEntry.Type type,
      LedgerEntry.Direction direction,
      BigDecimal value,
      Category category,
      String description,
      Instant occurredAt,
      UUID transferId) {
    record(
        wallet, user, type, direction, value, category, description, occurredAt, transferId, true);
  }

  private void record(
      Wallet wallet,
      AppUser user,
      LedgerEntry.Type type,
      LedgerEntry.Direction direction,
      BigDecimal value,
      Category category,
      String description,
      Instant occurredAt,
      UUID transferId,
      boolean mutateBalance) {
    if (mutateBalance) {
      if (direction == LedgerEntry.Direction.CREDIT) wallet.credit(value, occurredAt);
      else wallet.debit(value, occurredAt);
    }
    entries.save(
        new LedgerEntry(
            UUID.randomUUID(),
            wallet,
            user,
            type,
            direction,
            value,
            category,
            description,
            occurredAt,
            transferId,
            wallet.getCurrentBalance(),
            occurredAt));
  }

  private static BigDecimal amount(String value) {
    return new BigDecimal(value).setScale(4, RoundingMode.UNNECESSARY);
  }

  private static Instant occurred(YearMonth month, int day, int hour, Instant now) {
    int safeDay = Math.min(day, month.lengthOfMonth());
    Instant candidate =
        LocalDateTime.of(month.atDay(safeDay), java.time.LocalTime.of(hour, 0))
            .toInstant(ZoneOffset.UTC);
    Instant latestAllowed = now.minusSeconds((long) (32 - day) * 600);
    return candidate.isAfter(latestAllowed) ? latestAllowed : candidate;
  }
}
