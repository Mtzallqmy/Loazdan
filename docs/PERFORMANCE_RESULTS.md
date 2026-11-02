# Stage03 performance evidence

No physical midrange-device performance result is claimed. CI engine tests print generator_pair_ms p50/p95 over41 deterministic small dictionary seeds, with Java/OS metadata. This measures host generator work only, not Android UI startup or a worst-case dictionary. Generator nodes are capped50000 and grid15×15; generation is an authoring/off-main facility, not invoked on the Compose main thread. No two-second completion SLA is asserted.

The device script captures actual adb dumpsys gfxinfo and meminfo after interactive acceptance, plus screenshots. Results must be read from the completed device artifact before quoting frame/PSS values. No guessed memory/FPS/battery figure is provided here. Final measured host figures are added after CI execution.

Large-library paging and exhaustive low-memory stress/thermal/battery benchmarks remain part of Stage04/optimization work; the current debug library has22 records. Session restoration fetches pinned definitions instead of joining every revision, but the current catalog still loads the small installed list into memory. Do not claim multi-gigabyte library scalability from this test.

## Measured local JVM sample — 2026-10-28

Fresh engine compilation and an explicit classpath with engine.jar first passed47 tests. On this Linux/JVM17.0.20 host, the41-seed pair benchmark printed p50=4.660592ms and p95=30.518883ms with82 valid successes. These exact figures identify that one small-dictionary run; they are not a phone latency promise or a large-dictionary worst case. Final CI/emulator measurements remain pending.

CI run40704686380 atbe0d5a68 printed generator_pair_ms p50=5.598847 and p95=34.562145, runs41/successes82, Java17.0.21 Linux. Android build/Lint and71 tests passed. Device acceptance in that run stopped before the final capture phase, so no device PSS/frame result is inferred from it.
