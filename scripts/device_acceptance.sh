#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/../apps/android"
mkdir -p artifacts/stage03
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
  if [[ -n "${record_pid:-}" ]]; then
    adb shell 'pidof screenrecord | xargs -r kill -2' || true
    wait "$record_pid" || true
  fi
  mkdir -p artifacts/stage03
  adb shell df -k /data > artifacts/stage03/device-storage.txt || true
  adb pull /sdcard/Android/data/com.lughzdan.app/files/ artifacts/stage03/device-files || true
  for metric in artifacts/stage03/device-files/stage03-gfxinfo.txt artifacts/stage03/device-files/stage03-meminfo.txt; do
    if [[ -f "$metric" ]]; then cat "$metric"; fi
  done
  adb pull /sdcard/atlas-gameplay.mp4 artifacts/stage03/ || true
  if [[ "$result" -ne 0 ]]; then adb logcat -d -t 1000 > artifacts/stage03/failure-logcat.txt || true; fi
}
trap collect_evidence EXIT
run_phase() {
  local output
  output=$(adb shell am instrument -w -r -e class "com.lughzdan.app.${2:-OfflineDeviceTest}#$1" com.lughzdan.app.test/androidx.test.runner.AndroidJUnitRunner)
  printf '%s\n' "$output"
  [[ "$output" == *'OK (1 test)'* ]]
}
run_phase directoryDurabilityUsesAndroidFileDescriptors
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


# Exercise the signed Atlas bundle through real navigation after legacy acceptance.
./gradlew :app:assembleDebug :app:assembleDebugAndroidTest -PatlasDevice=true
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell am force-stop com.lughzdan.app
adb shell screenrecord --time-limit 90 /sdcard/atlas-gameplay.mp4 >/dev/null 2>&1 &
record_pid=$!
run_phase journeyNotebookAndDuelPersistBeforeProcessKill AtlasDeviceTest
adb shell 'pidof screenrecord | xargs -r kill -2' || true
wait "$record_pid" || true
unset record_pid
adb shell am force-stop com.lughzdan.app
adb shell svc wifi disable
adb shell svc data disable
run_phase duelRestoresOfflineWithoutChangingPlayerExperience AtlasDeviceTest
adb shell dumpsys gfxinfo com.lughzdan.app > artifacts/stage03/atlas-gfxinfo.txt
adb shell dumpsys meminfo com.lughzdan.app > artifacts/stage03/atlas-meminfo.txt
adb shell getprop ro.build.fingerprint > artifacts/stage03/device-build.txt

# Additional real UI captures on the modern emulator, with large Arabic text.
if [[ "$(adb shell getprop ro.build.version.sdk | tr -d '\r')" == "35" ]]; then
  adb shell settings put system font_scale 1.3
  adb shell wm density 320
  adb shell wm size 640x1280
  adb shell am force-stop com.lughzdan.app
  run_phase scaledAtlasLayoutKeepsSubmissionVisible AtlasDeviceTest
  adb shell wm size 960x1600
  adb shell am force-stop com.lughzdan.app
  run_phase scaledAtlasLayoutKeepsSubmissionVisible AtlasDeviceTest
  adb shell wm size reset
  adb shell wm density reset
  adb shell settings put system font_scale 1.0
fi
