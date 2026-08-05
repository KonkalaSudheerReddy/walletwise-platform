package com.walletwise.auth;

import com.walletwise.user.AppUser;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

public final class AuthDtos {
  private AuthDtos() {}

  public record RegisterRequest(
      @NotBlank @Size(max = 100) String displayName,
      @NotBlank @Email @Size(max = 320) String email,
      @NotBlank @Size(min = 12, max = 64) String password,
      @NotBlank @Pattern(regexp = "[A-Z]{3}") String preferredCurrency) {}

  public record LoginRequest(
      @NotBlank @Email @Size(max = 320) String email, @NotBlank @Size(max = 64) String password) {}

  public record UserResponse(
      UUID id,
      String displayName,
      String email,
      String role,
      String preferredCurrency,
      boolean enabled) {
    public static UserResponse from(AppUser user) {
      return new UserResponse(
          user.getId(),
          user.getDisplayName(),
          user.getEmailNormalized(),
          user.getRole().name(),
          user.getPreferredCurrency(),
          user.isEnabled());
    }
  }

  public record AuthResponse(
      String accessToken, String tokenType, Instant expiresAt, UserResponse user) {}

  public record Session(AuthResponse response, String refreshToken, Instant refreshExpiresAt) {}
}
