package com.walletwise.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.walletwise.config.AppProperties;
import com.walletwise.user.AppUser;
import com.walletwise.user.UserRole;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

class JwtServiceTest {
  @Test
  void issuesShortLivedSignedTokenWithIdentityAndRole() {
    Instant now = Instant.parse("2026-01-15T10:00:00Z");
    String secret = "0123456789abcdef0123456789abcdef";
    SecretKey key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    AppProperties properties =
        new AppProperties(
            new AppProperties.Jwt(
                "walletwise-test", secret, Duration.ofMinutes(15), Duration.ofDays(7)),
            false,
            List.of("http://localhost:5173"),
            false,
            Duration.ofDays(7),
            "",
            "",
            "http://localhost:8080");
    JwtService service =
        new JwtService(
            NimbusJwtEncoder.withSecretKey(key).algorithm(MacAlgorithm.HS256).build(),
            properties,
            Clock.fixed(now, ZoneOffset.UTC));
    AppUser user =
        new AppUser(
            UUID.randomUUID(),
            "Test User",
            "test@example.com",
            "hash",
            UserRole.USER,
            true,
            "USD",
            now);

    JwtService.IssuedAccessToken issued = service.issue(user);
    NimbusJwtDecoder decoder =
        NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
    decoder.setJwtValidator(token -> OAuth2TokenValidatorResult.success());
    Jwt decoded = decoder.decode(issued.value());

    assertThat(decoded.getSubject()).isEqualTo(user.getId().toString());
    assertThat(decoded.getClaimAsString("role")).isEqualTo("USER");
    assertThat(issued.expiresAt()).isEqualTo(now.plus(Duration.ofMinutes(15)));
  }
}
