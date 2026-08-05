import { randomUUID } from "node:crypto";

const baseUrl = (process.env.WALLETWISE_BASE_URL ?? "http://localhost:8080").replace(/\/$/, "");
const requestTimeoutMs = Number(process.env.WALLETWISE_REQUEST_TIMEOUT_MS ?? "15000");
const smokeEmail = process.env.WALLETWISE_SMOKE_EMAIL;
const smokePassword = process.env.WALLETWISE_SMOKE_PASSWORD ?? "Smoke@123456";

function invariant(condition, message) {
  if (!condition) throw new Error(message);
}

async function call(path, { method = "GET", body, token } = {}) {
  const headers = {
    Accept: "application/json, application/problem+json",
    "X-Correlation-Id": `persistence-${randomUUID()}`,
  };
  if (body !== undefined) headers["Content-Type"] = "application/json";
  if (token) headers.Authorization = `Bearer ${token}`;

  const response = await fetch(`${baseUrl}${path}`, {
    method,
    headers,
    body: body === undefined ? undefined : JSON.stringify(body),
    signal: AbortSignal.timeout(requestTimeoutMs),
  });
  const text = await response.text();
  let payload;
  if (text) {
    try {
      payload = JSON.parse(text);
    } catch {
      payload = undefined;
    }
  }
  if (!response.ok) {
    throw new Error(`${method} ${path} returned ${response.status}: ${payload?.detail ?? "failed"}`);
  }
  invariant(response.headers.get("x-correlation-id"), `${method} ${path} omitted X-Correlation-Id`);
  return payload;
}

async function login(email, password) {
  const payload = await call("/api/v1/auth/login", {
    method: "POST",
    body: { email, password },
  });
  invariant(payload?.accessToken, "Login did not return an access token");
  return payload.accessToken;
}

async function main() {
  invariant(smokeEmail, "WALLETWISE_SMOKE_EMAIL is required for persistence verification");
  console.log(`Verifying persisted WalletWise data after restart at ${baseUrl}`);

  const health = await call("/actuator/health");
  invariant(health?.status === "UP", "Health endpoint did not report UP after restart");

  const smokeToken = await login(smokeEmail, smokePassword);
  const smokeWallets = await call("/api/v1/wallets", { token: smokeToken });
  invariant(Array.isArray(smokeWallets), "Persisted wallet response was not an array");
  invariant(smokeWallets.length === 2, "API smoke wallets were missing or duplicated after restart");
  const sources = smokeWallets.filter((wallet) => wallet.name === "Smoke source");
  const destinations = smokeWallets.filter((wallet) => wallet.name === "Smoke destination");
  invariant(sources.length === 1 && destinations.length === 1, "API smoke wallet names changed");
  const source = sources[0];
  const destination = destinations[0];
  invariant(source && destination, "API smoke wallets did not persist across restart");
  invariant(Number(source.balance) === 625, "Persisted source wallet balance changed after restart");
  invariant(
    Number(destination.balance) === 125,
    "Persisted destination wallet balance changed after restart",
  );
  const smokeTransactions = await call("/api/v1/transactions?page=0&size=100", {
    token: smokeToken,
  });
  const smokeTransfers = await call("/api/v1/transfers?page=0&size=100", { token: smokeToken });
  invariant(smokeTransactions?.totalElements === 4, "Persisted smoke ledger was incomplete");
  invariant(smokeTransfers?.totalElements === 1, "Persisted smoke transfer was incomplete");
  console.log("  ✓ API-created wallets, ledger, balances, and transfer persisted across restart");

  const demoToken = await login("demo@walletwise.app", "Demo@12345");
  const demoWallets = await call("/api/v1/wallets", { token: demoToken });
  const demoWalletNames = demoWallets?.map((wallet) => wallet.name).sort();
  invariant(
    JSON.stringify(demoWalletNames) ===
      JSON.stringify(["Cash Wallet", "Emergency Savings", "Everyday Checking"]),
    "Demo wallets were missing or duplicated after restart",
  );

  const demoTransactions = await call("/api/v1/transactions?page=0&size=100", {
    token: demoToken,
  });
  const demoTransfers = await call("/api/v1/transfers?page=0&size=100", { token: demoToken });
  invariant(demoTransactions?.totalElements === 19, "Demo ledger was missing or duplicated after restart");
  invariant(demoTransfers?.totalElements === 1, "Demo transfer was missing or duplicated after restart");
  console.log("  ✓ demo reseeding rebuilt one deterministic, non-duplicated dataset");

  console.log("WalletWise restart and persistence verification completed successfully.");
}

main().catch((error) => {
  console.error(`WalletWise persistence verification failed: ${error.message}`);
  process.exitCode = 1;
});
