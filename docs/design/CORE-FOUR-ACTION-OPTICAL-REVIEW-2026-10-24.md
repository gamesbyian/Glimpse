# Core four-source task-action optical inspection · 2026-10-24

**Status: actual SVG-source-equivalent 401×890 phone image opened and visually compared. Draft only.** This is a visual/source inspection of the full-width [four-phone contact image](../../design/reviews/primary-actions-2026-10-24/home-reel-grid-detail.jpg), **not** a native Android, TalkBack, gesture, OS application or owner acceptance test.

## Exactly what was viewed

- [Completed focused CI #40125933149](https://github.com/gamesbyian/Glimpse/actions/runs/40125933149).
- [Exact source and contact hashes](../../design/reviews/primary-actions-2026-10-24/four-source-ledger.json): input source commit `0f0b1c5e024245e57468d6a8ca1380d6ed3bf198`, contact SHA-256 `3a7bde2eeebd85984773a26f4cecb848a0ac1963d6107c7c9af86b02ad2f98b1`.
- Three stages were **actually** opened: an earlier three-phone contact showing the initial Home/Reel/Detail action placements, an initial four-phone contact exposing compressed Reel navigation and synthetic Grid neighbor photos, and the corrected four-phone contact at the above exact hashes. All renders use CairoSVG/Pillow with a 401×890 phone crop (15,14–416,904) from 1160×920 annotated sources.

| Chosen source | Exact SHA-256 (SVG bytes) | Direct visible observation |
| --- | --- | --- |
| `r03-home-primary-actions-p04.svg` | `77e652b3bb0320f0e6f989770d6f565f602508f7840048a2b6238adc5eba1a5a` | Home shows **last Glimpse acknowledgement p04** and explicitly notes Android may now differ. Separate photographic p12 preview says selected/not applied. Four distinct large actions: Change Home now, Pause shuffle, Open Rainbows, Share saved p04. Activity/device check remains accessible as a text route. Navigation footer now says Automation in full. No obvious normal-size text clipping. |
| `r01-photo-primary-wallpaper-action.svg` | `9b9a0f18723340cdecec8eec38751f8c2e58b5678b53ec5ef05e20ab20a9852b` | p12 hero matches chosen photographic wedge; real p11/p13 neighboring images and finite 12/22 count are visible. Separate View all and Set wallpaper… controls are above the tactile wheel, with pointer now spaced below the action row. Previous/Next remain near the thumb, and four bottom navigation labels now have independent spacing. No obvious overlap of the action row/pointer in the corrected contact. |
| `r02-grid-primary-actions-p12.svg` | `d07b44d764c1b17c43b9d02a6973185e1c95122d18b2ae82ffbdd2f3fea416e4` | Selected p12 is source-identical. The p10, p11, p13, p14, p15 neighbors now use **distinct exact R01 JPEG fixture bytes**, replacing the visibly contradictory synthetic stripes in the earlier contact. Search/jump, Back to Reel, View p12, Share p12, Select multiple are separate visible controls. No obvious button/nav clipping in this crop. |
| `r02-photo-detail-actions-p12.svg` | `4d894bd3a70114dd7d23f4cbfc7d935f82810b382f89a4bc458ae1e30ad7173d` | Same real p12 photograph is hero; provenance and Rainbows identity are readable. Grid return at top plus distinct Set wallpaper…, Share photo and Edit copy occupy two large bottom action rows. No obvious crop or action label overflow. |

**Specific defects identified and corrected in source during this pass:** (1) Missing primary wallpaper action on selected Reel; (2) missing independent source-detail set/share/edit actions; (3) selected Home default omitted Change/Pause/Share; (4) selected Grid lacked search/share/multi-selection; (5) run-together Reel navigation and near-collision of paper pointer with new action row; (6) incorrect image identity for Grid p10/p11/p13/p14/p15 versus the Reel. These are fixes to **draft art**, not proof of application feature implementation.

## Still genuine review blockers

1. **p04 source truth:** Home p04 is a labelled vector illustration, because there is no independently verified p04 JPEG fixture in this lane. Never display the p12 JPG under p04 as a counterfeit identity. Final owner-grade photographic Home reference needs a genuinely distinct p04 image/provenance or an explicitly accepted non-photographic placeholder policy.
2. **Grid photo completeness:** Other visible tiles p06–p09/p16–p17 remain schematic (and are distinguishable from the six source-backed photos). This is honest but aesthetically inconsistent within the candidate; a complete current-generation photographic Grid should use source-linked fixtures for those IDs or a clearly indicated loading-placeholder treatment. Don't reuse another real ID’s photograph.
3. **Dynamic layout:** Four 401×890 source-equivalent crops show no gross text/image clipping at this *one* renderer/font scale. They cannot prove Android 200% text, 320dp short-window layout, 48dp touch bounds, insets, screen reader focus or gestures. The primary Reel action row is visually dense above the wheel; test real thumb reach, accessibility and motion before selection.
4. **Wallpaper/share semantics:** Set wallpaper… means open target choice. Actual WallpaperManager calls are distinct serializable user-confirmed operations; no OS success can be inferred from browsing or a chooser. Share/edit are distinct original/copy handoffs and do not prove external delivery or file durability. Change Home now uses active shuffle rather than silently applying selected p12.
5. **Whole-product style:** These four use photo-driven elements, but 20 other primary screens and 34 support variants remain in the curated review packet, some still legacy schematic drawings. Four optically legible references do **not** freeze a consistent 24-screen design system.

## Next proofs

- Preserve the exact four SHA entries in the draft [whole-product selection registry](../../design/proposals/whole-product-review-selection.v1.json), never set `ownerChoice` or `exactOwnerApprovalSHA256` until explicit human review.
- Bring the remaining Grid tiles and the actual p04 Home photographic identity into the same coherent source-backed vocabulary; avoid inventing user-owned source pixels.
- Compare the chosen four phone references against running Compose/Android emulator screenshots and hierarchy: Reel tap/swipe/snap/0–60 finite traversal, selected image continuity, explicit apply/unknown outcome, crop/save/share, 200% text, reduced motion and 320dp class.
- Keep the [60-question cycle A](glimpse-ux-audit-cycle-a.v1.json) at **18 source PASS / 8 ISSUE / 34 UNVERIFIED** unless question-specific new evidence suffices. This raster inspection only contributes to particular **draft visual evidence** gates.

**No owner approval and no native conformance claimed. PR #6 stays draft.**
