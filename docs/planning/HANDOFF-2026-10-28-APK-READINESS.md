# APK preparation and repository handoff — 2026-10-28

## Verified live repository state
- Repository: `gamesbyian/Glimpse`, default branch `main`.
- PR #79, implementation queue and Room schema reconciliation, merged by squash as `ac7ad6c108a7cdeed2fcb5e7a5f834fadba89b86` on 2026-10-28. Its PR head `486d413ca5d9488588174a06af9d5c9e9a4f4a70` had a successful Native Android slice workflow (run 40644698588).
- PR #6 remains OPEN/DRAFT on `eb6d62cc56921c53e5078374b1aab4ac985a2632`; do not merge or claim owner approval.
- Open issues #1, #7, #8, #9, #10, #13, #72 remain unresolved. Do not close acceptance issues based on a green compile.
- Merge #79 updated `docs/planning/implementation-queue.md` for merged PRs #73, #74, #75, #77 and #78, removing inaccurate in-development descriptions.
- No main-head workflow result after the #79 squash was established in this handoff. Inspect the exact current SHA and run a fresh source-head build before declaring APK ready. The GitHub connector's commit-workflow lookup only returns pull-request-triggered runs and does not establish main push CI health.

## Build handoff
- JDK 17 / Android SDK 37. Execute `./gradlew :app:packageGlimpseDebugApk :app:testDebugUnitTest`.
- Expected shareable filename: `app/build/distributions/glimpse-debug.apk`. AGP's internal emulator-install path remains `app/build/outputs/apk/debug/app-debug.apk`.
- Examine the Native Android slice workflow artifact and signer fingerprint for the exact current main commit. Check native tests and any emulator atlas failures before issuing the APK.
- **Issue #72 is a data-preservation blocker for an installed user.** Fresh GitHub-hosted debug signing certificates can differ. Never suggest uninstall/clear-data to solve signature mismatch; app-private originals/collections would be lost. Optional persistent encrypted PKCS#12 Actions secrets require separate authorized setup and verification across two builds. Changing signing now cannot upgrade previous differently signed builds.
- Native 320dp/200%-font, TalkBack, gesture ergonomics, owner visual acceptance (#13/#6) and real Pixel device observations remain distinct from build readiness.

## Product truth and next steps
- Working bounded native functionality includes Photo Picker import with offline SHA-verified originals, collection management, reel and grid navigation, explicit Home apply, foreground shuffle, export/restore collection originals, Sharesheet, deliberate clipboard intake and non-destructive crop/quarter-rotation editor with edited PNG outputs and collection copies. Read `app/README.md` for exact limitations.
- Unimplemented or unaccepted: automatic Google Photos album sync, guaranteed background five-minute wallpaper changes, full library/settings backups, widgets, Quick Settings, Lock/Both targets, full physical-device accessibility and production signing.
- Next agent: recheck main Actions runs at this exact head, repair any *actual* red jobs, preserve unmerged visual draft, verify APK package name and public signing certificate, publish a new test build only with explicit upgrade-safety warnings. Update this document with exact run/commit/artifact after verification.
