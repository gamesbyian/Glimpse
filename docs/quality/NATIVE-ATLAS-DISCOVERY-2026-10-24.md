# Native Android evidence discovery · 2026-10-24

**Finding:** The repository contains **actual emulator screenshot/hierarchy capture evidence** from a completed earlier Android run. Do **not** erase it from the evidence inventory by repeating the older “no native screenshots exist” shorthand. Equally, do **not** treat these captures as acceptance of the current photographic wheel, which has not been implemented in that build.

## Exact provenance and available captures

- Workflow: [Glimpse native visual atlas #39926620822](https://github.com/gamesbyian/Glimpse/actions/runs/39926620822).
- Android implementation checkout: `main` commit `6b19ca99c5944a4cb26a7971d0a129c00891a23d`, **NOT** the current draft PR #6 visual-source SHA.
- Emulator: GitHub Actions `reactivecircus/android-emulator-runner@v2`, **Android API 34**, `pixel_5` profile, `x86_64`, SwiftShader GPU, animations disabled in the runner, 1440×3120 screenshots logged.
- Run and `capture` job conclusion: **success**, including debug APK compilation, emulator install and `scripts/capture-android-atlas.py --mode full`.
- Artifact: **`glimpse-android-visual-atlas`**, ID **`12490081376`**, ~9.97 MB compressed, uploaded 2026-10-23 08:46 UTC, retention 14 days from workflow. Link: [the Actions run artifacts section](https://github.com/gamesbyian/Glimpse/actions/runs/39926620822).
- The **runner log actually printed**:
  
  | Capture label | Logged screenshot dimensions | Reported clickable nodes |
  | --- | ---: | ---: |
  | `22-reel-initial` | 1440×3120 | 5 |
  | `22-reel-and-hero` | 1440×3120 | 4 |
  | `22-controls-and-rotation` | 1440×3120 | 6 |
  | `03-reel-initial` | 1440×3120 | 5 |
  | `22-grid` | 1440×3120 | 14 |
  | `22-reel-text-200-percent` | 1440×3120 | 5 |
  | `22-text-200-percent-scrolled` | 1440×3120 | 3 |

These logs establish that **seven native screenshot capture commands completed**, and that accessibility hierarchy clickable-node counts were reported by the collector. They do **not** establish the photographed-wheel state, UI correctness, 48dp hit boxes, image content, TalkBack spoken focus order, or 200% font usability.

## Important limitations and anomalies

1. This is the **original native Android first-slice**, whose user reported the incorrect rounded-rectangle pseudo-reel and limited five-slot traversal. The later source-only R01 photographic wheel is **not running native code in this artifact**. Do not merge source mockup pixels and emulator pixels into a single “compliant” capture.
2. The emulator log contains warnings/errors on startup: one transient ADB daemon connection failure, `Failed to find ColorBuffer` and `Failed to bind to texture`; the process subsequently logged each screenshot and exited with success. **Inspect actual captured PNG content** for black/empty/corrupted frames instead of inferring completeness from command return codes.
3. At most seven state captures were logged. This is **not** the requested 0/1/3/5/22/60 photographic motion matrix, not actual 320dp/640dp insets, not a real owner's Pixel screenshot, not 200% font optical acceptance, and not gesture/timing/OS handoff proof.
4. The artifact is time-limited. Preserve native visual evidence as a separately labelled historical baseline if it is needed for comparison; preserve source SHA, build ID, API, resolution, semantics XML and state seed. Avoid publishing user photos or personally identifying material. The debug fixture labels suggest synthetic research data but must be independently confirmed before copying any image into the public repository.
5. **No artifact PNGs or XML were downloaded or visually inspected in the 2026-10-24 continuation.** This document is a log/artifact inventory inspection, **not** a retrospective optical pass.

## Next practical actions

Download artifact **12490081376**, inspect all seven 1440×3120 native screenshots and corresponding hierarchy XML; verify render integrity, selected photo ID, 3-vs-22 navigation, current reel geometry and actual icon/button focus bounds. Compare against source-hashed 401×890 photographic R01/Glimpse candidate drawings with appropriately documented device scaling/insets, not naive pixel equality of dissimilar reference/native viewports. Record distinct findings:

- **Proven native behavior** from actual screenshot/tree/interaction command.
- **Observed mismatch** between screenshot/hierarchy and an agreed proposal.
- **Unknown** because a frozen screenshot cannot prove scrolling, timing, taps or delivery.

Do not re-score [cycle A](../design/glimpse-ux-audit-cycle-a.v1.json) from an artifact's mere existence. Some questions may become answerable **after inspecting** the native images/XML, but the draft audit baseline remains **18 source-level PASS / 8 ISSUE / 34 UNVERIFIED** until then.

This discovery should replace blanket claims of “no emulator screenshots exist,” while retaining the separate current-design native-conformity gap.
