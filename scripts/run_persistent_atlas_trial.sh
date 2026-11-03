#!/usr/bin/env bash
# Research only: one Bash process, disposable emulator and real Glimpse APK.
set -euo pipefail
mkdir -p artifacts/persistent-android-atlas
export APPIUM_HOME="$RUNNER_TEMP/glimpse-appium-home"
"$RUNNER_TEMP/glimpse-appium/bin/appium" \
  --address 127.0.0.1 --port 4723 --log-no-colors \
  > artifacts/persistent-android-atlas/appium-server.log 2>&1 &
server_pid=$!
cleanup() {
  kill "$server_pid" 2>/dev/null || true
  wait "$server_pid" 2>/dev/null || true
}
trap cleanup EXIT

ready=0
for attempt in $(seq 1 35); do
  if curl --silent --fail "http://127.0.0.1:4723/status" -o /dev/null; then
    ready=1
    break
  fi
  if ! kill -0 "$server_pid" 2>/dev/null; then
    echo "::error::Pinned Appium service exited before readiness"
    break
  fi
  sleep 1
done
if [[ "$ready" != 1 ]]; then
  echo "::error::Pinned Appium service never became ready"
  exit 1
fi

adb wait-for-device
adb install -r app/build/outputs/apk/debug/app-debug.apk
python3 scripts/capture_atlas_persistent_trial.py \
  --mode full --out artifacts/persistent-android-atlas
