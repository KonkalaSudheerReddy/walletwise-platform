package com.walletwise.transfer;

import com.walletwise.audit.AuditService;
import com.walletwise.auth.AuthService;
import com.walletwise.common.ApiException;
import com.walletwise.common.PageResponse;
import com.walletwise.config.AppProperties;
import com.walletwise.ledger.LedgerEntry;
import com.walletwise.ledger.LedgerService;
import com.walletwise.transfer.TransferDtos.TransferRequest;
import com.walletwise.transfer.TransferDtos.TransferResponse;
import com.walletwise.user.AppUser;
import com.walletwise.user.CurrentUser;
import com.walletwise.wallet.Wallet;
import com.walletwise.wallet.WalletRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransferService {
  private static final String OPERATION = "CREATE_TRANSFER";
  private static final Pattern KEY_PATTERN = Pattern.compile("[A-Za-z0-9._:-]{8,128}");
  private final TransferRepository transfers;
  private final IdempotencyRepository idempotency;
  private final PostgresAdvisoryLock advisoryLock;
  private final WalletRepository wallets;
  private final LedgerService ledger;
  private final CurrentUser currentUser;
  private final AppProperties properties;
  private final Clock clock;
  private final AuditService audit;

  public TransferService(
      TransferRepository transfers,
      IdempotencyRepository idempotency,
      PostgresAdvisoryLock advisoryLock,
      WalletRepository wallets,
      LedgerService ledger,
      CurrentUser currentUser,
      AppProperties properties,
      Clock clock,
      AuditService audit) {
    this.transfers = transfers;
    this.idempotency = idempotency;
    this.advisoryLock = advisoryLock;
    this.wallets = wallets;
    this.ledger = ledger;
    this.currentUser = currentUser;
    this.properties = properties;
    this.clock = clock;
    this.audit = audit;
  }

  @Transactional
  public TransferResponse create(String key, TransferRequest request) {
    if (key == null || !KEY_PATTERN.matcher(key).matches()) {
      throw new ApiException(
          HttpStatus.BAD_REQUEST,
          "invalid_idempotency_key",
          "Idempotency-Key must be 8-128 safe characters");
    }
    AppUser user = currentUser.require();
    BigDecimal amount = LedgerService.money(request.amount());
    String note = request.note() == null || request.note().isBlank() ? null : request.note().trim();
    String requestHash =
        canonicalHash(request.sourceWalletId(), request.destinationWalletId(), amount, note);

    advisoryLock.acquire(user.getId() + ":" + OPERATION + ":" + key);
    IdempotencyRecord existing =
        idempotency
            .findByOwnerIdAndOperationAndIdempotencyKey(user.getId(), OPERATION, key)
            .orElse(null);
    if (existing != null) {
      if (!existing.getRequestHash().equals(requestHash)) {
        throw ApiException.conflict(
            "idempotency_key_reused", "Idempotency-Key was already used with a different request");
      }
      if (existing.getStatus() == IdempotencyRecord.Status.COMPLETED) {
        return transfers
            .findByIdAndOwnerId(existing.getResponseResourceId(), user.getId())
            .map(TransferResponse::from)
            .orElseThrow(
                () -> new IllegalStateException("Completed idempotency record lost its transfer"));
      }
      throw ApiException.conflict(
          "idempotency_in_progress", "A request with this Idempotency-Key is still processing");
    }

    Instant now = Instant.now(clock);
    IdempotencyRecord record =
        idempotency.save(
            new IdempotencyRecord(
                UUID.randomUUID(),
                user.getId(),
                OPERATION,
                key,
                requestHash,
                now,
                now.plus(properties.idempotencyRetention())));

    if (request.sourceWalletId().equals(request.destinationWalletId())) {
      throw ApiException.unprocessable(
          "same_wallet_transfer", "Source and destination wallets must differ");
    }
    Wallet first;
    Wallet second;
    if (request.sourceWalletId().compareTo(request.destinationWalletId()) < 0) {
      first = lock(request.sourceWalletId(), user.getId());
      second = lock(request.destinationWalletId(), user.getId());
    } else {
      first = lock(request.destinationWalletId(), user.getId());
      second = lock(request.sourceWalletId(), user.getId());
    }
    Wallet source = first.getId().equals(request.sourceWalletId()) ? first : second;
    Wallet destination = first.getId().equals(request.destinationWalletId()) ? first : second;
    if (source.isArchived() || destination.isArchived()) {
      throw ApiException.conflict(
          "wallet_archived", "Archived wallets cannot participate in transfers");
    }
    if (!source.getCurrency().equals(destination.getCurrency())) {
      throw ApiException.unprocessable(
          "currency_mismatch", "Transfers require wallets with the same currency");
    }
    LedgerService.ensureCanDebit(source, amount);

    Transfer transfer =
        transfers.save(
            new Transfer(
                UUID.randomUUID(),
                user,
                source,
                destination,
                amount,
                source.getCurrency(),
                note,
                key,
                now));
    source.debit(amount, now);
    destination.credit(amount, now);
    ledger.writeTransfer(
        source,
        user,
        LedgerEntry.Type.TRANSFER_OUT,
        LedgerEntry.Direction.DEBIT,
        amount,
        note,
        now,
        transfer.getId(),
        now);
    ledger.writeTransfer(
        destination,
        user,
        LedgerEntry.Type.TRANSFER_IN,
        LedgerEntry.Direction.CREDIT,
        amount,
        note,
        now,
        transfer.getId(),
        now);
    transfer.complete(now);
    audit.success(user.getId(), "TRANSFER_COMPLETED", "TRANSFER", transfer.getId());
    record.complete(HttpStatus.CREATED.value(), transfer.getId());
    return TransferResponse.from(transfer);
  }

  @Transactional(readOnly = true)
  public TransferResponse get(UUID id) {
    return transfers
        .findByIdAndOwnerId(id, currentUser.id())
        .map(TransferResponse::from)
        .orElseThrow(() -> ApiException.notFound("Transfer"));
  }

  @Transactional(readOnly = true)
  public PageResponse<TransferResponse> list(int page, int size) {
    return PageResponse.from(
        transfers
            .findAllByOwnerId(
                currentUser.id(),
                PageRequest.of(
                    Math.max(0, page),
                    Math.min(Math.max(1, size), 100),
                    Sort.by(Sort.Direction.DESC, "createdAt")))
            .map(TransferResponse::from));
  }

  @Scheduled(cron = "0 30 2 * * *", zone = "UTC")
  @Transactional
  public void deleteExpiredIdempotencyRecords() {
    idempotency.deleteByExpiresAtBefore(Instant.now(clock));
  }

  private Wallet lock(UUID id, UUID ownerId) {
    return wallets
        .findOwnedByIdForUpdate(id, ownerId)
        .orElseThrow(() -> ApiException.notFound("Wallet"));
  }

  private static String canonicalHash(
      UUID source, UUID destination, BigDecimal amount, String note) {
    String canonical =
        "v1\nsource="
            + source
            + "\ndestination="
            + destination
            + "\namount="
            + amount.stripTrailingZeros().toPlainString()
            + "\nnote="
            + (note == null ? "" : note);
    return AuthService.hash(canonical);
  }
}
