# ADR 0002: Use persistent idempotency for transfers

- Status: Accepted
- Date: 2026-08-05

## Context

A client cannot always know whether a timed-out transfer request committed. A
blind retry can move money twice. A process-local map cannot protect retries
after a restart or requests handled by another application instance, and a
random client key alone cannot detect reuse for a different request.

## Decision

Require `Idempotency-Key` on `POST /api/v1/transfers`. Scope the key by the
authenticated user and operation. Persist a record containing a canonical
request hash, processing state, response status, response representation or
reference, timestamps, and expiry. Enforce a unique database constraint on the
scope and key.

The first request reserves the key and performs the transfer within a database
transaction. An identical completed retry replays the original `201` response.
The same key with a different canonical request returns `409 Conflict`. A
concurrent in-progress duplicate receives a predictable conflict/retry response
and cannot execute a second transfer.

## Consequences

- Correctness survives application restarts and multiple instances.
- A successful retry returns the same outcome without another balance change.
- The canonicalization algorithm and stored response format are part of the
  operation's compatibility contract and require tests when request fields evolve.
- Idempotency rows consume storage and need a conservative expiry/cleanup policy;
  completed transfer and ledger records remain durable after the key expires.
- Database uniqueness still handles races even if two request threads pass an
  application-level existence check.
- This mechanism protects duplicate transfer intent; it does not replace
  authentication, authorization, balance locks, or a database transaction.

