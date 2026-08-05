package com.walletwise.security;

import com.walletwise.common.CorrelationIdFilter;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Component
public class ProblemDetailSecurityHandler implements AuthenticationEntryPoint, AccessDeniedHandler {
  private final JsonMapper json;
  private final Clock clock;

  public ProblemDetailSecurityHandler(JsonMapper json, Clock clock) {
    this.json = json;
    this.clock = clock;
  }

  @Override
  public void commence(
      HttpServletRequest request, HttpServletResponse response, AuthenticationException exception)
      throws IOException, ServletException {
    write(
        request,
        response,
        HttpStatus.UNAUTHORIZED,
        "authentication_required",
        "Authentication is required or invalid");
  }

  @Override
  public void handle(
      HttpServletRequest request, HttpServletResponse response, AccessDeniedException exception)
      throws IOException, ServletException {
    write(
        request,
        response,
        HttpStatus.FORBIDDEN,
        "access_denied",
        "You are not allowed to perform this action");
  }

  private void write(
      HttpServletRequest request,
      HttpServletResponse response,
      HttpStatus status,
      String code,
      String message)
      throws IOException {
    ProblemDetail detail = ProblemDetail.forStatusAndDetail(status, message);
    detail.setTitle(status.getReasonPhrase());
    detail.setType(URI.create("https://walletwise.app/problems/" + code));
    detail.setInstance(URI.create(request.getRequestURI()));
    detail.setProperty("code", code);
    detail.setProperty("timestamp", Instant.now(clock).toString());
    detail.setProperty("correlationId", request.getAttribute(CorrelationIdFilter.ATTRIBUTE));
    response.setStatus(status.value());
    response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
    json.writeValue(response.getOutputStream(), detail);
  }
}
