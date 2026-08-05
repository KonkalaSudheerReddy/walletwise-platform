package com.walletwise.wallet;

import com.walletwise.audit.AuditService;
import com.walletwise.common.ApiException;
import com.walletwise.ledger.LedgerDtos.LedgerEntryResponse;
import com.walletwise.ledger.LedgerEntryRepository;
import com.walletwise.ledger.LedgerService;
import com.walletwise.user.AppUser;
import com.walletwise.user.CurrentUser;
import com.walletwise.wallet.WalletDtos.CreateWalletRequest;
import com.walletwise.wallet.WalletDtos.UpdateWalletRequest;
import com.walletwise.wallet.WalletDtos.WalletResponse;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.Currency;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WalletService {
  private final WalletRepository wallets;
  private final LedgerEntryRepository entries;
  private final LedgerService ledger;
  private final CurrentUser currentUser;
  private final Clock clock;
  private final AuditService audit;

  public WalletService(
      WalletRepository wallets,
      LedgerEntryRepository entries,
      LedgerService ledger,
      CurrentUser currentUser,
      Clock clock,
      AuditService audit) {
    this.wallets = wallets;
    this.entries = entries;
    this.ledger = ledger;
    this.currentUser = currentUser;
    this.clock = clock;
    this.audit = audit;
  }

  @Transactional
  public WalletResponse create(CreateWalletRequest request) {
    AppUser user = currentUser.require();
    validateCurrency(request.currency());
    Instant now = Instant.now(clock);
    Wallet wallet =
        wallets.save(
            new Wallet(
                UUID.randomUUID(),
                user,
                request.name().trim(),
                request.type(),
                request.currency(),
                now));
    BigDecimal opening =
        request.openingBalance() == null ? BigDecimal.ZERO : request.openingBalance();
    if (opening.signum() > 0)
      ledger.writeOpeningBalance(wallet, user, LedgerService.money(opening), now);
    audit.success(user.getId(), "WALLET_CREATED", "WALLET", wallet.getId());
    return WalletResponse.from(wallet);
  }

  @Transactional(readOnly = true)
  public List<WalletResponse> list(boolean includeArchived) {
    UUID ownerId = currentUser.id();
    List<Wallet> owned =
        includeArchived
            ? wallets.findAllByOwnerIdOrderByCreatedAtDesc(ownerId)
            : wallets.findAllByOwnerIdAndArchivedFalseOrderByCreatedAtDesc(ownerId);
    return owned.stream().map(WalletResponse::from).toList();
  }

  @Transactional(readOnly = true)
  public WalletDetailResponse get(UUID id) {
    UUID ownerId = currentUser.id();
    Wallet wallet =
        wallets.findByIdAndOwnerId(id, ownerId).orElseThrow(() -> ApiException.notFound("Wallet"));
    List<LedgerEntryResponse> recent =
        entries.findTop10ByWalletIdAndOwnerIdOrderByOccurredAtDesc(id, ownerId).stream()
            .map(LedgerEntryResponse::from)
            .toList();
    return new WalletDetailResponse(WalletResponse.from(wallet), recent);
  }

  @Transactional
  public WalletResponse update(UUID id, UpdateWalletRequest request) {
    AppUser user = currentUser.require();
    Wallet wallet =
        wallets
            .findOwnedByIdForUpdate(id, user.getId())
            .orElseThrow(() -> ApiException.notFound("Wallet"));
    if (wallet.getCurrentBalance().signum() < 0 && request.type() != Wallet.Type.CREDIT) {
      throw ApiException.conflict(
          "negative_non_credit_balance",
          "A wallet with a negative balance must remain a credit wallet");
    }
    wallet.rename(request.name().trim(), request.type(), Instant.now(clock));
    audit.success(user.getId(), "WALLET_UPDATED", "WALLET", id);
    return WalletResponse.from(wallet);
  }

  @Transactional
  public WalletResponse archive(UUID id, boolean archived) {
    AppUser user = currentUser.require();
    Wallet wallet =
        wallets
            .findOwnedByIdForUpdate(id, user.getId())
            .orElseThrow(() -> ApiException.notFound("Wallet"));
    wallet.archive(archived, Instant.now(clock));
    audit.success(user.getId(), archived ? "WALLET_ARCHIVED" : "WALLET_RESTORED", "WALLET", id);
    return WalletResponse.from(wallet);
  }

  private static void validateCurrency(String currency) {
    try {
      Currency.getInstance(currency);
    } catch (IllegalArgumentException exception) {
      throw ApiException.unprocessable(
          "invalid_currency", "Currency must be a valid ISO 4217 code");
    }
  }

  public record WalletDetailResponse(
      WalletResponse wallet, List<LedgerEntryResponse> recentActivity) {}
}
