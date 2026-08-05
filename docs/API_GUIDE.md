# WalletWise API guide

The REST API is versioned under `/api/v1`. Local Swagger UI is available at
`http://localhost:8080/swagger-ui/index.html`, and the OpenAPI document is at
`http://localhost:8080/v3/api-docs`.

Examples use only synthetic data. Never commit a valid access token, refresh
cookie, production credential, or real financial information.

## Conventions

- JSON request and response bodies use `application/json`.
- Monetary values are decimal numbers and must not be calculated with binary
  floating point in a client.
- IDs are UUIDs.
- Timestamps are ISO-8601 values and are stored in UTC.
- Access-controlled calls use `Authorization: Bearer <access-token>`.
- Transfer creation also requires `Idempotency-Key`.
- Paginated endpoints cap `size` at 100.
- Expected errors use `application/problem+json` and RFC 7807 fields.
- Every response includes an `X-Correlation-Id` that can be used to locate
  sanitized server logs.

## Endpoint map

| Area | Method and path | Purpose |
|---|---|---|
| Authentication | `POST /api/v1/auth/register` | Register and start a session |
| Authentication | `POST /api/v1/auth/login` | Verify credentials and start a session |
| Authentication | `POST /api/v1/auth/refresh` | Rotate refresh token and return a new access token |
| Authentication | `POST /api/v1/auth/logout` | Revoke refresh token and clear cookie |
| Authentication | `GET /api/v1/auth/me` | Return the authenticated user |
| Categories | `GET /api/v1/categories` | List allowed income and expense categories |
| Wallets | `GET, POST /api/v1/wallets` | List or create owned wallets |
| Wallets | `GET /api/v1/wallets/{id}` | Get one owned wallet |
| Wallets | `PATCH /api/v1/wallets/{id}` | Rename or change the type of an owned wallet |
| Wallets | `POST /api/v1/wallets/{id}/archive` | Archive a wallet |
| Wallets | `POST /api/v1/wallets/{id}/restore` | Restore a wallet |
| Ledger | `POST /api/v1/transactions/income` | Record income |
| Ledger | `POST /api/v1/transactions/expense` | Record expense |
| Ledger | `POST /api/v1/transactions/adjustment` | Record an explicit correction |
| Ledger | `GET /api/v1/transactions` | Search, sort, and page owned entries |
| Ledger | `GET /api/v1/transactions/{id}` | Get one owned entry |
| Transfers | `POST /api/v1/transfers` | Atomically move virtual funds |
| Transfers | `GET /api/v1/transfers` | Page owned transfer history |
| Transfers | `GET /api/v1/transfers/{id}` | Get one owned transfer |
| Budgets | `GET, POST /api/v1/budgets` | List monthly progress or create a budget |
| Budgets | `GET /api/v1/budgets/{id}` | Get one owned budget and progress |
| Budgets | `PATCH, DELETE /api/v1/budgets/{id}` | Change or safely delete a budget |
| Analytics | `GET /api/v1/analytics/monthly` | Get monthly totals and breakdowns |
| Notifications | `GET /api/v1/notifications` | List owned notifications |
| Notifications | `GET /api/v1/notifications/unread-count` | Get unread count |
| Notifications | `PATCH /api/v1/notifications/{id}/read` | Mark one owned notification read |
| Notifications | `PATCH /api/v1/notifications/read-all` | Mark all owned notifications read |
| Administration | `GET /api/v1/admin/users` | Page users (`ADMIN`) |
| Administration | `PATCH /api/v1/admin/users/{id}/status` | Change enabled state (`ADMIN`) |
| Administration | `GET /api/v1/admin/audit-logs` | Search sanitized audits (`ADMIN`) |
| Administration | `POST /api/v1/admin/jobs/budget-alerts/run` | Trigger alert evaluation (`ADMIN`) |

OpenAPI is the authoritative list of status codes and schemas for a specific
release.

## Register and sign in

Registration request:

```bash
curl --request POST http://localhost:8080/api/v1/auth/register \
  --header "Content-Type: application/json" \
  --cookie-jar cookie-jar.txt \
  --data '{
    "displayName": "API Example",
    "email": "api.example@walletwise.test",
    "password": "Example@12345",
    "preferredCurrency": "USD"
  }'
```

Login uses the same cookie jar so curl retains the HttpOnly refresh cookie:

```bash
curl --request POST http://localhost:8080/api/v1/auth/login \
  --header "Content-Type: application/json" \
  --cookie-jar cookie-jar.txt \
  --data '{
    "email": "api.example@walletwise.test",
    "password": "Example@12345"
  }'
```

A successful authentication response has this shape:

```json
{
  "accessToken": "<short-lived-jwt>",
  "tokenType": "Bearer",
  "expiresAt": "2026-08-05T12:15:00Z",
  "user": {
    "id": "70e9b616-d45d-4c99-9ce4-229638bb9dd6",
    "displayName": "API Example",
    "email": "api.example@walletwise.test",
    "role": "USER",
    "preferredCurrency": "USD",
    "enabled": true
  }
}
```

The refresh token is not in this JSON. It is delivered in an HttpOnly cookie.
Copy the access token only into process memory or an API client variable for
subsequent examples.

## Refresh and logout

Refresh presents the cookie, rotates it, and returns a new access-token response:

```bash
curl --request POST http://localhost:8080/api/v1/auth/refresh \
  --cookie cookie-jar.txt \
  --cookie-jar cookie-jar.txt
```

Logout revokes the active refresh-token record and clears its cookie:

```bash
curl --request POST http://localhost:8080/api/v1/auth/logout \
  --cookie cookie-jar.txt \
  --cookie-jar cookie-jar.txt
```

The frontend automatically performs at most one access-token retry after a
successful refresh. It must not enter a refresh loop.

## Create a wallet

An opening balance creates an explicit `OPENING_BALANCE` ledger entry:

```bash
curl --request POST http://localhost:8080/api/v1/wallets \
  --header "Authorization: Bearer $ACCESS_TOKEN" \
  --header "Content-Type: application/json" \
  --data '{
    "name": "API checking",
    "type": "BANK",
    "currency": "USD",
    "openingBalance": 500.00
  }'
```

The caller may archive a wallet that has ledger history; the service does not
physically delete it.

## Record income and expense

Get an allowed `categoryId` from `GET /api/v1/categories`, then record income:

```bash
curl --request POST http://localhost:8080/api/v1/transactions/income \
  --header "Authorization: Bearer $ACCESS_TOKEN" \
  --header "Content-Type: application/json" \
  --data '{
    "walletId": "<wallet-uuid>",
    "amount": 1200.00,
    "categoryId": "<income-category-uuid>",
    "description": "Synthetic consulting income",
    "occurredAt": "2026-08-05T10:00:00Z"
  }'
```

Expense has the same shape at `/api/v1/transactions/expense` with an expense
category. A non-credit wallet cannot be taken below zero. `occurredAt` is
optional; the server clock is used when it is absent.

## Idempotent transfer

Generate one high-entropy key per intended transfer and keep it stable across
network retries:

```bash
curl --request POST http://localhost:8080/api/v1/transfers \
  --header "Authorization: Bearer $ACCESS_TOKEN" \
  --header "Content-Type: application/json" \
  --header "Idempotency-Key: 995cbd9a-98ca-4ee5-82c0-526a1738dbcb" \
  --data '{
    "sourceWalletId": "<source-wallet-uuid>",
    "destinationWalletId": "<destination-wallet-uuid>",
    "amount": 75.00,
    "note": "Synthetic savings transfer"
  }'
```

The first successful call returns `201 Created`. Retrying the identical
canonical request with the same key returns the original `201` response and
does not move the balance again. Reusing that key with a different source,
destination, amount, or note returns `409 Conflict`.

The key is scoped to the authenticated user and transfer operation. Another
user's same textual key is independent. An idempotency key is not an
authentication secret, but it should still be unguessable and bounded in size.

## Transaction filters and pagination

`GET /api/v1/transactions` supports server-side filters:

```bash
curl --get http://localhost:8080/api/v1/transactions \
  --header "Authorization: Bearer $ACCESS_TOKEN" \
  --data-urlencode "walletId=<wallet-uuid>" \
  --data-urlencode "type=EXPENSE" \
  --data-urlencode "categoryId=<category-uuid>" \
  --data-urlencode "startDate=2026-08-01T00:00:00Z" \
  --data-urlencode "endDate=2026-08-31T23:59:59Z" \
  --data-urlencode "minAmount=10.00" \
  --data-urlencode "maxAmount=250.00" \
  --data-urlencode "description=groceries" \
  --data-urlencode "page=0" \
  --data-urlencode "size=20" \
  --data-urlencode "sort=occurredAt" \
  --data-urlencode "direction=desc"
```

Pagination is zero-based. The response includes the current content plus page,
size, total-element, total-page, first, and last metadata. Stable sorting adds
an ID tie-breaker when necessary. Unknown sort fields and page sizes above 100
are rejected rather than interpolated into a query.

## Budgets and analytics

Create one budget per user, expense category, and calendar month:

```json
{
  "categoryId": "<expense-category-uuid>",
  "month": "2026-08",
  "limitAmount": 450.00,
  "alertThresholdPercent": 80
}
```

Update an existing budget with `PATCH /api/v1/budgets/{id}`:

```json
{
  "limitAmount": 500.00,
  "alertThresholdPercent": 85
}
```

List progress with `GET /api/v1/budgets?month=2026-08`. Monthly analytics are
available from `GET /api/v1/analytics/monthly?month=2026-08` and return totals
and category/wallet breakdowns derived by database aggregation.

## Problem details

Validation, authorization, business conflicts, and unexpected failures share a
sanitized format:

```json
{
  "type": "https://walletwise.app/problems/validation_failed",
  "title": "Bad Request",
  "status": 400,
  "detail": "Request validation failed",
  "instance": "/api/v1/transfers",
  "code": "validation_failed",
  "timestamp": "2026-08-05T12:00:00Z",
  "correlationId": "401adeb9-d5c9-4f73-8f79-d99824928c1d",
  "validationErrors": {
    "amount": "must be greater than 0"
  }
}
```

Common statuses are:

- `400` for malformed or invalid input;
- `401` for absent, expired, or invalid authentication;
- `403` for a known caller without the required role or ownership;
- `404` for an owned resource that is absent or intentionally concealed;
- `409` for business conflicts such as idempotency-key misuse;
- `422` when a syntactically valid operation violates a domain rule, when the
  documented endpoint uses that distinction; and
- `500` for an unexpected sanitized failure.

Problem responses never expose stack traces, SQL, database objects, internal
paths, cookies, or token values.

## Postman

Import both files under `postman/`, select the local environment, and run login
before protected requests. Collection scripts copy access tokens and newly
created UUIDs into collection variables. The committed environment leaves token
and resource values blank. Postman maintains the refresh cookie in its cookie
jar; never paste it into a variable or export it with the collection.
