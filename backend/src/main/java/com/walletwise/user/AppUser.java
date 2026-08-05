package com.walletwise.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "app_users")
public class AppUser {
  @Id private UUID id;

  @Column(name = "display_name", nullable = false, length = 100)
  private String displayName;

  @Column(name = "email_normalized", nullable = false, length = 320, unique = true)
  private String emailNormalized;

  @Column(name = "password_hash", nullable = false, length = 100)
  private String passwordHash;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private UserRole role;

  @Column(nullable = false)
  private boolean enabled;

  @Column(name = "preferred_currency", nullable = false, length = 3)
  private String preferredCurrency;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected AppUser() {}

  public AppUser(
      UUID id,
      String displayName,
      String emailNormalized,
      String passwordHash,
      UserRole role,
      boolean enabled,
      String preferredCurrency,
      Instant now) {
    this.id = id;
    this.displayName = displayName;
    this.emailNormalized = emailNormalized;
    this.passwordHash = passwordHash;
    this.role = role;
    this.enabled = enabled;
    this.preferredCurrency = preferredCurrency;
    this.createdAt = now;
    this.updatedAt = now;
  }

  public UUID getId() {
    return id;
  }

  public String getDisplayName() {
    return displayName;
  }

  public String getEmailNormalized() {
    return emailNormalized;
  }

  public String getPasswordHash() {
    return passwordHash;
  }

  public UserRole getRole() {
    return role;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public String getPreferredCurrency() {
    return preferredCurrency;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public void setEnabled(boolean enabled, Instant now) {
    this.enabled = enabled;
    this.updatedAt = now;
  }
}
