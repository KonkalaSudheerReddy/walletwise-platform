package com.walletwise.audit;

import com.walletwise.common.CorrelationIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

@Service
public class AuditService {
  private final AuditLogRepository repository;
  private final Clock clock;
  private final ObjectProvider<HttpServletRequest> requestProvider;

  public AuditService(
      AuditLogRepository repository,
      Clock clock,
      ObjectProvider<HttpServletRequest> requestProvider) {
    this.repository = repository;
    this.clock = clock;
    this.requestProvider = requestProvider;
  }

  public void success(UUID actorId, String action, String resourceType, UUID resourceId) {
    HttpServletRequest request = requestProvider.getIfAvailable();
    String correlationId =
        request == null ? null : (String) request.getAttribute(CorrelationIdFilter.ATTRIBUTE);
    String ip = request == null ? null : limit(request.getRemoteAddr(), 45);
    String userAgent = request == null ? null : limit(request.getHeader("User-Agent"), 512);
    repository.save(
        new AuditLog(
            UUID.randomUUID(),
            actorId,
            action,
            resourceType,
            resourceId,
            "SUCCESS",
            Instant.now(clock),
            correlationId,
            ip,
            userAgent,
            "{}"));
  }

  private static String limit(String value, int max) {
    return value == null || value.length() <= max ? value : value.substring(0, max);
  }
}
