# Stage 02 — verified test results

Verified on 2026-10-27 using real GitHub Actions runs. No result below is inferred from Gradle Sync.

## Final code verification

Code commit: `33471cb8fa98e020811f6ecacf1854200149569a`.
[Run 40582600981](https://github.com/Mtzallqmy/Loazdan/actions/runs/40582600981).

| Check | Result |
|---|---|
| Android debug APK, Kotlin compilation, Room KSP/schema export, Android Lint | BUILD SUCCESSFUL in 5m32s; job122466438251 |
| JVM/Robolectric tests: engine, pack integrity, Room recovery, DataStore, Compose UI |28 tests,28 passed,0 failures,0 errors,0 skipped |
| Backend strict typecheck/build and native PostgreSQL/Fastify tests |15 passed,0 failed; job122466438228 |
| npm vulnerability audit |Passed in CI; local lockfile audit reported0 vulnerabilities across117 dependencies |
| Contracts, debug fixtures, SHA256 and tracked-source secret patterns |Passed; job122466438475 |

Android command:

```bash
cd apps/android
gradle :app:assembleDebug :core:game-engine:test :data:testDebugUnitTest :core:datastore:testDebugUnitTest :app:testDebugUnitTest :app:lintDebug --stacktrace
```

Backend command: `npm ci`, `npm audit --audit-level=high`, then `npm run typecheck && npm run build && npm test`. Native PostgreSQL runs on the GitHub runner; the earlier restricted local container used explicit PGlite tests instead. Neither substitutes for the separately executed Neon role checks.

## Offline process restart acceptance

[Run40582536961](https://github.com/Mtzallqmy/Loazdan/actions/runs/40582536961), device job122465435154, commit`121ca6bb5fb7b2c4f2a0b115aed2457c86e37a44`: Android API28 x86_64 emulator; two instrumentation processes, each `OK (1 test)`.

The script installs fresh APKs, clears app data, solves the first vocabulary puzzle, receives100 points, force-stops the app, disables Wi-Fi and mobile data, launches a new instrumentation process, restores the saved result and solves the next puzzle. Subsequent commit33471cb changes CI concurrency only; it contains the same game and device-test source. The latest run also schedules the same device check. This is emulator evidence, not a physical-phone claim.

Room tests additionally cover an unfinished round restored as paused, explicit resume, future state preservation, retry score deduplication, retaining progress after content removal, invalid saved option IDs and mismatched pack ownership.

## Neon migration and permissions

Migration002 applied to isolated `stage02-integration` (`br-bold-band-baxyvly5`), project`young-hall-20561827`, database`lughzdan`. Seven SELECT policies and a restricted NOLOGIN catalog role were verified. Private identity/challenge/object-key reads and content writes were denied. The default/main branch was not changed by Stage02 testing. See DATABASE_MIGRATIONS.md for recorded checksums and baseline-adoption limits.

## Failures found and resolved

Earlier setup failed because the SDK action requested retired package`tools`; explicit package selection fixed it. A subsequent Compose test raced the asynchronous Room selection write and tried submitting while the button was temporarily unavailable. Tests now wait for the committed selection and enabled submit control; the final28-test run passed.

## Remaining repository gate

Dependency Review in PR#1 remains blocked by repository configuration. GitHub returned: `Dependency review is not supported on this repository. Please ensure that Dependency graph is enabled`.

Enable Dependency graph in [repository security settings](https://github.com/Mtzallqmy/Loazdan/settings/security_analysis), then rerun the PR check. The available connector has no repository-administration permission for this setting. Required status checks must also be configured before merging. The workflow keeps this check blocking; it was not disabled or made continue-on-error. npm audit does not replace Android dependency review.

No physical API26 phone, performance benchmark, TalkBack/font200% matrix, external signed-pack download/crash test or specialist penetration test is claimed. The four debug questions remain explicitly unreviewed fixtures and are excluded from release assets. No production API hosting or production APK release occurred.
