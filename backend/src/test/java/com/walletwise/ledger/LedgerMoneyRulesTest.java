package com.walletwise.ledger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.walletwise.common.ApiException;
import com.walletwise.user.AppUser;
import com.walletwise.user.UserRole;
import com.walletwise.wallet.Wallet;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class LedgerMoneyRulesTest {
  private static final Instant NOW = Instant.parse("2026-01-15T10:00:00Z");

  @Test
  void normalizesValidMoneyToDatabaseScale() {
    assertThat(LedgerService.money(new BigDecimal("12.34"))).isEqualByComparingTo("12.3400");
  }

  @Test
  void rejectsNonPositiveAndOverPreciseMoney() {
    assertThatThrownBy(() -> LedgerService.money(BigDecimal.ZERO)).isInstanceOf(ApiException.class);
    assertThatThrownBy(() -> LedgerService.money(new BigDecimal("1.00001")))
        .isInstanceOf(ApiException.class);
  }

  @Test
  void preventsNonCreditWalletFromGoingNegativeButAllowsCreditWallet() {
    AppUser owner = owner();
    Wallet cash = new Wallet(UUID.randomUUID(), owner, "Cash", Wallet.Type.CASH, "USD", NOW);
    Wallet credit = new Wallet(UUID.randomUUID(), owner, "Card", Wallet.Type.CREDIT, "USD", NOW);
    assertThatThrownBy(() -> LedgerService.ensureCanDebit(cash, new BigDecimal("1.0000")))
        .isInstanceOf(ApiException.class)
        .extracting(error -> ((ApiException) error).code())
        .isEqualTo("insufficient_balance");
    LedgerService.ensureCanDebit(credit, new BigDecimal("1.0000"));
  }

  private static AppUser owner() {
    return new AppUser(
        UUID.randomUUID(), "Test", "test@example.com", "hash", UserRole.USER, true, "USD", NOW);
  }
}
