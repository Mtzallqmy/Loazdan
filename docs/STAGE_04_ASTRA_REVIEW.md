# Stage 04 independent security and engineering review

Reviewed Android `PackInstaller`, `LocalContentRepository`, `LocalGameRepository`, Room v3 schema and queries, and `core/content` manifest, chunk download and assembly code. Static review of the working tree; builds and runtime tests were still pending. Client sync/backend were being edited separately and are outside this review. This report records findings at review time; subsequent fixes require revalidation.

Follow-up static verification: the implementation now rejects updates to an existing pack without a managed head before writing; removes the exact quarantined installed-version row in the rollback transaction; checks dependencies across all retained `COMMITTED` journals; and checks pause/cancel immediately before commit. These changes address the high finding and the two medium findings below by inspection. Runtime regression coverage remains required. No outstanding critical/high finding remains in this reviewed scope.

## High: first remote update can hide existing bundled content

`PackInstaller.install` creates a null active head when `head(id)` is absent. Existing bundled packs and packs migrated from Room v2 have installed rows but no heads/members. An attempted remote update of the same ID therefore hides the old catalog immediately, before the new version commits. A failed install leaves that catalog unavailable. Once a head exists, `LocalGameRepository.validateRecord` also requires membership for old pinned sessions, but no memberships were created for their bundled versions.

Either reject cloud updates to an existing unmanaged/bundled ID before any mutation, or explicitly migrate its installed versions and trustworthy membership while preserving its active version. Test a preexisting bundled version followed by failures at every install checkpoint, plus successful update and old pinned-session resume. Existing crash tests start from a cloud-installed version and do not cover this transition.

## Medium: quarantined versions remain eligible for pinned play

Recovery rolls back a damaged active directory and marks its journal `QUARANTINED`, but leaves the installed version eligible for `pinnedPack`, loaded sessions, and `act`. The loaded-session flow does not observe head/journal changes. A session pinned to the corrupt version can remain available even though recovery rejected its files.

Expose a version-availability condition that excludes quarantined versions, apply it consistently to pinned reads, and invalidate session availability when that condition changes. Preserve sessions, receipts, and progress. Test corrupt v2 with a v2 pinned session while v1 remains healthy.

## Medium: deletion ignores dependencies of retained versions

`delete` checks only each other pack's active manifest. If retained A v1 depends on B, A v2 removes that dependency, and an existing session still pins A v1, deleting B is permitted. This can remove assets/rights required by the retained session. Check dependencies of versions retained for playable pinned sessions, or mark affected sessions explicitly unavailable while preserving their data.

## Further verification needed

- Exercise pause/cancel during index import and just before commit: assembly checks the control, but import/commit do not explicitly check it; completion also writes from the original download row and can overwrite a user status.
- Run large real-size imports on device. Import performs one transaction per puzzle and keeps every puzzle identity in memory; the four-minute worker budget is checked only between download chunks, not during import or hashing.
- Generate/verify Room v3 exported schema and migration tests from both earlier versions; schema construction appears additive and preserves player tables by inspection.
- Test durable resume, cancellation, quarantine, and flow invalidation with the Android worker/runtime, not only core download fakes.

## Positive controls observed

Ed25519 verification uses explicit trusted keys and canonical JSON, rejects malformed UTF-8, duplicate keys through the canonicalizer, unsupported integer forms, and invalid signatures. Chunk and assembled-file hashes are independently verified. Paths are restricted, assembly rejects symlinks, HTTPS redirects are disabled, range responses require matching offsets/total/strong validators, and download sizes are bounded. New cloud pack visibility uses a transactional active pointer. Existing cloud versions and progress/session/receipt rows are retained through update/deletion. JSONL shards and lines are bounded and validated. No fixture fallback or fabricated cloud progress was introduced in the reviewed content code. Missing production API/key configuration intentionally fails closed and is not itself a defect.

The six game implementations were not rewritten by the reviewed content changes; complete regression confirmation depends on the pending build/tests. No critical signature bypass or direct player-table deletion was identified in this static review.
