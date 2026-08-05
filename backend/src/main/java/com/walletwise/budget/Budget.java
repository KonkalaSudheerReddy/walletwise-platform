package com.walletwise.budget;

import com.walletwise.category.Category;
import com.walletwise.user.AppUser;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "budgets")
public class Budget {
  @Id private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "owner_id", nullable = false)
  private AppUser owner;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "category_id", nullable = false)
  private Category category;

  @Column(name = "period_start", nullable = false)
  private LocalDate periodStart;

  @Column(name = "limit_amount", nullable = false, precision = 19, scale = 4)
  private BigDecimal limitAmount;

  @Column(name = "alert_threshold_percent", nullable = false)
  private int alertThresholdPercent;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected Budget() {}

  public Budget(
      UUID id,
      AppUser owner,
      Category category,
      LocalDate periodStart,
      BigDecimal limitAmount,
      int alertThresholdPercent,
      Instant now) {
    this.id = id;
    this.owner = owner;
    this.category = category;
    this.periodStart = periodStart;
    this.limitAmount = limitAmount;
    this.alertThresholdPercent = alertThresholdPercent;
    this.createdAt = now;
    this.updatedAt = now;
  }

  public UUID getId() {
    return id;
  }

  public AppUser getOwner() {
    return owner;
  }

  public Category getCategory() {
    return category;
  }

  public LocalDate getPeriodStart() {
    return periodStart;
  }

  public BigDecimal getLimitAmount() {
    return limitAmount;
  }

  public int getAlertThresholdPercent() {
    return alertThresholdPercent;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public void update(BigDecimal limit, int threshold, Instant now) {
    limitAmount = limit;
    alertThresholdPercent = threshold;
    updatedAt = now;
  }
}
