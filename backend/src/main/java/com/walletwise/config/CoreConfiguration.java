package com.walletwise.config;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.util.Arrays;
import java.util.Locale;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration(proxyBeanMethods = false)
public class CoreConfiguration {

  @Bean
  Clock clock() {
    return Clock.systemUTC();
  }

  @Bean
  PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder(12);
  }

  @Bean
  JwtSecretValidator jwtSecretValidator(AppProperties properties, Environment environment) {
    String secret = properties.jwt().secret();
    if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
      throw new IllegalStateException("JWT_SECRET must contain at least 32 UTF-8 bytes");
    }
    boolean production = Arrays.asList(environment.getActiveProfiles()).contains("prod");
    String normalized = secret.toLowerCase(Locale.ROOT);
    if (production
        && (normalized.startsWith("local-development-only")
            || normalized.contains("replace")
            || normalized.contains("change-me")
            || normalized.contains("placeholder"))) {
      throw new IllegalStateException("Production refuses a documented or placeholder JWT secret");
    }
    if (production && !properties.cookieSecure()) {
      throw new IllegalStateException("Production requires APP_COOKIE_SECURE=true");
    }
    Duration refreshGrace = properties.jwt().refreshReuseGrace();
    if (refreshGrace == null
        || refreshGrace.isNegative()
        || refreshGrace.compareTo(Duration.ofSeconds(30)) > 0) {
      throw new IllegalStateException("APP_REFRESH_REUSE_GRACE must be between PT0S and PT30S");
    }
    return new JwtSecretValidator();
  }

  public static final class JwtSecretValidator {}
}
