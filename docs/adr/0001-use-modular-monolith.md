# ADR 0001: Use a modular monolith

- Status: Accepted
- Date: 2026-08-05

## Context

WalletWise has authentication, wallets, ledger entries, transfers, budgets,
notifications, analytics, audits, and administrator behavior. Several workflows,
especially transfers, require one atomic transaction. The project should be
credible in a Java interview while remaining practical for one developer to
build, run, test, and deploy.

Splitting these capabilities into network services would require distributed
transactions or compensating workflows, independent deployments, service
authentication, more observability, and additional failure modes. None of those
costs solve a current product requirement.

## Decision

Build one Spring Boot deployment organized by feature packages with explicit
controller, service, repository, mapping, and DTO boundaries. Build the React
frontend separately during development, then serve its compiled assets from the
same process in production. Use one PostgreSQL database owned by the application.

Modules may cooperate through narrow Java service interfaces. A database
transaction can span modules when the business operation requires it; modules
must not bypass ownership or ledger rules by writing each other's tables
arbitrarily.

## Consequences

- Local setup, CI, deployment, logging, and debugging stay simple.
- Transfers can update both wallets, both ledger entries, audit state, and the
  response record atomically.
- Feature boundaries are still visible and can be discussed or extracted later.
- Frontend and backend production releases are coupled.
- Horizontal scaling shares one database and requires all correctness state,
  including idempotency, to be persisted rather than process-local.
- Future service extraction would require explicit contracts and data ownership;
  the current design does not pretend that package boundaries alone provide
  runtime isolation.

