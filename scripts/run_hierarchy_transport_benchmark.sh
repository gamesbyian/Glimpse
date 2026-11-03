#!/usr/bin/env bash
# ReactiveCircus executes each script field line as a separate shell command.
# Keep loops, background jobs and their cleanup in one child Bash process.
set -euo pipefail
mkdir -p artifacts/hierarchy-transport
export APPIUM_HOME="$RUNNER_TEMP/glimpse-appium-home"

"$RUNNER_TEMP/glimpse-appium/bin/appium" \
  --address 127.0.0.1 --port 4723 --log-no-colors \
  > artifacts/hierarchy-transport/appium-server.log 2>&1 &
appium_pid=$!
cleanup() {
  kill "$appium_pid" 2>/dev/null || true
  wait "$appium_pid" 2>/dev/null || true
}
trap cleanup EXIT

ready=0
for attempt in $(seq 1 35); do
  if curl --silent --fail "http://127.0.0.1:4723/status" -o /dev/null; then
    ready=1
    break
  fi
  if ! kill -0 "$appium_pid" 2>/dev/null; then
    echo "::error::Pinned Appium exited before readiness"
    break
  fi
  sleep 1
done
if [[ "$ready" != 1 ]]; then
  echo "::error::Pinned Appium service failed to become ready"
  exit 1
fi
adb wait-for-device
adb install -r app/build/outputs/apk/debug/app-debug.apk
python3 scripts/benchmark_hierarchy_transport.py \
  --out artifacts/hierarchy-transport
