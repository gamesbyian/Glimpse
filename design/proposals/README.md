# Glimpse visual proposal set

**Status: internally routed draft source files, not a user-approved UI atlas.** This directory is the safe starting point for the next coordinated design pass. Do not cherry-pick multiple mutually exclusive historical comparison layouts into production.

## Where to look

| Need | Location | Authority |
| --- | --- | --- |
| Current user-facing original app screen families | [24 annotated SVG proposals](plates/) | Draft layout plus textual required/forbidden inventory; the annotations **do not** establish that all required controls have been drawn |
| Real-photo paper reel boundary states | [Eight reel proposals](critical-reel/) | Reconciled wheel semantics and stable identity examples; inspect empty, one, three, 22, last, reduced motion, large text, interrupted |
| About, Help, feedback, crash, updates, ratings, privacy, licences and other support | [34 support-state SVG proposals](support-plates/) | 16 distinct support surfaces, two or more alternatives each |
| All 73 original workflows → 24 surfaces | [Main manifest](visual-coverage.v1.json) | Machine-readable mapping, constraints and 63 remaining full-state-visual needs |
| 16 common-app extensions → 34 states | [Support manifest](support-coverage.v1.json) | S01–S16, separate from historical W/F IDs |
| Complete conflicts and missing exact artwork | [Bidirectional audit](../../docs/design/reference-gap-audit.md) | Current disposition and V0–V3 gates |
| Shared color/navigation draft | [tokens](../tokens/brand.json) | One coherent proposal palette; **not** visual approval |
| Root identity/reference photographic five-window disc | [paper reel](../reference/reel-wide-apertures.png) | User-approved direction of form and materials, **not** final phone layout |
| Alternative dual-wheel studies | [research experiments](../research/reel-layout/) | **Unapproved mutually exclusive comparisons only** |

## Connected P0 interactions added after the baseline inventory

The [**mobile storyboard gallery**](../atlas/storyboards.html) contains **82 additional frames**, in **12 P0 packages X01–X12**. Use the [frame-level manifest](storyboard-coverage.v1.json) for ordered images, workflow traceability and invariant notes. Unlike the original 66 isolated proposed screens, these drawings depict user/system transitions, photos, status and failure outcomes. Their quality and exact action completeness still require whole-set review. Do not count them as 82 new core screen families or as accepted artwork.

[Offline CI package and rasterized contact sheet](https://github.com/gamesbyian/Glimpse/actions/runs/39938085295). Visual XML and sampled pixel rendering pass; animation physics, actual platform screens, screen reader focus and owner preference cannot pass based on these mockups alone.

## Audit coverage gaps (current inventory)

The [2026-10-23 current-generation mockup gap register](../../docs/design/proposal-mockup-coverage-gaps-2026-10-23.md) distinguishes 34 missing dedicated named core state variants from 27 additional scenario/storyboard/platform and stress-visual needs. It also warns against treating schematic annotation text or the 34 support-state filenames as proof of complete interactions. **This is mockup coverage preparation, not a quality audit or user approval.**

## Explicit reconciliation of 34 core state gaps (2026-10-23)

The [machine-readable per-state evidence map](core-state-evidence.v1.json) enumerates all 34 original unpictured named core variants, links each to an exact existing or new SVG, and labels whether the phone drawing is **pictured**, **partially pictured** or **only topically related**. Current source inspection: **34 structurally pictured, 0 partial, 0 related-only**. These 34 SVGs now picture dedicated states in the phone region, but are still not raster, owner, accessibility, or Android runtime signoff. The [34-state browser contact sheet](../atlas/core-state-contact-sheet.html) renders phone regions of those exact files for optical review. Twenty-five additional [state-specific source drawings](state-refinements/) fill false-positive reuse cases; they do **not** increase the original 66, P0 82 or P1 55 inventory totals. The 200%-text Home storyboard has also been reflowed, while its neighboring large-type pages still need visual scrutiny.

Before an optical signoff, inspect the whole SVG including the right annotation, check the phone controls independently and compare neighboring transition frames. The validator `python3 scripts/validate-core-state-evidence.py` confirms exact paths, statuses and some visibly pictured phone labels only. It cannot detect clipped glyphs, aesthetic mismatch or runtime behavior.

The companion [six representative flow sheets](../atlas/representative-flows.html) groups 60 connected draft frames to judge reel boundaries/poses, Google Photos import, OS apply and rotation outcomes, editing/share payloads and accessibility/errors together. X05 frames now carry explicit selection/pose metadata and motion transitions; X11 and X13 distinguish source `p12` from edited `p12c` in visible results and metadata. Run `python3 scripts/validate-storyboard-semantics.py` for small semantic assertions. The optional `scripts/render-visual-reference-sheets.py` makes real raster sheet PNGs for **subsequent** visual inspection.

**Optical reconnaissance:** [the 2026-10-23 actual raster review log](../../docs/design/optical-review-log-2026-10-23.md) documents eight X05 broken photo scenes (repaired with embedded data URIs), a falsely unrotated X11 preview (repaired), improved X09 large-type reflow, and still-open X07 reel-fidelity differences. The `scripts/validate-svg-photo-portability.py` check prevents unbundled image links. The optical evidence comes from the prior main artifact plus individually inspected local patches; the full updated PR remains optically **unverified**.

## Initial review set

These images should be judged **together** as the earliest coherent proposed family:

- [Home](plates/home.svg), [Collections](plates/collections.svg), [Reel](plates/reel.svg), [Grid](plates/grid.svg)
- [Reel with 22 photographs](critical-reel/reel-twenty-two.svg) and [three photographs](critical-reel/reel-three.svg)
- [Last real photograph](critical-reel/reel-last.svg), [empty collection](critical-reel/reel-empty.svg), [reduced motion](critical-reel/reel-reduced-motion.svg)
- [Quick import](plates/quick-import.svg), [Share](plates/share.svg), [Automation](plates/automation.svg)
- [About (sideload)](support-plates/about--sideload.svg), [Help](support-plates/help--topics.svg), [Crash recovery](support-plates/crash-recovery--suspected-crash.svg), [Diagnostic preview](support-plates/diagnostic-preview--minimal.svg), [Rate unavailable](support-plates/rate-review--not-available.svg)

All images currently use the same paper/teal design proposal, system sans typography and four-tab navigation convention. The large disc has five **physical apertures**, but the 22-photo state shows **22** as the collection total. Numbers in the critical reel wedges are synthetic fixture labels, not data from a real user's library.

## What the proposals do not prove

- A full 420×900 SVG is an **illustration**, not a calibrated Android dp screenshot.
- The screen-family SVGs are *annotated design sketches*, not final complete high-fidelity pixel references; missing or hypothetical controls are itemized beside the phone drawing.
- The dramatic rim, photo crops, hub, and material realism still need owner-directed refinement and physical-device visual acceptance.
- Accessibility at 200% scale and swipe interruption require native, instrumented verification. A reduced-motion/large-text illustration only communicates intent.
- No user asset, unverified personal photo, user-report log or private screenshot is incorporated into this public repo.

## Review and promotion workflow

1. Choose a feature, inspect the contract and its proposed image **plus neighboring screens**.
2. Check task consequences, previous/next, accessibility and all materially different empty/failure/unsupported states.
3. Adjust the editable SVG and update any affected manifest/contract together. Don't leave old screenshots in the root reference directory as competing authority.
4. Run `python3 scripts/validate-visual-spec.py`, then **inspect the actual graphics**, preferably via the target Android screenshot workflow once functional.
5. When the owner explicitly approves a whole compatible set, freeze exact source hashes in a new `design/approved/` manifest. **Do not infer approval merely from these files existing.**

Source-backed proposals are versioned; optional screenshot/atlas artifacts live in Actions and do not replace source or a separate runtime evidence ledger.


**X07 identity repair (draft source):** The Reel→Grid→Detail→Reel journey now shares p12's embedded public photograph across selected hero, Grid tile, detail and return frames. The three X07 reel plates reuse X05's actual five-aperture disc drawing; selected p12 is no longer depicted as a different synthetic stripe artwork on each surface. The other Grid tiles and X06 remain schematic. See the optical log and source-level validator; no fresh-branch raster or device signoff yet.
