package com.walletwise.auth;

import com.walletwise.audit.AuditService;
import com.walletwise.auth.AuthDtos.AuthResponse;
import com.walletwise.auth.AuthDtos.LoginRequest;
import com.walletwise.auth.AuthDtos.RegisterRequest;
import com.walletwise.auth.AuthDtos.Session;
import com.walletwise.auth.AuthDtos.UserResponse;
import com.walletwise.common.ApiException;
import com.walletwise.config.AppProperties;
import com.walletwise.security.JwtService;
import com.walletwise.security.JwtService.IssuedAccessToken;
import com.walletwise.user.AppUser;
import com.walletwise.user.CurrentUser;
import com.walletwise.user.UserRepository;
import com.walletwise.user.UserRole;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.text.Normalizer;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.Currency;
import java.util.HexFormat;
import java.util.Locale;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
  private static final SecureRandom RANDOM = new SecureRandom();
  private final UserRepository users;
  private final RefreshTokenRepository refreshTokens;
  private final PasswordEncoder passwords;
  private final JwtService jwtService;
  private final AppProperties properties;
  private final Clock clock;
  private final CurrentUser currentUser;
  private final AuditService audit;

  public AuthService(
      UserRepository users,
      RefreshTokenRepository refreshTokens,
      PasswordEncoder passwords,
      JwtService jwtService,
      AppProperties properties,
      Clock clock,
      CurrentUser currentUser,
      AuditService audit) {
    this.users = users;
    this.refreshTokens = refreshTokens;
    this.passwords = passwords;
    this.jwtService = jwtService;
    this.properties = properties;
    this.clock = clock;
    this.currentUser = currentUser;
    this.audit = audit;
  }

  @Transactional
  public Session register(RegisterRequest request) {
    String email = normalizeEmail(request.email());
    if (users.existsByEmailNormalized(email)) {
      throw ApiException.conflict(
          "email_already_registered", "An account with this email already exists");
    }
    if (request.password().getBytes(StandardCharsets.UTF_8).length > 72) {
      throw new ApiException(
          HttpStatus.BAD_REQUEST, "password_too_long", "Password must not exceed 72 UTF-8 bytes");
    }
    Instant now = Instant.now(clock);
    String preferredCurrency;
    try {
      preferredCurrency = Currency.getInstance(request.preferredCurrency()).getCurrencyCode();
    } catch (IllegalArgumentException exception) {
      throw ApiException.unprocessable(
          "invalid_currency", "Preferred currency must be a valid ISO 4217 code");
    }
    AppUser user =
        users.save(
            new AppUser(
                UUID.randomUUID(),
                request.displayName().trim(),
                email,
                passwords.encode(request.password()),
                UserRole.USER,
                true,
                preferredCurrency,
                now));
    audit.success(user.getId(), "USER_REGISTERED", "USER", user.getId());
    return newSession(user, UUID.randomUUID());
  }

  @Transactional
  public Session login(LoginRequest request) {
    AppUser user = users.findByEmailNormalized(normalizeEmail(request.email())).orElse(null);
    if (user == null
        || !passwords.matches(request.password(), user.getPasswordHash())
        || !user.isEnabled()) {
      throw new ApiException(
          HttpStatus.UNAUTHORIZED, "invalid_credentials", "Email or password is invalid");
    }
    audit.success(user.getId(), "USER_LOGIN", "USER", user.getId());
    return newSession(user, UUID.randomUUID());
  }

  @Transactional(noRollbackFor = ApiException.class)
  public Session refresh(String rawToken) {
    if (rawToken == null || rawToken.isBlank()) {
      throw new ApiException(
          HttpStatus.UNAUTHORIZED, "refresh_token_missing", "Refresh token is missing");
    }
    Instant now = Instant.now(clock);
    RefreshToken current =
        refreshTokens
            .findByTokenHash(hash(rawToken))
            .orElseThrow(
                () ->
                    new ApiException(
                        HttpStatus.UNAUTHORIZED,
                        "refresh_token_invalid",
                        "Refresh token is invalid"));
    if (!current.isActive(now)) {
      if (current.getRevokedAt() != null && current.getReplacedById() != null) {
        refreshTokens.revokeFamily(current.getFamilyId(), now);
      }
      throw new ApiException(
          HttpStatus.UNAUTHORIZED, "refresh_token_invalid", "Refresh token is expired or revoked");
    }
    if (!current.getUser().isEnabled()) {
      current.revoke(now);
      throw new ApiException(
          HttpStatus.UNAUTHORIZED, "account_disabled", "The account is disabled");
    }
    Session replacement = newSession(current.getUser(), current.getFamilyId());
    RefreshToken replacementEntity =
        refreshTokens.findByTokenHash(hash(replacement.refreshToken())).orElseThrow();
    current.rotateTo(replacementEntity.getId(), now);
    audit.success(current.getUser().getId(), "TOKEN_REFRESHED", "USER", current.getUser().getId());
    return replacement;
  }

  @Transactional
  public void logout(String rawToken) {
    if (rawToken == null || rawToken.isBlank()) return;
    refreshTokens
        .findByTokenHash(hash(rawToken))
        .ifPresent(
            token -> {
              token.revoke(Instant.now(clock));
              audit.success(
                  token.getUser().getId(), "USER_LOGOUT", "USER", token.getUser().getId());
            });
  }

  @Transactional(readOnly = true)
  public UserResponse me() {
    return UserResponse.from(currentUser.require());
  }

  private Session newSession(AppUser user, UUID familyId) {
    Instant now = Instant.now(clock);
    String rawRefresh = randomToken();
    Instant refreshExpiry = now.plus(properties.jwt().refreshTokenTtl());
    refreshTokens.save(
        new RefreshToken(UUID.randomUUID(), user, hash(rawRefresh), familyId, refreshExpiry, now));
    IssuedAccessToken access = jwtService.issue(user);
    return new Session(
        new AuthResponse(access.value(), "Bearer", access.expiresAt(), UserResponse.from(user)),
        rawRefresh,
        refreshExpiry);
  }

  public static String normalizeEmail(String email) {
    return Normalizer.normalize(email.trim(), Normalizer.Form.NFKC).toLowerCase(Locale.ROOT);
  }

  private static String randomToken() {
    byte[] bytes = new byte[32];
    RANDOM.nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }

  public static String hash(String value) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException impossible) {
      throw new IllegalStateException(impossible);
    }
  }
}
