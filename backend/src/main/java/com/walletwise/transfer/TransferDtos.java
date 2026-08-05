package com.walletwise.transfer;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public final class TransferDtos {
  private TransferDtos() {}

  public record TransferRequest(
      @NotNull UUID sourceWalletId,
      @NotNull UUID destinationWalletId,
      @NotNull @DecimalMin("0.0001") @Digits(integer = 15, fraction = 4) BigDecimal amount,
      @Size(max = 500) String note) {}

  public record TransferResponse(
      UUID id,
      UUID sourceWalletId,
      String sourceWalletName,
      UUID destinationWalletId,
      String destinationWalletName,
      BigDecimal amount,
      String currency,
      String status,
      String note,
      Instant createdAt,
      Instant completedAt) {
    public static TransferResponse from(Transfer transfer) {
      return new TransferResponse(
          transfer.getId(),
          transfer.getSourceWallet().getId(),
          transfer.getSourceWallet().getName(),
          transfer.getDestinationWallet().getId(),
          transfer.getDestinationWallet().getName(),
          transfer.getAmount(),
          transfer.getCurrency(),
          transfer.getStatus().name(),
          transfer.getNote(),
          transfer.getCreatedAt(),
          transfer.getCompletedAt());
    }
  }
}
