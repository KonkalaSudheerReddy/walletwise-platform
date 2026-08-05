package com.walletwise.analytics;

import com.walletwise.analytics.MonthlyAnalyticsService.MonthlyAnalyticsResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/analytics")
@Tag(name = "Analytics", description = "Server-side monthly financial aggregation")
public class AnalyticsController {
  private final MonthlyAnalyticsService analytics;

  public AnalyticsController(MonthlyAnalyticsService analytics) {
    this.analytics = analytics;
  }

  @GetMapping("/monthly")
  @Operation(
      summary = "Get monthly analytics",
      description =
          "Accepts YYYY-MM and ISO-4217 currency filters; no currency conversion is performed.")
  MonthlyAnalyticsResponse monthly(
      @RequestParam(required = false) String month,
      @RequestParam(required = false) String currency) {
    return analytics.monthly(month, currency);
  }
}
