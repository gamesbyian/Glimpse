# Glimpse debug APK build request — 2026-10-28

This branch is cut from the current main after the October 28 documentation reconciliation. It exists solely to request a fresh Android CI build/test/package on a pull-request head and retain auditable evidence for an owner-installable debug APK. No app logic changes are intended.

Acceptance: Android native CI passes; the uploaded distribution contains `glimpse-debug.apk`; report the exact artifact and SHA. Owner explicitly permits wiping existing local app data and uninstall/reinstall for personal testing, so ephemeral debug signing is acceptable for this test APK. Do not treat this as release-signing acceptance or as design approval.
