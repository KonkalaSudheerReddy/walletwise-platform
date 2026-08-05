package com.walletwise;

import org.junit.jupiter.api.Test;

class WalletWiseApplicationTests {

  @Test
  void applicationTypeIsAvailable() {
    org.assertj.core.api.Assertions.assertThat(WalletWiseApplication.class).isNotNull();
  }
}
