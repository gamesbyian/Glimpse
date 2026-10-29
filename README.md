# Glimpse

> **Verified implementation coverage (2026-10-28, post-#98):** The [73-workflow audit](docs/quality/IMPLEMENTATION-COVERAGE-2026-10-28.md) scores current merged native `main` at **4 complete / 14 partial / 55 not yet implemented**: **15.1% scenario-equivalent across M1–M4** using a declared half-credit for partial scenarios. First-use M1 is **75% scenario-equivalent** (4 complete/4 partial) but lacks production-grade 5-minute background scheduling and final accessibility/visual signoff. [#98](https://github.com/gamesbyian/Glimpse/pull/98) is merged with a real seven-day import filter; SQL pagination is still outstanding. [#99](https://github.com/gamesbyian/Glimpse/pull/99) pixel-resolution advice, [#100](https://github.com/gamesbyian/Glimpse/pull/100) persistent favorites, [#101](https://github.com/gamesbyian/Glimpse/pull/101) favorites-only contact sheet, and [#102](https://github.com/gamesbyian/Glimpse/pull/102) opt-in OS-delayed background work are implementation candidates pending Android acceptance, **not counted as merged**. Draft [#94](https://github.com/gamesbyian/Glimpse/pull/94)/[#95](https://github.com/gamesbyian/Glimpse/pull/95) two-reel UI and draft [#6](https://github.com/gamesbyian/Glimpse/pull/6) whole-product visual review require explicit owner approval.

> **Current Android baseline (2026-10-28):** Fixed edge-anchored photo reel (photo-window drags), photographic edge-to-edge Home, full-height contact sheet with Grid→Reel selection, landscape reel sizing, 48dp Home/Reel/Photo Details controls, verified offline-photo imports, ZIP photo-only restore, original and edited-PNG Android sharing are **merged on main**. Latest qualified merges: [#91](https://github.com/gamesbyian/Glimpse/pull/91) `79be2803`, [#89](https://github.com/gamesbyian/Glimpse/pull/89) `bab2a265`, [#92](https://github.com/gamesbyian/Glimpse/pull/92) `21e0d644`, [#90](https://github.com/gamesbyian/Glimpse/pull/90) `83d29bb6`, [#93](https://github.com/gamesbyian/Glimpse/pull/93) `d4fb7207`. Each had green final-head native and full Android visual-atlas CI. **Still pending:** [#94](https://github.com/gamesbyian/Glimpse/pull/94) experimental second *functional* top-right collection reel and its stacked [#95](https://github.com/gamesbyian/Glimpse/pull/95) real photographic collection-cover apertures, both in Android validation and requiring owner's visual/ergonomic approval; draft whole-product [#6](https://github.com/gamesbyian/Glimpse/pull/6) likewise remains unmerged. Neither CI nor a debug APK constitutes physical Pixel, final visual, TalkBack, release signing or reliable background five-minute rotation acceptance. The newest independently verified [contact-sheet APK](https://github.com/gamesbyian/Glimpse/actions/runs/40779234265/artifacts/12855209387) is older than the #89/#90/#92/#93 merges. Earlier incremental notes are in [the status archive](docs/planning/HISTORICAL-INTEGRATION-NOTES-2026-10-28.md).

**An installable, local-first Android wallpaper manager with tactile paper reels and photographic collection browsing.**

Glimpse is a native Android wallpaper manager concept built around a large-aperture paper photo reel, purposeful browsing, offline collections, and reliable, understandable wallpaper rotation. It uses the user's photos and supported external sources rather than operating its own cloud library.

> **Status (2026-10-23): An installable Android prototype now exists.** The owner successfully used its first version to import **22 photos from Google Photos**, apply Home wallpaper, and observe active-use shuffle. The newer [circular swipe/tap reel prototype](app/src/main/java/io/github/gamesbyian/glimpse/CircularPhotoReel.kt), compact controls and finite-navigation tests have [compiled and passed CI](https://github.com/gamesbyian/Glimpse/actions/runs/39924084187), but **the revised UI has not yet been tested on a physical phone or accepted as final artwork**. Android emulator screenshot and accessibility-hierarchy capture now run in CI, including compact/200%-text checks; these are evidence of running software, **not** owner signoff or physical-device acceptance.

## First native development slice

Choose several photos with Android Photo Picker (including cloud-backed selections when exposed by the picker), make verified durable offline copies, create a collection, and start shuffled **five-minute active-use** wallpaper rotation. Android's battery restrictions prohibit promising exact wake-from-sleep timing. The app must distinguish *preview*, *eligible to change*, *requested change*, and *successfully applied*.

The wallpaper browser uses a five-wide-window tactile paper reel, a large hero preview, honest finite photo counts, and an always-available Grid / View all alternative. Simply browsing a reel must never apply a wallpaper.

## Documentation

**Start with [the current decisions](docs/planning/current-decisions.md).** More recent specific decisions take precedence over older working-title documents.

| Topic | Start here |
| --- | --- |
| Product requirements | [Product specification](docs/product/product-specification.md), [73-scenario audit](docs/product/feature-audit.md) |
| Primary acceptance journey | [Rainbow quickstart](docs/product/rainbow-quickstart.md), [user workflows](docs/product/workflows.md) |
| Images, screenshots and sharesheet | [Import and sharing requirements](docs/product/image-import-sharing.md) |
| UX and navigation | [Reel interaction](docs/design/reel-interaction.md), [critical UX audit](docs/design/reel-ux-audit.md), [surface inventory](docs/design/ui-surface-inventory.md) |
| Reels and comparative evidence | [Reel layout comparison](docs/design/reel-layout-comparison.md), [human expectations](docs/design/human-expectations.md) |
| Android implementation plan | [Native architecture](docs/architecture/android.md), [behavior contracts](docs/architecture/behavior-contracts.md), [motion/rendering](docs/architecture/reel-motion.md) |
| Testable UI contracts | [Screen contract](docs/design/screen-contract.json), [reference acceptance](docs/quality/reference-acceptance.md) |
| Work queue | [Implementation queue](docs/planning/implementation-queue.md), [design decision queue](docs/planning/decision-queue.md) |
| Visual proposals | [Searchable 66-image review atlas](design/atlas/README.md), [reconciled reference set](design/proposals/README.md) |
| Common app experience | [About, Help, crash reporting, privacy, updates, reviews, and support specification](docs/product/everyday-app-experience.md) |
| Design files and asset availability | [Design directory](design/README.md), [asset inventory](docs/assets.md) |
| Development history | [Conversation record](docs/history/conversation-record-2026-10-22.md) |

A [complete documentation map](docs/README.md) links the supporting matrices and plans.

## Native implementation

An Android/Gradle project with a provisional native circular reel now lives in [app/](app/README.md), with Kotlin, Jetpack Compose, Room/KSP, Photo Picker, WallpaperManager and a foreground-only active-use shuffle. WorkManager policies and Glance widgets are future additions, not implemented features. With SDK 37 and JDK 17, run `./gradlew :app:packageGlimpseDebugApk :app:testDebugUnitTest`. Downloadable APKs are packaged as `glimpse-debug.apk` inside the **glimpse-debug-test-only** Actions artifact; the internal Gradle output (`app-debug.apk`) is retained for emulator workflows. A [verified successful Actions run](https://github.com/gamesbyian/Glimpse/actions/runs/39904252446) contains the downloadable **glimpse-debug-test-only** artifact (not a signed release). See the [implementation queue](docs/planning/implementation-queue.md), [issue #1](https://github.com/gamesbyian/Glimpse/issues/1) and [prediction/evidence ledger](docs/quality/first-slice-evidence.md).

The initial vertical slice is **import → offline collection → preview → apply wallpaper → shuffle**. Advanced rules, visual refinements and provider integrations follow once the basic workflow works. Test real Android capabilities on actual hardware rather than claiming mockups prove implementation.

## Repository conventions

- `docs/product/`: what the app should do and users expect
- `docs/design/`: layout, behavior, screen semantics and human-expectations research
- `docs/architecture/`: engineering design and platform constraints
- `docs/planning/`: current decisions and remaining work
- `docs/quality/`: acceptance plans
- `docs/history/`: historical evidence, not current feature authority
- `app/`: native implementation; build and run instructions in [app/README.md](app/README.md)
- `design/reference/`: reference drawings (schematic; no blanket pixel-perfect approval)
- `design/research/`: alternative designs and their generators
- `design/tokens/`: provisional brand tokens

## Migration and provenance

The planning material originated in the **private** `gamesbyian/general-purpose/Glimpse` directory and was separated into this repository on 2026-10-22. The source archive has been retained. Old Waltz slide decks, 16 superseded screen sketches, and temporary binary-transfer chunks are intentionally excluded. [Asset status and preservation notes](docs/assets.md) distinguish transferred materials from assets still in the private archive. The new repository is public, so image and font licensing must be confirmed before publishing additional fixture binaries.

No licence file has been selected. Until then, the repository's public visibility alone does not grant an open-source licence.
