# Next experience — implementation ledger

Baseline: `4348f41446d79e3247db4173f21342b8530e1096`, published 1.0.0 (100).
Branch: `feat/lughzdan-next-experience`.

## Verified source defects
- Six letter-icon navigation destinations; full-history collection at the root.
- 22 choice answers in position one, 16 solved initial orders, 20 aligned pair boards.
- Sequence rule displayed before submission; bitmap sampling divisor not initialized.
- Each action disabled all controls and waited for the complete history observer.

## Evidence policy
Existing Debug emulator screenshots and 92-frame metrics are historical evidence only.
No Release phone baseline has been measured yet. Performance targets are acceptance goals,
not results. CI device measurements must identify emulator, API, build, scenario and iterations.
Published packs remain immutable; editorial changes require new revisions and signed versions.
No release is ready until build, device, restoration, upgrade and signature checks pass.

## Implemented in this continuation
- `b8e46f3`: one durable daily session per Asia/Aden date; no invented bonus/server receipt. Introductory candidates only; existing session pins puzzle revision and pack version.
- History starts at 50 rows, expands in increments of 50, and caches validated unchanged rows. Content installation/head changes invalidate that cache. Active gameplay subscribes directly to its session; history is collected only on home/history screens.
- `a02c75d`: long-press word-search path selection and preview with two-tap alternative; long-press ordering with accessible up/down alternatives; collapsible crossword clues/keyboard; library search; IME insets.
- `c3e7028`: distinct submission-in-flight state without disabling ordinary board input; restoration tests follow current navigation and read shuffled order from displayed semantics.
- `scripts/audit_experience_content.py`: reproducible triage ledger for 120 records, and 14 schema-valid revision-2 drafts. Published content and signatures are unchanged. This is NOT a completed scholarly review. Candidates are NOT installed or published.

## Validation and release gates
Local checks passed: debug content schema/solutions/digest (34 fixtures), opening pack technical validation (120 records), candidate draft schema validation, diff whitespace checks. Full local Gradle execution is blocked by unavailable distribution download; local OpenAPI validation additionally lacks `openapi_spec_validator`.

CI on `a02c75d` compiled Debug and Release and passed contracts/backend, but failed UI assertions tied to old navigation and crossword controls. Those failures are addressed in the next commit; no success is inferred before its run completes.

The owner keystore, its password, and content-signing private key are not present in this execution environment. Existing release automation only republishes the PREVIOUS pre-signed 1.0.0 artifact; it does not sign a new build. Do not republish that artifact as this change. Do not generate a substitute signing key.

## Still required before release
- Finish and independently source-check the entire editorial pass, review candidates, produce genuinely varied visual clues, and publish a newly signed pack with retained old revisions.
- Complete physical-device Release performance baseline/comparison, large-history latency and memory measurements; no P50/P95, startup or jank target is claimed as met.
- Review actual emulator evidence, exercise all gesture alternatives, large fonts, 320–480dp widths, dark mode, accessibility and process restoration on the final commit.
- Complete fixed-position gameplay actions, progression/difficulty calibration, meaningful motion with reduced-motion support, and readable source details.
- Sign the final ARM64 APK with the original owner key, increment versionCode, verify its certificate and SHA-256, test upgrade over 1.0.0, and only then merge/release.
- The subsequent expansion of modes and Arabic knowledge remains a separate phase after these release gates. No claim of market superiority is made.

## Feedback preview 1.1.1 (102)
- Choice selection is distinct from correctness. Submitted wrong options retain a red cross; the correct option is revealed in green only at the end.
- Crossword clue, direction, length and previous/next question controls share a card with the square grid. Submitted cell feedback is persisted; editing a cell removes its stale mark until checked again. The result retains the grid.
- Success, retry and discovery cues follow engine state transitions, not button presses. Restored sessions and duplicate submissions do not replay cues. Sound is enabled by default unless explicitly disabled, with a switch in the round.
- Prior preview 101 passed Android unit/Lint and API 26/28 emulator suites in run 42271217885. Its standalone APK passed signature, minSdk, ARM64 and bundled-content checks. Owner signing remained blocked by the missing original keystore secret.
- Preview 102 validation is pending CI. No new content pack or phone performance result is claimed.

Preview 102 uses `com.lughzdan.app.preview.feedback` and the label «لُغْزَدان — ألوان وأصوات». The prior ephemeral test certificate was not retained; this separate package avoids uninstalling preview 101 or losing its data. Neither preview changes the production package or transfers data between installations.


## Atlas 2.0.0 delivery — 2026-11-08

The earlier pending notes above are historical. The delivery authority is now docs/atlas/DELIVERY.md and docs/atlas/RELEASE.json, verified against de43bc49f1c49ef15c4c272bbac575b93a17e62f.

Build, Lint, release-content verification, contracts/backend and all API26/28/35 device gates passed: 145 JVM successes, two skipped cases and 32 successful device phases. Real captures cover 320/480dp at fontScale1.3 and both themes. Geometry reads in instrumentation execute on the UI thread, and progress visibility waits for restored content to settle.

The retained original owner certificate was recovered outside the repository and matched1.0.0. Both production and Diwan APKs are signed and verified for V2/V3 with their respective existing certificates, versionCode104, minSdk26, ARM64 and debuggable=false. Production remains com.lughzdan.app; Diwan remains com.lughzdan.app.preview.diwan. Neither APK transfers data between those installations.

The remote owner-signing job still fails because its GitHub secret is not configured; local owner signing has passed. No whole-workflow success, physical-phone upgrade test, physical-device performance target, account sync or independent scholarly content review is claimed.
