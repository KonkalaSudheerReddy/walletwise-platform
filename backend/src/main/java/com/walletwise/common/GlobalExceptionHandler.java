package com.walletwise.common;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public final class GlobalExceptionHandler {
  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
  private final Clock clock;

  public GlobalExceptionHandler(Clock clock) {
    this.clock = clock;
  }

  @ExceptionHandler(ApiException.class)
  ProblemDetail api(ApiException exception, HttpServletRequest request) {
    return problem(exception.status(), exception.code(), exception.getMessage(), request);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  ProblemDetail validation(MethodArgumentNotValidException exception, HttpServletRequest request) {
    ProblemDetail detail =
        problem(HttpStatus.BAD_REQUEST, "validation_failed", "Request validation failed", request);
    Map<String, String> errors = new LinkedHashMap<>();
    exception
        .getBindingResult()
        .getFieldErrors()
        .forEach(error -> errors.putIfAbsent(error.getField(), error.getDefaultMessage()));
    detail.setProperty("validationErrors", errors);
    return detail;
  }

  @ExceptionHandler(ConstraintViolationException.class)
  ProblemDetail constraint(ConstraintViolationException exception, HttpServletRequest request) {
    return problem(
        HttpStatus.BAD_REQUEST,
        "validation_failed",
        "One or more request parameters are invalid",
        request);
  }

  @ExceptionHandler(HandlerMethodValidationException.class)
  ProblemDetail methodValidation(
      HandlerMethodValidationException exception, HttpServletRequest request) {
    return problem(
        HttpStatus.BAD_REQUEST,
        "validation_failed",
        "One or more request parameters are invalid",
        request);
  }

  @ExceptionHandler({
    ServletRequestBindingException.class,
    MethodArgumentTypeMismatchException.class,
    HttpMessageNotReadableException.class
  })
  ProblemDetail malformedRequest(Exception exception, HttpServletRequest request) {
    return problem(
        HttpStatus.BAD_REQUEST,
        "malformed_request",
        "The request is missing required data or contains an invalid value",
        request);
  }

  @ExceptionHandler(NoResourceFoundException.class)
  ProblemDetail notFound(NoResourceFoundException exception, HttpServletRequest request) {
    return problem(HttpStatus.NOT_FOUND, "resource_not_found", "Resource not found", request);
  }

  @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
  ProblemDetail methodNotAllowed(
      HttpRequestMethodNotSupportedException exception, HttpServletRequest request) {
    return problem(
        HttpStatus.METHOD_NOT_ALLOWED,
        "method_not_allowed",
        "The HTTP method is not supported for this resource",
        request);
  }

  @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
  ProblemDetail mediaTypeNotSupported(
      HttpMediaTypeNotSupportedException exception, HttpServletRequest request) {
    return problem(
        HttpStatus.UNSUPPORTED_MEDIA_TYPE,
        "unsupported_media_type",
        "The request content type is not supported",
        request);
  }

  @ExceptionHandler(AuthenticationException.class)
  ProblemDetail authentication(AuthenticationException exception, HttpServletRequest request) {
    return problem(
        HttpStatus.UNAUTHORIZED,
        "authentication_failed",
        "Authentication is required or invalid",
        request);
  }

  @ExceptionHandler(AccessDeniedException.class)
  ProblemDetail denied(AccessDeniedException exception, HttpServletRequest request) {
    return problem(
        HttpStatus.FORBIDDEN,
        "access_denied",
        "You are not allowed to perform this action",
        request);
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  ProblemDetail integrity(DataIntegrityViolationException exception, HttpServletRequest request) {
    return problem(
        HttpStatus.CONFLICT, "data_conflict", "The request conflicts with existing data", request);
  }

  @ExceptionHandler(Exception.class)
  ProblemDetail unexpected(Exception exception, HttpServletRequest request) {
    log.error("Unhandled request failure", exception);
    return problem(
        HttpStatus.INTERNAL_SERVER_ERROR,
        "internal_error",
        "An unexpected error occurred",
        request);
  }

  private ProblemDetail problem(
      HttpStatus status, String code, String message, HttpServletRequest request) {
    ProblemDetail detail = ProblemDetail.forStatusAndDetail(status, message);
    detail.setTitle(status.getReasonPhrase());
    detail.setType(URI.create("https://walletwise.app/problems/" + code));
    detail.setInstance(URI.create(request.getRequestURI()));
    detail.setProperty("code", code);
    detail.setProperty("timestamp", Instant.now(clock).toString());
    detail.setProperty("correlationId", request.getAttribute(CorrelationIdFilter.ATTRIBUTE));
    return detail;
  }
}
