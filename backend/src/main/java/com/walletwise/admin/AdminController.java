package com.walletwise.admin;

import com.walletwise.admin.AdminService.AdminUserResponse;
import com.walletwise.admin.AdminService.AuditLogResponse;
import com.walletwise.admin.AdminService.JobRunResponse;
import com.walletwise.admin.AdminService.UserStatusRequest;
import com.walletwise.common.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin")
@Tag(name = "Administration", description = "Administrator-only user and audit operations")
public class AdminController {
  private final AdminService admin;

  public AdminController(AdminService admin) {
    this.admin = admin;
  }

  @GetMapping("/users")
  @Operation(summary = "Search users")
  PageResponse<AdminUserResponse> users(
      @RequestParam(required = false) String search,
      @RequestParam(required = false) Boolean enabled,
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
    return admin.users(search, enabled, page, size);
  }

  @PatchMapping("/users/{id}/status")
  @Operation(summary = "Enable or disable a user")
  AdminUserResponse status(@PathVariable UUID id, @Valid @RequestBody UserStatusRequest request) {
    return admin.setStatus(id, request);
  }

  @GetMapping("/audit-logs")
  @Operation(summary = "Search immutable audit records")
  PageResponse<AuditLogResponse> auditLogs(
      @RequestParam(required = false) UUID actorId,
      @RequestParam(required = false) String action,
      @RequestParam(required = false) String resourceType,
      @RequestParam(required = false) String outcome,
      @RequestParam(required = false) LocalDate startDate,
      @RequestParam(required = false) LocalDate endDate,
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
    Instant start = startDate == null ? null : startDate.atStartOfDay(ZoneOffset.UTC).toInstant();
    Instant end =
        endDate == null ? null : endDate.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
    return admin.auditLogs(actorId, action, resourceType, outcome, start, end, page, size);
  }

  @PostMapping("/jobs/budget-alerts/run")
  @Operation(summary = "Evaluate current-month budget alerts")
  JobRunResponse runBudgetAlerts() {
    return admin.runBudgetAlerts();
  }
}
