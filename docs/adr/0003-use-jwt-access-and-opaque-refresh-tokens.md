# ADR 0003: Use JWT access and opaque refresh tokens

- Status: Accepted
- Date: 2026-08-05

## Context

The React client needs an authenticated session without sending a password on
every API call. Long-lived browser JWTs are difficult to revoke, while checking
a database session for every ordinary request removes the main simplicity of a
short-lived signed access token. Browser persistence such as localStorage
increases the exposure of bearer tokens to injected scripts.

## Decision

Issue a signed access JWT with an approximately 15-minute lifetime. Its subject
is the user UUID and it includes the role. Keep it only in frontend memory and
send it in the `Authorization` header.

Issue a cryptographically random opaque refresh token with an approximately
seven-day lifetime in an HttpOnly cookie. Persist only a secure hash. Rotate the
token on every successful refresh, revoke it on logout, and reject expired,
revoked, or replaced records. Use `Secure` in production, `SameSite=Lax` for the
same-origin deployment, and a restricted cookie path.

## Consequences

- Ordinary API authentication is stateless until access-token expiry.
- The server retains control over session continuation through refresh-token
  rotation and revocation.
- A stolen access JWT remains usable until expiry unless a broader emergency
  signing-key or account policy is applied.
- A page reload requires a refresh call because the access token is not stored
  in localStorage or sessionStorage.
- Refresh coordination in the frontend must prevent duplicate refresh storms and
  infinite retry loops.
- The signing secret and database token hashes are security-sensitive and must
  not appear in code, logs, audits, screenshots, or API examples.

