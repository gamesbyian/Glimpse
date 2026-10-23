# Glimpse visual-reference completion and reconciliation handoff · 2026-10-23

**Pull request:** [#6 (draft)](https://github.com/gamesbyian/Glimpse/pull/6); do not promote until structural and optical review. **Work lane:** Visual Reference Completion and Reconciliation. **Branch:** [`agent/visual-state-reconciliation-20261023`](https://github.com/gamesbyian/Glimpse/tree/agent/visual-state-reconciliation-20261023). **Start main SHA:** `4a4f858871d210939679bc9a5ee69550aedda620`. Main was rechecked against the live connector and remained at that SHA before PR creation. Recheck HEAD and all active PRs before subsequent edits.

## Exact changes and evidence classes

- Retained original **66** proposal SVGs, **82** P0 connected frames X01–X12, **55** P1 advanced frames X13–X23. Existing static validation and original handoff describe these files, not optical acceptance.
- New [`core-state-evidence.v1.json`](../../design/proposals/core-state-evidence.v1.json) maps **each of the original 34 missing named core variant slots** to an exact existing or supplemental SVG path. Evidence: **34 structurally pictured-state, 0 partial-pictured, 0 related-only** after 15 dedicated same-surface drawings; full visual/flow sufficiency still requires inspection. These labels are based on inspection of SVG phone-side contents, NOT independently verified raster pixels or owner signoff.
- Added **25** targeted, individually editable SVGs in [`design/proposals/state-refinements/`](../../design/proposals/state-refinements/): screenshot import progress/fail, Automation pause/delay, editor blur extension, unsupported Quickstart, insufficient simulator, blocked Quick Settings, first-use no photos and high-contrast settings. They are additive *supplemental refinements*, never reclassified as original 66/P0/P1.
- Reflowed the **X09 200% Home text** phone layout. Increased actual pictured labels/actions and used a two-row navigation example rather than leaving ordinary 13px text under a “200%” caption. This still must be inspected for 420×900 crop, OS insets and real Android font scaling.
- X11 crop now has a pictured result inset; its Save and Use Now branches display distinct edited `p12c` art/metadata while original `p12` remains. X13 selected edited payload metadata corrected to `p12c`. The Use Now **pre-confirmation** status is unchanged, rather than implying application already started. Wallpaper application always requires an explicit command and Android outcome.
- All **eight X05** motion frames have `journeyId`, `selectedAssetId`, pose metadata and non-apply status. Seven explicit edges in the [P0 manifest](../../design/proposals/storyboard-coverage.v1.json) record p12→p13 only on snap, maintain p13 on reverse/interruption and treat last-photo jump as an explicit action, not one swipe. Poses remain drawings, not actual touch inertia or Android motion data.
- [34-state phone contact sheet](../../design/atlas/core-state-contact-sheet.html) and [six representative flow sheets](../../design/atlas/representative-flows.html) allow direct browser-rendered image comparison. The latter has **60 intentionally repeated/selected frame entries**, covering reel 0/1/3/5/22/60, eight motion poses, import/OS handoff, Apply Home/Lock, active/paused/delayed rotation, edit/share identity, font/focus/error. Neither is a completed agent-observed optical review merely because it exists.
- Added [`validate-core-state-evidence.py`](../../scripts/validate-core-state-evidence.py), [`validate-storyboard-semantics.py`](../../scripts/validate-storyboard-semantics.py), and opt-in [`render-visual-reference-sheets.py`](../../scripts/render-visual-reference-sheets.py). The first verifies all 34 exact paths, baselines, registry and phone-side visible-text SVG elements (x<420). The second verifies key X05/X06/X11/X13 identities and statuses. **Text positions may still be clipped by clip paths or font metrics, so neither proves an optical PASS.** The renderer uses actual CairoSVG/Pillow PNG rasterization to make comparable panels with no emulator CI.
- Updated `AGENTS.md`, both visual manifests, source/atlas READMEs, atlas homepage, gap register, bidirectional reference audit and implementation queue. Native Kotlin, Room, import, Photo Picker, apply, shuffle, Android workflows and resources are untouched.

## Post-handoff continuation: actual raster reconnaissance and critical fixes

A [separate source-hashed optical findings log](../design/optical-review-log-2026-10-23.md) records **69 rendered frame slots across nine comparison sheets**, generated using CairoSVG/Pillow from the original offline Actions visual reference artifact (main `74b5f23`, not the new PR branch). This first-pass optical inspection exposed and led to repaired X05 broken image paths (**24 missing images in eight scenes**); repaired X05 scenes were reproduced locally and raster reviewed before the three research images were embedded directly into the branch SVGs. It also exposed and led to a repaired false X11 90°-rotation illustration and X09 large-text Home/Reel/Import reflows; new Reel/Import and rotation layouts were rasterized locally before source reproduction. A new fast [photo-portability validator](../../scripts/validate-svg-photo-portability.py) detects future external file dependencies. The **fresh branch source and 10 new state refinements have still not been optically reviewed as a complete package**; other P1 and 15 named states are pending. [PR #6](https://github.com/gamesbyian/Glimpse/pull/6) remains deliberately draft.

## Executed verification and outstanding verification

**Executed with live GitHub file reads:** checked that all 34 `phoneEvidenceLabels` are present as x<420 SVG text elements in their exact paths, with zero missing; independently read X05 frame selection labels (p12 until snap, p13 after snap and interruption, p22 at last) and X06 boundary fixture labels (1/1, 2/3, 3/5, 12/22, 39/60, 22/22). Read X11 `p12c` source and X13 `p12c` metadata after editing. This is source-level verification only.

**Not yet executed for the current PR HEAD:** local Python validators, end-to-end fresh-branch CairoSVG contact sheets, complete pixel/text-overlap coverage, device screenshots, TalkBack, physical swipe interruption and OEM handoffs. **Executed against recovered pre-PR source:** representative CairoSVG/Pillow rasters and optical inspection detailed above, including local reproduction of corrective renders. There was no source checkout or GitHub Action directly executed from this agent environment. Do not state those tests passed or that any reference is approved. Next agent should run the following in a checkout from branch / merged main:

```sh
python3 scripts/validate-visual-spec.py
python3 scripts/validate-storyboard-coverage.py
python3 scripts/validate-core-state-evidence.py
python3 scripts/validate-storyboard-semantics.py
python3 scripts/validate-svg-photo-portability.py
python3 -m pip install cairosvg pillow    # local disposable environment only
python3 scripts/render-visual-reference-sheets.py --output /tmp/glimpse-optical
python3 scripts/render-visual-reference-sheets.py --output /tmp/glimpse-optical-full --full
```

Open the **actual PNGs at 100%**, inspect phone crops plus full annotated originals. Check cutouts, typography and selected-image identity, not only validator output. Use minimal optical fixes with source hashes. If any validator or CI fails, repair before merging; do not infer a pass from script existence.

## Second-pass dedicated states: illustration complete, optical review still required

The 15 formerly partial or wrong-surface references now have individual editable phone mockups in `design/proposals/state-refinements/`. This resolves the **surface-evidence reuse** for `collections.large`, `discover.offline`, `destinations.lock`, `destinations.unsupported`, `activity.failed`, `activity.uncertain`, `automation-editor.nested`, `source-detail.offline`, `photo-detail.excluded`, `diagnostics.failure`, `settings.permissions`, `backup-and-restore.restore`, and `error-empty-permission.{empty,offline,storage}`. A manifest declaration of **34 pictured / 0 partial / 0 related-only** means the state, distinguishing text and appropriate action are now visible in the phone region. It does **not** prove complete flow transitions, native UX, full text fitting, input/hit targets or owner acceptance. All remain explicitly draft and optical-untested.

## High priority next optical checks, not the future 60-question audit

1. Compare **entire original Home/Collections/Reel/Grid family** with X08 and critical paper-disc photographs; current source drawings use synthetic rainbow vector examples and a rounded five-photo disc. Ensure photo windows appear as apertures cut from a physical circular paper reel, rather than unrelated rectangular cards.
2. Inspect **X05/X06/X07 at native resolution** for clipping and material/rotation consistency. Five apertures on the full disc do not imply five visible positions on a cropped phone; the number of real collection items must stay exact. Test first/last disablement, p IDs, reversing, snapping and no accidental apply. Metadata asserts identity but does not prove frame-to-frame pose continuity.
3. Verify **X01** Google Photos bulk selection → cloud pending → verified local copy (22 selected vs verified count) → recovery, plus Android Picker/Sharesheet **illustrative only**, and no implicit full-album permission.
4. Check **X03/X04** explicit target Home vs Lock/Both, pending vs acknowledged vs unknown, safe recovery, active vs paused vs eligibility delayed by Android and no exact five-minute background guarantee.
5. Inspect **X09/X12** 200% labels, two-row Home bottom navigation and all focus/inset overlays: target sizes, clip, keyboard, high contrast, accessibility labels and Android-owned bar/handoff remain unverified.
6. Inspect **X11/X13/X14** distinct original p12 / cropped p12c / next p13 / outbound URI result and Home/Lock composition. A source snapshot is never overwritten or sent until explicit action.
7. Inspect and refine the 15 newly illustrated same-surface phone states without claiming “34/34 optical completion”, then prepare a *source-hashed owner-review set* without promoting any draft into `design/approved/` until explicit owner approval.


## Recovery continuation after interrupted session

The session recovery independently confirmed that current `main` still points at `4a4f858871d210939679bc9a5ee69550aedda620`. All substantive work remains committed to [draft PR #6](https://github.com/gamesbyian/Glimpse/pull/6) on `agent/visual-state-reconciliation-20261023` (do not imply it is merged). The recovered source artifact is `glimpse-visual-proposals` from run 39939557437. A direct offline ZIP scan of its **121 SVG image nodes** found **24 nonportable references**, confined to X05's eight files. All 24 were already repaired on the branch with self-contained images; full fresh-branch optical validation remains pending.

Inspection of the original critical reel raster sheet also revealed clipping/overlap in `design/proposals/critical-reel/reel-large-text.svg`: the collection subtitle exceeded the phone width and the 32px selected-counter collided with Grid. The branch now has a targeted source change (wrapped subtitle, resized title and counter), but **has not rerendered this specific correction**. See VRC-07 in the optical log. The next action is a fresh branch checkout and opt-in raster plus script checks for all 25 targeted refinements, not the 60-question UX audit.

## Authority and product safeguards

Read [`AGENTS.md`](../../AGENTS.md), [previous P0 handoff](HANDOFF-2026-10-23-VISUAL-STORYBOARDS.md), [coverage gaps](../design/proposal-mockup-coverage-gaps-2026-10-23.md), [reference audit](../design/reference-gap-audit.md), [current decisions](current-decisions.md) and [implementation queue](implementation-queue.md) before further work. Keep **originals immutable**, **browse/select distinct from apply**, Photo Picker import narrow and durable, five-window finite meaningful reel with Grid parity, no cloud library of our own and OS result status honest. Native import/apply/shuffle has real first-slice device observations: preserve it.

**Scope boundary:** no substantive 60-question UI/UX audit started. Draft reference maps, source checks, browser viewable assets and future PNG optical output are distinct from approval, actual native screens and human usability evidence.


## Latest X07 continuation checkpoint

Six X07 frames now share X05's embedded p12 image fixture; three reel frames additionally reuse the five physical photographic aperture paths. X07 Grid thumbnails other than selected p12 are still schematic, and X06 is still a distinct schematic presentation. The transient invalid definitions from the first assembly pass were replaced with fresh 90–276KB valid-fixture source commits from main originals. The portable-SVG validator now checks XML, local use/clip targets, fixture counts and JPEG decoding. The semantic validator checks p12 fixture byte identity across X05/X07. **Neither revised script nor the six SVGs has been executed/raster inspected in a fresh PR checkout, and there is no Android acceptance.** Inspect and run validators before reviewing or merging.


## Original Source Detail baseline correction

`design/proposals/plates/source-detail.svg` has been reauthored from the erroneous duplicate Discover directory into a single-source Google Photos detail page with scoped permissions, illustrative preview, rights/provenance uncertainty and explicit picker/Back actions. It remains one of the **original 66** proposal plates, not a new screen in the baseline inventory. This correction is source-inspected but not freshly raster or Android verified. Confirm the phone drawing in the optical pass alongside `state-refinements/source-detail-offline.svg`.


## Source-hashed optional optical export

`scripts/render-visual-reference-sheets.py` now has ten groups, including all 34 named phone states, all 25 new refinements, the original/offline Source Detail comparison and critical large-text cases. To bound work, inspect relevant PNGs from:

```sh
python3 scripts/render-visual-reference-sheets.py --list-sets
python3 scripts/render-visual-reference-sheets.py --set core-34-named-states --set source-detail-paired --native --output /tmp/glimpse-review
python3 scripts/render-visual-reference-sheets.py --set reel-grid-crop-share --set critical-type-comparison --native --output /tmp/glimpse-reel-review
```

Each native crop is 401×890 and the `index.json` ledger records its source file SHA-256. **Not executed on fresh branch in this session.** Do not count manifest mapping or an emitted PNG as a human optical acceptance.


## Visual-mechanism refinement checkpoint

Eight of the new state-specific phone drawings were upgraded beyond generic status cards: actual multiple collection rows, Lock crop clock-safe preview, device capability choices, grouped automation AND/OR conditions, photo exclusion membership, restore conflict radio alternatives, and distinct failed/unknown timeline histories. Source retrieval confirmed expected phone-side evidence and no simple text width overruns, but **no fresh raster or on-device check**. All 34 remain drafts; do not substitute this work for the planned 60-question audit.


## Complete baseline optical sweep and last fixes

The entire **66 original + 82 P0 + 55 P1 = 203** baseline proposal images were rendered to 203 exact-source-hashed native phone PNG crops in [successful Actions run 40023670567](https://github.com/gamesbyian/Glimpse/actions/runs/40023670567). An 18-page contact atlas containing all 203 was optically inspected, finding no completely blank reference phones. This is **full corpus presence / first-pass optical defect reconnaissance only**, NOT individual pixel-perfect signoff or the separately planned 60-question audit. See VRC-14 in the optical findings log.

Recent fixes not included in that particular 203-crop artifact: X05's final p22 used the same snowy photo as p13 and was fixed to embedded architecture `art4.jpg` with matching hero and aperture; X08 Home current status text exceeded the selected tag, and X10 delayed status exceeded a chip. Corrected all three chip scenes. Strict source assertions now require distinct p22 fixture bytes and status chip width. A source-hashed recheck was requested through the branch one-shot workflow; its exact resulting status must be verified from `docs/design/visual-one-shot-evidence.md`, not assumed.
