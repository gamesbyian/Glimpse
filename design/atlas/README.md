# Glimpse draft visual evidence atlas

**2026-10-24 checkpoint · Draft PR #6. Every design here remains unapproved.** This is an offline-browser reference system, not an Android screenshot gallery or proof of real-world UX acceptance.

To browse from a checkout:

```sh
python3 -m http.server 8000
# Open http://localhost:8000/design/atlas/whole-set-evidence-atlas.html
```

## Which viewer should I open?

| Viewer | Purpose | Evidence boundary |
| --- | --- | --- |
| [Whole-product review candidate](whole-product-review-selection.html) | 24 current review choices, 34 support variants, 23 journey links and eight unresolved owner questions | Editable selection, not final approval; [source-hashed draft snapshot](whole-product-review-snapshot.v1.json) |
| [Whole-set evidence atlas](whole-set-evidence-atlas.html) | All **271 unique currently checked-in draft SVGs**, searchable with a two-phone side-by-side comparison tray | Source discovery and browser SVG rendering, **not** 271 optical passes or one approved design |
| [39-source preliminary preferred review spine](canonical-review-spine.html) | Human task-order journey through the intended visual language | Incomplete selection, not final specification |
| [Original reference atlas](index.html) | 66 original designs: 24 core, 8 critical reel, 34 support states | Original design families; not extra additions |
| [P0 connected storyboard viewer](storyboards.html) | 82 P0 frames X01–X12 | Interaction drawings, not working Android flows |
| [Named-state contact sheet](core-state-contact-sheet.html) | The **34 originally missing named states**, now 34/34 structurally pictured with exact source paths | Phone-side same-surface presence only, not optical/human validation |
| [Representative flow comparisons](representative-flows.html) | 60 selected entries linking high-risk transitions | Useful visual reconnaissance, not complete audit |

The full count is **203 baseline** (66 original + 82 P0 + 55 P1), plus **25 separately classified refinements**, plus **43 audit-driven revision studies** after the N=3/5/60 photographic additions. The source inventory is computed rather than manually transcribed: [SHA-256 index](whole-set-evidence-index.json). The 39-source preferred tour and 34 state names are **views into** those sources, not extra assets to add to 271.

[All 56 audit-revision scenarios](../proposals/audit-scenario-evidence.v1.json) are now mapped to source evidence as **56 directly pictured, 0 partial, 0 missing-specific**. The formerly missing-specific 320×640 compact-phone case is now illustrated with the exact R01 photographic paper-wheel geometry; its [focused CI proof](https://github.com/gamesbyian/Glimpse/actions/runs/40070030469) generated real pixels, but no independent optical or Android acceptance is implied. These numbers describe *illustration availability*, not native correctness, optical acceptance or progress against the 60-question scored UX audit. The 60-question cycle A scores stay **18 source-level PASS / 8 ISSUE / 34 UNVERIFIED**.

## Rebuild and verify without expensive emulator CI

```sh
python3 scripts/validate-visual-spec.py
python3 scripts/validate-storyboard-coverage.py
python3 scripts/validate-core-state-evidence.py
python3 scripts/validate-storyboard-semantics.py
python3 scripts/validate-audit-scenario-trace.py
python3 scripts/build-whole-set-evidence-atlas.py --check
python3 scripts/validate-whole-product-review-selection.py
python3 scripts/build-whole-product-review-viewer.py --check
python3 scripts/build-whole-set-evidence-atlas.py
```

The new [scoped atlas workflow](../../.github/workflows/glimpse-whole-set-evidence-atlas.yml) validates sources and generates the full atlas and the draft owner-review selection/snapshot only after intentional manifest/generator/workflow changes or a manual dispatch. Every SHA-256 describes its exact SVG source bytes. **Changing a mockup without regenerating the index leaves a valid historical snapshot, not a claim about current HEAD.** Link the source commit and regenerating Actions run when sharing a review packet.

Native optical proof is separate. The [finite N=3/5/60 R01 workflow](../../.github/workflows/glimpse-r01-finite-size-proof.yml) commits editable source and emits 401×890 PNGs. Those automated renders are not individually inspected just because they exist. Other targeted R01, R04, R05 and R06–R08 optical logs distinguish actual agent-visible PNG inspection from mere pixel generation. See [review gates](../../docs/design/LATEST-GENERATION-HUMAN-REVIEW-GATES-2026-10-23.md) and the [running handoff](../../docs/planning/HANDOFF-2026-10-23-VISUAL-RECONCILIATION.md).

**Rule for approval:** the owner explicitly chooses and approves one coherent set of exact SVG versions. Historic alternatives remain identifiable and accessible; neither CI nor inclusion in an atlas promotes them. Real Android screenshots, font-scale/focus measurements, Photo Picker and Sharesheet outcomes, wallpaper targeting, and scheduler behavior require separate evidence.

The [R01 first-item photographic paper-disc candidate](../proposals/audit-revisions/r01-photo-first-22.svg) now complements the finite-last p22 candidate; its [targeted source/raster workflow](https://github.com/gamesbyian/Glimpse/actions/runs/40070370730) succeeded with three genuine filled apertures and two empty leading slots. The new crop still needs independent visual inspection and native navigation tests.

R04 now also includes [a same-surface offline partial-import draft](../proposals/audit-revisions/r04-import-offline-17-of-22.svg): 17 verified local copies usable, five provider items pending, Resume disabled offline, no automatic wallpaper change. [Focused five-state raster run #40070821890](https://github.com/gamesbyian/Glimpse/actions/runs/40070821890) succeeded. Neither the new offline SVG nor its screenshot has been independently visually inspected in this continuation; an actual provider interruption and resume remain untested natively.

**Latest current-generation reconciliation:** [271-source and whole-product review handoff](../../docs/planning/HANDOFF-2026-10-24-WHOLE-PRODUCT-REVIEW.md) records the 56/56 source-illustration coverage and 18 current generation phone crops in [six optical comparison sheets](../reviews/targeted-2026-10-24/), plus the discovered real p12-source inconsistency and focused repair. Human selection, Android runtime and accessibility proof remain separate.
