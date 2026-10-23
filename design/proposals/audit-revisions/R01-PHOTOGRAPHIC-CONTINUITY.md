# R01 photographic continuity: source-backed revision checkpoint

**Audit cycle A refinement.** Owner goal: substantial tactile five-window paper reel, faithful finite photo identities and a stationary preview. Existing [X05 storyboard](../storyboards/x05/01-rest.svg) supplied proven wedge geometry; the first [R01 round-chip variants](r01-idle-22.svg) provided mapping tests but failed first optical scrutiny. This revision combines those strengths without silently approving either.

## Photographic frame assets already committed

| New draft source | Reused tactile geometry | Focused photo | Unique visible photo fixtures |
| --- | --- | --- | --- |
| [Photo rest 22](r01-photo-rest-22.svg) | X05 rest | p12, art1 | p10 art2, p11 art3, p12 art1, p13 art4, p14 art5 |
| [Photo drag 18°](r01-photo-drag-18.svg) | X05 mid-drag | p12, art1 remains committed | Same five IDs/assets; wheel moves without selection change |
| [Photo snap 13](r01-photo-snap-13.svg) | X05 settle | p13, art4 | p11 art3, p12 art1, p13 art4, p14 art5, p15 art6 |
| [Photo final p22](r01-photo-last-22.svg) | X05 last item | p22, art6 | p20 art2, p21 art3, p22 art6, no invented p23/p24 |

**Fixture media clarification:** The six public JPEG assets include landscape artwork, scenic imagery, flowers, architecture, botanical illustration and a starfield illustration. They are *real embedded image files*, not six photographic captures, not 22 unique user originals and not evidence of a Google Photos provider import. The source-identity comparison is about different visual pixels and verified asset bytes, not photographic provenance.

The deterministic [photo source generator](../../../scripts/build-audit-r01-photo-variants.py) reads the six first-party checked-in JPEGs in `design/research/reel-layout/fixtures/source/art1.jpg` through `art6.jpg`. It embeds actual JPEG bytes into portable SVGs and records per-photo SHA-256, so adjacent labels no longer disguise identical rainbow placeholders. **The images represent a curated six-photo fixture set, NOT 22 individually witnessed actual user photos.** Distant photo IDs may reuse a test fixture, but no two simultaneously visible neighbours do.

## Empty and one-photo extension

The generator also defines `r01-photo-empty-0.svg` and `r01-photo-single-1.svg` from the same physical paper-wheel geometry. Empty: 0 real photos, no hero photo, five unfilled paper openings and Add Photos. Single: p01 art1 under the fixed top pointer and stationary hero, four unfilled openings, Previous/Next disabled. The source-output workflow has been retriggered, but **these two files must be verified present and source-sha raster inspected before they are counted as delivered**.

## Optical acceptance and unresolved questions

- Five physically meaningful photographic apertures, off-screen circular rim and a paper hub should survive a true 401×890 crop. Compare against [first round-chip audit optical defects](../../../docs/design/AUDIT-CANDIDATE-OPTICAL-REVIEW-2026-10-23.md).
- The top focused wedge and stationary preview must render the *identical photographic pixels*, not only the same text ID. Hero and photo in wedge may have different viewports/crops, but source bytes must match.
- Art1-p12 stays invariant between rest and drag, and art4-p13 must persist between neighbouring and snapped frames. No photo appears in empty apertures; p22 visibly has no next photo.
- The generator and source assertions only prove draft SVGs. Independent photo crop comparison, native Android 0/1/3/5/22/60 snapshots, one-handed swipe, TalkBack, 200% font, interruptions and explicit owner preference **remain unverified**.
- Original X05 assets, the first R01 geometry studies, and all audit findings are preserved for reversible comparison; these new proposed frames are **not approved final styling**.

Next: optical inspect six photo-backed crops; compare them at native device scale with X05 and the current native implementation; then update R01 audit and spec wording using actual evidence.
