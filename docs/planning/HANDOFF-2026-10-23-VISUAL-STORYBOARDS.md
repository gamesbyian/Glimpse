# Glimpse visual-storyboard handoff — 2026-10-23

**Handoff prepared from live GitHub `main` at `74b5f23f7d4d9abfa0c92e555f7463a061b769ee`.** Work in progress may advance independently: reread HEAD, issues, workflow results and manifests before editing.

## Verified repository state at handoff

- **66 original current-generation SVG proposals:** 24 core, 8 reel special states, 34 support alternatives. **Never conflate these with newly authored storyboards.**
- **82 P0 connected storyboard frames across all twelve X01–X12 packages** in `design/proposals/storyboards/x??/*.svg`. X02 (6), X07 (7), X08 (6), X06 (7), X09 (8), X10 (7), X11 (7), and X12 (5) were added in this workstream; X01 (7), X03 (7), X04 (7), and X05 (8) were already committed by another agent.
- **55 additional P1 advanced-storyboard frames** already present in `design/proposals/advanced-storyboards/x??/*.svg` from a concurrent workstream. Do not recreate, overwrite or infer their visual acceptance.
- The [P0 storyboard manifest](../../design/proposals/storyboard-coverage.v1.json) declares 12 packages / 82 frames, preserving exact artwork paths and semantic steps.
- [Coverage gap register](../design/proposal-mockup-coverage-gaps-2026-10-23.md) documents the **34 originally undeveloped states**, the 27 work packages, and the prohibition against treating annotated requirement lists as pictured controls.
- [Visual atlas](../../design/atlas/README.md) includes a storyboard viewer and offline packaging. [Source validation](../../scripts/validate-storyboard-coverage.py) checks file registries and SVG structure.
- Latest observed successful visual-reference CI: [GitHub Actions run 39939557437](https://github.com/gamesbyian/Glimpse/actions/runs/39939557437). An earlier failed run, 39939521202, was followed by the success; do not present a structural pass as optical/raster/user approval.

## What these proposals establish, and what they do not

The new P0 frames show connected state/action/outcome narratives for Google Photos import, collection creation and deletion, application to wallpaper, rotation, reel gestures and finite endpoints, Reel↔Grid↔detail identity continuity, compact layouts, large text, import/OS failures, screenshot editing, and touch/keyboard/focus boundaries.

**They are provisional, schematic, and sometimes use stylized vector photo placeholders.** In particular:
- They do **not** prove every frame is pixel-exact against the original identity art or that five photo windows behave realistically under animation.
- A static drawn OS Photo Picker or Sharesheet is **illustrative**; its actual appearance belongs to Android, version, selected provider and device.
- Touch-target sizes, inset labels and TalkBack focus sequences illustrated as metadata are **requirements**, not verified native accessibility behavior.
- Original photo IDs, Home/Lock targets, preview vs applied state and no-automatic-share rules must be checked across each connected frame. Beware generic `p12` placeholders being reused where the next frame should visibly use a distinct image.
- Draft references require **whole-family review** and owner approval before promotion to `design/approved/`; their presence in source or passing XML tests cannot approve them.
- The app itself has a previously tested working import/apply/rotation pipeline; no broad redesign of these backend paths is authorized by this visual storyboard work.

## Highest-value next tasks

1. **Audit source and actual rendered pixels**: retrieve/package the 82 P0 and 55 P1 image sets, rasterize representative frames, inspect typography clipping, navigation bars, portrait crop, photo identity and text/geometry consistency. Prefer small image montages (0/1/3/5/22/60 reel endpoints, import and rotation branches, 200% text) and reproducible scripts over unbounded runner campaigns.
2. **Complete explicit state-to-asset mappings:** verify which of the 34 declared core states are satisfied by a new storyboard frame versus a standalone variant. Store exact paths in a machine-readable manifest; never equate an explanatory annotation to a depicted interactive control, and do not add 34 redundant pictures when a frame already clearly demonstrates the state.
3. **Strengthen storyboards as interaction evidence:** check each X package's ordering, action consequences, stable selected photo IDs, affirmative/reversible destructive behavior, error recovery, read-only simulation, no incidental wallpaper modification and Android-owned UI handoffs.
4. **Cross-compare coherent art direction** with the original warm tactile paper / large-aperture round reel reference. Flag and correct differences in tab labels, typography, colors, hub/edge geometry, photo framing and available actions. Preserve intentional Android OEM variations.
5. **Fix visual gaps rather than duplicating work:** especially genuine image-before/after in crop/edit/share, physically credible moving reel intermediate positions and return-to-same-photo behavior, compact/large-text states with genuine no-overlap geometry, redacted reports and Home/Lock confirmation variants.
6. **Keep native visual inspection independent**: the source screenshot workflow produces actual Compose/emulator evidence and is not an approved design atlas. Continue physical-device verification for touch, TalkBack, accessibility and OS scheduling. Reuse existing first-owner observations.
7. **Reconcile documentation, manifest counts, workflow status and AGENTS.md** at the end. Lean CI: validate affected slices, not permanent high-cost emulator/image campaigns. Use focused branches or safe main commits according to repo policy; avoid conflicting concurrent owner lanes.

## Completion/acceptance gates

- **Structural coverage:** every promised frame exists and XML parses, manifest exact-path correspondence, 66 original + 82 P0 + 55 P1 classes counted separately.
- **Optical audit:** visual image contents are opened/inspected; state transitions visually obvious and art direction coherently maintained, not only described beside the phone.
- **Owner review:** explicitly approves a specific compatible source-hashed set after observing full family; *not achieved here*.
- **Runtime verification:** actual native capture comparison and physical handset action/accessibility tests; *separate from source proposals*.

## Reference entry points

- [Coverage gaps](../design/proposal-mockup-coverage-gaps-2026-10-23.md)
- [Unified proposal index](../../design/proposals/README.md)
- [Storyboard manifest](../../design/proposals/storyboard-coverage.v1.json)
- [Visual atlas](../../design/atlas/README.md)
- [Original spec-to-image audit](../design/reference-gap-audit.md)
- [Current implementation queue](implementation-queue.md)
- [Android owner observations](../quality/first-slice-evidence.md)
