# Resume and interview guide

Use these statements only after the corresponding code and verification are
present in the public repository. Do not add performance, scale, availability,
security-certification, or business-impact numbers that were not measured.

## Four resume bullets

- Built WalletWise, a Java 21 and Spring Boot modular monolith with a React and
  TypeScript interface for virtual wallets, immutable ledger activity, monthly
  budgets, notifications, and financial analytics.
- Implemented atomic wallet transfers with PostgreSQL row locks, deterministic
  lock ordering, two linked ledger entries, and persistent caller-scoped
  idempotency that safely replays identical requests and rejects key misuse.
- Designed JWT authentication with short-lived in-memory access tokens, rotated
  opaque refresh tokens stored as hashes, role and ownership authorization,
  RFC 7807 errors, correlation IDs, and append-only sanitized audit records.
- Created PostgreSQL/Testcontainers, JUnit, Vitest, and Playwright verification,
  plus a multi-stage non-root Docker image, Compose environment, GitHub Actions,
  Codespaces configuration, OpenAPI, and deployment-ready Render/Neon docs.

## 30-second explanation

WalletWise is a portfolio-quality virtual wallet and expense tracker that I
built with Java 21, Spring Boot, React, and PostgreSQL. The main engineering
feature is a transfer API that moves virtual funds exactly once from the user's
perspective: it persists an idempotency key, locks both wallet rows in a stable
order, updates two balances, creates two linked immutable ledger entries, and
records an audit event in one transaction. It also demonstrates secure JWT and
refresh-token handling, server-side transaction filtering, monthly budgets and
analytics, tests at several levels, and a production Docker delivery path.

## Two-minute architecture explanation

WalletWise is a modular monolith because the domain has several features but
important workflows need one ACID transaction. The backend is organized by
authentication, users, wallets, ledger transactions, transfers, budgets,
notifications, analytics, audits, and admin behavior. Controllers work with
validated request and response records rather than JPA entities. Services own
business transactions, and repositories always scope user resources by both
resource ID and the authenticated owner ID.

PostgreSQL is the source of truth, and Flyway owns the schema. Each wallet keeps
a current balance for efficient reads, but every balance change creates an
immutable ledger entry in the same transaction. For a transfer, I require an
`Idempotency-Key`, persist a canonical request hash, and enforce a unique
user/operation/key constraint. I then lock both wallet rows in UUID order to
avoid inconsistent balance changes and reduce deadlock risk. Both balance
updates, the transfer record, outgoing and incoming ledger rows, the audit, and
the stored response commit together. The same key and body replays the original
result; the same key with a different body is a conflict.

Security uses a short-lived signed JWT for ordinary API calls and a random
opaque refresh token in an HttpOnly cookie. Only the refresh-token hash is
stored, and it is rotated on refresh and revoked on logout. The frontend keeps
the access token in memory, performs one coordinated refresh retry, and uses
same-origin production delivery. React is built during the multi-stage Docker
build and served by Spring Boot, so the deployed system is one web process plus
PostgreSQL. Unit, PostgreSQL/Testcontainers integration, component, API smoke,
and Playwright tests cover the different failure boundaries.

## Core engineering explanations

### Why use BigDecimal?

Binary floating-point types cannot represent many decimal fractions exactly;
for example, repeated operations involving `0.1` can accumulate representation
error. Money needs explicit decimal precision and rounding rules. Java
`BigDecimal` maps cleanly to PostgreSQL `numeric`, preserves decimal intent, and
allows scale and rounding to be chosen at controlled boundaries. Values are
constructed from decimal strings or parsed JSON, not `new BigDecimal(double)`.

### How does transfer atomicity work?

The transfer service method is one database transaction. It locks both wallets,
validates the business rules again while locked, subtracts and adds the amount,
creates `TRANSFER_OUT` and `TRANSFER_IN` ledger entries, completes the transfer,
records the audit, and stores the idempotent response. Spring commits once at
the service boundary. Any unchecked failure marks the transaction for rollback,
so users cannot observe one updated wallet or only one side of the ledger.

### How does idempotency work?

The client creates one unique key for one intended transfer. The server scopes
that key to the authenticated user and operation, canonicalizes relevant body
fields, and stores their hash. A unique database constraint resolves races. If
the completed key returns with the same hash, the server replays the stored
status and response. A different hash means the caller reused a key for another
operation and receives `409 Conflict`. Persisting this state means a retry is
safe after a process restart.

### How are concurrent transfer requests handled?

Idempotency serializes concurrent copies of one logical request. Wallet row
locks handle distinct logical transfers touching the same balance. The service
sorts wallet UUIDs and acquires locks in the same order for every request,
reducing cyclic waits. A waiting transaction validates the newly committed
balance after it receives the lock, so it cannot spend from stale application
state. Integration tests use coordinated threads and assert the final ledger
and balances.

### How do JWT and refresh tokens work?

The access JWT lasts about 15 minutes and includes the user UUID and role. Its
signature lets the server validate ordinary calls without a session query. The
frontend holds it only in memory. The seven-day refresh token is an opaque
random value in an HttpOnly cookie; its hash, expiry, and revocation state are
stored in PostgreSQL. Refresh rotates both credentials, and logout revokes the
refresh record. This balances fast ordinary calls with server-side control of
session continuation.

### How is user ownership enforced?

The server derives the user ID from the validated JWT subject. A request body or
path can identify a resource but cannot assert its owner. Repositories and
services load resources by `resourceId AND ownerId`, and cross-feature actions
repeat ownership checks for every involved resource. Method security separately
enforces administrator roles. Tests create two users and prove one cannot read
or modify the other's UUIDs.

### Why use Flyway?

Application entities describe the current Java model but do not provide a safe,
reviewable history of schema changes. Flyway applies ordered SQL migrations,
records checksums, and creates the same constraints and indexes locally, in CI,
and in hosted PostgreSQL. Hibernate uses `validate`, so drift is detected rather
than silently repaired. Applied migrations are not casually edited; changes are
forward migrations.

### How does pagination work?

The client sends a zero-based page, bounded size, allowed sort field, and
direction. Filters become database predicates, and the database returns only
the requested slice plus a count for page metadata. The API caps size at 100,
rejects unknown sort fields, and uses a stable ID tie-breaker. This prevents
loading the complete ledger into Java memory and keeps responses predictable as
history grows.

### Why a modular monolith?

The modules make responsibilities and dependencies visible without introducing
network calls or distributed transactions. Wallet transfers strongly benefit
from one ACID boundary, and a portfolio deployment benefits from one image, one
log stream, and one health check. If scale or team boundaries later justify a
service split, the feature interfaces identify candidates, but extraction would
still require deliberate data ownership and distributed-failure design.

## Common interview questions

### Why store a wallet balance if there is a ledger?

The stored balance makes wallet lists and authorization-time balance checks
cheap. It is safe only because the service updates it in the same transaction
as the immutable ledger entry. Tests recalculate and compare the invariant. A
larger regulated ledger might derive or reconcile balances differently; this
project chooses a pragmatic consistency model and documents it.

### Why not use optimistic locking?

Optimistic version checks can work with bounded retries, but transfers touch two
wallets and need predictable behavior under contention. PostgreSQL pessimistic
row locks make the critical section explicit and let the second transaction
validate the committed balance. Deterministic order reduces deadlock risk. The
tradeoff is shorter throughput under heavy contention, so the transaction must
perform no slow network calls while locks are held.

### What if the application crashes after moving money but before replying?

The database has either committed the entire transaction or rolled it back. If
it committed, the completed idempotency record contains the response and a
client retry replays it. If it did not commit, the reservation and money changes
are both absent and a retry can execute normally. Correctness does not depend on
the original HTTP connection surviving.

### Why not use Redis for idempotency?

PostgreSQL is already the transactional system of record. Keeping the
idempotency reservation and transfer outcome in the same database avoids a
cross-system commit problem and remains correct after restart. Redis could help
at much higher request volume, but it would need a carefully designed durable
coordination strategy and is unnecessary for this project's requirements.

### Are JWTs revocable immediately?

Refresh tokens are revocable immediately, but an already-issued access JWT can
normally remain valid until its short expiry. Disabling a user can be enforced
with an additional account-state lookup or token-version mechanism if immediate
revocation is required. The project keeps access lifetime short and states this
tradeoff instead of claiming perfect stateless revocation.

### Why does identical idempotent replay return 201 again?

It replays the original HTTP outcome rather than pretending to create a second
resource or changing the contract based on network timing. The body identifies
the same transfer. The critical guarantee is that balances and ledger entries
are not duplicated.

### How do you avoid exposing internal errors?

A global exception handler maps known validation, authentication, authorization,
not-found, and conflict cases to RFC 7807 problem documents. Unknown exceptions
are logged with a correlation ID and returned as a generic problem. The response
does not include stack traces, SQL, internal paths, or credentials, and logging
filters redact sensitive headers and bodies.

### How would you scale the project?

First measure query, lock, connection, and JVM behavior. Stateless access-token
validation permits multiple application instances because sessions and
idempotency live in PostgreSQL. I would tune indexes and projections, size the
connection pool, add reconciliation and backups, and move static assets to a CDN
before considering services. A service split would be driven by independent
scale or team ownership, not by feature count alone.

## Honest limitations and future improvements

- All balances are virtual; there is no bank, payment, or card integration.
- Transfers require matching currencies; there is no exchange-rate engine.
- There is no MFA, email verification, password-reset delivery, or device view.
- Access-token immediate revocation, fine-grained permissions, rate limiting,
  managed key rotation, and formal reconciliation would be needed for a more
  demanding environment.
- The architecture runs one database and one application deployment; it is not
  designed or benchmarked for a stated transaction volume.
- Hosted free tiers can sleep, impose usage limits, and have cold starts.
- The system has no legal compliance or security-certification claim and must
  not contain real financial or personal data.
- Sensible future work includes accessibility audits, stronger operational
  dashboards, scheduled backup/restore exercises, ledger reconciliation, email
  notifications, and carefully designed multi-currency support.

