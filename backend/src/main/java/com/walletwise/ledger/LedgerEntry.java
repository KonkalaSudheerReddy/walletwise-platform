package com.walletwise.ledger;

import com.walletwise.category.Category;
import com.walletwise.user.AppUser;
import com.walletwise.wallet.Wallet;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ledger_entries")
public class LedgerEntry {
  public enum Type {
    OPENING_BALANCE,
    INCOME,
    EXPENSE,
    TRANSFER_IN,
    TRANSFER_OUT,
    ADJUSTMENT
  }

  public enum Direction {
    CREDIT,
    DEBIT
  }

  @Id private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "wallet_id", nullable = false)
  private Wallet wallet;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "owner_id", nullable = false)
  private AppUser owner;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private Type type;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 10)
  private Direction direction;

  @Column(nullable = false, precision = 19, scale = 4)
  private BigDecimal amount;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "category_id")
  private Category category;

  @Column(length = 500)
  private String description;

  @Column(name = "occurred_at", nullable = false)
  private Instant occurredAt;

  @Column(name = "transfer_id")
  private UUID transferId;

  @Column(name = "balance_after", nullable = false, precision = 19, scale = 4)
  private BigDecimal balanceAfter;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  protected LedgerEntry() {}

  public LedgerEntry(
      UUID id,
      Wallet wallet,
      AppUser owner,
      Type type,
      Direction direction,
      BigDecimal amount,
      Category category,
      String description,
      Instant occurredAt,
      UUID transferId,
      BigDecimal balanceAfter,
      Instant createdAt) {
    this.id = id;
    this.wallet = wallet;
    this.owner = owner;
    this.type = type;
    this.direction = direction;
    this.amount = amount;
    this.category = category;
    this.description = description;
    this.occurredAt = occurredAt;
    this.transferId = transferId;
    this.balanceAfter = balanceAfter;
    this.createdAt = createdAt;
  }

  public UUID getId() {
    return id;
  }

  public Wallet getWallet() {
    return wallet;
  }

  public AppUser getOwner() {
    return owner;
  }

  public Type getType() {
    return type;
  }

  public Direction getDirection() {
    return direction;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public Category getCategory() {
    return category;
  }

  public String getDescription() {
    return description;
  }

  public Instant getOccurredAt() {
    return occurredAt;
  }

  public UUID getTransferId() {
    return transferId;
  }

  public BigDecimal getBalanceAfter() {
    return balanceAfter;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
