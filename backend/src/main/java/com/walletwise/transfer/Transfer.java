package com.walletwise.transfer;

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
@Table(name = "transfers")
public class Transfer {
  public enum Status {
    PENDING,
    COMPLETED,
    FAILED
  }

  @Id private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "owner_id", nullable = false)
  private AppUser owner;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "source_wallet_id", nullable = false)
  private Wallet sourceWallet;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "destination_wallet_id", nullable = false)
  private Wallet destinationWallet;

  @Column(nullable = false, precision = 19, scale = 4)
  private BigDecimal amount;

  @Column(nullable = false, length = 3)
  private String currency;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private Status status;

  @Column(length = 500)
  private String note;

  @Column(name = "idempotency_key", nullable = false, length = 128)
  private String idempotencyKey;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "completed_at")
  private Instant completedAt;

  protected Transfer() {}

  public Transfer(
      UUID id,
      AppUser owner,
      Wallet sourceWallet,
      Wallet destinationWallet,
      BigDecimal amount,
      String currency,
      String note,
      String idempotencyKey,
      Instant createdAt) {
    this.id = id;
    this.owner = owner;
    this.sourceWallet = sourceWallet;
    this.destinationWallet = destinationWallet;
    this.amount = amount;
    this.currency = currency;
    this.note = note;
    this.idempotencyKey = idempotencyKey;
    this.status = Status.PENDING;
    this.createdAt = createdAt;
  }

  public UUID getId() {
    return id;
  }

  public AppUser getOwner() {
    return owner;
  }

  public Wallet getSourceWallet() {
    return sourceWallet;
  }

  public Wallet getDestinationWallet() {
    return destinationWallet;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public String getCurrency() {
    return currency;
  }

  public Status getStatus() {
    return status;
  }

  public String getNote() {
    return note;
  }

  public String getIdempotencyKey() {
    return idempotencyKey;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getCompletedAt() {
    return completedAt;
  }

  public void complete(Instant now) {
    status = Status.COMPLETED;
    completedAt = now;
  }
}
