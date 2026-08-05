package com.walletwise.analytics;

import com.walletwise.analytics.MonthlyAnalyticsService.MonthlyAnalyticsResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/analytics")
public class AnalyticsController {
  private final MonthlyAnalyticsService analytics;

  public AnalyticsController(MonthlyAnalyticsService analytics) {
    this.analytics = analytics;
  }

  @GetMapping("/monthly")
  MonthlyAnalyticsResponse monthly(
      @RequestParam(required = false) String month,
      @RequestParam(required = false) String currency) {
    return analytics.monthly(month, currency);
  }
}
