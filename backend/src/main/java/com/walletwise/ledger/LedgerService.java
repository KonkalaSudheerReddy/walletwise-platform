package com.walletwise.ledger;

import com.walletwise.audit.AuditService;
import com.walletwise.category.Category;
import com.walletwise.category.CategoryRepository;
import com.walletwise.common.ApiException;
import com.walletwise.common.PageResponse;
import com.walletwise.ledger.LedgerDtos.AdjustmentRequest;
import com.walletwise.ledger.LedgerDtos.CreateEntryRequest;
import com.walletwise.ledger.LedgerDtos.LedgerEntryResponse;
import com.walletwise.user.AppUser;
import com.walletwise.user.CurrentUser;
import com.walletwise.wallet.Wallet;
import com.walletwise.wallet.WalletRepository;
import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LedgerService {
  private static final Set<String> SORTABLE = Set.of("occurredAt", "createdAt", "amount", "type");
  private final LedgerEntryRepository entries;
  private final WalletRepository wallets;
  private final CategoryRepository categories;
  private final CurrentUser currentUser;
  private final Clock clock;
  private final ApplicationEventPublisher events;
  private final AuditService audit;

  public LedgerService(
      LedgerEntryRepository entries,
      WalletRepository wallets,
      CategoryRepository categories,
      CurrentUser currentUser,
      Clock clock,
      ApplicationEventPublisher events,
      AuditService audit) {
    this.entries = entries;
    this.wallets = wallets;
    this.categories = categories;
    this.currentUser = currentUser;
    this.clock = clock;
    this.events = events;
    this.audit = audit;
  }

  @Transactional
  public LedgerEntryResponse income(CreateEntryRequest request) {
    return createCategorized(
        request, LedgerEntry.Type.INCOME, LedgerEntry.Direction.CREDIT, Category.Type.INCOME);
  }

  @Transactional
  public LedgerEntryResponse expense(CreateEntryRequest request) {
    LedgerEntryResponse response =
        createCategorized(
            request, LedgerEntry.Type.EXPENSE, LedgerEntry.Direction.DEBIT, Category.Type.EXPENSE);
    events.publishEvent(
        new ExpenseRecordedEvent(currentUser.id(), request.categoryId(), response.occurredAt()));
    return response;
  }

  @Transactional
  public LedgerEntryResponse adjustment(AdjustmentRequest request) {
    AppUser user = currentUser.require();
    Wallet wallet = lockWallet(request.walletId(), user.getId());
    ensureActive(wallet);
    BigDecimal amount = money(request.amount());
    Category category =
        request.categoryId() == null
            ? null
            : categories
                .findById(request.categoryId())
                .orElseThrow(() -> ApiException.notFound("Category"));
    Instant now = Instant.now(clock);
    if (request.direction() == LedgerEntry.Direction.DEBIT) {
      ensureCanDebit(wallet, amount);
      wallet.debit(amount, now);
    } else {
      wallet.credit(amount, now);
    }
    LedgerEntry entry =
        write(
            wallet,
            user,
            LedgerEntry.Type.ADJUSTMENT,
            request.direction(),
            amount,
            category,
            normalizeDescription(request.description()),
            occurredAt(request.occurredAt(), now),
            null,
            now);
    audit.success(user.getId(), "LEDGER_ADJUSTED", "LEDGER_ENTRY", entry.getId());
    return LedgerEntryResponse.from(entry);
  }

  @Transactional(readOnly = true)
  public LedgerEntryResponse get(UUID id) {
    return entries
        .findByIdAndOwnerId(id, currentUser.id())
        .map(LedgerEntryResponse::from)
        .orElseThrow(() -> ApiException.notFound("Transaction"));
  }

  @Transactional(readOnly = true)
  public PageResponse<LedgerEntryResponse> search(
      Filter filter, int page, int size, String sort, String direction) {
    UUID ownerId = currentUser.id();
    String sortField = SORTABLE.contains(sort) ? sort : "occurredAt";
    Sort.Direction sortDirection =
        "asc".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC;
    Page<LedgerEntryResponse> result =
        entries
            .findAll(
                specification(ownerId, filter),
                PageRequest.of(
                    Math.max(page, 0),
                    Math.min(Math.max(size, 1), 100),
                    Sort.by(sortDirection, sortField)))
            .map(LedgerEntryResponse::from);
    return PageResponse.from(result);
  }

  public LedgerEntry writeTransfer(
      Wallet wallet,
      AppUser owner,
      LedgerEntry.Type type,
      LedgerEntry.Direction direction,
      BigDecimal amount,
      String note,
      Instant occurredAt,
      UUID transferId,
      Instant now) {
    return write(wallet, owner, type, direction, amount, null, note, occurredAt, transferId, now);
  }

  public LedgerEntry writeOpeningBalance(
      Wallet wallet, AppUser owner, BigDecimal amount, Instant now) {
    wallet.credit(amount, now);
    return write(
        wallet,
        owner,
        LedgerEntry.Type.OPENING_BALANCE,
        LedgerEntry.Direction.CREDIT,
        amount,
        null,
        "Opening balance",
        now,
        null,
        now);
  }

  private LedgerEntryResponse createCategorized(
      CreateEntryRequest request,
      LedgerEntry.Type type,
      LedgerEntry.Direction direction,
      Category.Type expectedCategory) {
    AppUser user = currentUser.require();
    Wallet wallet = lockWallet(request.walletId(), user.getId());
    ensureActive(wallet);
    Category category =
        categories
            .findById(request.categoryId())
            .filter(Category::isActive)
            .orElseThrow(() -> ApiException.notFound("Category"));
    if (category.getType() != expectedCategory) {
      throw ApiException.unprocessable(
          "category_type_mismatch", "Category does not match the transaction type");
    }
    BigDecimal amount = money(request.amount());
    Instant now = Instant.now(clock);
    if (direction == LedgerEntry.Direction.DEBIT) {
      ensureCanDebit(wallet, amount);
      wallet.debit(amount, now);
    } else {
      wallet.credit(amount, now);
    }
    LedgerEntry entry =
        write(
            wallet,
            user,
            type,
            direction,
            amount,
            category,
            normalizeDescription(request.description()),
            occurredAt(request.occurredAt(), now),
            null,
            now);
    audit.success(
        user.getId(),
        type == LedgerEntry.Type.INCOME ? "INCOME_RECORDED" : "EXPENSE_RECORDED",
        "LEDGER_ENTRY",
        entry.getId());
    return LedgerEntryResponse.from(entry);
  }

  private LedgerEntry write(
      Wallet wallet,
      AppUser owner,
      LedgerEntry.Type type,
      LedgerEntry.Direction direction,
      BigDecimal amount,
      Category category,
      String description,
      Instant occurredAt,
      UUID transferId,
      Instant now) {
    return entries.save(
        new LedgerEntry(
            UUID.randomUUID(),
            wallet,
            owner,
            type,
            direction,
            amount,
            category,
            description,
            occurredAt,
            transferId,
            wallet.getCurrentBalance(),
            now));
  }

  private Wallet lockWallet(UUID id, UUID ownerId) {
    return wallets
        .findOwnedByIdForUpdate(id, ownerId)
        .orElseThrow(() -> ApiException.notFound("Wallet"));
  }

  public static BigDecimal money(BigDecimal amount) {
    if (amount == null
        || amount.signum() <= 0
        || amount.scale() > 4
        || amount.precision() - amount.scale() > 15) {
      throw ApiException.unprocessable(
          "invalid_amount", "Amount must be positive with at most four decimal places");
    }
    return amount.setScale(4, RoundingMode.UNNECESSARY);
  }

  public static void ensureCanDebit(Wallet wallet, BigDecimal amount) {
    if (wallet.getType() != Wallet.Type.CREDIT
        && wallet.getCurrentBalance().compareTo(amount) < 0) {
      throw ApiException.unprocessable("insufficient_balance", "Wallet has insufficient balance");
    }
  }

  private static void ensureActive(Wallet wallet) {
    if (wallet.isArchived())
      throw ApiException.conflict("wallet_archived", "Archived wallets cannot accept transactions");
  }

  private static Instant occurredAt(Instant requested, Instant now) {
    Instant value = requested == null ? now : requested;
    if (value.isAfter(now.plusSeconds(300))) {
      throw ApiException.unprocessable(
          "future_transaction", "Transaction time cannot be more than five minutes in the future");
    }
    return value;
  }

  private static String normalizeDescription(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }

  private static Specification<LedgerEntry> specification(UUID ownerId, Filter filter) {
    return (root, query, builder) -> {
      List<Predicate> predicates = new ArrayList<>();
      predicates.add(builder.equal(root.get("owner").get("id"), ownerId));
      if (filter.walletId() != null)
        predicates.add(builder.equal(root.get("wallet").get("id"), filter.walletId()));
      if (filter.type() != null) predicates.add(builder.equal(root.get("type"), filter.type()));
      if (filter.categoryId() != null)
        predicates.add(builder.equal(root.get("category").get("id"), filter.categoryId()));
      if (filter.startDate() != null)
        predicates.add(builder.greaterThanOrEqualTo(root.get("occurredAt"), filter.startDate()));
      if (filter.endDate() != null)
        predicates.add(builder.lessThan(root.get("occurredAt"), filter.endDate()));
      if (filter.minAmount() != null)
        predicates.add(builder.greaterThanOrEqualTo(root.get("amount"), filter.minAmount()));
      if (filter.maxAmount() != null)
        predicates.add(builder.lessThanOrEqualTo(root.get("amount"), filter.maxAmount()));
      if (filter.description() != null && !filter.description().isBlank()) {
        predicates.add(
            builder.like(
                builder.lower(root.get("description")),
                "%" + filter.description().trim().toLowerCase(Locale.ROOT) + "%"));
      }
      return builder.and(predicates.toArray(Predicate[]::new));
    };
  }

  public record Filter(
      UUID walletId,
      LedgerEntry.Type type,
      UUID categoryId,
      Instant startDate,
      Instant endDate,
      BigDecimal minAmount,
      BigDecimal maxAmount,
      String description) {}
}
