package com.walletwise.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.walletwise.config.AppProperties;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

class SecurityConfigurationTest {
  private static final String DEVELOPMENT_ORIGIN = "http://localhost:5173";
  private static final String CODESPACES_ORIGIN = "https://example-codespace-8080.app.github.dev";

  @Test
  void excludesStaticAssetsFromApiCorsPolicy() throws Exception {
    CorsFilter filter = new CorsFilter(corsConfigurationSource());
    MockHttpServletRequest request = request("/assets/application.js", CODESPACES_ORIGIN);
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilter(
        request, response, (ignoredRequest, ignoredResponse) -> response.setStatus(200));

    assertThat(response.getStatus()).isEqualTo(200);
  }

  @Test
  void keepsRejectingUntrustedOriginsForApiRoutes() throws Exception {
    CorsFilter filter = new CorsFilter(corsConfigurationSource());
    MockHttpServletRequest request = request("/api/v1/auth/login", CODESPACES_ORIGIN);
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilter(
        request, response, (ignoredRequest, ignoredResponse) -> response.setStatus(200));

    assertThat(response.getStatus()).isEqualTo(403);
  }

  @Test
  void allowsConfiguredDevelopmentOriginForApiRoutes() throws Exception {
    CorsFilter filter = new CorsFilter(corsConfigurationSource());
    MockHttpServletRequest request = request("/api/v1/auth/login", DEVELOPMENT_ORIGIN);
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilter(
        request, response, (ignoredRequest, ignoredResponse) -> response.setStatus(200));

    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(response.getHeader("Access-Control-Allow-Origin")).isEqualTo(DEVELOPMENT_ORIGIN);
  }

  private static MockHttpServletRequest request(String path, String origin) {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", path);
    request.addHeader("Origin", origin);
    return request;
  }

  private static CorsConfigurationSource corsConfigurationSource() {
    AppProperties properties =
        new AppProperties(null, false, List.of(DEVELOPMENT_ORIGIN), false, null, null, null, null);
    return new SecurityConfiguration().corsConfigurationSource(properties);
  }
}
