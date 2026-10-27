# Stage02 catalog API

The Node.js 22+ service in `services/api` uses Fastify and PostgreSQL through `pg`. It serves a read-only public practice catalog. Official challenges, answers in the competition bank, authentication, submissions, synchronization writes, publication, and pack download URLs are not implemented in this stage.

## Install and run

```bash
cd services/api
npm ci
npm run typecheck
npm run build
# Supply secrets through the deployment environment, never commit them.
MIGRATION_DATABASE_URL='<direct owner connection>' npm run db:migrate
DATABASE_URL='<dedicated catalog login connection>' npm start
```

`PORT` defaults to 3001 and `HOST` to `127.0.0.1`. Use `HOST=0.0.0.0` behind the deployment proxy. `NODE_ENV=production` enforces certificate verification, including when a connection string contains `sslmode=disable`. Remote connections always verify TLS. Migration credentials must never be supplied to the running API.

After migration 002, the operator provisions a separate PostgreSQL LOGIN role with an externally managed password, `NOSUPERUSER NOCREATEDB NOCREATEROLE NOREPLICATION NOBYPASSRLS`, and grants `api_catalog` membership to it. Do not connect the service as the database owner. The group itself is NOLOGIN. Every query runs in `BEGIN READ ONLY`, applies `SET LOCAL ROLE api_catalog` and a five-second statement timeout, and commits or rolls back before releasing the pooled connection. This supports transaction pooling without leaking role state.

## Routes

| Method | Path | Result |
| --- | --- | --- |
| GET | `/health` | Process liveness |
| GET | `/health/ready` | Database connectivity and restricted active role |
| GET | `/api/v1/categories` | Category page |
| GET | `/api/v1/games` | Enabled game page |
| GET | `/api/v1/puzzles` | Published practice puzzle summaries |
| GET | `/api/v1/puzzles/:id` | Validated offline practice definition |
| GET | `/api/v1/content-packs` | Latest compatible published pack metadata |
| GET | `/api/v1/content-packs/:id` | Latest published pack metadata |
| GET | `/api/v1/content-packs/:id/versions` | Published versions, descending |

Lists accept `limit` (1–100, default 20) and an opaque `cursor`. Puzzle filters are `gameId`, `categoryId`, `type`, and `difficulty`; pack filters are `categoryId` and `appVersionCode`. Cursors are bound to the endpoint and filters. Unknown parameters, duplicate parameters, malformed UUIDs and invalid enum values return 400. Unknown or unpublished resources return 404. Database errors return a sanitized 503. Responses include request IDs, ETags, conditional 304 support, and public five-minute caching for successful catalog reads; errors use `no-store`.

Public practice definitions intentionally contain the local answer required for offline play. Summary lists omit answers. RLS hides drafts, withdrawn publications, fixture-only definitions, and puzzles under disabled games. Column grants exclude author identifiers, object keys and private pack manifests. Official challenge tables and identity/play/ops/editorial schemas are inaccessible.

## Verification completed 2026-10-27

From `services/api`, `npm run typecheck` and `npm run build` succeeded. `TEST_DATABASE_ENGINE=pglite npm test` passed **15 tests, zero failures and zero skipped**, in 4455.985474 ms after the hosted-role compatibility and rate-limiting fixes. Tests exercise the actual PostgreSQL SQL engine through a loopback pg wire connection and Fastify request injection: migrations/checksums, draft seed idempotence, positive and negative RLS visibility, denied private columns and writes, Arabic detail schemas, filtering, bounded pagination, ETags, sanitized errors, immutability, erasure, and TLS configuration.

The default test engine is native embedded PostgreSQL. This container cannot initialize it because its uid map cannot run initdb as an unprivileged account; the explicit PGlite fallback verifies SQL/RLS but does not validate native process or concurrent connection behavior. Hosted Neon PostgreSQL separately passed role switching, catalog SELECTs, seven SELECT policies, denied private reads/writes, and role flags as recorded in `DATABASE_MIGRATIONS.md`. No production HTTP API deployment was performed.

## Public request limiting

`@fastify/rate-limit` enforces 120 requests per 60-second window per socket IP before route handlers, across public routes. The local LRU stores at most 5,000 IP counters; exhausted capacity may evict old counters. Throttled requests return structured `application/problem+json`, `RATE_LIMITED`, status 429, request ID, `Cache-Control: no-store`, and integer `Retry-After` seconds. `trustProxy` is explicitly false: untrusted `X-Forwarded-For` values cannot select a new bucket. The test sends 120 accepted requests, verifies the 121st is denied despite changing forwarded IPs, and checks a separate socket IP remains allowed.

This local limiter is suitable for single-instance development. Public or multi-instance deployment is gated on an ingress/gateway that enforces a shared 120/minute client-IP limit and strips client-supplied forwarding headers. Behind a proxy, the default socket-IP limiter groups requests under the proxy address until a reviewed trusted-proxy configuration is implemented. LRU eviction and multiple processes make the in-process store insufficient as a distributed abuse boundary.
