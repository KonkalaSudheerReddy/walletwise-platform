package com.walletwise.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "audit_logs")
public class AuditLog {
  @Id private UUID id;

  @Column(name = "actor_user_id")
  private UUID actorUserId;

  @Column(nullable = false, length = 80)
  private String action;

  @Column(name = "resource_type", nullable = false, length = 80)
  private String resourceType;

  @Column(name = "resource_id")
  private UUID resourceId;

  @Column(nullable = false, length = 20)
  private String outcome;

  @Column(name = "occurred_at", nullable = false)
  private Instant occurredAt;

  @Column(name = "correlation_id", length = 64)
  private String correlationId;

  @Column(name = "client_ip", length = 45)
  private String clientIp;

  @Column(name = "user_agent", length = 512)
  private String userAgent;

  @Column(name = "metadata_json", columnDefinition = "text")
  private String metadataJson;

  protected AuditLog() {}

  public AuditLog(
      UUID id,
      UUID actorUserId,
      String action,
      String resourceType,
      UUID resourceId,
      String outcome,
      Instant occurredAt,
      String correlationId,
      String clientIp,
      String userAgent,
      String metadataJson) {
    this.id = id;
    this.actorUserId = actorUserId;
    this.action = action;
    this.resourceType = resourceType;
    this.resourceId = resourceId;
    this.outcome = outcome;
    this.occurredAt = occurredAt;
    this.correlationId = correlationId;
    this.clientIp = clientIp;
    this.userAgent = userAgent;
    this.metadataJson = metadataJson;
  }

  public UUID getId() {
    return id;
  }

  public UUID getActorUserId() {
    return actorUserId;
  }

  public String getAction() {
    return action;
  }

  public String getResourceType() {
    return resourceType;
  }

  public UUID getResourceId() {
    return resourceId;
  }

  public String getOutcome() {
    return outcome;
  }

  public Instant getOccurredAt() {
    return occurredAt;
  }

  public String getCorrelationId() {
    return correlationId;
  }

  public String getClientIp() {
    return clientIp;
  }

  public String getUserAgent() {
    return userAgent;
  }

  public String getMetadataJson() {
    return metadataJson;
  }
}
