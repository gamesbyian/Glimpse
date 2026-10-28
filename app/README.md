# Native Android prototype

Development began from [issue #1](https://github.com/gamesbyian/Glimpse/issues/1). This is a deliberately bounded Compose/Room implementation, **a buildable debug prototype, not an Android-device-validated app or finished visual design**.

With JDK 17 and Android SDK 37 installed:

    ./gradlew :app:packageGlimpseDebugApk :app:testDebugUnitTest

[New native reel CI run 39924084187](https://github.com/gamesbyian/Glimpse/actions/runs/39924084187) completed successfully with `:app:assembleDebug` and `:app:testDebugUnitTest` and uploaded the **glimpse-debug-test-only** artifact. The distributable debug APK is `app/build/distributions/glimpse-debug.apk`. The Gradle-generated internal APK remains `app/build/outputs/apk/debug/app-debug.apk` for emulator installs and tooling. `:app:packageGlimpseDebugApk` runs `assembleDebug` before copying the distributable with the stable `glimpse` filename. A path-filtered workflow also builds/tests native changes and allows manual dispatch.

The native prototype can create collections; import Android Photo Picker selections and explicitly accepted Android Share images into SHA-verified offline originals; retry failed selections during the same running process; preserve per-collection focus; browse through a provisional animated five-aperture reel, Grid, and full-photo detail; explicitly set Home wallpaper; share an unchanged original through Android Sharesheet; and run no-repeat shuffle while the Activity is visible. Help & Privacy also exposes truthful build/channel disclosures, a read-only count/byte estimate and a user-edited opt-in diagnostic report. Exact original integrity is rechecked before outgoing sharing and wallpaper apply. Native reel material/motion and one-handed accessibility remain provisional until a real device validates the experience. Android Doze prevents any promise of exact background five-minute scheduling.

Core code: `data/GlimpseDatabase.kt` (Room), `data/PhotoImporter.kt` and `data/ImportJournal.kt` (durable snapshots, transient failure retry, redacted recovery), `data/LocalStorageInventory.kt` (safe read-only storage metadata), `domain/ShuffleBag.kt` and `domain/WallpaperCommands.kt` (single OS mutation gateway), `GlimpseModel.kt` (state), `domain/ReelWindow.kt` and `domain/ReelMotion.kt` (finite selection/gestures), `CircularPhotoReel.kt` (disc), `OriginalPhotoSharing.kt` (bounded original-only FileProvider handoff), `GlimpseSupportScreen.kt`, and `MainActivity.kt`.

Not implemented: automatic Google Photos album sync; exact five-minute background scheduling; full arbitrary-angle straightening and production-accepted on-device editor UX; direct metadata-sanitized Sharesheet sharing of edited results; Lock/Both wallpaper targets; widgets and Quick Settings; comprehensive library/settings backup and restore; deletion of a photo's final collection membership and original; cache-clearing UI; durable external-provider retry grants. Inbound Android Share image intake and user-tapped clipboard Paste image are implemented, but crop/edit continuation is not. Android read grants can expire, and no clipboard monitoring runs. An existing verified photo can be reused or unlinked across collections without deleting its original. Failed-import retry works only while its picker URIs remain available in memory. Native build/CI does not substitute for handset validation. Review [evidence and predictions](../docs/quality/first-slice-evidence.md) before claiming any acceptance.

### Debug APK signatures and safe upgrades

**Do not uninstall a working Glimpse installation merely to install a new CI
debug APK.** Uninstalling or clearing its storage removes its private original
photos and collections. GitHub Actions' default, automatically generated debug
certificate is not stable across fresh runners. Two successful October 27 CI
artifacts were independently examined and had different signing-certificate
SHA-256 fingerprints, so an in-place update between them is not supported.
The APK artifact includes a public certificate-fingerprint text file to let
testers compare builds. Matching Android package IDs do not imply matching
signatures.

For future *main-branch* debug builds, the workflow supports an optional,
persistent **PKCS#12 signing identity stored entirely in encrypted GitHub
Actions secrets**. All four secrets must be present together:
`GLIMPSE_CI_SIGNING_KEYSTORE_BASE64` (base64 of the private .p12 file),
`GLIMPSE_CI_SIGNING_STORE_PASSWORD`, `GLIMPSE_CI_SIGNING_KEY_ALIAS`,
and `GLIMPSE_CI_SIGNING_KEY_PASSWORD`. Never commit or upload the keystore
as a repository source, PR artifact, screenshot or chat attachment.
For a generated identity, use `keytool -genkeypair -storetype PKCS12` in a
trusted environment and preserve secure offline copies of the keystore and
passwords. Without configured secrets, builds stay ephemeral **test-only**
APKs and CI emits a visible warning. PR builds never receive the persistent signing secrets, including
same-repository PRs; they use ephemeral test signing and are not upgrade
acceptance candidates.

Configuring a stable identity for the first time does not retroactively make
older, differently signed debug APKs upgrade-compatible. A protected
backup/export-and-restore path and a separate release-signing policy remain
necessary before asking users to migrate devices or uninstall older versions.

### Collection original export and restore (implemented originals only)

Select a collection and choose **Export collection ZIP** from Home. Android's
Save As picker controls the destination. This is a portable originals-only ZIP,
with a versioned manifest and exact SHA-verified copies. From Help & Privacy,
**Restore collection ZIP…** opens Android Documents to select a compatible archive,
validates all entries and exact original bytes, then creates a new collection.
This is not a full library/settings backup or restore; it cannot recreate
rotation state, wallpaper acknowledgments or app preferences. A receiving
document provider may leave a partial file on export error. The external provider can see original EXIF
metadata and the collection title. Validate real provider persistence before
deleting any originals. The developer format contract is in
[collection-zip-export.md](../docs/architecture/collection-zip-export.md).


### Photo Detail: non-destructive editor

An explicit **Edit photo (crop and rotate)** panel previews 90° clockwise or
counterclockwise rotation and individually adjustable normalized crop edges.
**Reset crop and rotation** restores the unaltered preview. **Save edited PNG**
opens Android Save As and outputs a bounded (3072 px longest edge) PNG without
copying the source's original EXIF metadata. The untouched SHA-verified import
continues to back wallpaper and original-sharing actions. Crop recipes can be retained in private app preferences, and users can save edited images as separate SHA-verified originals in their collection and explicitly apply the result through the existing Home wallpaper gateway. Freeform touch framing, fine straightening, arbitrary full-resolution export and a comprehensive editing history remain future work. Physical-device editor acceptance remains outstanding. A receiving document provider may retain a partial
file on interrupted output, so verify important exports before relying on them.

### Personal test APK installation policy (2026-10-28)

For the repository owner's current personal test cycle, clean uninstall and reinstall are explicitly acceptable, including loss of existing Glimpse private data. The native CI's ephemeral-signing debug APK may therefore be used for this limited purpose; if Android reports a signature mismatch, uninstalling the previous test version is an owner-authorized option. This does not change production requirements for stable signing, data-preserving upgrades, migration or backup/restore. Always distinguish a CI-pass test artifact from a release-qualified build.
