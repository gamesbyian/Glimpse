# Explicit owner-test Glimpse APK publication

The normal `Native Android slice` workflow compiles, tests and lints the
debug APK, but does **not** retain that disposable binary. Lint and failure
evidence remain retained under their existing limits.

When the owner specifically requests a new APK, dispatch **Native Android
slice** on `main` with `publish_owner_test_apk = true`. Only this manually
authorized mode transfers a SHA-256-checked APK to a separate, main-only
publication job. That job creates a `glimpse-owner-test-<source SHA12>`
GitHub prerelease containing `glimpse-debug.apk`, its SHA256 and its
signing-certificate fingerprint. After the new release exists, it retires
older `glimpse-owner-test-*` prereleases. It does not delete other
releases, test receipts, source files or photo evidence.

The one-day Actions handoff artifact is transient transport for this job;
the current GitHub Release stays available until explicitly superseded.

**Important signing safety:** The repository may lack a configured persistent
debug signing certificate. A newly built APK might therefore **fail to update
an existing installation**. Never instruct an owner to uninstall Glimpse to
work around a signing mismatch without independently protecting app-owned
photo data. Source SHA and file digest prove build identity, not real-device
or data-preserving upgrade acceptance (see existing signing issue #72).
