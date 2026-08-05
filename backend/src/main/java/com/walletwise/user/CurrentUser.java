package com.walletwise.user;

import com.walletwise.common.ApiException;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public final class CurrentUser {
  private final UserRepository users;

  public CurrentUser(UserRepository users) {
    this.users = users;
  }

  public AppUser require() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !authentication.isAuthenticated()) {
      throw new ApiException(
          HttpStatus.UNAUTHORIZED, "authentication_required", "Authentication is required");
    }
    UUID id;
    try {
      id = UUID.fromString(authentication.getName());
    } catch (IllegalArgumentException exception) {
      throw new ApiException(
          HttpStatus.UNAUTHORIZED, "invalid_identity", "Authentication identity is invalid");
    }
    AppUser user =
        users
            .findById(id)
            .orElseThrow(
                () ->
                    new ApiException(
                        HttpStatus.UNAUTHORIZED,
                        "invalid_identity",
                        "Authentication identity is invalid"));
    if (!user.isEnabled()) {
      throw new ApiException(
          HttpStatus.UNAUTHORIZED, "account_disabled", "The account is disabled");
    }
    return user;
  }

  public UUID id() {
    return require().getId();
  }
}
