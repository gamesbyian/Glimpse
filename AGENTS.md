# Glimpse: agent and contributor guide

## Scope and truth

This repository contains **product specifications and an early Android source prototype**. Compilation, APK installation, OS behavior and visual acceptance are separate evidence gates. Never describe a mockup or source commit as an achieved handset result. Start with [current decisions](docs/planning/current-decisions.md), then follow the relevant detailed contract through [docs index](docs/README.md).

Current engineering queue: [implementation queue](docs/planning/implementation-queue.md), [issue #1](https://github.com/gamesbyian/Glimpse/issues/1), and [prediction → observation ledger](docs/quality/first-slice-evidence.md). Keep hypotheses clearly separate from observed human behavior.

## Feature priorities

First working slice: native photo import → verified local collection → choose/preview → apply wallpaper → active-use shuffle. Platform compliance, accessibility, correctness and user agency come before decorative complexity. The paper reel must perform real selection work; Grid and reduced-motion equivalents remain available. Never silently apply a wallpaper when browsing or promise exact background work through Doze.

## Change rules

- Keep specifications in `docs/` and design/research sources in `design/`.
- Update cross-references, the [asset inventory](docs/assets.md) and the decision queue when changing project structure or source-of-truth decisions.
- Don't re-import older Waltz screen decks, transfer chunks, superseded alternate layouts or archival material without a reason documented in the relevant decision record.
- The original `gamesbyian/general-purpose/Glimpse` directory is a **private preservation archive**. Don't delete or rewrite it as part of this repo's cleanup.
- Don't claim a binary/reference asset is transferred until the actual destination file has been verified; scrutinize photo licenses before public upload.
- When app code is added, prefer narrow tests tied to affected functions and real-device proof at relevant decision gates; no giant regression/screenshot suites for every small edit.
- Treat prior UI audits as specifications and hypotheses; distinguish explicit user approvals from design proposals.

## Repository

An Android/Gradle source project now exists under [app/](app/README.md); no verified release APK, device acceptance or license is present yet. This repository is public. Public visibility does not itself grant an open-source licence.
