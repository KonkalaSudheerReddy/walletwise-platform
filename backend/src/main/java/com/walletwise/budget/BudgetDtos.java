package com.walletwise.budget;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public final class BudgetDtos {
  private BudgetDtos() {}

  public record CreateBudgetRequest(
      @NotNull UUID categoryId,
      @NotBlank @Pattern(regexp = "\\d{4}-(0[1-9]|1[0-2])") String month,
      @NotNull @DecimalMin("0.0001") @Digits(integer = 15, fraction = 4) BigDecimal limitAmount,
      @Min(1) @Max(99) Integer alertThresholdPercent) {}

  public record UpdateBudgetRequest(
      @NotNull @DecimalMin("0.0001") @Digits(integer = 15, fraction = 4) BigDecimal limitAmount,
      @NotNull @Min(1) @Max(99) Integer alertThresholdPercent) {}

  public record BudgetResponse(
      UUID id,
      UUID categoryId,
      String categoryName,
      String month,
      String currency,
      BigDecimal limitAmount,
      int alertThresholdPercent,
      BigDecimal spentAmount,
      BigDecimal remainingAmount,
      BigDecimal utilizationPercent,
      Instant createdAt,
      Instant updatedAt) {}
}
