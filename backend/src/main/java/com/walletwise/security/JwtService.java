package com.walletwise.security;

import com.walletwise.config.AppProperties;
import com.walletwise.user.AppUser;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

@Service
public final class JwtService {
  private final JwtEncoder encoder;
  private final AppProperties properties;
  private final Clock clock;

  public JwtService(JwtEncoder encoder, AppProperties properties, Clock clock) {
    this.encoder = encoder;
    this.properties = properties;
    this.clock = clock;
  }

  public IssuedAccessToken issue(AppUser user) {
    Instant issuedAt = Instant.now(clock);
    Instant expiresAt = issuedAt.plus(properties.jwt().accessTokenTtl());
    JwtClaimsSet claims =
        JwtClaimsSet.builder()
            .issuer(properties.jwt().issuer())
            .subject(user.getId().toString())
            .issuedAt(issuedAt)
            .expiresAt(expiresAt)
            .id(UUID.randomUUID().toString())
            .claim("role", user.getRole().name())
            .build();
    JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).type("JWT").build();
    String value = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    return new IssuedAccessToken(value, expiresAt);
  }

  public record IssuedAccessToken(String value, Instant expiresAt) {}
}
