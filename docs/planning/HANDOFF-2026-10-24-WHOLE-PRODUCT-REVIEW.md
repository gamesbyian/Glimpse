# Glimpse whole-product visual review reconciliation · 2026-10-24

**Working branch:** `agent/visual-state-reconciliation-20261023`  
**PR:** [#6 · DRAFT · do not merge without owner approval](https://github.com/gamesbyian/Glimpse/pull/6).  
**Supersedes operationally:** [the earlier same-day 264-source checkpoint](HANDOFF-2026-10-24-VISUAL-PACKET-CONTINUATION.md). Keep that document as chronological evidence, not a competing live status.

## Verified repository evidence at this checkpoint

| Scope | Count | Evidence class |
| --- | ---: | --- |
| Original baseline (66 core/reel/support + 82 P0 + 55 P1) | **203** | Historical source images, not 203 final approved screens |
| Supplemental states | **25** | Additional drafts (34 formerly missing named states remain pictured) |
| Audit-driven R01–R08 studies | **43** | Photographic wheel, edit/import/OS, access and recovery choices |
| **All unique source-editable SVG references** | **271** | Exact source-hashed draft inventory in [full atlas](../../design/atlas/whole-set-evidence-atlas.html) |
| R01–R08 named illustration scenarios | **56 / 56** | **PICTURED structurally** (0 partial, 0 without a reference); not 56 native/optical PASS |
| Human review *selection candidate* | **24 core + 34 support variants + 23 connected journeys** | [Curated offline browser](../../design/atlas/whole-product-review-selection.html) and [selection registry](../../design/proposals/whole-product-review-selection.v1.json) |
| Full storyboard journey references | **137 frames** | 82 P0 + 55 P1; no runtime transitions implied |
| Preview snapshot of selected source identities | **253 distinct source paths** | [SHA-256 ledger](../../design/atlas/whole-product-review-snapshot.v1.json), owner-approved false |
| Separately documented 60-question audit | **18 source PASS / 8 ISSUE / 34 UNVERIFIED** | [Cycle-A ledger](../design/glimpse-ux-audit-cycle-a.v1.json); **unchanged** |

**Visual approval is still absent** for every source. The new review selection is deliberately an *editorial candidate*, not an `approved/` freeze or a statement that old schematic plates are sufficiently modern. It includes alternate direct links for comparison on high-risk surfaces and eight explicit human product decisions.

## Material results from this continuation

1. **Real photographed tap and 36° paper-wheel frames, source-identical to X05**: `r01-photo-tap-p13-pressed.svg` (p12 still selected), `r01-photo-tap-p13-settled.svg` (p13 new hero and selected wedge), and `r01-photo-drag-36.svg` (p12 held pending settle). All preserve last Glimpse Home acknowledgement p04 and the same five distinct nearby photo fixtures as R01. [Successful focused Actions #40115632444](https://github.com/gamesbyian/Glimpse/actions/runs/40115632444).
2. **Home photographic crescent and 200%-type density candidates**: `r01-photo-home-crescent.svg` demonstrates a **tap-only collection route** using the real R01 clipped paper wheel, not a second draggable Home selector. `r01-photo-large-type.svg` uses photographic p12 hero, real five-window geometry and conventional Grid/Previous/Next/two-row bottom navigation. [Successful source + 401×890 raster #40115223639](https://github.com/gamesbyian/Glimpse/actions/runs/40115223639). The depicted 20–31pt layout is **not Android 200% text acceptance**.
3. **Two visually explicit cross-screen return/truth scenes**: `r03-home-last-ack-p04.svg` distinguishes selected preview p12, app last acknowledgement p04 and no OS readback. `r08-skip-return-rainbows.svg` returns to Rainbows with p12 selected and uncommitted restore conflict still available. [Successful CI #40114890526](https://github.com/gamesbyian/Glimpse/actions/runs/40114890526). Real Android OS-state/Back-stack proof remains absent.
4. **Direct 12-frame optical inspection, not just a render claim**: [full phone-size reviewed contact JPEGs](../../design/reviews/targeted-2026-10-24/) and [independent observation record](../design/OPTICAL-REVIEW-2026-10-24-TARGETED-12.md) confirm no obvious gross clipping at fixed 401×890 and that first/empty/single IDs are depicted honestly. The review raised significant cross-screen inconsistency: R03 p12 was vector stripes while R01/R02/Grid all used photographic p12.
5. **Concrete optical defect repaired**: [generator](../../scripts/build-r03-photographic-truth.py) replaces both R03 preview and UNKNOWN vector images with the exact source JPEG p12 and records its digest. [Focused successful #40115923016](https://github.com/gamesbyian/Glimpse/actions/runs/40115923016), followed by [latest copy-consistency successful #40116237813](https://github.com/gamesbyian/Glimpse/actions/runs/40116237813). The R03 preview now labels last Glimpse acknowledgement p04 and uses human-facing “Preview does not change your wallpaper” copy; do not preserve the older more technical live text. Phone-size cross-source comparison was opened in the refreshed 18-source contact set [#40115984371](https://github.com/gamesbyian/Glimpse/actions/runs/40115984371), which predates only the final wording revision. A newer [contact rerender](https://github.com/gamesbyian/Glimpse/actions/runs/40116392107) was triggered for the latest wording; confirm its exact conclusion and inspect updated pixels before closing its optical review.
6. **Coherent review selection infrastructure**: [machine-readable registry](../../design/proposals/whole-product-review-selection.v1.json), [24/34/23 completeness validator](../../scripts/validate-whole-product-review-selection.py), deterministic [offline owner-review viewer](../../design/atlas/whole-product-review-selection.html) and [source SHA snapshot](../../design/atlas/whole-product-review-snapshot.v1.json) were generated by [successful scoped atlas workflow #40116367838](https://github.com/gamesbyian/Glimpse/actions/runs/40116367838). These do not imply whole-set optical approval or owner choices.
7. **All 56 illustration targets sourced**: [scenario matrix](../../design/proposals/audit-scenario-evidence.v1.json) now has 56 direct phone-side scene sources; [validator](../../scripts/validate-audit-scenario-trace.py) checks each source, finite 0/1/3/5/22/60 metadata, R04 locally verified count and no counterfeit approval. The [full 271-source atlas](../../design/atlas/whole-set-evidence-atlas.html) displays draft and historical alternatives separately.

## Remaining high-stakes defects or proof boundaries

- **Current-generation continuity**: Original support/core plate schematics and old storyboards are still mixed with newer photographic R01/R02/R03 content. Treat 24 default review choices as a working shortlist, not a consistent final art direction. In particular, p04 Home-ack hero currently uses a synthetic rainbow illustration, not a separately verified photo p04 fixture. Do not accidentally reuse another stable photo ID’s JPEG to fill that visual gap.
- **Physical reel/reachability**: Five physical openings extend off-screen by design. Check actual 0, 1, 3, 5, 22 and 60 item selection and interruption on Android, especially at finite edges. 36° drag labels approaching crop bounds must not become a touch/visual trap. A still PNG does not test gestures or inertia.
- **Home crescent**: The subordinate arc is intelligible with an explicit Open collection button in the proposal, but it may create a second gesture metaphor without enough benefit. Compare owner interpretation against the simple card before selecting it.
- **Text and assistive tech**: Source mockup labels fit the sampled raster, but actual 200% Android font scale, 320dp/640dp, TalkBack traversal, 48dp physical touch bounds and OEM safe areas are unverified.
- **Native photo/OS behavior**: The successful [historical seven-capture emulator gallery](../quality/NATIVE-ATLAS-DISCOVERY-2026-10-24.md) is a valid *older prototype* baseline and its artifact should be inspected. Its rounded-rectangle pseudo-reel predates current sources; do not label it as native conformity. Google Photos picker, Sharesheet delivery, WallpaperManager per-target acknowledgements, best-effort scheduling, storage/restore journal must still be tested.
- **Authoritative review**: Draft source hash snapshot proves exactly which art was available, not which art the owner prefers. The eight product-owner decision questions are left unanswered. Approval must explicitly name exact source SHAs and any conditions, followed by separate native screenshot/a11y conformity proof.

## Work forward without babysitting CI

1. Inspect the latest 18-source photographic gesture/p12-truth contact images (and the newest wording-correct R03 preview image), record directly seen defects with SHA, correct real optical problems and regenerate only changed frames.
2. Finish one *consistent* editorial current-generation design choice across the 24 primary screen families and 34 support variant cases. Existing plate schematics might require redesigns; select alternatives in [review selection](../../design/atlas/whole-product-review-selection.html), not merely paths from the 271-source evidence inventory.
3. For the actual running first-slice, download the seven-native-screenshot emulator artifact and inspect pixels/hierarchy at original dimensions; use that baseline to scope **new** photographic motion/native a11y instrumentation rather than blindly repeating full Android CI.
4. Run a second scored audit iteration **only with evidence that genuinely satisfies its individual question's source/device/human proof requirement**; do not convert the 34 UNVERIFIED to PASS by drawing another screen.
5. Keep [PR #6](https://github.com/gamesbyian/Glimpse/pull/6) **DRAFT**. Owner approval, visual freeze, full implementation and native conformity remain separate steps.

**Useful local commands from a complete checkout:**
```sh
python3 scripts/build-r01-photo-motion-continuity.py
python3 scripts/build-r01-home-large-type.py
python3 scripts/build-r03-r08-context-states.py
python3 scripts/build-r03-photographic-truth.py
python3 scripts/validate-audit-scenario-trace.py
python3 scripts/validate-whole-product-review-selection.py
python3 scripts/build-whole-product-review-viewer.py --check
python3 scripts/build-whole-set-evidence-atlas.py --check
python3 -m http.server 8000
# /design/atlas/whole-product-review-selection.html
```
