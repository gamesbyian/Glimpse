# Glimpse open-source reuse reconnaissance
**Research date:** 2026-10-29. **Target:** `gamesbyian/Glimpse`, native Android main. **Type:** engineering research and a proposed reuse/adoption queue, **not** a new product specification or verification of copied code in Glimpse.

## Starting point and decision rule

The source/queue audit on 2026-10-29 reports **4 complete / 15 partial / 54 unimplemented** of 73 workflows (15.8% scenario-equivalent; M1 75%). See [coverage crosswalk](../quality/IMPLEMENTATION-COVERAGE-2026-10-28.md) and [implementation queue](../planning/implementation-queue.md). Current main already has Kotlin/Compose/Room, SHA-verified imported originals, a photo-only ZIP archive, a bounded image decode path, a serialized wallpaper apply gateway, a no-repeat bag, basic Photo Detail edit geometry, full-height contact sheet and the physical reel. **Do not replace these indiscriminately.**

Primary implementation rule: reuse **permissively licensed small components**, borrow **patterns** where code is GPL, and measure against existing Glimpse contracts before introducing libraries. The repository currently has no top-level license file; public GitHub visibility alone does not authorize third-party code reuse *from Glimpse* or determine Glimpse's own redistribution terms. A license/attribution decision is needed before distributing copied MIT/Apache code; preserve notices and check asset/model/transitive licenses individually.

This study inspected public repositories, project licenses/activity metadata, file trees and selected Kotlin source, as well as Android's current platform references. Individual files are **candidates**, not certified security audits or proof of exact OEM behavior. Repository activity/branches can change.

## A. OSS donor catalogue

Priority denotes **value to unfinished Glimpse scope**, not a blanket recommendation to install.

| Candidate (licence confirmed 2026-10-29) | Concrete reusable material | Glimpse scenarios / work | Direction |
|---|---|---|---|
| [NineCSdev/nothing-wallpaper-changer](https://github.com/NineCSdev/nothing-wallpaper-changer) (MIT; active 2026-10-29) | `RotationCoordinator`, `RotationScheduler`, `RotationEngine`, `WallpaperApplier`, `ServiceLifecycle`, `ScreenStateReceiver`, `WallpaperTileService`, typed destination and battery policy; `BackupManifest`/`BackupImporter`/`BackupExporter`; folder diff and editing math | W01/W11/W17/W18/W22/W51/W54/W60/W62/W64/W70 | **First source-inspection spike.** Adapt a few pure models and tests, not the entire foreground service or Hilt architecture |
| [muzei/muzei](https://github.com/muzei/muzei) (Apache-2.0; active 2026-10-26) | `muzei-api/.../provider/MuzeiArtProvider.kt`, `ProviderClient.kt`, `Artwork.kt`; `android-client-common/.../sync/ProviderManager.kt`, `ArtworkLoadWorker.kt`; widget and TileService; live wallpaper | W31-40/W61/W62, optional live mode | **Provider contract benchmark.** Prefer a typed internal Glimpse adapter; consider exposing Muzei API interoperability only as a separate optional milestone |
| [saad-khan-rind/NOSAtmosphereEffect](https://github.com/saad-khan-rind/NOSAtmosphereEffect) (MIT; active 2026-10-28) | Preview-to-live-render consistency, effect parameter models, crop/zoom playlists, optional subject segmentation, theme-playlist switching, live WallpaperService lifecycle | W21/W24/W25/W30/W51-53, later live mode | **High-value later study.** Do not add ongoing GPU shaders until current static wallpaper and accessibility acceptance |
| [Anthonyy232/Paperize](https://github.com/Anthonyy232/Paperize) (GPL-3.0; active 2026-10-28) | `WallpaperScheduler.kt`, `WallpaperChangeWorker.kt`, `WallpaperChangeLock.kt`, `WallpaperQueueEntity.kt`, `WallpaperCurrentEntity.kt`; scheduler and controller JVM/instrumentation tests; optional GL live mode | W01/W17/W30/W42-50 | **Read/test architecture only**, unless Glimpse's license strategy consciously permits GPL code |
| [ammargitham/WallFlow](https://github.com/ammargitham/WallFlow) (GPL-3.0; last pushed 2024-08) | `AutoWallpaperHistoryDao`, `AutoWallpaperWorker`, `ChangeWallpaperTileService`, `ChangeWallpaperBroadcastReceiver`, crop and object detection approach, Favorites and multi-source models, `WallpapersRemoteMediator` | W05/W06/W07/W24/W33-35/W43/W54/W61-63 | **Feature/UX/test reference only**. Code is GPL and older; particularly valuable for Tasker broadcasts and source data boundaries |
| [coil-kt/coil](https://github.com/coil-kt/coil) (Apache-2.0; active 2026-10-29) | `coil-compose`: bounded size-specific loads, memory/disk caching, cancellation, decoder integration | W07, Reel/Grid/hero performance | **Pilot one thumbnail/cache integration** against current bounded decoder, 16MB cache policy and strict photo-ID correctness |
| [saket/telephoto](https://github.com/saket/telephoto) (Apache-2.0; active 2026-07) | `ZoomableImage`/subsampling/keyboard/double-tap/inertia/Compose API; Coil3 adapter | W51/W56, Photo Detail, large sources | **Preferred zoom spike** if compatible with Glimpse's existing crop overlay. Choose only one zoom stack |
| [panpf/zoomimage](https://github.com/panpf/zoomimage) (Apache-2.0; active 2026-09) | Alternate Compose zoom, subsampling, keyboard, EXIF, locate and rotate with Coil support | W51/W56 | Compare to Telephoto in one controlled spike; **do not ship both** |
| [CanHub/Android-Image-Cropper](https://github.com/CanHub/Android-Image-Cropper) (Apache-2.0; active 2026-04) | `CropImageView` gesture math, rotation, sampling, configurable limits | F071/F073/W51/W58-59 | Evaluate reuse of geometry/interaction only if canonical preview/export/apply renderer remains one |
| [prof18/RSS-Parser](https://github.com/prof18/RSS-Parser) (Apache-2.0; active 2026-10) | Kotlin RSS/Atom/RDF parser, feed metadata and media enclosures | W37, remote sources | **Direct dependency candidate**, gated on safe HTTPS fetch policy, image rights and user-granted source access |
| [commons-app/apps-android-commons](https://github.com/commons-app/apps-android-commons) (Apache-2.0; active 2026-10) | Wikimedia/MediaWiki client, entity descriptions and license/attribution UX | W32/W39 | Adapt source metadata/rights display approach; uploading code is not an image-download API adapter |
| [android/nowinandroid](https://github.com/android/nowinandroid) (Apache-2.0; active 2026-10) | Compose testing, offline-first repository patterns, Baseline Profiles, performance/build convention | W07/W67, M1/M2 reliability | Selective architecture/testing patterns only; avoid gratuitous Gradle multi-module migration |
| [android/compose-samples](https://github.com/android/compose-samples) (Apache-2.0; active 2026-10) | Responsive Compose layouts, UI tests, state and accessibility patterns | W66/W67 and actual screens | Test-asset/template donor, no branded UI replacement |
| [nasa/apod-api](https://github.com/nasa/apod-api) (Apache-2.0; API service being superseded) | APOD response parser/metadata background, API migration information | W31 | **Do not adopt legacy endpoint as the permanent provider**; see API change below |
| [buerlino/APODroid](https://github.com/buerlino/APODroid) (GPL-3.0; active 2026-10) | An Android example selecting image-only NASA APOD and other NASA photos, aspect framing and background rotation | W31 | Behavioural reference only; inspect endpoints and do not copy GPL code |

Licensing above is from GitHub repository license metadata and/or the projects' published LICENSE/readmes. **No source files are copied in this research PR.** Repository license does not necessarily cover third-party pictures, fonts, model weights, icons or API-delivered media.

## B. Source-level reading itinerary

**MIT Nothing Wallpaper Changer:** Open the following on `main`, under [`app/src/main/java/com/ninecsdev/wallpaperchanger/`](https://github.com/NineCSdev/nothing-wallpaper-changer/tree/main/app/src/main/java/com/ninecsdev/wallpaperchanger):
- `logic/RotationCoordinator.kt`: handles multiple rotation outcomes, a guard against overlapping rotation and deferred-display confirmations. Glimpse must preserve its stricter durable `requested/pending/confirmed/unknown` semantics and its own command gateway.
- `logic/RotationEngine.kt` and `logic/BufferManager.kt`: prepared buffer, refill, synchronization and screen-off policy. Compare queue reservation/commit with Glimpse's persistent no-repeat shuffle bag. Never advance a bag merely on preparation.
- `logic/RotationScheduler.kt` and `service/ScreenStateReceiver.kt`: screen-on/off-driven scheduling, screen-off delay and cancellation gates. Treat a foreground-service implementation as a **capability/Play-policy experiment**, not proof of reliable 5-minute wakeups.
- `logic/WallpaperApplier.kt`: static `WallpaperManager.setStream` versus live-engine deferred delivery, destination-bit flags and acknowledgement distinctions. Keep a single Glimpse gateway.
- `logic/ServiceLifecycle.kt` + `service/ServiceRestartReceiver.kt`: desired service intent versus currently alive service, Battery Saver/quiet-hours, boot and upgrade recovery. Worth adapting the **pure transition table and failure cases**. Several source TODOs still call for tests.
- `data/backup/BackupManifest.kt`, `BackupExporter.kt`, `BackupImporter.kt`: device vs portable modes, manifest/version refusal, staging, availability checks, restore planning and partial-result handling. Glimpse already has a secure ZIP *photo-only* pipeline; **extend its format** with settings/recipe manifest, don't replace its validated streaming archive.
- `logic/EditTransform.kt`: normalized offsets and matrix transform calculations. Compare to Glimpse's `PhotoEditGeometry` in property-based tests (EXIF, rotation, crop).
- `data/source/FolderSync.kt`: independent pure add/remove/exclusion diff. In Glimpse, restore-safe exclusion tombstones and failures must prevent an empty/failed scan deleting the library.

**Apache Muzei:** [`muzei-api/.../provider/MuzeiArtProvider.kt`](https://github.com/muzei/muzei/blob/main/muzei-api/src/main/java/com/google/android/apps/muzei/api/provider/MuzeiArtProvider.kt); [`ProviderClient.kt`](https://github.com/muzei/muzei/blob/main/muzei-api/src/main/java/com/google/android/apps/muzei/api/provider/ProviderClient.kt); [`ProviderManager.kt`](https://github.com/muzei/muzei/blob/main/android-client-common/src/main/java/com/google/android/apps/muzei/sync/ProviderManager.kt); [`NextArtworkTileService.kt`](https://github.com/muzei/muzei/blob/main/main/src/main/java/com/google/android/apps/muzei/quicksettings/NextArtworkTileService.kt). Extract *capability-described provider and source-health semantics*, and avoid inheriting a plugin's storage/permissions implicitly. A Muzei-compatible integration could be a user-controlled export/provider bridge, separate from Glimpse's own schedule.

**GPL architecture references:** [Paperize `WallpaperScheduler.kt`](https://github.com/Anthonyy232/Paperize/blob/master/app/src/main/java/com/anthonyla/paperize/service/worker/WallpaperScheduler.kt) and [its scheduler tests](https://github.com/Anthonyy232/Paperize/blob/master/app/src/test/java/com/anthonyla/paperize/service/worker/WallpaperSchedulerTest.kt); [WallFlow `AutoWallpaperWorker.kt`](https://github.com/ammargitham/WallFlow/blob/main/app/src/main/java/com/ammar/wallflow/workers/AutoWallpaperWorker.kt), [`ChangeWallpaperBroadcastReceiver.kt`](https://github.com/ammargitham/WallFlow/blob/main/app/src/main/java/com/ammar/wallflow/receivers/ChangeWallpaperBroadcastReceiver.kt) and [`WallpapersRemoteMediator.kt`](https://github.com/ammargitham/WallFlow/blob/main/app/src/main/java/com/ammar/wallflow/data/repository/WallpapersRemoteMediator.kt). Understand responsibility splitting and test scenarios, then independently implement the required Glimpse behaviour if keeping a permissive distribution strategy.

## C. Priority-ranked implementation reuse plan

1. **R0: preserve core + production blockers, no unnecessary imports.** Finish stable signing custody and physical Pixel/reel accessibility acceptance first. Research cannot substitute for user optical approval, Doze observation or release-signed upgrade testing.
2. **P0 / M1 reliability: scenario-driven scheduler adaptation.** Derive a pure state table from MIT `ServiceLifecycle` and Paperize's test cases: process kill, restart, Battery Saver, screen off/on, delayed work, two destinations, pending OS mutation, duplicate trigger, no catch-up bursts. Extend the existing `BestEffortRotationWork` rather than adding a parallel scheduler. Accept actual WorkManager >=15 minute best-effort cadence, with user-facing lag transparency.
3. **P0 / M1 accessibility: use platform Compose semantics/focus and native screen capture.** Borrow official Compose test patterns, not another product's entire theme. Preserve fixed reel selection IDs, minimum touch target and 320dp/200% font fallback. Owner approval remains a separate gate.
4. **P1 / M2 image handling:** Pilot **Coil thumbnail caching** and **Telephoto Photo Detail zoom** separately on 22/60/12,000 photo fixtures; compare memory, startup/scroll, correct ID-to-thumbnail binding, 200% text, large EXIF sources, cache eviction. Never pass a cached thumbnail as final wallpaper/export pixels.
5. **P1 / M2 full device backups:** Keep Glimpse's existing ZIP image manifest and streaming verifier. Add versioned settings/recipes, restore plan, ID remap, missing-photo preview and encrypted optional settings later. Study MIT BackupImporter for staging/transaction ideas. Refuse unknown schema versions rather than guessing. Test wrong passwords, truncated ZIP, ZIP bombs, symlinks, interruption, duplicate source IDs and low storage.
6. **P1 / M2 widget, shortcut, Quick Settings:** Use AndroidX Glance and Muzei's tile mechanics as interface examples; each sends a typed command into Glimpse's **single** wallpaper executor. No independent tile-side shuffle, import or setter.
7. **P1 / M2 large library:** Migrate Grid to **Room PagingSource + Paging 3 + Paging Compose**, with stable photo ID keys and filter/sort queries. WallFlow's `WallpapersRemoteMediator` is only a reference for later network-mediated collections, not needed to page app-private local Room rows.
8. **P2 / M3 rule grammar and simulation:** Build a deterministic typed, versioned pure evaluator returning `EligibleCandidates + ExplanationTrace`; reuse existing `ShuffleBag` and eligibility foundation. Reference Paperize/WallFlow for sequencing and fallback *ideas*. Do not import an arbitrary untrusted script-execution engine or share state with the Android worker in a way that makes the read-only simulator advance actual history.
9. **P2 / M3 external sources:** Implement a capability-gated `SourceAdapter` with fetch/status/rights/attribution/content type/rate limiting, inspired by Muzei; choose single small providers first. RSS Parser is only an XML parser: Glimpse must implement SSRF/HTTPS/redirect protection, content-size caps, MIME sniffing, licence evidence, moderation queue, offline fallback and user approvals.
10. **P3 / optional live wallpaper:** Compare MIT Atmo and Apache Muzei for `WallpaperService.Engine` visibility, render/prefetch separation, explicit target capability and preview parity. Keep static background and live modes separate. No forced switch to a persistent service just to claim an exact five-minute schedule.

## D. Platform corrections and newly discovered external change

- **Five-minute background schedule is not solved by installing an OSS scheduler.** Android says WorkManager is durable but inexact, periodic work has a 15-minute minimum; exact alarms are for precise user-visible alarms and are constrained. Foreground-service `specialUse` may require a documented valid use case and Google Play review. Observe actual Pixel and OEM behavior, and avoid claiming universal reliability merely because Paperize/NineCS uses a service. See [Android scheduling](https://developer.android.com/develop/background-work/background-tasks/persistent), [FGS types](https://developer.android.com/develop/background-work/services/fgs/service-types), [Play FGS review](https://support.google.com/googleplay/android-developer/answer/13392821).
- **System wallpaper readback is limited:** Android's [WallpaperManager reference](https://developer.android.com/reference/android/app/WallpaperManager) notes Android 13+ cannot generally retrieve the actual current wallpaper through `getDrawable`. Preserve Glimpse's last-acknowledged ID and unknown-external-change semantics.
- **Photo Picker is individual user-authorized media selection**, including eligible cloud assets, not a general Google Photos album-sync API. See [Android Photo Picker](https://developer.android.com/training/data-storage/shared/photo-picker) and [Google Photos help](https://support.google.com/photos/answer/14442861). Continue verified offline snapshots.
- **Time-sensitive NASA APOD source warning (discovered 2026-10-29):** [NASA API portal](https://api.nasa.gov/) says the legacy `api.nasa.gov/planetary/apod` service moved to an API backed by `https://science.nasa.gov/wp-json/wp/v2/apod-basic` on **2026-09-10**, and the legacy service is slated for retirement **2026-12-01**. Build W31 against the updated contract with typed parser tests, video filtering, rights review, endpoint feature differences, provider-status fallback; avoid implementing and then immediately retiring a legacy APOD client.
- **Google Play and OSS licensing:** source-code license compatibility is not permission to reuse media supplied by NASA, Wikimedia, wallpapers sites, ML model weights or contributors. Keep rights metadata and source URLs; do not automatically equate an API response with wallpaper-use rights.
- **Avoid unmeasured dynamic color assumptions:** MIT Atmo documents local extracted live-wallpaper palettes but OEM-controlled system theming. This does not imply an arbitrary third-party static app can force Android Monet palette selection.

## E. Minimal, independently qualified technical spikes

Each spike should be a small PR with old behaviour preserved, exact-head JVM/native tests and Android atlas only where rendering/semantics change. Avoid unrelated Gradle dependency churn.

| Spike | Testable result | Failure / rollback |
|---|---|---|
| S1 scheduler event reducer | Pure `event + persisted state → next state + planned effects` with JVM cases for pending/unknown/reboot/battery/duplicates/two targets | Test-only design first; no replacement service until emulator + Pixel policy evidence |
| S2 Coil thumb vs current bounded decoder | Thumbnail cache budget, stable selection, 22/60/12k fixture, memory counters, no full decode on UI thread | Feature flag/back out if no measurable benefit |
| S3 Telephoto zoom | Correct EXIF, huge photo pan/zoom, focus retention and keyboard, no changed crop/export bytes | Keep existing Photo Detail if transform ownership conflicts |
| S4 Room Paging 3 Grid | SQL-backed stable paged sorted/filter results, no all-record materialization, reel selection stable while pages load | Keep existing Room queries until parity verified |
| S5 full backup manifest extension | Round-trip / wrong version / missing image / interrupted restore / low space / collision / no source erasure | Opt-in upgrade; old photo-only ZIP restore remains supported |
| S6 TileService and Glance | User tap → existing wallpaper command → precise outcome; 320dp / large text / unavailable targets | Explicit disabled/error state, not optimistic success |
| S7 provider interface | Fake NASA and Commons adapters, quota/outage/rights/video/SSRF tests, local fallback and attribution | External source off by default until verified policies and consent |
| S8 rules simulator | Deterministic same engine used for read-only simulation and real eligibility; no reserve/commit in simulation | Versioned rule schema; unknown predicates disabled with reason |

## F. What **not** to crib

- Any GPL-3.0 implementation into a non-GPL Glimpse build without an explicit license decision and compliant source distribution.
- A second photo database, command gateway, shuffle engine, or schedulers running from widgets/services independently.
- Unbounded wallpaper/thumbnail decoding, arbitrary URIs or network fetch without caps, and source-controlled ZIP paths.
- A wallpaper changer that treats `WallpaperManager.setStream` return as proof that a user can see exact pixels, or treats applying Home/Lock as atomically supported everywhere.
- New reels rendered for numeric settings or rule AST controls, or replacing Glimpse's physical art/semantics with a generic Material gallery.
- ML crop inference as a silent override for user-defined focal regions; external models must preserve opt-in, provenance, size/privacy gates.
- OSS sample screenshots/test assets as shipped Glimpse wallpapers without their **own** reuse clearance.

## G. Source and reproducibility index

1. [Nothing Wallpaper Changer, MIT](https://github.com/NineCSdev/nothing-wallpaper-changer) (code source path above)
2. [Muzei, Apache-2.0](https://github.com/muzei/muzei), [Muzei API docs](https://api.muzei.co/)
3. [Paperize, GPL-3.0](https://github.com/Anthonyy232/Paperize)
4. [WallFlow, GPL-3.0](https://github.com/ammargitham/WallFlow)
5. [Atmo Engine, MIT](https://github.com/saad-khan-rind/NOSAtmosphereEffect)
6. [Coil and Compose](https://coil-kt.github.io/coil/)
7. [Telephoto](https://saket.github.io/telephoto/zoomableimage/), [subsampling](https://saket.github.io/telephoto/zoomableimage/sub-sampling/)
8. [ZoomImage](https://github.com/panpf/zoomimage)
9. [CanHub image cropper](https://github.com/CanHub/Android-Image-Cropper)
10. [RSS Parser](https://github.com/prof18/RSS-Parser)
11. [Wikimedia Commons Android](https://github.com/commons-app/apps-android-commons)
12. [Now in Android](https://github.com/android/nowinandroid), [Compose samples](https://github.com/android/compose-samples)
13. [NASA APOD API migration](https://api.nasa.gov/), [NASA APOD service repository](https://github.com/nasa/apod-api)
14. [Android WorkManager/task scheduling](https://developer.android.com/develop/background-work/background-tasks/persistent)
15. [Android WallpaperManager](https://developer.android.com/reference/android/app/WallpaperManager)
16. [Android Photo Picker](https://developer.android.com/training/data-storage/shared/photo-picker)
17. [Android Paging 3](https://developer.android.com/topic/libraries/architecture/paging/v3-overview)
18. [Android Glance](https://developer.android.com/jetpack/androidx/releases/glance)
19. [Android FGS service types](https://developer.android.com/develop/background-work/services/fgs/service-types)

**Next action:** accept/reject one small reuse spike (S1 first after blocker work), execute its tests and compare the exact native behaviour before promoting any source idea into an implementation-queue completion claim.
