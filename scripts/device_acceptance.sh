#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/../apps/android"
./gradlew :app:assembleDebug :app:assembleDebugAndroidTest
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
prepare_emulator_shell() {
  # API26 AOSP does not grant CHANGE_WIFI_STATE to the shell UID.
  # Root only the disposable emulator; never request app/device privileges.
  if [[ "$(adb shell getprop ro.build.version.sdk | tr -d '\r')" == "26" ]]; then
    [[ "$(adb shell getprop ro.kernel.qemu | tr -d '\r')" == "1" ]]
    adb root
    adb wait-for-device
  fi
}
prepare_emulator_shell
adb shell pm clear com.lughzdan.app
collect_evidence() {
  local result=$?
  mkdir -p artifacts/stage03
  adb pull /sdcard/Android/data/com.lughzdan.app/files/ artifacts/stage03/device-files || true
  for metric in artifacts/stage03/device-files/stage03-gfxinfo.txt artifacts/stage03/device-files/stage03-meminfo.txt; do
    if [[ -f "$metric" ]]; then cat "$metric"; fi
  done
  if [[ "$result" -ne 0 ]]; then adb logcat -d -t 1000 > artifacts/stage03/failure-logcat.txt || true; fi
}
trap collect_evidence EXIT
run_phase() {
  local output
  output=$(adb shell am instrument -w -r -e class "com.lughzdan.app.${2:-OfflineDeviceTest}#$1" com.lughzdan.app.test/androidx.test.runner.AndroidJUnitRunner)
  printf '%s\n' "$output"
  [[ "$output" == *'OK (1 test)'* ]]
}
run_phase phaseOneSolve
adb shell am force-stop com.lughzdan.app
adb shell svc wifi disable
adb shell svc data disable
run_phase phaseTwoRestoreOffline

adb shell dumpsys gfxinfo com.lughzdan.app reset
run_phase phaseThreeCrosswordPartial SixGameDeviceTest
adb shell am force-stop com.lughzdan.app
run_phase phaseFourRestoreCrosswordAndBuilder SixGameDeviceTest
adb reboot
adb wait-for-device
for attempt in {1..90}; do
  if [[ "$(adb shell getprop sys.boot_completed | tr -d '\r')" == "1" ]]; then break; fi
  sleep 1
done
[[ "$(adb shell getprop sys.boot_completed | tr -d '\r')" == "1" ]]
prepare_emulator_shell
adb shell input keyevent 82
adb shell svc wifi disable
adb shell svc data disable
run_phase phaseFiveVerifyBuilderResult SixGameDeviceTest
adb shell svc wifi enable
adb shell svc data enable
run_phase phaseSixOtherBoards SixGameDeviceTest
