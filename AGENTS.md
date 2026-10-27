# Glimpse agent guide

> **26 October 2026 build authority:** The owner endorsed the physical cream paper-reel/editorial-photography direction (not every illustrated control). Follow [native build plan](docs/docs/planning/BUILD-PLAN-2026-10-26.md) and [tactile material constraints](docs/docs/design/TACTILE-REEL-MATERIAL-2026-10-26.md) before starting new artwork or Android UI. The separate [native reel PR #27](https://github.com/gamesbyian/Glimpse/pull/27) implements provisional actual disc motion + a baked five-aperture raster face, not final owner-approved art or device-proven behavior. The first real emulator screenshot revealed a flat/full-width presentation, prompting shared lower-corner geometry correction. Keep the [master visual-review PR #6](https://github.com/gamesbyian/Glimpse/pull/6) draft/unmerged. Do not invent settings/provider/categorical reels from illustrative boards.


Glimpse is a **native Android wallpaper manager** with a tactile paper-reel interaction. The repository has an implemented *first-slice prototype* and a GitHub Actions-built debug APK; installed-device behavior, OS reliability, human usability, and a signed release are **not yet accepted**. The source, mockups, a successful JVM/Gradle job and a working handset are four different evidence classes.

All draft visual references can be browsed through the [66-image mobile atlas](design/atlas/README.md); the [source-validated packaging workflow](https://github.com/gamesbyian/Glimpse/actions/runs/39922564218) supplies a self-contained offline review artifact. The current **circular photo reel is provisional code**; [native CI](https://github.com/gamesbyian/Glimpse/actions/runs/39924084187) does not imply geometry, motion or owner approval. First native screenshot evidence is still unresolved; inspect the GHA emulator logs before claiming visual acceptance.

The [P0 storyboard gallery](design/atlas/storyboards.html) and [source-traced manifest](design/proposals/storyboard-coverage.v1.json) now show **82 additional frames across X01–X12**. Distinguish the 66 original boards, 82 supplementary P0 frames, the unillustrated/missing dedicated 34 variant slots, and native emulator captures in every completion claim. P0 art is rendered structurally, **not owner-approved or full UX-audited**. Do not reimplement X01–X12 blindly. Audit individual requirements and transition quality, then fill P1 gaps X13–X23.

Visual design work must begin with [the two-way visual gap audit](docs/design/reference-gap-audit.md), [the unified draft reference family](design/proposals/README.md) and [the everyday support specification](docs/product/everyday-app-experience.md). **Proposed SVGs are not user-approved full-fidelity reference images.** The static validator checks structure and traceability, not appearance, transitions or approval. Keep one coherent whole-set vocabulary; no cherry-picking obsolete dual-reel proposals.

## Start and choose work

1. Start from current `main`; inspect [the live implementation queue](docs/planning/implementation-queue.md), [issue #1](https://github.com/gamesbyian/Glimpse/issues/1), active PRs and the [first-slice evidence ledger](docs/quality/first-slice-evidence.md). Pick the first incomplete dependency that can be verified, not the most visually interesting historical mockup.
2. Read [current decisions](docs/planning/current-decisions.md) for approved product direction and supersession. It contains older planning-only language; its dated **native development status update** and the live queue override that historical no-code instruction. Use [the documentation index](docs/README.md) to locate a specialist contract.
3. Read only the relevant source and specialist documents before editing. A dated historical Waltz proposal, alternate reel deck or research essay is not approval to implement it.

| Task | Primary authority |
|---|---|
| Android build, wiring, tests, package | [app source guide](app/README.md), [Android architecture](docs/architecture/android.md), current Gradle/workflow files |
| Collection/import, local files, Photo Picker, dedup and crash recovery | [technical behavior contracts](docs/architecture/behavior-contracts.md), [image import/sharing](docs/product/image-import-sharing.md), source data/repository tests |
| Wallpaper apply, foreground shuffle, scheduling and OS limitations | [technical behavior contracts](docs/architecture/behavior-contracts.md), implementation queue, source gateway and Android API documentation |
| Reel/Grid semantics, gesture, motion, focus | [reel interaction](docs/design/reel-interaction.md), [reel UX audit](docs/design/reel-ux-audit.md), [motion contract](docs/architecture/reel-motion.md) |
| UI completeness, new screen and controls | [screen inventory](docs/design/ui-surface-inventory.md), [screen contract](docs/design/screen-contract.json), [user workflows](docs/product/workflows.md) |
| Human experience, ergonomics and accessibility | [human-expectations walkthroughs](docs/design/human-expectations.md), [first-slice evidence](docs/quality/first-slice-evidence.md), actual source/device evidence |
| Visual reference, asset decision, licensing | [reference acceptance](docs/quality/reference-acceptance.md), [asset inventory](docs/assets.md), [editable sources](design/README.md) |
| Large scope or conflicting proposed feature | [current decisions](docs/planning/current-decisions.md), [decision queue](docs/planning/decision-queue.md), [feature audit](docs/product/feature-audit.md) |

## Product invariants

- Primary journey: import a photo via Android-supported picker/provider → make a verified local snapshot → browse/select with **Reel or Grid** → preview → **explicitly apply** a wallpaper → choose active-use rotation. Browsing, scrolling or selecting never silently applies wallpaper.
- Glimpse's identity is the **meaningful finite paper reel**: large photographic windows, a partial off-screen reel, a stable hero preview, real position/count and usable deliberate selection. Ordinary Grid/buttons, large text, reduced motion and TalkBack must offer equivalent navigation. Do not bolt reels onto settings where they make the task harder.
- User photographs remain the user's files. The app has no proprietary cloud, social network or mandatory account. Keep import permissions as narrow as possible; an imported private copy must be durable, provenance-aware and reversible. Never assume broad Google Photos album access or uninterrupted background sync simply because the source is called Google Photos.
- The local collection, applied wallpaper and scheduling intent are separate states. Serialize competing apply operations through one command gateway; distinguish **requested**, **pending**, **confirmed** and **unknown/failed** OS outcomes. Retry safely without duplicate destructive mutations.
- A desired five-minute shuffle interval is not a guarantee of exact five-minute Android background execution: Doze, OEM limits, process death and battery policies govern actual results. Label foreground-only behavior honestly until scheduled background capability is exercised on real devices.
- Import, crop, export and sharing must preserve originals and use the Android-provided Sharesheet/URI/FileProvider model. Avoid leaking private image paths or recording user photos in logs, tests, artifacts or issues.

## Engineering and agent workflow

1. **Discover before duplicating.** Reuse the current Kotlin/Compose, Room, media import, wallpaper-command and reel-state boundaries. Prefer one source of truth for selection, active-set eligibility, import file integrity and apply acknowledgement across app screens, widgets and future Quick Settings. Don't create another scheduler, photo database or wallpaper setter inside a new feature.
2. **Work for the player's actual goal.** For a UI proposal, state what the person expects to happen when touching the control, what they can see, and how they recover from a mistake. Inspect focus order, accessibility labels, 44dp/48dp-class touch comfort, large fonts, reduced motion, latency and one-hand reach as appropriate. Emulator scripts and agent guesses are **not** observed human preferences.
3. **Use evidence proportional to risk.** Pure collection/shuffle logic needs deterministic JVM/unit tests and relevant build tasks. OS integration needs instrumented/provider scenarios and eventual physical Android confirmation. Visual changes need actual on-screen judgement. Preserve exact app/Gradle commands and results in the PR; the [source guide](app/README.md) owns executable build instructions.
4. **Avoid false acceptance.** A green GitHub Actions compile proves source can be built in that environment. It does not prove Photo Picker providers, backups, Doze timing, lock-screen targeting, device cutouts, rendering motion, accessibility or actual human comprehension.
5. **Coordinate and finish.** Inspect active PRs and other agents' source ownership before editing shared Compose/navigation/Gradle files. Prefer focused branches and PRs; update the implementation queue only for shipped state, the evidence ledger only for actual observations, and current decisions only for genuinely changed decisions. End with exact tests, artifacts/SHAs, limitations, pending device checks and one clear next action. Avoid redundant documentation recaps and broad screenshot CI.
6. **Keep development clean.** No unrelated dependency churn or mockup restyling during OS-behavior work. Review third-party assets, copyright/licensing and external SDK permissions before making public changes. Do not publish user photos or private data.

## Archive and precedence

The private `gamesbyian/general-purpose/Glimpse` directory is a preservation archive, not the active implementation. Do not move, delete or overwrite it during ordinary work. Older Waltz decks, transfer chunks and superseded mockups remain historical evidence. The active repository is public; public visibility does not itself provide an open-source license.

When sources conflict: **current observed code and device evidence** determine what works; **current decisions** determine intended UX; the **implementation queue** determines what is next; the **evidence ledger** determines which acceptance gates have actually been met. Report the conflict rather than quietly promoting a proposal into an accepted feature.
