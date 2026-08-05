package com.walletwise.auth;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AuthUtilitiesTest {
  @Test
  void normalizesEmailDeterministically() {
    assertThat(AuthService.normalizeEmail("  UsEr@Example.COM ")).isEqualTo("user@example.com");
  }

  @Test
  void hashesWithoutPersistingRawToken() {
    assertThat(AuthService.hash("abc"))
        .isEqualTo("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
    assertThat(AuthService.hash("abc")).doesNotContain("abc");
  }
}
