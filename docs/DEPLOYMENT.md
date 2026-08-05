# Deployment guide

WalletWise packages the React frontend and Spring Boot API into one Docker image.
The intended public topology is one Render web service plus Neon PostgreSQL,
with a separate static project showcase on GitHub Pages.

## Current deployment status

The repository contains deployment-ready configuration, but this source tree
does not prove that a Render service or Neon database exists. Provider setup
requires the repository owner's authenticated Render and Neon sessions. Do not
publish or invent an application, Swagger, or health URL until the verification
steps below succeed.

## Local Docker Compose

Prerequisites are Docker Desktop or Docker Engine with Compose v2. From the
repository root:

```bash
docker compose up --build
```

Expected local endpoints are:

| Resource | URL |
|---|---|
| Application | `http://localhost:8080` |
| Swagger UI | `http://localhost:8080/swagger-ui/index.html` |
| OpenAPI JSON | `http://localhost:8080/v3/api-docs` |
| Health | `http://localhost:8080/actuator/health` |
| Build information | `http://localhost:8080/actuator/info` |

Compose starts PostgreSQL 16 with a health check, waits before starting the app,
and stores development data in a named volume. Stop containers without deleting
the database volume:

```bash
docker compose down
```

Deleting the named volume destroys local application data and is intentionally
not part of the normal stop command.

## Production image

Build and inspect the image locally:

```bash
docker build --tag walletwise-platform:local .
docker run --rm --publish 8080:8080 \
  --env SPRING_PROFILES_ACTIVE=prod \
  --env SPRING_DATASOURCE_URL=jdbc:postgresql://host/database \
  --env SPRING_DATASOURCE_USERNAME=walletwise \
  --env SPRING_DATASOURCE_PASSWORD=replace-me \
  --env JWT_SECRET=replace-with-at-least-32-random-characters \
  --env APP_COOKIE_SECURE=true \
  walletwise-platform:local
```

Use a secret store or temporary shell environment; do not place real values in
source, a Dockerfile, image build arguments, shell history, or documentation.
The final image must run as a non-root user, contain no Node/Maven build tools,
listen on `${PORT:-8080}`, and expose no build-time secrets through `docker
history`.

## GitHub Container Registry

`.github/workflows/docker-publish.yml` publishes verified main revisions and
semantic-version tags to:

```text
ghcr.io/konkalasudheerreddy/walletwise-platform
```

The workflow uses its repository-scoped `GITHUB_TOKEN`, requests `packages:
write`, and generates provenance, an SBOM, and a GitHub attestation. It does not
need a personal access token. Published tags include `latest`, the commit SHA,
and semantic-version forms on a release tag.

The first GHCR publication is private by default even when the repository is
public. After the first successful workflow, an owner must open the package
settings and deliberately change visibility to **Public** if anonymous pulls are
desired. This visibility change cannot be reversed. Verify before advertising:

```bash
docker pull ghcr.io/konkalasudheerreddy/walletwise-platform:latest
gh attestation verify oci://ghcr.io/konkalasudheerreddy/walletwise-platform:latest \
  --repo KonkalaSudheerReddy/walletwise-platform
```

## Neon PostgreSQL

1. Sign in to Neon and create a project and production database.
2. Create a dedicated application role rather than reusing an administrator
   role where the selected plan permits it.
3. In the connection dialog, enable **Pooled connection** and require TLS.
4. Convert the provided URI into a JDBC URL without placing the password in it,
   for example:

   ```text
   jdbc:postgresql://ep-example-pooler.region.aws.neon.tech/walletwise?sslmode=require
   ```

5. Keep the URL, username, and password only in Render's secret environment.
6. Use a small Hikari pool appropriate to one free-tier web process and Neon's
   pooled endpoint.
7. Verify Flyway migration history and Hibernate validation in startup logs.
8. Confirm TLS from the database side when possible, for example through
   `pg_stat_ssl`, without logging credentials.

Neon may suspend idle compute depending on the account plan. The first database
query after inactivity can therefore add startup latency.

## Render web service

The repository's `render.yaml` defines a Docker web service, automatic main
deploys, and `/actuator/health` as the health check. Start the Blueprint flow:

[Deploy the WalletWise Blueprint to Render](https://render.com/deploy?repo=https://github.com/KonkalaSudheerReddy/walletwise-platform)

This is a setup link, not a live-application link. During authenticated setup:

1. grant Render access only to this repository;
2. provide the Neon JDBC URL, username, and password for the `sync: false`
   variables;
3. allow Render to generate `JWT_SECRET` and never copy its value into source;
4. set `APP_PUBLIC_URL` and `APP_CORS_ALLOWED_ORIGINS` to the final HTTPS Render
   origin after the service name is known;
5. keep `APP_COOKIE_SECURE=true` and `SPRING_PROFILES_ACTIVE=prod,demo` for the
   synthetic public demonstration; and
6. deploy from a verified `main` revision.

Render injects `PORT`; Spring must bind to it and to all container interfaces.
No persistent data belongs on Render's local filesystem.

### Hosted verification gate

Do not add hosted links to the README or showcase until all of these pass:

- deployment reaches a successful state and health returns `UP`;
- the UI eventually loads after any free-instance cold start;
- Swagger and OpenAPI load without exposing a secret;
- the demo user signs in and refresh/logout cookie behavior is correct;
- one sample income and idempotent transfer flow succeeds;
- an identical retry does not move money twice and conflicting key reuse is
  `409`;
- data survives an application restart;
- the database connection uses TLS;
- cookies are `Secure` and no user-facing stack trace is present; and
- logs contain no token, password, cookie, or database credential.

Free Render services can spin down after inactivity and have plan-specific
resource and usage limits. Document the observed cold-start behavior without
inventing an availability commitment.

## GitHub Pages showcase

`.github/workflows/pages.yml` publishes only `showcase/` plus verified images
from `docs/images/`. It does not attempt to run Spring Boot on GitHub Pages.
Repository Pages settings must use **GitHub Actions** as the source. After a
successful deployment, verify the URL returned by the workflow, internal links,
responsive layout, and image loading before setting it as the repository
homepage.

Do not add a Pages badge or URL based only on the expected owner/repository path;
use the URL reported by the successful deployment.

## GitHub Codespaces

The dev container installs Java 21, Node.js 24, Maven dependencies, npm's locked
dependency tree, and Docker Compose support. It forwards ports 8080, 5173, and
5432 without committing any secret.

After the public repository exists, start a Codespace from:

```text
https://codespaces.new/KonkalaSudheerReddy/walletwise-platform?quickstart=1
```

Then run `docker compose up --build` and open the forwarded application port.
Creating a Codespace requires the viewer's GitHub authentication, entitlement,
and available usage quota. CLI creation also requires the GitHub OAuth
`codespace` scope; the public link itself does not.

## Environment variables

| Variable | Required | Purpose | Secret |
|---|---:|---|---:|
| `SPRING_PROFILES_ACTIVE` | Yes | Selects `dev`, `prod`, and optional `demo` behavior | No |
| `SPRING_DATASOURCE_URL` | Yes | PostgreSQL JDBC URL; production requires TLS | Treat as sensitive |
| `SPRING_DATASOURCE_USERNAME` | Yes | Dedicated database role | Yes |
| `SPRING_DATASOURCE_PASSWORD` | Yes | Database credential | Yes |
| `JWT_SECRET` | Yes | Signing material; production requires at least 32 unpredictable characters | Yes |
| `APP_CORS_ALLOWED_ORIGINS` | Development / split origin | Explicit comma-separated browser origins | No |
| `APP_COOKIE_SECURE` | Yes | Requires HTTPS-only refresh cookie in production | No |
| `APP_DEMO_SEED_ENABLED` | Optional | Rebuilds only the synthetic demo account at startup | No |
| `APP_ADMIN_EMAIL` | Optional | Enables an explicit admin seed when paired with a password | Sensitive |
| `APP_ADMIN_PASSWORD` | Optional | Administrator seed password | Yes |
| `APP_PUBLIC_URL` | Hosted demo | Canonical same-origin deployment URL | No |
| `PORT` | Platform supplied | HTTP listener port; defaults to 8080 locally | No |
| `VITE_API_BASE_URL` | Native frontend only | Backend origin when no Vite proxy is used; normally empty | No |
| `DB_POOL_SIZE` | Optional | Hikari maximum pool size; defaults to 5 | No |
| `APP_ACCESS_TOKEN_TTL` | Optional | ISO-8601 access-token duration; defaults to `PT15M` | No |
| `APP_REFRESH_TOKEN_TTL` | Optional | ISO-8601 refresh-token duration; defaults to `P7D` | No |
| `APP_REFRESH_REUSE_GRACE` | Optional | Bounded duplicate-rotation grace; defaults to `PT3S` and may not exceed 30 seconds | No |
| `APP_IDEMPOTENCY_RETENTION` | Optional | ISO-8601 idempotency-record retention; defaults to `P7D` | No |

## Troubleshooting

### Application waits for PostgreSQL

Check `docker compose ps` and the PostgreSQL health check, then inspect sanitized
container logs. Confirm the JDBC hostname is the Compose service name from
inside Docker, not `localhost`.

### Flyway validation fails

Do not switch Hibernate to `update`. Compare the migration checksum and schema
history, restore the expected migration, or add a forward migration. Never edit
an applied production migration casually.

### Render reports no open port

Confirm Spring reads `PORT`, binds to `0.0.0.0`, and the Docker `CMD` starts the
executable JAR. Verify `/actuator/health` does not require authentication.

### Login works locally but refresh fails when hosted

Inspect cookie attributes and request origin without exposing cookie contents.
Hosted production requires HTTPS, `Secure`, the intended SameSite policy, and a
cookie path that includes the refresh and logout endpoints.

### Neon rejects connections

Confirm the pooled hostname, database, application role, and `sslmode=require`.
Rotate the password if it was copied into an unsafe location; do not paste it in
an issue.
