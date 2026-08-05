package com.walletwise;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.walletwise.audit.AuditService;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@SpringBootTest(
    properties = {"app.demo-seed-enabled=false", "spring.task.scheduling.enabled=false"})
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class WalletWisePostgresIntegrationTest {
  @Container @ServiceConnection
  static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16.10-alpine3.22");

  @Autowired MockMvc mvc;
  @Autowired JdbcTemplate jdbc;
  @MockitoSpyBean AuditService audit;

  @Test
  void completeTransferIsAtomicAndIdempotent() throws Exception {
    String email = "integration-" + UUID.randomUUID() + "@example.com";
    String registration =
        """
        {"displayName":"Integration User","email":"%s","password":"StrongPass@123","preferredCurrency":"USD"}
        """
            .formatted(email);
    String authBody =
        mvc.perform(
                post("/api/v1/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(registration))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    String token = JsonPath.read(authBody, "$.accessToken");

    UUID source = createWallet(token, "Source", "1000.00");
    UUID destination = createWallet(token, "Destination", "100.00");
    String transfer =
        """
        {"sourceWalletId":"%s","destinationWalletId":"%s","amount":250.00,"note":"Test transfer"}
        """
            .formatted(source, destination);
    String first =
        mvc.perform(
                post("/api/v1/transfers")
                    .header("Authorization", "Bearer " + token)
                    .header("Idempotency-Key", "integration-key-0001")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(transfer))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("COMPLETED"))
            .andReturn()
            .getResponse()
            .getContentAsString();
    String replay =
        mvc.perform(
                post("/api/v1/transfers")
                    .header("Authorization", "Bearer " + token)
                    .header("Idempotency-Key", "integration-key-0001")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(transfer))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();

    assertThat(JsonPath.<String>read(replay, "$.id")).isEqualTo(JsonPath.read(first, "$.id"));
    assertThat(
            jdbc.queryForObject(
                "select count(*) from ledger_entries where transfer_id = ?",
                Long.class,
                UUID.fromString(JsonPath.read(first, "$.id"))))
        .isEqualTo(2L);

    String changed = transfer.replace("250.00", "200.00");
    mvc.perform(
            post("/api/v1/transfers")
                .header("Authorization", "Bearer " + token)
                .header("Idempotency-Key", "integration-key-0001")
                .contentType(MediaType.APPLICATION_JSON)
                .content(changed))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("idempotency_key_reused"));
  }

  @Test
  void healthAndFlywayManagedSchemaAreAvailable() throws Exception {
    mvc.perform(get("/actuator/health"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("UP"));
    assertThat(
            jdbc.queryForObject(
                "select count(*) from flyway_schema_history where success", Integer.class))
        .isGreaterThanOrEqualTo(2);
  }

  @Test
  void transferRejectsInvalidOwnershipCurrencyWalletAndBalanceWithoutMutation() throws Exception {
    String ownerToken = registerUser("Owner");
    String otherToken = registerUser("Other");
    UUID source = createWallet(ownerToken, "Rules source", "100.00");
    UUID destination = createWallet(ownerToken, "Rules destination", "10.00");
    UUID euros = createWallet(ownerToken, "Euro wallet", "10.00", "EUR");
    UUID otherWallet = createWallet(otherToken, "Other user wallet", "10.00");

    expectTransferStatus(ownerToken, source, source, "10.00", UUID.randomUUID().toString(), 422);
    expectTransferStatus(ownerToken, source, euros, "10.00", UUID.randomUUID().toString(), 422);
    expectTransferStatus(
        ownerToken, source, otherWallet, "10.00", UUID.randomUUID().toString(), 404);
    expectTransferStatus(
        ownerToken, source, destination, "500.00", UUID.randomUUID().toString(), 422);

    assertThat(balance(source)).isEqualByComparingTo("100.0000");
    assertThat(balance(destination)).isEqualByComparingTo("10.0000");
    assertThat(
            jdbc.queryForObject(
                "select count(*) from transfers where owner_id = (select owner_id from wallets where id = ?)",
                Long.class,
                source))
        .isZero();
  }

  @Test
  void concurrentDuplicateRequestsReplayOneTransfer() throws Exception {
    String token = registerUser("Concurrent duplicate");
    UUID source = createWallet(token, "Duplicate source", "1000.00");
    UUID destination = createWallet(token, "Duplicate destination", "0.00");
    String key = "duplicate-" + UUID.randomUUID();

    List<MvcResult> results =
        runConcurrently(
            () -> performTransfer(token, source, destination, "125.00", key),
            () -> performTransfer(token, source, destination, "125.00", key));

    assertThat(results)
        .allSatisfy(result -> assertThat(result.getResponse().getStatus()).isEqualTo(201));
    List<String> ids =
        results.stream()
            .map(
                result ->
                    JsonPath.<String>read(
                        new String(
                            result.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8),
                        "$.id"))
            .toList();
    assertThat(ids.get(0)).isEqualTo(ids.get(1));
    assertThat(balance(source)).isEqualByComparingTo("875.0000");
    assertThat(balance(destination)).isEqualByComparingTo("125.0000");
    assertThat(
            jdbc.queryForObject(
                "select count(*) from ledger_entries where transfer_id = ?",
                Long.class,
                UUID.fromString(ids.get(0))))
        .isEqualTo(2L);
  }

  @Test
  void concurrentTransfersSerializeOnTheSourceWallet() throws Exception {
    String token = registerUser("Concurrent balance");
    UUID source = createWallet(token, "Shared source", "1000.00");
    UUID destinationOne = createWallet(token, "Destination one", "0.00");
    UUID destinationTwo = createWallet(token, "Destination two", "0.00");

    List<MvcResult> results =
        runConcurrently(
            () ->
                performTransfer(
                    token, source, destinationOne, "800.00", "balance-one-" + UUID.randomUUID()),
            () ->
                performTransfer(
                    token, source, destinationTwo, "800.00", "balance-two-" + UUID.randomUUID()));
    List<Integer> statuses =
        new ArrayList<>(results.stream().map(result -> result.getResponse().getStatus()).toList());
    Collections.sort(statuses);

    assertThat(statuses).containsExactly(201, 422);
    assertThat(balance(source)).isEqualByComparingTo("200.0000");
    assertThat(balance(destinationOne).add(balance(destinationTwo)))
        .isEqualByComparingTo("800.0000");
  }

  @Test
  void transferRollsBackWhenADownstreamAuditWriteFails() throws Exception {
    String token = registerUser("Rollback");
    UUID source = createWallet(token, "Rollback source", "500.00");
    UUID destination = createWallet(token, "Rollback destination", "25.00");
    String key = "rollback-" + UUID.randomUUID();
    doThrow(new IllegalStateException("forced audit failure"))
        .when(audit)
        .success(any(), eq("TRANSFER_COMPLETED"), eq("TRANSFER"), any());

    try {
      expectTransferStatus(token, source, destination, "100.00", key, 500);
    } finally {
      reset(audit);
    }

    assertThat(balance(source)).isEqualByComparingTo("500.0000");
    assertThat(balance(destination)).isEqualByComparingTo("25.0000");
    assertThat(
            jdbc.queryForObject(
                "select count(*) from idempotency_records where idempotency_key = ?",
                Long.class,
                key))
        .isZero();
    assertThat(
            jdbc.queryForObject(
                "select count(*) from transfers where idempotency_key = ?", Long.class, key))
        .isZero();
  }

  @Test
  void regularUserCannotAccessAdministration() throws Exception {
    String token = registerUser("Regular user");
    mvc.perform(get("/api/v1/admin/users").header("Authorization", "Bearer " + token))
        .andExpect(status().isForbidden())
        .andExpect(header().exists("X-Correlation-Id"))
        .andExpect(jsonPath("$.correlationId").isNotEmpty());
  }

  @Test
  void disabledAccountCannotUseAnExistingAccessToken() throws Exception {
    String token = registerUser("Disabled user");
    String me =
        mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    UUID userId = UUID.fromString(JsonPath.read(me, "$.id"));
    jdbc.update("update app_users set enabled = false where id = ?", userId);

    mvc.perform(get("/api/v1/categories").header("Authorization", "Bearer " + token))
        .andExpect(status().isUnauthorized())
        .andExpect(header().exists("X-Correlation-Id"))
        .andExpect(jsonPath("$.code").value("authentication_required"));
  }

  @Test
  void registrationRejectsUnknownIsoCurrency() throws Exception {
    mvc.perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"displayName":"Currency User","email":"currency-%s@example.com","password":"StrongPass@123","preferredCurrency":"ZZZ"}
                    """
                        .formatted(UUID.randomUUID())))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(jsonPath("$.code").value("invalid_currency"));
  }

  @Test
  void malformedRequestsReturnProblemDetailsInsteadOfServerErrors() throws Exception {
    String token = registerUser("Malformed requests");
    UUID source = createWallet(token, "Malformed source", "100.00");
    UUID destination = createWallet(token, "Malformed destination", "0.00");
    String transfer =
        """
        {"sourceWalletId":"%s","destinationWalletId":"%s","amount":10.00}
        """
            .formatted(source, destination);

    mvc.perform(
            post("/api/v1/transfers")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(transfer))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("malformed_request"));
    mvc.perform(
            post("/api/v1/wallets")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("malformed_request"));
    mvc.perform(
            get("/api/v1/transactions?type=NOT_A_TYPE").header("Authorization", "Bearer " + token))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("malformed_request"));
    mvc.perform(get("/api/v1/does-not-exist").header("Authorization", "Bearer " + token))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("resource_not_found"));
  }

  @Test
  void unauthenticatedProblemsCarryCorrelationIds() throws Exception {
    mvc.perform(get("/api/v1/wallets"))
        .andExpect(status().isUnauthorized())
        .andExpect(header().exists("X-Correlation-Id"))
        .andExpect(jsonPath("$.correlationId").isNotEmpty());
  }

  private UUID createWallet(String token, String name, String openingBalance) throws Exception {
    return createWallet(token, name, openingBalance, "USD");
  }

  private UUID createWallet(String token, String name, String openingBalance, String currency)
      throws Exception {
    String response =
        mvc.perform(
                post("/api/v1/wallets")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {"name":"%s","type":"BANK","currency":"%s","openingBalance":%s}
                        """
                            .formatted(name, currency, openingBalance)))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return UUID.fromString(JsonPath.read(response, "$.id"));
  }

  private String registerUser(String displayName) throws Exception {
    String email = "integration-" + UUID.randomUUID() + "@example.com";
    String response =
        mvc.perform(
                post("/api/v1/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {"displayName":"%s","email":"%s","password":"StrongPass@123","preferredCurrency":"USD"}
                        """
                            .formatted(displayName, email)))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return JsonPath.read(response, "$.accessToken");
  }

  private void expectTransferStatus(
      String token, UUID source, UUID destination, String amount, String key, int expectedStatus)
      throws Exception {
    assertThat(performTransfer(token, source, destination, amount, key).getResponse().getStatus())
        .isEqualTo(expectedStatus);
  }

  private MvcResult performTransfer(
      String token, UUID source, UUID destination, String amount, String key) throws Exception {
    return mvc.perform(
            post("/api/v1/transfers")
                .header("Authorization", "Bearer " + token)
                .header("Idempotency-Key", key)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"sourceWalletId":"%s","destinationWalletId":"%s","amount":%s,"note":"Integration transfer"}
                    """
                        .formatted(source, destination, amount)))
        .andReturn();
  }

  @SafeVarargs
  private List<MvcResult> runConcurrently(ThrowingRequest... requests) throws Exception {
    CountDownLatch ready = new CountDownLatch(requests.length);
    CountDownLatch start = new CountDownLatch(1);
    try (ExecutorService executor = Executors.newFixedThreadPool(requests.length)) {
      List<Future<MvcResult>> futures = new ArrayList<>();
      for (ThrowingRequest request : requests) {
        futures.add(
            executor.submit(
                () -> {
                  ready.countDown();
                  start.await();
                  return request.perform();
                }));
      }
      ready.await();
      start.countDown();
      List<MvcResult> results = new ArrayList<>();
      for (Future<MvcResult> future : futures) results.add(future.get());
      return results;
    }
  }

  private BigDecimal balance(UUID walletId) {
    return jdbc.queryForObject(
        "select current_balance from wallets where id = ?", BigDecimal.class, walletId);
  }

  @FunctionalInterface
  private interface ThrowingRequest {
    MvcResult perform() throws Exception;
  }
}
