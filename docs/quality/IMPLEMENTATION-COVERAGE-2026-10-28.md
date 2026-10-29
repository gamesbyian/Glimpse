# Glimpse implementation coverage of the 73 workflow specification

**Audit date:** 2026-10-28. **Authority:** current merged Android `main` and [73-workflow contractual specification](../product/feature-audit.md), supplemented by [AGENTS.md](../../AGENTS.md), [implementation queue](../planning/implementation-queue.md) and final-head Android CI. **This is a conservative capability audit, not a code-completion claim or owner design acceptance.**

## Counting method

Each of the **73** scenarios receives one of three states. *Implemented* means the principal scenario and side-effect boundaries exist in merged native code with relevant automated evidence; *Partial* means a material usable slice exists but a required behavior/acceptance contract is missing; *Not implemented* means there is no sufficiently complete native end-to-end scenario, even if enabling infrastructure exists. Partials receive **0.5 scenario-equivalent credit** for a rough comparison; this is a declared convention, not an objective measure of engineering effort or remaining time. M3/M4 proposals may require external permissions, Android capability proofs, rights or additional owner decisions.

| Tier | Complete | Partial | Not implemented | Total | Scenario-equivalent coverage |
|---|---:|---:|---:|---:|---:|
| M1 | 4 | 4 | 0 | 8 | 75% |
| M2 | 0 | 6 | 16 | 22 | 14% |
| M3 | 0 | 3 | 39 | 42 | 4% |
| M4 | 0 | 0 | 1 | 1 | 0% |
| **All** | **4** | **13** | **56** | **73** | **14.4%** |

**Interpretation:** M1 core is substantially implemented but not production complete. The whole ambitious scope is mostly unimplemented because advanced schedule/rule, provider, per-target, widget and extensibility scenarios have not been built. This tally does **not** count several independently implemented supporting capabilities (collection rename, exact-byte import journal, ZIP safety, photo-only membership copy, diagnostic report, editor PNG export, real contact sheet) as *extra* scenarios. It does **not** count proposed mockups as working native screens.

## Workflow-by-workflow conservative crosswalk

| ID | Tier | Status | Why / missing completion gate |
|---|---|---|---|
| W01 | M1 | Partial | User-selected rainbow collection, shuffle and foreground five-minute evaluation exist; Android background five-minute scheduling does not. |
| W02 | M1 | Implemented | Real ACTION_SEND/SEND_MULTIPLE import, destination consent and exact-byte local copy. |
| W03 | M2 | Not implemented | No evidence of a complete, independently operable native workflow matching this scenario; platform/provider feasibility may be gated. |
| W04 | M1 | Implemented | Photo Picker imports verify immutable local byte copies that survive offline/source loss. |
| W05 | M2 | Not implemented | No evidence of a complete, independently operable native workflow matching this scenario; platform/provider feasibility may be gated. |
| W06 | M2 | Not implemented | No evidence of a complete, independently operable native workflow matching this scenario; platform/provider feasibility may be gated. |
| W07 | M2 | Not implemented | No evidence of a complete, independently operable native workflow matching this scenario; platform/provider feasibility may be gated. |
| W08 | M3 | Not implemented | No evidence of a complete, independently operable native workflow matching this scenario; platform/provider feasibility may be gated. |
| W09 | M3 | Not implemented | No evidence of a complete, independently operable native workflow matching this scenario; platform/provider feasibility may be gated. |
| W10 | M3 | Not implemented | No evidence of a complete, independently operable native workflow matching this scenario; platform/provider feasibility may be gated. |
| W11 | M3 | Not implemented | No evidence of a complete, independently operable native workflow matching this scenario; platform/provider feasibility may be gated. |
| W12 | M3 | Not implemented | No evidence of a complete, independently operable native workflow matching this scenario; platform/provider feasibility may be gated. |
| W13 | M3 | Not implemented | No evidence of a complete, independently operable native workflow matching this scenario; platform/provider feasibility may be gated. |
| W14 | M3 | Not implemented | No evidence of a complete, independently operable native workflow matching this scenario; platform/provider feasibility may be gated. |
| W15 | M3 | Not implemented | No evidence of a complete, independently operable native workflow matching this scenario; platform/provider feasibility may be gated. |
| W16 | M3 | Not implemented | No evidence of a complete, independently operable native workflow matching this scenario; platform/provider feasibility may be gated. |
| W17 | M3 | Not implemented | No evidence of a complete, independently operable native workflow matching this scenario; platform/provider feasibility may be gated. |
| W18 | M2 | Partial | Merged [#97](https://github.com/gamesbyian/Glimpse/pull/97) defers due automatic foreground rotation while Android Battery Saver is active without consuming the no-repeat bag or creating catch-up bursts. Native/JVM policy tests passed. Background/OS-lifecycle and physical Battery Saver acceptance remain open. |
| W19 | M3 | Not implemented | No evidence of a complete, independently operable native workflow matching this scenario; platform/provider feasibility may be gated. |
| W20 | M3 | Not implemented | No evidence of a complete, independently operable native workflow matching this scenario; platform/provider feasibility may be gated. |
| W21 | M3 | Not implemented | No evidence of a complete, independently operable native workflow matching this scenario; platform/provider feasibility may be gated. |
| W22 | M3 | Not implemented | No evidence of a complete, independently operable native workflow matching this scenario; platform/provider feasibility may be gated. |
| W23 | M3 | Not implemented | No evidence of a complete, independently operable native workflow matching this scenario; platform/provider feasibility may be gated. |
| W24 | M3 | Not implemented | No evidence of a complete, independently operable native workflow matching this scenario; platform/provider feasibility may be gated. |
| W25 | M2 | Not implemented | No evidence of a complete, independently operable native workflow matching this scenario; platform/provider feasibility may be gated. |
| W26 | M2 | Not implemented | No evidence of a complete, independently operable native workflow matching this scenario; platform/provider feasibility may be gated. |
| W27 | M4 | Not implemented | No evidence of a complete, independently operable native workflow matching this scenario; platform/provider feasibility may be gated. |
| W28 | M3 | Not implemented | No evidence of a complete, independently operable native workflow matching this scenario; platform/provider feasibility may be gated. |
| W29 | M3 | Partial | Crop/edit actions exist; arbitrary OS Material You palette override and full colour adjustments do not. |
| W30 | M1 | Implemented | Main physical reel observes Android animator scale and power-save reduced-motion preference. |
| W31 | M3 | Not implemented | No evidence of a complete, independently operable native workflow matching this scenario; platform/provider feasibility may be gated. |
| W32 | M3 | Not implemented | No evidence of a complete, independently operable native workflow matching this scenario; platform/provider feasibility may be gated. |
| W33 | M3 | Not implemented | No evidence of a complete, independently operable native workflow matching this scenario; platform/provider feasibility may be gated. |
| W34 | M3 | Not implemented | No evidence of a complete, independently operable native workflow matching this scenario; platform/provider feasibility may be gated. |
| W35 | M3 | Not implemented | No evidence of a complete, independently operable native workflow matching this scenario; platform/provider feasibility may be gated. |
| W36 | M3 | Not implemented | No evidence of a complete, independently operable native workflow matching this scenario; platform/provider feasibility may be gated. |
| W37 | M3 | Not implemented | No evidence of a complete, independently operable native workflow matching this scenario; platform/provider feasibility may be gated. |
| W38 | M3 | Not implemented | No evidence of a complete, independently operable native workflow matching this scenario; platform/provider feasibility may be gated. |
| W39 | M3 | Not implemented | No evidence of a complete, independently operable native workflow matching this scenario; platform/provider feasibility may be gated. |
| W40 | M3 | Not implemented | No evidence of a complete, independently operable native workflow matching this scenario; platform/provider feasibility may be gated. |
| W41 | M3 | Not implemented | No evidence of a complete, independently operable native workflow matching this scenario; platform/provider feasibility may be gated. |
| W42 | M3 | Not implemented | No evidence of a complete, independently operable native workflow matching this scenario; platform/provider feasibility may be gated. |
| W43 | M3 | Partial | Persistent no-repeat selection exists; adjacent perceptual similarity constraints and bounded relaxation do not. |
| W44 | M3 | Partial | Exact-byte SHA dedup exists; cross-collection 14-day recently-shown filter does not. |
| W45 | M3 | Not implemented | No evidence of a complete, independently operable native workflow matching this scenario; platform/provider feasibility may be gated. |
| W46 | M3 | Not implemented | No evidence of a complete, independently operable native workflow matching this scenario; platform/provider feasibility may be gated. |
| W47 | M3 | Not implemented | No evidence of a complete, independently operable native workflow matching this scenario; platform/provider feasibility may be gated. |
| W48 | M3 | Not implemented | No evidence of a complete, independently operable native workflow matching this scenario; platform/provider feasibility may be gated. |
| W49 | M3 | Not implemented | No evidence of a complete, independently operable native workflow matching this scenario; platform/provider feasibility may be gated. |
| W50 | M3 | Not implemented | No evidence of a complete, independently operable native workflow matching this scenario; platform/provider feasibility may be gated. |
| W51 | M2 | Partial | Reversible crop edges and quarter-turn recipes exist; destination-scoped focal positioning is not implemented. |
| W52 | M2 | Not implemented | No evidence of a complete, independently operable native workflow matching this scenario; platform/provider feasibility may be gated. |
| W53 | M3 | Not implemented | No evidence of a complete, independently operable native workflow matching this scenario; platform/provider feasibility may be gated. |
| W54 | M2 | Not implemented | No evidence of a complete, independently operable native workflow matching this scenario; platform/provider feasibility may be gated. |
| W55 | M2 | Not implemented | No evidence of a complete, independently operable native workflow matching this scenario; platform/provider feasibility may be gated. |
| W56 | M2 | Not implemented | No evidence of a complete, independently operable native workflow matching this scenario; platform/provider feasibility may be gated. |
| W57 | M3 | Not implemented | No evidence of a complete, independently operable native workflow matching this scenario; platform/provider feasibility may be gated. |
| W58 | M2 | Not implemented | No evidence of a complete, independently operable native workflow matching this scenario; platform/provider feasibility may be gated. |
| W59 | M2 | Not implemented | No evidence of a complete, independently operable native workflow matching this scenario; platform/provider feasibility may be gated. |
| W60 | M2 | Not implemented | No evidence of a complete, independently operable native workflow matching this scenario; platform/provider feasibility may be gated. |
| W61 | M2 | Not implemented | No evidence of a complete, independently operable native workflow matching this scenario; platform/provider feasibility may be gated. |
| W62 | M2 | Not implemented | No evidence of a complete, independently operable native workflow matching this scenario; platform/provider feasibility may be gated. |
| W63 | M3 | Not implemented | No evidence of a complete, independently operable native workflow matching this scenario; platform/provider feasibility may be gated. |
| W64 | M2 | Partial | Versioned, exact-byte validated photo-only ZIP export and restore exist; full settings/rules backup does not. |
| W65 | M2 | Not implemented | No evidence of a complete, independently operable native workflow matching this scenario; platform/provider feasibility may be gated. |
| W66 | M2 | Partial | Core reels, Grid and buttons have semantic non-gesture actions; an accessible advanced rule editor is absent. |
| W67 | M1 | Partial | Core 320dp/200% text scenarios have emulator coverage; full app-wide adaptive/focus acceptance remains open. |
| W68 | M1 | Partial | Reduced-motion reel handling exists; app-wide photosensitivity/flash-bound enforcement is absent. |
| W69 | M3 | Not implemented | No evidence of a complete, independently operable native workflow matching this scenario; platform/provider feasibility may be gated. |
| W70 | M2 | Partial | Import journal and wallpaper acknowledgment/uncertainty exist; full rule lifecycle/reason journal does not. |
| F071 | M1 | Partial | Android screenshot share/paste, subsequent crop/rotate, save and apply exist; one fluid intake/editor outcome flow is incomplete. |
| F072 | M2 | Partial | Standard Android Sharesheet can offer installed compatible recipients; dedicated Lens/visual-search discovery/eligibility is not implemented. |
| F073 | M1 | Implemented | Both original and edited sanitized PNG Android Sharesheet handoffs exist, with temporary URI grants and verified bytes. |

## Largest remaining material gaps

1. **True Android background cadence:** current five-minute shuffle evaluation is foreground/visible-app only. No verified process-death, Doze/OEM-safe scheduled service with persistent due-window semantics; never advertise guaranteed five-minute background delivery.
2. **User-facing design and human acceptance:** fixed bottom reel, responsive Hero/Editor/Grid and photo aperture drag exist, but the complete approved-reference visual system, physical Pixel gestures, real TalkBack, focus, 24 primary/34 support surfaces and second-reel composition remain open. Experimental [#94](https://github.com/gamesbyian/Glimpse/pull/94) / [#95](https://github.com/gamesbyian/Glimpse/pull/95) require explicit visual owner approval even if CI passes.
3. **M2 daily-driver:** favorites/filters/recents, crop focal/blur extension, reversible applied-wallpaper history, pin/hold, independent Home/Lock recipes, widget/Quick Settings, settings backup and full per-operation diagnostics.
4. **M3/M4 ambition:** scheduled condition/rule editor, nested/composite weighted collections, remote licensed media/source adapters, per-destination and display integration, simulation and external automation.
5. **Release:** durable stable release signing, install/update compatibility, device manufacturer/background validation, security/privacy and visual acceptance. Debug APK and emulator CI do not satisfy a publishable release gate.

## Evidence limits and recalculation

Use [first-slice evidence](../quality/first-slice-evidence.md) for real owner-device observations, [implementation queue](../planning/implementation-queue.md) for merged PR exact SHAs, [integration issue #81](https://github.com/gamesbyian/Glimpse/issues/81) for ongoing visual acceptance, and [accessibility issue #13](https://github.com/gamesbyian/Glimpse/issues/13) for focus/large-font/TalkBack gaps. If a feature later ships, update its row only after inspecting merged source and relevant accepted tests. Recompute the tier and total table, rather than updating percentages independently.

