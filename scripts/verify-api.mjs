import { randomUUID } from "node:crypto";

const baseUrl = (process.env.WALLETWISE_BASE_URL ?? "http://localhost:8080").replace(/\/$/, "");
const requestTimeoutMs = Number(process.env.WALLETWISE_REQUEST_TIMEOUT_MS ?? "15000");
let accessToken;
let refreshCookie;

function invariant(condition, message) {
  if (!condition) {
    throw new Error(message);
  }
}

function readCookie(response) {
  const setCookie = response.headers.get("set-cookie");
  if (!setCookie) return;
  const firstPart = setCookie.split(";", 1)[0];
  if (firstPart.includes("=")) refreshCookie = firstPart;
}

async function call(path, { method = "GET", body, statuses = [200], headers = {}, auth = true } = {}) {
  const requestHeaders = {
    Accept: "application/json, application/problem+json",
    "X-Correlation-Id": `api-smoke-${randomUUID()}`,
    ...headers,
  };

  if (body !== undefined) requestHeaders["Content-Type"] = "application/json";
  if (auth && accessToken) requestHeaders.Authorization = `Bearer ${accessToken}`;
  if (refreshCookie) requestHeaders.Cookie = refreshCookie;

  const response = await fetch(`${baseUrl}${path}`, {
    method,
    headers: requestHeaders,
    body: body === undefined ? undefined : JSON.stringify(body),
    redirect: "manual",
    signal: AbortSignal.timeout(requestTimeoutMs),
  });
  readCookie(response);

  const text = await response.text();
  let payload;
  if (text) {
    try {
      payload = JSON.parse(text);
    } catch {
      payload = undefined;
    }
  }

  if (!statuses.includes(response.status)) {
    const safeDetail = payload?.detail ?? payload?.title ?? "No safe problem detail returned";
    throw new Error(`${method} ${path} returned ${response.status}: ${safeDetail}`);
  }

  invariant(response.headers.get("x-correlation-id"), `${method} ${path} omitted X-Correlation-Id`);
  return { response, payload };
}

async function main() {
  console.log(`Verifying WalletWise API at ${baseUrl}`);

  const health = await call("/actuator/health", { auth: false });
  invariant(health.payload?.status === "UP", "Health endpoint did not report UP");
  const openApi = await call("/v3/api-docs", { auth: false });
  invariant(openApi.payload?.openapi?.startsWith("3."), "OpenAPI document was unavailable");
  invariant(openApi.payload?.paths?.["/api/v1/transfers"], "OpenAPI omitted the transfer contract");
  const swagger = await call("/swagger-ui/index.html", { auth: false });
  invariant(
    swagger.response.headers.get("content-type")?.includes("text/html"),
    "Swagger UI did not return HTML"
  );
  console.log("  ✓ health, OpenAPI, and Swagger UI");

  const email = `api.smoke.${Date.now()}.${randomUUID().slice(0, 8)}@walletwise.test`;
  const password = "Smoke@123456";
  const registration = await call("/api/v1/auth/register", {
    method: "POST",
    auth: false,
    statuses: [201],
    body: { displayName: "API Smoke User", email, password, preferredCurrency: "USD" },
  });
  accessToken = registration.payload?.accessToken;
  invariant(accessToken, "Registration did not return an access token");
  invariant(refreshCookie, "Registration did not set a refresh cookie");

  const login = await call("/api/v1/auth/login", {
    method: "POST",
    auth: false,
    body: { email, password },
  });
  accessToken = login.payload?.accessToken;
  invariant(accessToken, "Login did not return an access token");
  console.log("  ✓ registration and login");

  const incomeCategories = await call("/api/v1/categories?type=INCOME");
  const expenseCategories = await call("/api/v1/categories?type=EXPENSE");
  const incomeCategoryId = incomeCategories.payload?.[0]?.id;
  const expenseCategoryId = expenseCategories.payload?.[0]?.id;
  invariant(incomeCategoryId && expenseCategoryId, "Default categories were not available");

  const source = await call("/api/v1/wallets", {
    method: "POST",
    statuses: [201],
    body: { name: "Smoke source", type: "BANK", currency: "USD", openingBalance: 500.0 },
  });
  const destination = await call("/api/v1/wallets", {
    method: "POST",
    statuses: [201],
    body: { name: "Smoke destination", type: "SAVINGS", currency: "USD", openingBalance: 0.0 },
  });
  const sourceWalletId = source.payload?.id;
  const destinationWalletId = destination.payload?.id;
  invariant(sourceWalletId && destinationWalletId, "Wallet creation did not return IDs");

  await call("/api/v1/transactions/income", {
    method: "POST",
    statuses: [201],
    body: {
      walletId: sourceWalletId,
      amount: 250.0,
      categoryId: incomeCategoryId,
      description: "API smoke synthetic income",
    },
  });
  console.log("  ✓ wallets and income");

  const idempotencyKey = randomUUID();
  const transferBody = {
    sourceWalletId,
    destinationWalletId,
    amount: 125.0,
    note: "API smoke idempotent transfer",
  };
  const firstTransfer = await call("/api/v1/transfers", {
    method: "POST",
    statuses: [201],
    headers: { "Idempotency-Key": idempotencyKey },
    body: transferBody,
  });
  const replay = await call("/api/v1/transfers", {
    method: "POST",
    statuses: [201],
    headers: { "Idempotency-Key": idempotencyKey },
    body: transferBody,
  });
  invariant(firstTransfer.payload?.id === replay.payload?.id, "Idempotent retry returned another transfer");

  await call("/api/v1/transfers", {
    method: "POST",
    statuses: [409],
    headers: { "Idempotency-Key": idempotencyKey },
    body: { ...transferBody, amount: 126.0 },
  });

  const sourceAfter = await call(`/api/v1/wallets/${sourceWalletId}`);
  const destinationAfter = await call(`/api/v1/wallets/${destinationWalletId}`);
  invariant(Number(sourceAfter.payload?.wallet?.balance) === 625, "Source balance indicates a missing or duplicate transfer");
  invariant(Number(destinationAfter.payload?.wallet?.balance) === 125, "Destination balance indicates a missing or duplicate transfer");
  console.log("  ✓ idempotent transfer, replay, conflict, and balances");

  const transactionQuery = new URLSearchParams({
    walletId: sourceWalletId,
    type: "INCOME",
    minAmount: "200.00",
    description: "API smoke",
    page: "0",
    size: "20",
    sort: "occurredAt",
    direction: "desc",
  });
  const transactions = await call(`/api/v1/transactions?${transactionQuery}`);
  invariant(Array.isArray(transactions.payload?.content), "Transaction response did not contain page content");
  invariant(transactions.payload.content.length >= 1, "Filtered transaction query returned no income entry");

  const now = new Date();
  const month = `${now.getUTCFullYear()}-${String(now.getUTCMonth() + 1).padStart(2, "0")}`;
  const analytics = await call(`/api/v1/analytics/monthly?month=${month}`);
  invariant(analytics.payload?.month === month, "Monthly analytics returned the wrong month");
  console.log("  ✓ filters, pagination, and analytics");

  const refresh = await call("/api/v1/auth/refresh", { method: "POST", auth: false });
  accessToken = refresh.payload?.accessToken;
  invariant(accessToken, "Refresh did not return a new access token");
  await call("/api/v1/auth/logout", { method: "POST", auth: false, statuses: [200, 204] });
  console.log("  ✓ refresh rotation and logout");

  console.log("WalletWise API verification completed successfully.");
}

main().catch((error) => {
  console.error(`WalletWise API verification failed: ${error.message}`);
  process.exitCode = 1;
});
