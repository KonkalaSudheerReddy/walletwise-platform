package com.walletwise.admin;

import com.walletwise.audit.AuditLog;
import com.walletwise.audit.AuditLogRepository;
import com.walletwise.audit.AuditService;
import com.walletwise.auth.RefreshTokenRepository;
import com.walletwise.budget.BudgetAlertService;
import com.walletwise.common.ApiException;
import com.walletwise.common.PageResponse;
import com.walletwise.user.AppUser;
import com.walletwise.user.CurrentUser;
import com.walletwise.user.UserRepository;
import jakarta.persistence.criteria.Predicate;
import jakarta.validation.constraints.NotNull;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminService {
  private final UserRepository users;
  private final RefreshTokenRepository refreshTokens;
  private final AuditLogRepository auditLogs;
  private final AuditService audit;
  private final BudgetAlertService budgetAlerts;
  private final CurrentUser currentUser;
  private final Clock clock;

  public AdminService(
      UserRepository users,
      RefreshTokenRepository refreshTokens,
      AuditLogRepository auditLogs,
      AuditService audit,
      BudgetAlertService budgetAlerts,
      CurrentUser currentUser,
      Clock clock) {
    this.users = users;
    this.refreshTokens = refreshTokens;
    this.auditLogs = auditLogs;
    this.audit = audit;
    this.budgetAlerts = budgetAlerts;
    this.currentUser = currentUser;
    this.clock = clock;
  }

  @Transactional(readOnly = true)
  public PageResponse<AdminUserResponse> users(String search, Boolean enabled, int page, int size) {
    Specification<AppUser> specification =
        (root, query, builder) -> {
          List<Predicate> predicates = new ArrayList<>();
          if (enabled != null) predicates.add(builder.equal(root.get("enabled"), enabled));
          if (search != null && !search.isBlank()) {
            String pattern = "%" + search.trim().toLowerCase(Locale.ROOT) + "%";
            predicates.add(
                builder.or(
                    builder.like(builder.lower(root.get("emailNormalized")), pattern),
                    builder.like(builder.lower(root.get("displayName")), pattern)));
          }
          return builder.and(predicates.toArray(Predicate[]::new));
        };
    return PageResponse.from(
        users
            .findAll(
                specification,
                PageRequest.of(
                    Math.max(0, page),
                    Math.min(Math.max(1, size), 100),
                    Sort.by(Sort.Direction.DESC, "createdAt")))
            .map(AdminUserResponse::from));
  }

  @Transactional
  public AdminUserResponse setStatus(UUID id, UserStatusRequest request) {
    AppUser actor = currentUser.require();
    if (actor.getId().equals(id) && !request.enabled()) {
      throw ApiException.conflict(
          "cannot_disable_self", "An administrator cannot disable their own account");
    }
    AppUser target = users.findById(id).orElseThrow(() -> ApiException.notFound("User"));
    Instant now = Instant.now(clock);
    target.setEnabled(request.enabled(), now);
    if (!request.enabled()) refreshTokens.revokeAllForUser(id, now);
    audit.success(actor.getId(), request.enabled() ? "USER_ENABLED" : "USER_DISABLED", "USER", id);
    return AdminUserResponse.from(target);
  }

  @Transactional(readOnly = true)
  public PageResponse<AuditLogResponse> auditLogs(
      UUID actorId,
      String action,
      String resourceType,
      String outcome,
      Instant startDate,
      Instant endDate,
      int page,
      int size) {
    Specification<AuditLog> specification =
        (root, query, builder) -> {
          List<Predicate> predicates = new ArrayList<>();
          if (actorId != null) predicates.add(builder.equal(root.get("actorUserId"), actorId));
          if (action != null && !action.isBlank())
            predicates.add(builder.equal(root.get("action"), action));
          if (resourceType != null && !resourceType.isBlank())
            predicates.add(builder.equal(root.get("resourceType"), resourceType));
          if (outcome != null && !outcome.isBlank())
            predicates.add(builder.equal(root.get("outcome"), outcome));
          if (startDate != null)
            predicates.add(builder.greaterThanOrEqualTo(root.get("occurredAt"), startDate));
          if (endDate != null) predicates.add(builder.lessThan(root.get("occurredAt"), endDate));
          return builder.and(predicates.toArray(Predicate[]::new));
        };
    return PageResponse.from(
        auditLogs
            .findAll(
                specification,
                PageRequest.of(
                    Math.max(0, page),
                    Math.min(Math.max(1, size), 100),
                    Sort.by(Sort.Direction.DESC, "occurredAt")))
            .map(AuditLogResponse::from));
  }

  @Transactional
  public JobRunResponse runBudgetAlerts() {
    int evaluated = budgetAlerts.runCurrentMonth();
    audit.success(currentUser.id(), "BUDGET_ALERT_JOB_RUN", "JOB", null);
    return new JobRunResponse("COMPLETED", evaluated, Instant.now(clock));
  }

  public record UserStatusRequest(@NotNull Boolean enabled) {}

  public record AdminUserResponse(
      UUID id,
      String displayName,
      String email,
      String role,
      boolean enabled,
      String preferredCurrency,
      Instant createdAt,
      Instant updatedAt) {
    static AdminUserResponse from(AppUser user) {
      return new AdminUserResponse(
          user.getId(),
          user.getDisplayName(),
          user.getEmailNormalized(),
          user.getRole().name(),
          user.isEnabled(),
          user.getPreferredCurrency(),
          user.getCreatedAt(),
          user.getUpdatedAt());
    }
  }

  public record AuditLogResponse(
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
    static AuditLogResponse from(AuditLog log) {
      return new AuditLogResponse(
          log.getId(),
          log.getActorUserId(),
          log.getAction(),
          log.getResourceType(),
          log.getResourceId(),
          log.getOutcome(),
          log.getOccurredAt(),
          log.getCorrelationId(),
          log.getClientIp(),
          log.getUserAgent(),
          log.getMetadataJson());
    }
  }

  public record JobRunResponse(String status, int budgetsEvaluated, Instant completedAt) {}
}
