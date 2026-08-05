package com.walletwise.ledger;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public final class LedgerDtos {
  private LedgerDtos() {}

  public record CreateEntryRequest(
      @NotNull UUID walletId,
      @NotNull @DecimalMin(value = "0.0001") @Digits(integer = 15, fraction = 4) BigDecimal amount,
      @NotNull UUID categoryId,
      @Size(max = 500) String description,
      Instant occurredAt) {}

  public record AdjustmentRequest(
      @NotNull UUID walletId,
      @NotNull @DecimalMin(value = "0.0001") @Digits(integer = 15, fraction = 4) BigDecimal amount,
      @NotNull LedgerEntry.Direction direction,
      UUID categoryId,
      @NotNull @Size(min = 3, max = 500) String description,
      Instant occurredAt) {}

  public record LedgerEntryResponse(
      UUID id,
      UUID walletId,
      String walletName,
      String currency,
      String type,
      String direction,
      BigDecimal amount,
      UUID categoryId,
      String categoryName,
      String description,
      Instant occurredAt,
      UUID transferId,
      BigDecimal balanceAfter,
      Instant createdAt) {
    public static LedgerEntryResponse from(LedgerEntry entry) {
      return new LedgerEntryResponse(
          entry.getId(),
          entry.getWallet().getId(),
          entry.getWallet().getName(),
          entry.getWallet().getCurrency(),
          entry.getType().name(),
          entry.getDirection().name(),
          entry.getAmount(),
          entry.getCategory() == null ? null : entry.getCategory().getId(),
          entry.getCategory() == null ? null : entry.getCategory().getName(),
          entry.getDescription(),
          entry.getOccurredAt(),
          entry.getTransferId(),
          entry.getBalanceAfter(),
          entry.getCreatedAt());
    }
  }
}
