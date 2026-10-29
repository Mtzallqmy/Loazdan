# Database migration operations

SQL migrations live in `database/migrations`; development drafts live in `database/seeds/001_development.sql`. The TypeScript migration runner uses a dedicated direct PostgreSQL owner connection, an advisory lock, an SHA-256 ledger in `public.lughzdan_schema_migrations`, and one transaction per migration. Reapplying unchanged files is a no-op; changing an applied file is rejected.

```bash
cd services/api
MIGRATION_DATABASE_URL='<direct owner connection>' npm run db:migrate
```

For an already-reviewed Stage01 database without a ledger, explicitly pass `--baseline-existing`. The runner validates all initial tables with forced RLS, five reviewed challenge-attempt columns, and four immutability triggers before recording 001. This is adoption of the reviewed baseline, not a full structural schema comparison. Never use it to adopt an unknown schema.

```bash
MIGRATION_DATABASE_URL='<direct owner connection>' npm run db:migrate -- --baseline-existing
```

Development fixtures require both `ALLOW_DEVELOPMENT_SEED=1` and a nonproduction environment. They remain draft and fixture-only and can be applied repeatedly. They do not constitute editorial approval.

```bash
ALLOW_DEVELOPMENT_SEED=1 MIGRATION_DATABASE_URL='<development direct connection>' npm run db:migrate -- --seed-development
```

Migration 002 creates the NOLOGIN `api_catalog` group, grants only specific catalog columns, installs seven SELECT policies, and adds catalog indexes. Because hosted PostgreSQL owners cannot change superuser, replication, or BYPASSRLS flags even to false, the migration rejects unsafe preexisting roles and relies on safe defaults when creating the role. LOGIN provisioning and membership are deployment operations; no password is stored in migrations.

## Applied isolated branch

On 2026-10-27, migration 002 was applied transactionally to **stage02-integration**, branch `br-bold-band-baxyvly5`, project `young-hall-20561827`, database `lughzdan`, using `lughzdan_owner`. The branch was verified ready, non-default, non-primary, and a child of `br-still-mode-baryzkrp`. The main/default branch was not mutated. Existing Stage01 tables, forced RLS, challenge columns and triggers passed the baseline guards before the ledger adopted 001. No development content was seeded into the hosted database.

| Migration | Recorded SHA-256 |
| --- | --- |
| `001_initial.sql` | `aeb3a71265f42e72b6832b285adc87694557d9ba694f952f44b43b3c5373d6c4` |
| `002_catalog_runtime.sql` | `4cb20bab1e1d15ff00cee09bb4c9a1bdfdba88f0e6136c1ed9e2a6d3e412f129` |

Hosted verification switched to `api_catalog` inside a transaction and executed category/game/puzzle-version SELECTs successfully (zero rows in this unseeded branch). It verified seven catalog SELECT policies; NOLOGIN, nonsuperuser, no replication and no BYPASSRLS; no identity/play schema usage, no object-key/author-column access, and no game UPDATE grant. Executed private challenge, identity and object-key reads plus UPDATE attempts all raised insufficient-privilege errors, caught by assertions. Temporary owner membership used solely for verification was revoked in the same transaction. Local PGlite tests separately cover nonempty published/draft/withdrawn/fixture/disabled visibility and migration idempotence.

An initial hosted attempt hit the role-flag restriction and rolled back. The corrected migration then applied successfully, with the final hashes above. The ledger and policy counts were read back after commit. No runtime LOGIN or deployed service credentials were created.

## Stage03 metadata migration

`003_stage_three_types.sql` adds metadata for the six implemented rule types using INSERT ON CONFLICT DO NOTHING. It does not alter content definitions, published rows or player data. Applied on2026-10-28 to `stage03-integration` (`br-still-grass-baixdxhx`), a child of the reviewed Stage02 test branch in the same project/database. Read-back returned the six IDs with engine/schema versions; no approved/published puzzles were inserted. Runtime policies remain inherited and default deny outside the catalog. PostgreSQL native/PGlite migration tests run003 as part of empty→latest and verify repeated migration no-op. This is separate from the Android Room1→2 migration.

Fresh read-back on2026-10-28 confirmed the003 ledger checksum `942e62106c55caaba013c23222253db4eda5cf6b8fc94338240102153d04273e`, exactly matching the committed SQL file. The ledger records application at2026-10-28T01:47:38.935Z; migrations001/002 retain their original checksums. This verification was read-only on the isolated Stage03 branch.
