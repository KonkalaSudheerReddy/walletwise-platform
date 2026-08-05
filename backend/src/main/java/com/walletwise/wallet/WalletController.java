package com.walletwise.wallet;

import com.walletwise.wallet.WalletDtos.CreateWalletRequest;
import com.walletwise.wallet.WalletDtos.UpdateWalletRequest;
import com.walletwise.wallet.WalletDtos.WalletResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/wallets")
public class WalletController {
  private final WalletService wallets;

  public WalletController(WalletService wallets) {
    this.wallets = wallets;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  WalletResponse create(@Valid @RequestBody CreateWalletRequest request) {
    return wallets.create(request);
  }

  @GetMapping
  List<WalletResponse> list(@RequestParam(defaultValue = "false") boolean includeArchived) {
    return wallets.list(includeArchived);
  }

  @GetMapping("/{id}")
  WalletService.WalletDetailResponse get(@PathVariable UUID id) {
    return wallets.get(id);
  }

  @PatchMapping("/{id}")
  WalletResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateWalletRequest request) {
    return wallets.update(id, request);
  }

  @PostMapping("/{id}/archive")
  WalletResponse archive(@PathVariable UUID id) {
    return wallets.archive(id, true);
  }

  @PostMapping("/{id}/restore")
  WalletResponse restore(@PathVariable UUID id) {
    return wallets.archive(id, false);
  }
}
