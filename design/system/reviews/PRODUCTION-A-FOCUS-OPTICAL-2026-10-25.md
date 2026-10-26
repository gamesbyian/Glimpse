# Production A independently inspected keyboard focus comparison · 2026-10-25

**Evidence class: static optical render at 401×890 only.** No native Android focus, keyboard, switch access, TalkBack, 320dp/200%-font or human usability PASS is implied.

## Exact source and raster evidence

- Unselected focus source: `design/proposals/whole-product-candidates/accessibility-keyboard-focus-visual.v1.svg`, SHA-256 `4c18210da552445fbbffb2ad572cdb06ba7919631978fba9ba1450f3acb26d1f`.
- Default primary source: `design/proposals/plates/accessibility-preferences.svg`, SHA-256 `a62db6dab0849ac9bfdeaa76e3809a9e359f5d9762524a2945702b2ee00591cd`. This source remains unchanged.
- Focus 401×890 PNG: `design/system/reviews/ci-renders/accessibility-keyboard-focus-visual.png`, SHA-256 `8ae325f044e2677e4e0fa41059187ec9d4e5f4604fa1ea1e7756c61fd066d25d`.
- Side-by-side 401×890 pair: `design/system/reviews/ci-renders/accessibility-focus-before-after.jpg`, SHA-256 `905d838fed6fc66b08f4bf988d3f732132899858c0256929c363f85b1dcb54ac`.
- Exact automated render and localized-pixel-change assertions: [Production A #40247154086](https://github.com/gamesbyian/Glimpse/actions/runs/40247154086), SUCCESS. C independent photo source gate [#40247154133](https://github.com/gamesbyian/Glimpse/actions/runs/40247154133), SUCCESS. Current PR branch may advance with CI raster archival commits; do not infer later commits were independently retested.

## Direct optical observations

I directly opened the source-indexed final before/after JPEG, not merely the SVG markup. The outline is visibly continuous, dark teal, rounded and offset around the **Prefer Grid** row; it does not obscure the Grid label, descriptive line or chevron. The default state retains the existing light border. The **Previous and Next** alternative, Reduce reel animation, Android text-size notice, TalkBack row, four conventional bottom destinations and footer remain visibly unchanged. The final caption separator renders correctly as a colon. The screen clearly says `Proposed, not native proof`; **no visual approval is inferred**.

The workflow also asserts pixel changes occur only within phone y=300..400 (covering the targeted 362×66 row), and records a SHA-256 for the comparison. This is a local-difference guarantee against unintended design drift, not proof that focus will actually render or move in Android.

## Remaining gates and integration request

1. Keep this candidate **unselected** unless integration deliberately adds a focus-state reviewer example; it is not an additional selected primary family.
2. Confirm native keyboard/D-pad/switch focus indication, focus traversal and focus restoration after Android-owned picker/chooser return. Check high contrast against actual dynamic/photo backgrounds, particularly if reused outside this warm paper setting.
3. Verify 320dp + fontScale 2 without cropped labels or unreachable bottom navigation; investigate focus clipping near screen/inset edges. TalkBack announcements require actual accessibility-node evidence.
4. Keep the existing CC0 six-screen photo migration atomic and unselected. Integration #10 selects sources and owns owner decisions; A does not touch PR #6 or `design/approved/`.
