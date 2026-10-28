# Local game progress

Room version 2 retains every session and puzzle revision. A session pins puzzle ID/revision and pack ID/version; installed pack identity is a composite key, so upgrades cannot reinterpret an older round. Library queries show the latest revision; restoration fetches exact pinned revisions without loading the whole library.

`MIGRATION_1_2` copies installed pack metadata, adds `gameType` and `xp`, and preserves sessions, receipts and achievements. Existing progress belongs to `choice`; its best score becomes its initial XP. Register this migration; no destructive fallback is used.

A bundled upgrade is verified and all puzzles validated before the transaction. Inside one transaction, the repository rejects changed definitions for existing revisions or changes to a puzzle's game type/pack identity, inserts new revisions and installs new metadata. Older metadata and revisions remain. Failed verification or transaction leaves healthy content intact. Bundled downgrades are ignored.

Plugin session validation guards restored and saved records. Unsupported/inconsistent records remain stored and are reported unavailable. Valid active rounds become paused once per repository lifetime. Legacy choice records remain supported.

Domain `Progress` exposes total score/XP/completions, per-game score/completions/attempts/XP and achievement IDs. These are local offline projections, not server-authoritative rewards or leaderboard results. Unique receipt insertion and progress updates share the session transaction.

Practice unlocks: difficulty1 initially, +1 per300 best-score XP up to5; player level=1+floor(XP/250). The catalog, next-puzzle navigation and repository start transaction enforce the shared ProgressionRules gate; each game has an entry level. Replay preserves max XP and cannot farm unlocks. Home shows per-game totals and earned achievements.
