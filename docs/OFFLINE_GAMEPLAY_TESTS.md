# Stage03 offline acceptance

Automated device procedure lives in scripts/device_acceptance.sh. Existing two Stage02 instrumentation phases remain. New SixGameDeviceTest adds:

1. Open crossword and save one actual cell, assert it is displayed.
2. Force-stop, disable Wi-Fi/data, restart and explicitly resume; assert the saved cell before completing the crossword.
3. Open builder, spend one hint, solve, save exact aggregate progress text.
4. Reboot the emulator, wait for sys.boot_completed, disable networks again, verify exact progress and hinted score; revisit result and assert no duplicate award.
5. Reconnect, solve word search, semantic matching and ordering; collect light/dark screenshots and device resource diagnostics.

Room tests cover populated raw-v1 migration through Room2 and verified bundled upgrade, immutable revisions, rollback on invalid upgrade, future-state quarantine, duplicate completion and board-session roundtrip. These are executable tests, not assertions of success. Final execution results are recorded in STAGE_03_REPORT.md; this procedure is not evidence by itself.

The emulator is API28 x86_64. Physical Android8/midrange hardware, font200% and full TalkBack verification remain separate evidence requirements; no unexecuted scenario is called passed.
