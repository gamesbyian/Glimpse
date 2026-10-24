# Glimpse audit cycle A: findings and revision programme

**2026-10-23 · Audit performed as a first-pass evidence-constrained desk assessment.** Source of truth: [60 weighted questions](glimpse-ux-audit-60.v1.json) and [machine-readable decisions](glimpse-ux-audit-cycle-a.v1.json). This is **not owner approval, human usability testing, Android runtime acceptance or a final exhaustive visual score.** The 60 questions have each been considered; where defensible evidence is missing the outcome is `UNVERIFIED`, never an invented pass.

## Findings at this checkpoint

- **18 PASS at the draft source/contract evidence level, 8 ISSUE, 34 UNVERIFIED.** 26/60 questions have a defensible source-level decision; weighted assessed coverage is **43.1%**, not quality or readiness percentage. Scoring weights are relevance to Glimpse, not confidence in native correctness. The first 203-image baseline render is pinned to [Actions 40023670567](https://github.com/gamesbyian/Glimpse/actions/runs/40023670567); it predates five repaired source drawings.
- **Severe confirmed owner issue, GUX-02/03/59:** the first running app used a generic rounded-rectangle slot strip and obscured collection reachability beyond five, in contrast to the central tangible reel concept. Preserve successful Google Photos import, Home apply and shuffle while replacing the selector. See [owner-firsthand record](../quality/first-slice-evidence.md).
- **Observed reference defects corrected in draft, verification outstanding, GUX-16/25/54:** fake-looking blur sidebars, enabled-looking Continue at 78%, status badges overflowing labels. Treat the fix as `source-corrected/needs-targeted-raster`, not a persistent untouched design defect or closed native defect.
- **Trust and native gaps, GUX-34/35 and many UNVERIFIED:** TalkBack/focus, actual 200% text, OEM Lock/Both, background eligibility, picker interruption and exact screen dimensions are missing independent device evidence; do not score these as a known functional regression merely because no testing exists.

## Revision candidates (specific rather than stylistic whims)

| ID | Priority | Intended spec delta | Minimum new comparison assets | Proof before acceptance |
| --- | --- | --- | --- | --- |
| R01 | P0 | Specify **one genuinely circular finite reel**: 5 equally spaced photographic apertures, partial disc off-screen, stationary pointer/hero; unlimited finite data by virtual mapping; settled and dragged states; explicit edges at N=0/1/3/5/22/60. | Reference versus current-native screenshot pair; 0/1/3/5/22/60 contact sheet; touch-down/drag/snap/boundary sequence | On-device screenshot/hierarchy, finite index mapping and drag/tap/Previous/Next parity |
| R02 | P0 | Specify canonical **photo selection identity** across Reel→Grid→Detail→Reel; differentiate selected, edited, confirmed applied and eligible-next state. | p12 reel, grid, detail, edit p12c, share, return sequence with same raster source and an explicit applied p04 mismatch | Identity assertions, image-byte and crop transform checks; no wallpaper apply on browse |
| R03 | P0 | Status **truthfulness grammar**: requested / acknowledged / failed / unknown; never caption unverified wallpaper as current; clear Home versus Lock capabilities. | Home active, pending, failed, unknown and Activity comparison; Home/Lock/Both supported versus unsupported | UI state-machine trace and device capability probes; owner interpretation check |
| R04 | P1 | Formalize **pending verification visual state**: disabled until bytes verified, cancel rollback, partial/retry, low storage and picker offline case. | 0%, 78%, 100%, failed and resumed import at identical layout, varied counts | Confirm real import transaction and safe saved-copy behavior |
| R05 | P0 | Motion-independent, accessible reel equivalent with **44/48dp target diagnostics** (platform-specific), semantic item index/total and focus preservation. | Standard/200% text, high contrast, reduced motion, TalkBack focus route and short viewport overlays | Measured Android hierarchy and TalkBack traversal, not just SVG labels |
| R06 | P1 | Edit/share **bit-preserving originals** and visible p12 versus cropped p12c difference; share-result status never claims downstream recipient acceptance. | Original versus cropped side-by-side; Android Sharesheet handoff and return/no-return pair | Byte-preservation, result contract and source-paired optical screenshot |
| R07 | P1 | Define automation interval as **next eligibility** with foreground/battery restriction caveats; distinguish paused, waiting, delayed and resumed. | Status timelines covering acknowledged p12, delayed unknown attempt, pause/resume and OS restriction | Actual schedule observations without exact-five-minute claim |
| R08 | P1 | Make offline/provider rights/storage/restore recovery distinct, with non-destructive decisions, no inaccessible backend assumptions. | Source offline, provider revoked, low storage, conflicting restore, unsupported target pair | Device/service capability and interrupted transaction cases |

## Change discipline

1. Treat **R01, R02 and R03** as one semantic spine. None can be declared satisfied by adding more decorative photoreel geometry without stable IDs, finite indexing and applied-state distinction.
2. Every revised drawing must link the original finding ID, normative contract and source identity. Keep a **before/after pair** at identical 401×890 phone crop, plus a full source-editable 1160×920 SVG with state metadata.
3. Record source SHA-256 in the atlas, keep raw Android screenshot and device logs separately, and label OS-owned representations as illustrative when a real screen was not captured.
4. Re-score only touched audit questions after new proof, preserving all unchanged assessments and prior history. Do not overwrite raw findings with success language based solely on a repaired caption.
5. Owner visual approval is the last gate, not automatically inferred from CI, mockup output or the agent's judgment.

## Audit preparation gaps requiring evidence, not more mockups

- Latest CI re-render of five post-atlas corrections, documented in [post-atlas source ledger](POST-ATLAS-VERIFICATION-2026-10-23.md).
- A **native screenshot atlas** for supported device sizes and true 200% font scale, with touch bounds and accessibility semantics, compared with the physical reel reference. Existing native source/CI infrastructure is described in [implementation queue](../planning/implementation-queue.md), but this desk assessment did not obtain a source-matched artifact.
- Complete decision on content hierarchy when a collection is empty, huge or offline, and an explicit technical feasibility pass for native reel animations and background eligibility.
- Human owner usability observation for interpretation of current/selected photo, one-handed discoverability and confidence after unknown apply. Current first-slice report supports only the narrow observed behaviours.

**Audit cycle A is a reproducible beginning, not a final pass/fail certificate.** The correct next cycle is: obtain missing evidence → rerun the 60-question grading → implement spec deltas → draw and render before/after proposals → compare new Android screens → request owner signoff.
