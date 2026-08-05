package com.walletwise.common;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public final class CorrelationIdFilter extends OncePerRequestFilter {
  private static final Logger log = LoggerFactory.getLogger(CorrelationIdFilter.class);
  public static final String HEADER = "X-Correlation-Id";
  public static final String ATTRIBUTE = CorrelationIdFilter.class.getName() + ".id";
  private static final Pattern SAFE = Pattern.compile("[A-Za-z0-9._-]{1,64}");

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    String supplied = request.getHeader(HEADER);
    String id =
        supplied != null && SAFE.matcher(supplied).matches()
            ? supplied
            : UUID.randomUUID().toString();
    request.setAttribute(ATTRIBUTE, id);
    response.setHeader(HEADER, id);
    long startedAt = System.nanoTime();
    try (MDC.MDCCloseable ignored = MDC.putCloseable("correlationId", id)) {
      try {
        chain.doFilter(request, response);
      } finally {
        long durationMillis = (System.nanoTime() - startedAt) / 1_000_000;
        log.info(
            "http_request method={} path={} status={} durationMs={}",
            request.getMethod(),
            request.getRequestURI(),
            response.getStatus(),
            durationMillis);
      }
    }
  }
}
