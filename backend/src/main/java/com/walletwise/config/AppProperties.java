package com.walletwise.config;

import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app")
public record AppProperties(
    Jwt jwt,
    boolean cookieSecure,
    List<String> corsAllowedOrigins,
    boolean demoSeedEnabled,
    Duration idempotencyRetention,
    String adminEmail,
    String adminPassword,
    String publicUrl) {

  public record Jwt(
      String issuer, String secret, Duration accessTokenTtl, Duration refreshTokenTtl) {}
}
