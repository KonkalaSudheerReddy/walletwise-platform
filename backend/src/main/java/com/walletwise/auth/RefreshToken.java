package com.walletwise.auth;

import com.walletwise.user.AppUser;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "refresh_tokens")
public class RefreshToken {
  @Id private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private AppUser user;

  @Column(name = "token_hash", nullable = false, unique = true, length = 64)
  private String tokenHash;

  @Column(name = "family_id", nullable = false)
  private UUID familyId;

  @Column(name = "expires_at", nullable = false)
  private Instant expiresAt;

  @Column(name = "revoked_at")
  private Instant revokedAt;

  @Column(name = "replaced_by_id")
  private UUID replacedById;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  protected RefreshToken() {}

  public RefreshToken(
      UUID id,
      AppUser user,
      String tokenHash,
      UUID familyId,
      Instant expiresAt,
      Instant createdAt) {
    this.id = id;
    this.user = user;
    this.tokenHash = tokenHash;
    this.familyId = familyId;
    this.expiresAt = expiresAt;
    this.createdAt = createdAt;
  }

  public UUID getId() {
    return id;
  }

  public AppUser getUser() {
    return user;
  }

  public String getTokenHash() {
    return tokenHash;
  }

  public UUID getFamilyId() {
    return familyId;
  }

  public Instant getExpiresAt() {
    return expiresAt;
  }

  public Instant getRevokedAt() {
    return revokedAt;
  }

  public UUID getReplacedById() {
    return replacedById;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public boolean isActive(Instant now) {
    return revokedAt == null && expiresAt.isAfter(now);
  }

  public void rotateTo(UUID replacementId, Instant now) {
    revokedAt = now;
    replacedById = replacementId;
  }

  public void revoke(Instant now) {
    if (revokedAt == null) revokedAt = now;
  }
}
