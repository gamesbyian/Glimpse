# Glimpse

**A tactile, local-first Android wallpaper manager.**

Glimpse is a planned native Android app for organizing, browsing, sharing, and automatically rotating wallpapers drawn from your own images and supported external sources. Its defining visual idea is a warm paper photo reel with five generous image windows, a continuous-feeling browse interaction, and a conventional grid alternative.

> **Project status (22 October 2026): Design and engineering specification.** No functioning Android app, installable APK, or verified device implementation exists yet. Screens and deck artwork are proposals and references, not screenshots of running software.

## The first useful experience

Import selected photos through the Android Photo Picker (including supported cloud-backed selections), save durable offline copies into a collection, and activate shuffled wallpaper rotation with a five-minute *active-use eligibility* interval. See the [Rainbow quickstart acceptance contract](RAINBOW-QUICKSTART-CONTRACT.md).

Android may defer background work during sleep, Doze, or manufacturer power restrictions. Glimpse must show last successful change and next eligibility honestly instead of promising exact five-minute timing.

## What Glimpse is designed to do

- **Browse with purpose.** A tactile, finite paper reel selects and previews photographs; rotating the reel never silently changes the applied wallpaper. Grid / View all provides equal access.
- **Import and organize.** Use native Photo Picker, Android Sharesheet and document-provider integrations; maintain local collections and verified offline copies, with duplicates and failures explained.
- **Apply and rotate.** Select home or lock destinations where supported, shuffle without replacement, manually advance or undo, pause and inspect scheduling outcomes.
- **Edit and share.** Preview, rotate and crop screenshots or photos, save compositions, and share an original or edited copy to compatible Android apps through the system Sharesheet.
- **Expand deliberately.** Add sources, widgets, Quick Settings actions, contextual schedules and explainable rules when useful without forcing advanced setup into the basic workflow.
- **Stay private and accessible.** No Glimpse-hosted photo library, mandatory account, ad platform, or covert telemetry; preserve reduced-motion, TalkBack, keyboard, and large-text alternatives.

Features above are **requirements and plans**, not implemented features.

## Current design direction

The selected aesthetic is a broad-aperture photographic **paper reel**, deliberately cropped at the screen edge and paired with a stationary, full-size hero image. The number of photos is real and finite. Specific dual-reel arrangements, control positions, and many deck mockups remain hypotheses rather than approved solutions.

Start with the [current decisions](GLIMPSE-CURRENT-DECISIONS.md), [reel design](GLIMPSE-CONTINUOUS-REEL-DESIGN.md), [critical UX audit](GLIMPSE-REEL-UX-CRITICAL-AUDIT.md), [layout comparison](GLIMPSE-REEL-LAYOUT-COMPARISON-2026-10-22.md), and [motion/rendering plan](GLIMPSE-MOTION-RENDERING-ARCHITECTURE.md).

## Documentation map

| Need | Reference |
| --- | --- |
| What is approved, provisional or blocked? | [Current decisions](GLIMPSE-CURRENT-DECISIONS.md) |
| Complete product scope and historical context | [Product specification](PRODUCT-SPECIFICATION.md) |
| Natural-language journeys | [70 workflows](70-HUMAN-WORKFLOWS.md), [screenshot and sharing requirements](SCREENSHOT-AND-LENS-FEATURES.md) |
| Requirement completeness | [73-scenario feature audit](GLIMPSE-FEATURE-SPEC-COMPLETENESS-AUDIT.md) |
| Command and state semantics | [Technical behavior contracts](GLIMPSE-TECHNICAL-BEHAVIOR-CONTRACTS.md), [screen contract](GLIMPSE-SCREEN-CONTRACT.json) |
| Proposed Android stack | [Native Android architecture](NATIVE-ANDROID-ARCHITECTURE.md) |
| Screen and interaction inventory | [UI surface census](GLIMPSE-UI-SURFACE-CENSUS.md), [human expectations walkthroughs](GLIMPSE-HUMAN-EXPECTATIONS-WALKTHROUGHS.md) |
| Acceptance and next decisions | [Reference acceptance plan](GLIMPSE-REFERENCE-ACCEPTANCE-PLAN.md), [decision queue](GLIMPSE-SPEC-AUDIT-DECISION-QUEUE.md) |
| Design references and provenance | [Design directory](design/), [asset inventory](ASSET-INVENTORY.md), [publication plan](GLIMPSE-ASSET-PUBLICATION-PLAN.md) |
| Project conversation evidence | [Production diary](production-diary/2026-10-22-conversation-record.md) |

The long-form product specification was originally developed under the working title **Waltz**. Where older text disagrees with the current contracts, consult [current decisions](GLIMPSE-CURRENT-DECISIONS.md) before implementing.

## Proposed implementation

The architecture currently favors **Kotlin**, **Jetpack Compose**, **Room**, Android's **Photo Picker / Storage Access Framework**, **WallpaperManager**, **WorkManager** with appropriately qualified timing guarantees, and **Glance** for widgets. The proposed deliverable is a native sideloadable Android APK. Nothing in this repository should be described as buildable until an Android project is actually present.

Initial engineering milestones:

1. Establish Android project, image storage, import pipeline and accessible navigation.
2. Complete the Rainbow import → verified local collection → preview → home wallpaper → shuffled active-use rotation path.
3. Prototype real reel semantics and animations, compare against Grid, and test on physical hardware.
4. Add sharing, composition, diagnostics, widgets and deeper rules incrementally, with focused acceptance tests.

## Repository migration and assets

This repository was separated from `gamesbyian/general-purpose/Glimpse` on **22 October 2026**. Text specifications, vector design sketches, and source scripts were transferred directly. **Some large binary files and reference images may still reside only in the original directory**. The [asset inventory](ASSET-INVENTORY.md) records the original publication history; check the [source directory](https://github.com/gamesbyian/general-purpose/tree/main/Glimpse) before assuming a linked binary is present here. Do not delete the source folder until a complete inventory and hash comparison are verified. Some historical documents also link to issue numbers in the old repository.

## Principles

- Build the simple workflow first; make advanced controls discoverable rather than compulsory.
- Treat preview, eligibility, requested application, and verified wallpaper application as separate states.
- Design for Android permission, provider, and scheduling realities; disclose capability gaps.
- Use the reel only when it meaningfully improves selection, orientation or navigation.
- Preserve originals, provenance, reversibility, accessibility and user agency.

**License:** No license has been established in the migrated repository. A license file should be chosen explicitly before claiming an open-source grant.
