# Agent C recovered historical Android evidence and direct optical review · 2026-10-24

**Evidence type:** seven *actual* Android API34 emulator screencaps + original UIAutomator XML, **not** current proposed mockup raster and **not** physical-device/owner or accessibility acceptance.

## Verifiable provenance

- Original [run #39926620822](https://github.com/gamesbyian/Glimpse/actions/runs/39926620822), artifact `glimpse-android-visual-atlas`, ID `12490081376`, previous captured APK SHA `6b19ca99c5944a4cb26a7971d0a129c00891a23d`.
- API34 `pixel_5` emulation configured to 1440×3120 pixels, density 560dpi; screenshot font scale 1.0 except labelled 2.0 captures. UI sourced from actual `MainActivity` via fixture launcher and synthetic art, not owner personal photos.
- Agent C **downloaded/extracted the actual ZIP and directly opened** a seven-plate full screenshot contact sheet. Seven source images have exact PNG SHA-256 as follows:

| Native PNG/XML stem | Actual PNG SHA-256 | Optical finding |
| --- | --- | --- |
| `03-reel-initial` | `7e986052bb8bac8d6c9f7b087a02631346fd8d2f520e62b0cca145e500ef5479` | Photo 1 of 3, synthetic hero and circular paper-wedge reel partially visible below; rest requires vertical scrolling |
| `22-reel-initial` | `8853f1e8d37de0316572f55c1aa35e9f159bdda3eb22019425ee9633ca03fbd6` | 22 items indicated, p01 hero and top paper aperture visible, not the full interaction surface on first viewport |
| `22-reel-and-hero` | `264dd577eb70f31710118750a8598089070377cb4743e78e8909a6f45ec26c15` | Actual circular/wedge photo Reel visible after scrolling; stationary selected hero above; Previous/Next and explicit Home apply below |
| `22-controls-and-rotation` | `998958019a3acee0e37c5ef82283c0c35333bf74b262b6fda688c7a33b7e1522` | Paper wheel hub cropped by its viewport; explicit apply and foreground shuffle label appear together; no proof of scheduling timing |
| `22-grid` | `2708607433a491661758c7ebfe42edf604a492408d07f8a05971875c6b446af1` | Actual 3-column photo Grid with first nine synthetic numbered images; p01 selected apparent outline; source identity matching on real data still needs dynamic tracing |
| `22-reel-text-200-percent` | `67093f4a2300b4d7cae16c939ebb71508a1e4d174557f37821dec45a09701b4b` | Enlarged text wraps preview explanation; Reel primarily below first fold; cannot assert full 200% usability |
| `22-text-200-percent-scrolled` | `94f78b4214600e4b0f586fc8e5feec53e4d6567288391c02a472ac1f7243fb94` | Next photo wraps two lines and control becomes tall; explicit apply is partly cut by viewport/nav bottom in this snapshot; must scroll to see full actionable surface |

## Independent hierarchy inspection

- Actual XML had respectively 5, 5, 4, 6, 14, 5 and 3 clickable-node entries in the table's file order. The original UIAutomator dumps report clickable parent views with blank `text` and `content-desc`, while separate child nodes contain visible labels. That is a **semantic accessibility audit trigger**, not automatic proof that TalkBack will announce an unlabeled control. Instrument the accessibility service or Compose semantics tree separately.
- At font scale 1.0, all surfaced clickable bounds in the recovered XML met **44dp raw visible-size threshold** (not a guarantee of adequate semantics or safe hit targets). At 2.0 after scrolling, one clip-visible clickable entry reports ~343×24dp, correlating with bottom-cut apply button. The underlying full control might be >44dp when scrolled fully into view; **do not call it an intrinsic 24dp button**.
- None of these frames establishes gesture settlement, interruption, 0/1/5/60 counts, native OS chooser, wallpaper readback, process-death persistence, source rights or Doze eligibility. Pictured 3 and 22 counts are genuine debug Room fixture state, not 3 or 22 separately licensed user images.

## Important correction to earlier descriptions

An earlier handoff labels this specific historical run as a “rounded rectangular pseudo-reel.” **Direct rendered pixels actually show a large partial circular paper wedge and hub.** The owner reported a rounded rectangular selector for an earlier personally installed build without retained install SHA; do not collapse that owner report onto run #39926620822. Both are valid *different* historical observations. The current proposed R01 paper-reel SVG geometry still requires independent native parity and owner acceptance.

This report is an **independently inspected fixed historical artifact**. All seven source PNGs and corresponding XML remain downloadable from the linked Actions artifact during its retention period. Retain exact source SHA and generation run; no graphics are promoted to `design/approved/`.

## Next native evidence

The Agent C opt-in `agent-c-proof.yml` exercises 0/1/3/5/22/60 source-ID cases with explicit Next/Previous commands and 320dp/200%-text captures on actual emulator `MainActivity`. Its results must be analyzed separately when CI produces complete PNG/XML; a green native compilation alone is insufficient.
