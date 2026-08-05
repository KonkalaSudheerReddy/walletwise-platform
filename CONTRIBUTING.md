# Contributing to WalletWise

Thank you for considering an improvement to WalletWise. This repository is a
portfolio and educational project, but the same review discipline used on a
team project is encouraged.

## Development workflow

1. Fork the repository and create a focused branch from `main`.
2. Copy `.env.example` to `.env` only for local use. Never commit `.env`.
3. Start the complete stack with `docker compose up --build`, or follow the
   native setup in the README.
4. Add or update tests with every behavior change.
5. Run `./scripts/verify.sh` on Linux/macOS or
   `./scripts/verify.ps1` on Windows.
6. Open a pull request that explains the problem, the solution, and how it was
   verified.

## Project conventions

- Backend code uses Java 21, constructor injection, package-by-feature, and
  Java records for suitable API DTOs.
- API controllers never expose JPA entities directly.
- Flyway owns the database schema; do not rely on Hibernate schema generation.
- Money uses `BigDecimal`; timestamps are stored in UTC.
- Frontend code uses strict TypeScript and feature-oriented modules.
- Never place access tokens, refresh tokens, credentials, personal data, or
  real financial information in source, fixtures, screenshots, or logs.
- Keep pull requests small enough to review and use Conventional Commit-style
  subjects such as `feat:`, `fix:`, `test:`, `build:`, and `docs:`.

## Quality gate

A contribution is ready when backend tests and quality checks, frontend lint
and tests, the production frontend build, and the production Docker build all
pass. Changes to public API behavior must also update OpenAPI annotations,
Postman examples, and relevant documentation.

## Reporting security issues

Do not open a public issue for a suspected vulnerability. Follow the private
reporting guidance in [SECURITY.md](SECURITY.md).
