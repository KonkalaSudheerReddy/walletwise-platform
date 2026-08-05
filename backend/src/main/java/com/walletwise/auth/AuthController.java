package com.walletwise.auth;

import com.walletwise.auth.AuthDtos.AuthResponse;
import com.walletwise.auth.AuthDtos.LoginRequest;
import com.walletwise.auth.AuthDtos.RegisterRequest;
import com.walletwise.auth.AuthDtos.Session;
import com.walletwise.auth.AuthDtos.UserResponse;
import com.walletwise.config.AppProperties;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.Duration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication")
public class AuthController {
  private static final String COOKIE = "walletwise_refresh";
  private final AuthService auth;
  private final AppProperties properties;

  public AuthController(AuthService auth, AppProperties properties) {
    this.auth = auth;
    this.properties = properties;
  }

  @PostMapping("/register")
  @Operation(summary = "Register a user and start a session")
  @SecurityRequirements
  ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
    Session session = auth.register(request);
    return ResponseEntity.status(HttpStatus.CREATED)
        .header(HttpHeaders.SET_COOKIE, cookie(session).toString())
        .body(session.response());
  }

  @PostMapping("/login")
  @Operation(summary = "Authenticate and start a session")
  @SecurityRequirements
  ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
    Session session = auth.login(request);
    return ResponseEntity.ok()
        .header(HttpHeaders.SET_COOKIE, cookie(session).toString())
        .body(session.response());
  }

  @PostMapping("/refresh")
  @Operation(summary = "Rotate the refresh token and issue a new access token")
  @SecurityRequirements
  ResponseEntity<AuthResponse> refresh(@CookieValue(name = COOKIE, required = false) String token) {
    Session session = auth.refresh(token);
    return ResponseEntity.ok()
        .header(HttpHeaders.SET_COOKIE, cookie(session).toString())
        .body(session.response());
  }

  @PostMapping("/logout")
  @Operation(summary = "Revoke the current refresh token")
  @SecurityRequirements
  ResponseEntity<Void> logout(@CookieValue(name = COOKIE, required = false) String token) {
    auth.logout(token);
    return ResponseEntity.noContent()
        .header(HttpHeaders.SET_COOKIE, clearCookie().toString())
        .build();
  }

  @GetMapping("/me")
  @Operation(summary = "Return the authenticated user")
  UserResponse me() {
    return auth.me();
  }

  private ResponseCookie cookie(Session session) {
    return ResponseCookie.from(COOKIE, session.refreshToken())
        .httpOnly(true)
        .secure(properties.cookieSecure())
        .sameSite("Lax")
        .path("/api/v1/auth")
        .maxAge(properties.jwt().refreshTokenTtl())
        .build();
  }

  private ResponseCookie clearCookie() {
    return ResponseCookie.from(COOKIE, "")
        .httpOnly(true)
        .secure(properties.cookieSecure())
        .sameSite("Lax")
        .path("/api/v1/auth")
        .maxAge(Duration.ZERO)
        .build();
  }
}
