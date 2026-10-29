# Stage03 offline acceptance

Automated device procedure lives in scripts/device_acceptance.sh. Existing two Stage02 instrumentation phases remain. New SixGameDeviceTest adds:

1. Open crossword and save one actual cell, assert it is displayed.
2. Force-stop, disable Wi-Fi/data, restart and explicitly resume; assert the saved cell before completing the crossword.
3. Open builder, spend one hint, solve, save exact aggregate progress text.
4. Reboot the emulator, wait for sys.boot_completed, disable networks again, verify exact progress and hinted score; revisit result and assert no duplicate award.
5. Reconnect, solve word search, semantic matching and ordering; collect light/dark screenshots and device resource diagnostics.

Room tests cover populated raw-v1 migration through Room2 and verified bundled upgrade, immutable revisions, rollback on invalid upgrade, future-state quarantine, duplicate completion and board-session roundtrip. These are executable tests, not assertions of success. Final execution results are recorded in STAGE_03_REPORT.md; this procedure is not evidence by itself.

CI runs x86_64 emulators at API26 and API28. Compose tests also exercise 280dp width with fontScale200% in light/dark; their results must be read from the final run. Physical midrange hardware and full TalkBack verification remain separate evidence requirements; no unexecuted scenario is called passed.

Nested grid/keyboard interaction first scrolls the vertical page to the exact target, then scrolls the horizontal ancestor if necessary, asserts visibility and injects the touch. Calling performScrollTo alone targets only the closest horizontal scroll ancestor and can leave a cell outside the page viewport. The acceptance check still requires the expected letter at its exact coordinate before and after process restart.
