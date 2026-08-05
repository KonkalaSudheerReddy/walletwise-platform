package com.walletwise.wallet;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public final class WalletDtos {
  private WalletDtos() {}

  public record CreateWalletRequest(
      @NotBlank @Size(max = 100) String name,
      @NotNull Wallet.Type type,
      @NotBlank @Pattern(regexp = "[A-Z]{3}") String currency,
      @DecimalMin("0.0000") @Digits(integer = 15, fraction = 4) BigDecimal openingBalance) {}

  public record UpdateWalletRequest(
      @NotBlank @Size(max = 100) String name, @NotNull Wallet.Type type) {}

  public record WalletResponse(
      UUID id,
      String name,
      String type,
      String currency,
      BigDecimal balance,
      boolean archived,
      Instant createdAt,
      Instant updatedAt) {
    public static WalletResponse from(Wallet wallet) {
      return new WalletResponse(
          wallet.getId(),
          wallet.getName(),
          wallet.getType().name(),
          wallet.getCurrency(),
          wallet.getCurrentBalance(),
          wallet.isArchived(),
          wallet.getCreatedAt(),
          wallet.getUpdatedAt());
    }
  }
}
