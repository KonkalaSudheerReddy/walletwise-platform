package com.walletwise.transfer;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "idempotency_records")
public class IdempotencyRecord {
  public enum Status {
    PROCESSING,
    COMPLETED
  }

  @Id private UUID id;

  @Column(name = "owner_id", nullable = false)
  private UUID ownerId;

  @Column(nullable = false, length = 80)
  private String operation;

  @Column(name = "idempotency_key", nullable = false, length = 128)
  private String idempotencyKey;

  @Column(name = "request_hash", nullable = false, length = 64)
  private String requestHash;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private Status status;

  @Column(name = "response_status")
  private Integer responseStatus;

  @Column(name = "response_resource_id")
  private UUID responseResourceId;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "expires_at", nullable = false)
  private Instant expiresAt;

  protected IdempotencyRecord() {}

  public IdempotencyRecord(
      UUID id,
      UUID ownerId,
      String operation,
      String idempotencyKey,
      String requestHash,
      Instant createdAt,
      Instant expiresAt) {
    this.id = id;
    this.ownerId = ownerId;
    this.operation = operation;
    this.idempotencyKey = idempotencyKey;
    this.requestHash = requestHash;
    this.status = Status.PROCESSING;
    this.createdAt = createdAt;
    this.expiresAt = expiresAt;
  }

  public String getRequestHash() {
    return requestHash;
  }

  public Status getStatus() {
    return status;
  }

  public Integer getResponseStatus() {
    return responseStatus;
  }

  public UUID getResponseResourceId() {
    return responseResourceId;
  }

  public void complete(int statusCode, UUID resourceId) {
    this.status = Status.COMPLETED;
    this.responseStatus = statusCode;
    this.responseResourceId = resourceId;
  }
}
