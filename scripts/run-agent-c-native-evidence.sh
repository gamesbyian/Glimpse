#!/usr/bin/env bash
# Execute independent emulator validations even when one fails. A parent GHA
# action wrapper may impose -e on its own shell, so this is a single child
# script with explicit failure aggregation, not multiple action script lines.
set -euo pipefail
adb install -r app/build/outputs/apk/debug/app-debug.apk
# Instrumentation leaves an active target-app process on some API34
# emulators, making a subsequent pm clear fail. Capture seeded Room journeys
# first; JUnit OS grant/readback instrumentation runs second, independently.
capture_rc=0
python3 scripts/capture-agent-c-native.py --out artifacts/agent-c-native || capture_rc=$?
tests_rc=0
./gradlew --no-daemon :app:connectedDebugAndroidTest || tests_rc=$?
printf 'Independent native checks: instrumentation=%s, screenshots=%s\n' "$tests_rc" "$capture_rc"
if [[ "$tests_rc" -ne 0 || "$capture_rc" -ne 0 ]]; then
  exit 1
fi
