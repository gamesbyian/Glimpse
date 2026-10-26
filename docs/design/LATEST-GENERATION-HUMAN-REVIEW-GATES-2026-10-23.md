# Glimpse latest-generation proposal acceptance: human review packet plan

**2026-10-23 · Draft design integration for PR #6 · NOT APPROVED**

## What constitutes a complete reference set

The product director will review **one consistent implementable whole-set**, not a gallery of mutually incompatible studies. The reference must show (a) an obvious first-use journey, (b) the tactile photographic five-aperture finite reel in Reel and conventional Grid/Detail parity, (c) distinct browsing/queued/applied/unknown states, (d) editing of immutable originals into saved derivatives, (e) real copy-sharing with external handoff limits, (f) honest timing and eligibility, (g) provider and storage failure recovery, (h) large text, TalkBack and reduced-motion alternate paths, and (i) platform insets and permissions without impersonating Android UI.

A proposal is **illustrated** when there is exact editable source with visible commands; **optically reviewed** only when a source-hashed pixel render was directly examined; **native verified** only for documented Android observations; **human approved** only for exact selected source revisions with affirmative signoff. A draft is never accepted by producing additional SVGs.

## Integration decisions for human review

1. **One physical reel treatment**: use R01 *photo-backed paper apertures* rather than earlier round image chips. Preserve fixed pointer, finite indexing, stationary hero, 0/1/N truthful apertures, and explicit Grid escape. Do not count five windows as collection capacity.
2. **One identity**: exact source asset ID persists across Reel → Grid → Detail → Edit. Original p12 bytes remain immutable; p12c only after an acknowledged save. Selected image is not last-confirmed applied wallpaper.
3. **One result vocabulary**: previewed, queued/eligible, attempting, last acknowledged, failed, and unknown. Do not conflate UI selection, worker wake, OS accepted, or user-observed wallpaper.
4. **One recovery policy**: local verified copies survive provider offline/revocation; cancellation and storage failures preserve committed members; restore conflict requires a visible non-destructive choice.
5. **One mobile visual language**: photographic paper textures for the reel; standard compact but comfortable controls elsewhere. Never force decorative reel selectors into utility/settings screens.
6. **One honest external handoff**: Android picker and Sharesheet belong to the OS. Illustrate the app immediately before and after, not fabricated external app screenshots or recipient delivery outcomes.
7. **One accessible parallel route**: Grid/Previous/Next for reel, labeled crop controls, high contrast and text status, reduced-motion alternatives, focus-return after intents. A visual overlay predicts semantics but does not establish real Android bounds.

## Human review itinerary and gates

| Gate | Scope | Evidence before showing as complete | Remaining status |
|---|---|---|---|
| P0 visual continuity | Home, Collections, R01 Reel, Grid, Detail, X05/X06 gestures | Exact source-hashed 401×890 renders at N=0/1/3/5/22/60; actual image fixture ID continuity; pointer/snap and inset inspection | Candidate source partial/optical targeted; whole-set approval absent |
| State/command truth | R02 edit/share, R03 selected/applied, R06 dismissal | Annotated vs annotation-free before/after; semantic command table and per-target acknowledgement | Candidate C and five drawings source-only; native absent |
| Import/storage | R04 import 0/17/22, cancel, no space | Existing targeted optical run 40049428896 plus explicit photo-picker and crash-recovery traces | Optical candidate exists; native absent |
| Scheduling | R07 eligible/paused/delayed/restricted/unknown | Same-framed statuses, timestamp/eligibility semantics, no exact five-minute guarantee | Existing paused/delayed + two new source-only states; native absent |
| Provider/restore | R08 offline/revoked/restore/unsupported target | Source vs verified local copies, conflict choices, no silent fallback | Existing + two new source-only states; native absent |
| Accessibility | R05/reel/Grid/edit/support, 200% and reduced motion | 401×890 optical pass, tested 200% layouts and actual TalkBack hierarchy | Illustrated R05 proposals; native absent |
| Human decision | Consistent cross-screen whole set | Named exact files + hashes, alternatives explicitly retired, undecided questions and approval record | **Not started; no implied approval** |

## Review method (deliberately skeptical)

- **First pass, no annotations:** sequential phone crops, user task and expected first action only. Mark confused affordance, illegible copy, implausible touch target, lost selection, or false status before reading designer notes.
- **Second pass, transition pairs:** show preceding and following frame with selected/source/variant ID, target, requested action, Android result and Back route. Reject changes that occur without a user-visible initiating command.
- **Third pass, adversity:** inspect zero/one/60 photos, source revoked while local copy remains, no space midway through 22 imports, restore collision, unsupported Lock, Sharesheet dismissal, battery delay, unknown apply, 200% text and reduced-motion.
- **Fourth pass, implementation feasibility:** each visible affordance maps to an existing or proposed command in `docs/architecture/behavior-contracts.md`; flag unsupported OS capabilities rather than design them as guaranteed.
- **Final human approval:** product director reviews actual rendered, source-hashed whole-set and records approved SHA(s), rejected alternatives, conditions and change-control rules. Only then declare V2 frozen. Device conformity (V3) requires independent later observations.

## Remaining near-term work, ordered by evidence value

1. Run `python3 scripts/validate-audit-r06-r08.py`, then source-sha render and inspect the **five** new status/decision drawings at exactly 401×890, comparing with their existing prior frames. Fix literal text clipping and crop geometry; no new full 203-image render needed for this delta.
2. Review R03 per-target partial apply and unknown outcome before naming wallpaper truth visually complete; inspect R06 original/edited photo byte identity against R02.
3. Optical-audit revised X07/X13 source images, the 25 supplemental refinements and all 55 P1 storyboards as coherent journeys; triage defects by user harm rather than file count.
4. Build a compact human-review atlas with annotation-free comparisons and source/revision provenance for just the canonical selected set; keep historical/alternative studies discoverable but out of the approval candidate.
5. Request a **separate** product-director approval of the canonical reference set. Do not merge draft PR #6 on the strength of source validators.

**Audit history:** Cycle A remains exactly **18 source-level PASS / 8 ISSUE / 34 UNVERIFIED**. Drawings and specification work in this packet do not justify rescoring.
