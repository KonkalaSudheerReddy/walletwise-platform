package com.walletwise.budget;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.walletwise.common.ApiException;
import java.time.YearMonth;
import org.junit.jupiter.api.Test;

class BudgetMonthTest {
  @Test
  void parsesYearMonthContract() {
    assertThat(BudgetService.parseMonth("2026-08")).isEqualTo(YearMonth.of(2026, 8));
  }

  @Test
  void rejectsInvalidMonth() {
    assertThatThrownBy(() -> BudgetService.parseMonth("08/2026")).isInstanceOf(ApiException.class);
  }
}
