# Stage04 backend

Additive implementation preserves the Stage03 catalog routes. Published pack manifests are filtered by published status, publication timestamp, withdrawal and database RLS. Public storage object keys remain inaccessible to api_catalog. Signed Ed25519 manifests are verified against an injected trust map before delivery; download objects use SHA256 names and HTTPS URLs. Local storage additionally verifies bytes before returning a URL. The manifest/download endpoints fail with 503 until a trusted manifest verifier and storage adapter are supplied. The executable supports `CONTENT_TRUST_FILE` (JSON map of key ID to Ed25519 SPKI public PEM) and `CONTENT_STORAGE_BASE_URL` (HTTPS immutable-object base), configured together. No private signing material is accepted.

Implemented routes:

- GET `/api/v1/content-packs/:id/manifest?version=N`: signed manifest.
- GET `/api/v1/content-packs/:id/download?version=N`: `{packId,version,objects:[{sha256,url}]}`.
- GET `/api/v1/content-packs/:id/dependencies?version=N`: `{packId,version,dependencies:[{packId,version}]}`.
- POST `/api/v1/content-packs/updates`: `{installed:[{packId,version}],appVersionCode}` → `{updates:[ContentPack]}` with latest compatible published versions.
- POST `/api/v1/sync/push`: exact `contracts/sync.schema.json` body → `{results:[{operationId,status,serverVersion,current?,code?}],cursor}`. Status is accepted, duplicate or conflict.
- GET `/api/v1/sync/pull?cursor=N&limit=N`: `{changes:[{version,ordinal,entityType,entityId,value}],cursor,hasMore}`.

Sync requires injected `AuthVerifier` and `SyncRepository`. No bearer decoding or client user ID trust is implemented. The default executable supplies neither and fails closed with 401. Production OIDC verification and account/device registration remain deployment work; no external identity or storage service was changed.

Each batch uses a transaction, SET LOCAL ROLE api_sync, transaction-local user identity and a locked per-user clock. Durable operation receipts reject operation ID or device sequence reuse with changed payload. Session deduplication rejects changed session results and prevents repeated attempts/rewards. Practice progress uses max score, OR completion and attempt counting once per unique session. Scores remain self_reported; they never affect competition data. Preferences and saved words use server-version CAS; conflicts return current state. The supported achievement rule is exactly rule_version 1 / `{kind:"first_win"}`, awarded uniquely and emitted with a separate clock version. Unknown rules are not interpreted. Each emitted entity receives its own version, so limit-based pull pages cannot split a version. No retention deletion is enabled; cursor reset/snapshot lifecycle is outside this implementation.

The TypeScript build imports contract JSON so compiled runtime receives schema copies under dist/contracts; compiled app health injection returned 200.

Validation on the local tree: `npm --prefix services/api run typecheck` passed. `TEST_DATABASE_ENGINE=pglite npm --prefix services/api test` passed 19 tests, zero skipped. This executes all four SQL migrations and SQL/RLS through PostgreSQL WASM and the pg wire protocol. Coverage includes existing catalog isolation, canonical signatures and corruption, HTTPS/hash storage, operation replay/conflicts, preference CAS, per-user RLS, unique session counting, score/completion merge, first-win award union and paged pull. Native `npm test` was attempted; its database setup cannot chown to nobody in this container (EINVAL), so native process/concurrency behavior is not claimed as verified.
