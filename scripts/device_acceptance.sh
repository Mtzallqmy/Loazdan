#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/../apps/android"
./gradlew :app:assembleDebug :app:assembleDebugAndroidTest
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell pm clear com.lughzdan.app
run_phase() {
  local output
  output=$(adb shell am instrument -w -r -e class "com.lughzdan.app.OfflineDeviceTest#$1" com.lughzdan.app.test/androidx.test.runner.AndroidJUnitRunner)
  printf '%s\n' "$output"
  [[ "$output" == *'OK (1 test)'* ]]
}
run_phase phaseOneSolve
adb shell am force-stop com.lughzdan.app
adb shell svc wifi disable
adb shell svc data disable
run_phase phaseTwoRestoreOffline
