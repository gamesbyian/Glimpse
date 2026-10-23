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


## Latest post-stall recovery

Verified the earlier interrupted semantic-regression write was committed. P1 X13 edited-share p12c now uses clipped 150% path scaling and no longer presents the identical bitmap composition as original p12; the source p12 remains unmodified. Reconstructed and visually inspected the patched local phone crop. New assertion guards distinct edited geometry. One-shot CI rerun requested on the visual PR branch to check 34 semantic/source assertions and the 203 full-corpus phone-image exports. Do not infer a PASS from the workflow dispatch until its evidence ledger reflects the source commit.


## Exact post-atlas verification checkpoint

The five source assets modified after the successful 203-frame optical artifact are now enumerated with **exact current GitHub blob SHAs** in [POST-ATLAS-VERIFICATION-2026-10-23.md](../design/POST-ATLAS-VERIFICATION-2026-10-23.md). P0 p22 source identity, three badge widths and P1 p12c's actual crop differ from the first atlas. Source references and regression assertions were checked; the latest full-branch CI artifact is still required for independent image confirmation. The [one-shot CI workflow](../../.github/workflows/glimpse-reference-one-shot.yml) has been opt-in retriggered; do not infer a run succeeded until its proof ledger advances.


## Audit-cycle A initiated and fully recorded

The owner authorized the sequence **prepare all evidence → audit → use findings for spec and visual proposal revisions**. A reproducible [60-question weighted audit instrument](../design/glimpse-ux-audit-60.v1.json) and [first evidence-constrained 60-question scored ledger](../design/glimpse-ux-audit-cycle-a.v1.json) are committed. The [audit report](../design/GLIMPSE-AUDIT-CYCLE-A-REPORT.md) records 18 source-level passes, 8 issues and 34 questions unverified, with assessed weighted coverage 43.1%; this is not an Android/human usability PASS. [Proposed spec amendment A1](AUDIT-CANDIDATE-A1-REEL-AND-STATE-TRUTH.md) and [eight-lane mockup revision manifest](../../design/proposals/audit-driven-revision-plan.v1.json) specify a total of **56 targeted scenario outputs**, not yet rendered. Preserve the old proposal corpus and draft PR approval state. Reassess unsupported questions after real source-matched proof, then iterate spec → source-editable SVG proposals → raster/native comparison.


## R01 audit-driven six-state reel source checkpoint

Six [R01 finite reel proposals](../../design/proposals/audit-revisions/README.md) were committed, covering settled 22, 18-degree drag (selection remains p12), settled p13, finite-last p22, empty and one-photo. They are **semantic circular window studies** and do not supplant photographic X05 paper reel, nor satisfy native motion and device acceptance. The selected aperture is aligned beneath the fixed top pointer. Ordered clockwise, slots are `[s,s+1,s+2,s-2,s-1]`, invalid photos become empty; source evidence for idle s=12 shows `[12,13,14,10,11]`. [R01–R08 manifest](../../design/proposals/audit-driven-revision-plan.v1.json) and source-hashed renderer now include the six assets.

**CI caveat:** first R01 proof [run 40032396530](https://github.com/gamesbyian/Glimpse/actions/runs/40032396530) failed before raster because the *new validation script* had an over-escaped regular expression, initially finding no photo labels. The SVG source itself contains the correct labels. Fixed in `scripts/validate-audit-cycle.py` and triggered fresh opt-in Actions via workflow commit `9c75f743e25949c699e42a9cf1a4eddc3fbc5974`. **Do not claim current R01 raster/CI success until the proof file advances and all six new 401×890 PNG crops have been inspected.**


## R02 exact identity chain and retry checkpoint

[Eight-step R02 source-chain manifest](../../design/proposals/audit-revisions/r02-identity-chain.v1.json) now connects the existing X07 Reel/Grid/Detail round-trip with X11 source/crop and X13 original-versus-edited sharing. The [continuity acceptance packet](../../design/proposals/audit-revisions/R02-CONTINUITY-ACCEPTANCE.md) identifies the still-undepicted **explicit Save edited copy p12→p12c transition**. The validator checks each source's own embedded metadata, so an inconsistent manifest cannot create false evidence. No native sequence or OS delivery proof claimed. The R01 regex test was fixed, but first R01 one-shot run 40032396530 remains recorded as failed; a fresh consolidated workflow trigger `3ed59609a23f247d1c30ce30015972ca3e983268` requests corrected R01/R02 validation. Until source-sha evidence advances, leave independent raster status pending.


## R02 source-editable save boundary proposals

The previously absent crop/save handoff now has two [R02 candidate phone illustrations](../../design/proposals/audit-revisions/): `r02-crop-pending.svg` (source p12 selected, no p12c exists, save not acknowledged) and `r02-copy-saved.svg` (saved derived p12c, original p12 intact, last confirmed wallpaper p04 unchanged). Both are vector **design hypotheses**, not real photo/Android tests. [R02 identity chain](../../design/proposals/audit-revisions/r02-identity-chain.v1.json) and the [eight-step acceptance packet](../../design/proposals/audit-revisions/R02-CONTINUITY-ACCEPTANCE.md) remain authoritative evidence routes. The audit validator asserts source/derivative IDs and explicit save acknowledgement. The renderer has a dedicated `audit-r02-copy-transition` source-hashed group; no current native crop or owner approval is claimed. The 60-question audit baseline scores remain unchanged until valid new evidence is reviewed.


## Source-sha optical review and R02 Share/Return continuation

A successful independent [CI render 40033741630](https://github.com/gamesbyian/Glimpse/actions/runs/40033741630) produced all 203 baseline SVG PNGs and targeted 2 R03 + 6 R01 + 2 R02 native-sized candidates. The artifact was obtained and its R01, R02 and R03 contact-sheet PNGs directly inspected. Detailed observed defects are in [AUDIT-CANDIDATE-OPTICAL-REVIEW-2026-10-23.md](../design/AUDIT-CANDIDATE-OPTICAL-REVIEW-2026-10-23.md). **Most important:** six R01 geometry diagrams are not sufficient visual substitutes for X05: photo windows too small and individual IDs repeat identical rainbow illustrations, so source ID checks cannot show actual image continuity. R02 crop-before/save-after visibly differ but likewise use placeholder vectors; R03 visually distinguishes preview and unknown outcome but still lacks native OS proof.

New R02 source drawings `r02-share-choice.svg` and `r02-return-dismiss.svg` extend the before/after pair with distinct choice and return labels, preserve source p12, edited p12c, applied p04, and explicitly assert `shareRecipientConfirmed=false`. The R02 renderer now includes four source screens and the audit validator checks share-result honesty. Opt-in proof for this extension requested by workflow commit `6219d10e635913f03da0bb8ea33d2a7f456dfa84`; **do not mark the added two raster-reviewed until proof ledger advances**. Audit scored baseline remains 18/8/34. PR #6 stays draft/unmerged.


## Photographic audit-cycle revision: latest source state

The owner-directed audit-design loop now includes six photographic R01 paper-wheel candidates, not just six earlier round-chip *geometry studies*. [R01 photographic continuity and X05 comparison](../../design/proposals/audit-revisions/R01-PHOTOGRAPHIC-CONTINUITY.md) links all six exact sources: rest p12 art1, drag p12 art1, snap p13 art4, last p22 art6, no-photo paper reel, and single p01 art1. The six distinct public JPEG sources are from `design/research/reel-layout/fixtures/source/art1.jpg`…`art6.jpg`; neighbouring IDs have distinct image bytes. The generator is deterministic, and the zero/one scenes were optimized to embed **0 and 1 JPEG respectively**, with corresponding truthful source metadata. Original 66/82/55 baseline is unchanged.

Four R02 edit/share/return proposals now use the **exact X07 p12 JPEG pixels**, not same-looking vector rainbow substitutes. The cropped source becomes separately saved p12c only when Save is acknowledged; share choice contains the original and a centered clipped derivative, with no recipient-success claim. The [source validator](../../scripts/validate-audit-cycle.py) guards JPEG equality, source/derivative state, and the corrected thumbnail crop transform.

New independent review tools: [mobile 12-pair phone-crop atlas](../../design/atlas/audit-visual-comparisons.html), [source-link validator](../../scripts/validate-audit-visual-comparisons.py), [native dataset proof matrix](../design/R01-R02-NATIVE-EVIDENCE-MATRIX.md), and [optical defect dispositions](../design/AUDIT-CANDIDATE-OPTICAL-REVIEW-2026-10-23.md). Latest six/photo candidate source artifacts have been confirmed in GitHub; **current-source independent CI raster review and actual Android behaviour remain separate and cannot be inferred from this checkpoint**. Audit results still read 18 source-level PASS / 8 ISSUE / 34 UNVERIFIED; PR #6 stays draft and unmerged.


## 40041023803 independent optical inspection and ghost-index fix

The [full-source one-shot run 40041023803](https://github.com/gamesbyian/Glimpse/actions/runs/40041023803) passed at commit `9eab98d1916b93f26bd1d999ff80e227e79d6fdd`, including the 203 baseline, 6 photo-backed R01 and 4 source-photo R02 native phone crops. The two new targeted rendered contact sheets were **actually opened and optically inspected**. See [detailed latest optical review](../design/AUDIT-CANDIDATE-OPTICAL-REVIEW-2026-10-23.md). The paper-wedge images materially improve the previously rejected small round chips, and R02 is now photographic, but the R01 empty/single scenes still had ghost p10..p14 number badges despite correct image absence. Corrected the sources to 0 empty numeric labels / only 01 singleton, and added a strict generator guard. The previous full run predates this fix; independently rerender the two changed scenes before marking their optical issue closed. No native or owner approval has been granted. [Twelve-scene audit viewer](../../design/atlas/audit-visual-comparisons.html) and [review matrix](../design/R01-R02-NATIVE-EVIDENCE-MATRIX.md) remain the handoff tools. Retain the original 60-question audit evidence counts pending Android and human proof.
