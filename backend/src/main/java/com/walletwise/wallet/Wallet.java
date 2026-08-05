package com.walletwise.wallet;

import com.walletwise.user.AppUser;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "wallets")
public class Wallet {
  public enum Type {
    CASH,
    BANK,
    SAVINGS,
    CREDIT,
    OTHER
  }

  @Id private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "owner_id", nullable = false)
  private AppUser owner;

  @Column(nullable = false, length = 100)
  private String name;

  @Enumerated(EnumType.STRING)
  @Column(name = "wallet_type", nullable = false, length = 20)
  private Type type;

  @Column(nullable = false, length = 3)
  private String currency;

  @Column(name = "current_balance", nullable = false, precision = 19, scale = 4)
  private BigDecimal currentBalance;

  @Column(nullable = false)
  private boolean archived;

  @Version
  @Column(nullable = false)
  private long version;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected Wallet() {}

  public Wallet(UUID id, AppUser owner, String name, Type type, String currency, Instant now) {
    this.id = id;
    this.owner = owner;
    this.name = name;
    this.type = type;
    this.currency = currency;
    this.currentBalance = BigDecimal.ZERO.setScale(4);
    this.createdAt = now;
    this.updatedAt = now;
  }

  public UUID getId() {
    return id;
  }

  public AppUser getOwner() {
    return owner;
  }

  public String getName() {
    return name;
  }

  public Type getType() {
    return type;
  }

  public String getCurrency() {
    return currency;
  }

  public BigDecimal getCurrentBalance() {
    return currentBalance;
  }

  public boolean isArchived() {
    return archived;
  }

  public long getVersion() {
    return version;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public void rename(String name, Type type, Instant now) {
    this.name = name;
    this.type = type;
    this.updatedAt = now;
  }

  public void archive(boolean archived, Instant now) {
    this.archived = archived;
    this.updatedAt = now;
  }

  public void credit(BigDecimal amount, Instant now) {
    currentBalance = currentBalance.add(amount);
    updatedAt = now;
  }

  public void debit(BigDecimal amount, Instant now) {
    currentBalance = currentBalance.subtract(amount);
    updatedAt = now;
  }
}
