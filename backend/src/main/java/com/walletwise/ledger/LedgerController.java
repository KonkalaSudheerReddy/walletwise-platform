package com.walletwise.ledger;

import com.walletwise.common.PageResponse;
import com.walletwise.ledger.LedgerDtos.AdjustmentRequest;
import com.walletwise.ledger.LedgerDtos.CreateEntryRequest;
import com.walletwise.ledger.LedgerDtos.LedgerEntryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/transactions")
@Tag(name = "Transactions", description = "Immutable income, expense, and correction ledger")
public class LedgerController {
  private final LedgerService ledger;

  public LedgerController(LedgerService ledger) {
    this.ledger = ledger;
  }

  @PostMapping("/income")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Record income")
  LedgerEntryResponse income(@Valid @RequestBody CreateEntryRequest request) {
    return ledger.income(request);
  }

  @PostMapping("/expense")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Record an expense")
  LedgerEntryResponse expense(@Valid @RequestBody CreateEntryRequest request) {
    return ledger.expense(request);
  }

  @PostMapping("/adjustment")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Record an auditable balance correction")
  LedgerEntryResponse adjustment(@Valid @RequestBody AdjustmentRequest request) {
    return ledger.adjustment(request);
  }

  @GetMapping("/{id}")
  @Operation(summary = "Get an owned ledger entry")
  LedgerEntryResponse get(@PathVariable UUID id) {
    return ledger.get(id);
  }

  @GetMapping
  @Operation(
      summary = "Search ledger entries",
      description =
          "Supports wallet, type, category, date, amount, description, sort, and pagination filters.")
  PageResponse<LedgerEntryResponse> list(
      @RequestParam(required = false) UUID walletId,
      @RequestParam(required = false) LedgerEntry.Type type,
      @RequestParam(required = false) UUID categoryId,
      @RequestParam(required = false) LocalDate startDate,
      @RequestParam(required = false) LocalDate endDate,
      @RequestParam(required = false) BigDecimal minAmount,
      @RequestParam(required = false) BigDecimal maxAmount,
      @RequestParam(required = false) String description,
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
      @RequestParam(defaultValue = "occurredAt") String sort,
      @RequestParam(defaultValue = "desc") String direction) {
    return ledger.search(
        new LedgerService.Filter(
            walletId,
            type,
            categoryId,
            startDate == null ? null : startDate.atStartOfDay(ZoneOffset.UTC).toInstant(),
            endDate == null ? null : endDate.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant(),
            minAmount,
            maxAmount,
            description),
        page,
        size,
        sort,
        direction);
  }
}
