# ADR 0004: Serve React from Spring Boot in production

- Status: Accepted
- Date: 2026-08-05

## Context

The application needs a responsive React interface and a Java API. Deploying
them as separate public services requires two deployments, cross-origin cookie
and CORS configuration, two public URLs, and extra free-tier resources. WalletWise
does not need independent frontend scaling or release cadence.

## Decision

Use Vite's development server and proxy locally. In the production Docker build,
compile React and copy its static output into Spring Boot's classpath. Serve the
SPA and `/api/v1` from one Spring Boot process and one origin.

Forward non-file browser routes to `index.html`, but never intercept `/api/**`,
`/actuator/**`, `/swagger-ui/**`, or `/v3/api-docs/**`. Cache fingerprinted
assets aggressively and avoid long caching for `index.html`.

## Consequences

- Production needs one web service and one database.
- Secure same-origin refresh cookies and CORS are easier to reason about.
- The production image proves the exact frontend and backend revisions released
  together.
- A frontend-only change still rebuilds and redeploys the Java image.
- Spring Boot serves static traffic that a CDN could handle more efficiently at
  larger scale.
- GitHub Pages remains a separate project showcase and must not be presented as
  the live application.

