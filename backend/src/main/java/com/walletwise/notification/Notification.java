package com.walletwise.notification;

import com.walletwise.budget.Budget;
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
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notifications")
public class Notification {
  public enum Type {
    BUDGET_APPROACHING,
    BUDGET_REACHED,
    BUDGET_EXCEEDED
  }

  @Id private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "owner_id", nullable = false)
  private AppUser owner;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 40)
  private Type type;

  @Column(nullable = false, length = 160)
  private String title;

  @Column(nullable = false, length = 500)
  private String message;

  @Column(name = "related_resource_id")
  private UUID relatedResourceId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "budget_id")
  private Budget budget;

  @Column(name = "read_at")
  private Instant readAt;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  protected Notification() {}

  public UUID getId() {
    return id;
  }

  public AppUser getOwner() {
    return owner;
  }

  public Type getType() {
    return type;
  }

  public String getTitle() {
    return title;
  }

  public String getMessage() {
    return message;
  }

  public UUID getRelatedResourceId() {
    return relatedResourceId;
  }

  public Instant getReadAt() {
    return readAt;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void markRead(Instant now) {
    if (readAt == null) readAt = now;
  }
}
