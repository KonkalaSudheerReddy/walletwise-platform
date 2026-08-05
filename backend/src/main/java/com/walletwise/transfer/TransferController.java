package com.walletwise.transfer;

import com.walletwise.common.PageResponse;
import com.walletwise.transfer.TransferDtos.TransferRequest;
import com.walletwise.transfer.TransferDtos.TransferResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/transfers")
@Tag(name = "Transfers", description = "Atomic, idempotent transfers between owned wallets")
public class TransferController {
  private final TransferService transfers;

  public TransferController(TransferService transfers) {
    this.transfers = transfers;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
      summary = "Transfer funds between wallets",
      description = "Uses a caller-supplied idempotency key and locks both wallet rows atomically.")
  @ApiResponses({
    @ApiResponse(responseCode = "400", ref = "#/components/responses/BadRequest"),
    @ApiResponse(responseCode = "401", ref = "#/components/responses/Unauthorized"),
    @ApiResponse(responseCode = "409", ref = "#/components/responses/Conflict")
  })
  TransferResponse create(
      @Parameter(
              description = "Unique retry key scoped to this user and operation",
              required = true,
              example = "transfer-018f6f90-8f8b-7f42-a063-4f22bd12f244",
              schema = @Schema(minLength = 8, maxLength = 128, pattern = "[A-Za-z0-9._:-]{8,128}"))
          @RequestHeader("Idempotency-Key")
          String key,
      @Valid @RequestBody TransferRequest request) {
    return transfers.create(key, request);
  }

  @GetMapping
  @Operation(summary = "List transfers with pagination")
  PageResponse<TransferResponse> list(
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
    return transfers.list(page, size);
  }

  @GetMapping("/{id}")
  @Operation(summary = "Get an owned transfer")
  TransferResponse get(@PathVariable UUID id) {
    return transfers.get(id);
  }
}
