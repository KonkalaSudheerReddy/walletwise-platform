# Security design

WalletWise applies realistic application-security controls while remaining an
educational portfolio system. It is not a payment processor, bank, regulated
ledger, security certification, or substitute for a formal production threat
model.

## Assets and trust boundaries

The protected assets are user credentials, session tokens, virtual wallet and
ledger data, budget and notification data, and administrator-only audit
information. The browser, HTTP boundary, application process, and PostgreSQL
connection are separate trust boundaries. Hosted-provider consoles and CI are
operational trust boundaries and receive only the minimum secrets they need.

## Threat assumptions

| Threat | Primary control |
|---|---|
| Credential stuffing or password disclosure | BCrypt hashes, minimum password rules, generic sensitive errors, TLS in hosted environments |
| Access-token theft | Short lifetime, in-memory browser storage, no token logging |
| Refresh-token theft or replay | HttpOnly cookie, server-side hash, expiry, rotation, revocation, restricted path |
| Cross-user object access | Principal-derived owner ID and ownership-scoped queries/services |
| Duplicate transfer retry | Persistent caller-scoped idempotency record and request hash |
| Concurrent overspending | Pessimistic wallet locks and atomic balance/ledger transaction |
| Cross-site authenticated requests | SameSite cookie, restricted cookie path, same-origin production delivery, explicit CORS origins |
| Injection or malformed input | Typed request records, Bean Validation, parameterized JPA queries, bounded filters |
| Sensitive operational disclosure | Sanitized RFC 7807 errors, safe Actuator exposure, log and audit redaction |
| Secret leakage in delivery | Environment variables, ignored `.env`, secret scanning, provider secret stores, no build arguments for secrets |

## Password handling

Passwords are accepted only on authentication endpoints and hashed with BCrypt
at a reasonable cost. Plaintext values are not persisted, logged, included in
audit metadata, returned by the API, or placed in exception messages. Email
addresses are consistently trimmed and normalized before uniqueness checks and
authentication. Sensitive recovery-style flows should not reveal whether an
address exists.

## Access JWT

The access token is a signed JWT with an approximately 15-minute lifetime. Its
subject is the immutable user UUID and its role claim supports `USER` or
`ADMIN` authorization. A production signing secret is injected through
`JWT_SECRET`; startup rejects missing or shorter-than-32-byte secrets, and the
production profile also rejects the documented local-development prefix. The
frontend keeps the token in memory and sends it as a
Bearer credential. It is never stored in localStorage or sessionStorage.

Signing a token does not make its claims secret. No password, refresh token, or
sensitive financial detail belongs in the JWT.

## Opaque refresh tokens

Refresh tokens are generated from cryptographically secure random bytes and
last approximately seven days. The raw value is returned only through an
HttpOnly cookie; only a one-way hash is stored in PostgreSQL. A successful
refresh revokes the presented record, creates a replacement, rotates the cookie,
and returns a new access token. Logout revokes the current refresh record and
expires the cookie.

Same-origin browser tabs coordinate refresh through an exclusive Web Lock and
broadcast the new in-memory access token without persisting it. The server also
uses a three-second duplicate-request grace: immediate reuse is rejected without
revoking the successful replacement, while reuse after that bound revokes the
token family as a replay signal.

Production cookies use `Secure`, `HttpOnly`, `SameSite=Lax`, and the narrowest
practical path. Local HTTP development explicitly disables only `Secure`.

## Authorization and ownership

Spring method security protects role-sensitive services and administrator
routes. Regular service and repository methods combine the resource UUID with
the authenticated owner UUID. A client-supplied owner ID is never authoritative.
Disabled users cannot create a new session. Every bearer token is also checked
against current account status, so disabling an account invalidates existing
access without waiting for JWT expiry.

Administrator access is intentionally narrow. Admin users can inspect users and
sanitized audits, change enabled state, and trigger a controlled budget-alert
job; they do not receive another user's refresh token or password hash.

## CORS and request forgery

Native development allows only configured origins such as
`http://localhost:5173`. Wildcard origins are not combined with credentials.
Production serves the React application and API from the same origin. The
refresh cookie's SameSite policy and restricted endpoint reduce cross-site
requests; deployments with a genuinely cross-site frontend would require an
explicit CSRF design rather than a wildcard CORS change.

## Error and log safety

Expected failures use RFC 7807 `ProblemDetail` documents with a timestamp and
correlation ID. Validation errors identify fields without echoing secret input.
Production responses do not expose stack traces, SQL, table names, internal
paths, signing details, or database host information.

The correlation filter accepts only a bounded safe `X-Correlation-Id` or
creates a UUID, places it in logging context, and returns it in the response.
Request logging excludes bodies, cookies, `Authorization`, password fields, and
token values.

## Audit logging

Audit rows are append-only and record actor, action, resource, outcome,
timestamp, correlation ID, safely derived client information, and non-sensitive
JSON metadata. They never contain passwords, JWTs, opaque refresh tokens,
Authorization headers, cookies, complete authentication requests, or arbitrary
exception dumps.

## Secret management

- `.env.example` contains names and development placeholders, never real values.
- Local `.env` files and database volumes are ignored.
- Render and Neon values are entered in provider secret settings.
- `render.yaml` uses `sync: false` or provider-generated values for secrets.
- GitHub workflows use the repository-scoped `GITHUB_TOKEN` for GHCR and Pages;
  no personal token is committed.
- Docker build stages do not copy `.env` or accept secrets as image arguments.

Rotate a secret immediately if it appears in source or logs, then remove it from
history using an appropriate incident process. Deleting the visible line alone
is not sufficient.

## Actuator and API documentation

Only safe health and info endpoints are exposed. Environment, beans,
configuration properties, heap dumps, and other administrative endpoints stay
unavailable publicly. Swagger documents Bearer authentication and examples but
never embeds a valid token. Whether Swagger is public in a hosted demo is an
explicit deployment choice and not a substitute for endpoint authorization.

## Remaining risks and limitations

- A single symmetric signing secret has a larger blast radius than managed
  asymmetric key rotation.
- There is no email verification, MFA, password reset, device management, or
  centralized revocation list for already-issued access JWTs.
- Rate limiting and bot defense depend on the hosting edge and are not a core
  version-1 capability.
- The demo profile intentionally creates known synthetic credentials and must
  never be used with private or real financial information.
- Browser security headers and dependency alerts reduce risk but do not replace
  penetration testing, operational monitoring, backups, or incident response.
- This design makes no legal or regulatory compliance claim.
