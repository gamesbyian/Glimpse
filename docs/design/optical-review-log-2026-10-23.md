# Raster optical reconnaissance · Glimpse proposal storyboards · 2026-10-23

**Scope:** pre-audit visual-defect reconnaissance only. **Not** the planned substantive 60-question UI/UX audit, a comprehensive 203/213-SVG optical pass, accessibility certification, Android emulator testing or owner approval.

## Provenance and procedure

Recovered an existing GitHub Actions offline artifact `glimpse-visual-proposals` from successful workflow run [39939557437](https://github.com/gamesbyian/Glimpse/actions/runs/39939557437), built from `main` at `74b5f23f7d4d9abfa0c92e555f7463a061b769ee`. It contains 203 original+P0+P1 proposal SVGs. Extracted to a disposable local folder; used CairoSVG at each SVG's native 1160×920 viewport, then Pillow to crop the **actual phone region** (x15–416, y14–904) and assemble pixel-based contact sheets. Opened and visually examined representative sheets for X01 (7 frames), X03 (7), X04 (7), X05 (8), X06 (7), X07 (7), X09 (8), X11 (7) and 11 P1 support-family frames. This includes **69 frame slots in nine raster-review sheets** with some adjacent duplicates of concepts; review was representative, not comprehensive. The new 25 supplemental refinements, edited branch assets and other original families were *not* in that older artifact.

Rerendered all eight X05 scenes locally **after reconstructing their proposed new self-contained embedded-photo changes** and visually inspected the repaired motion contact sheet. Separately drafted, rasterized, inspected and then reproduced source edits for X11 90° rotation and X09 Reel/Import enlarged typography. These local iterations are representative of the new PR source, but a fresh end-to-end checkout/CI artifact has not yet been obtained.

## Findings and committed repair decisions

### VRC-01 | P0 X05 image assets silently fail to render (critical, repaired)

All **eight X05** phone drawings showed a large empty white hero card and featureless paper reel apertures, despite right-side claims about real photograph identities and tactile motion. These SVGs carried **24 unresolvable relative image references** (`../../../research/reel-layout/fixtures/source/art1–3.jpg`, three per SVG), omitted from the otherwise standalone Actions visual-proposals artifact. This was a concrete false-positive risk for annotation-based “coverage”.

**Repair:** embedded the three already-used research JPEG data URIs (taken from the existing X03 source fixture declarations) directly into every X05 SVG. In local post-repair CairoSVG renders, the selected preview and all aperture sectors now paint actual photography while the disc rotates through rest, touchdown, 18° and 36° drags, 72° snap, reverse, interruption and finite last. This preserves all eight source-specific poses and photos, with p12→p13 selection changing only on snap. Added `scripts/validate-svg-photo-portability.py` to refuse unresolved image `href` attributes in proposal SVGs. It is a source-level regression gate; do not confuse it with optical acceptance. The repaired images still need branch-artifact raster comparison after CI/checkout.

### VRC-02 | X11 Rotate was text-only (critical, repaired)

The old `x11/02-rotate.svg` annotated “Rotate preview 90°” and “working copy only”, while the five horizontal source stripes remained unchanged from the original. A crop rectangle alone was insufficient to prove rotation.

**Repair:** apply `rotate(90 211 338)` to the actual five-color artwork inside a precisely clipped image area, leaving crop handles and original preservation distinct. Local post-repair raster render was inspected: stripes are vertical and the original remains otherwise unchanged. X11 Save and Use Now separately picture `p12c` (edited) instead of `p12` source, with explicit Home action still pending. X13 selected-file metadata corrected from `p12` to `p12c`; the edited-share thumbnail itself still merits an image-level crop distinction pass.

### VRC-03 | X09 “200% text” illustrations largely drew normal-size text (high, partly repaired)

In the baseline raster sheet X09 Home, Reel and Import had 12–18px labels and standard four-column tiny bottom navigation while claiming a double font scale. The 200% scenario was therefore not genuinely pictured. **Repair:** reflowed actual Home, Reel and Import phone content to enlarged titles/action labels. Reel and Import local amended versions were rasterized, viewed and adjusted until large bottom-nav names did not overlap the wheel buttons, and long import provider status wrapped cleanly. X09 Home was source-reflowed independently. Remaining Help, high contrast, font-scale stress states and real Android 200% device tests remain open; no blanket X09 accessibility PASS.

### VRC-04 | X07 reel consistency (high, partially source repaired; optical pending)

Rendered X07 Reel→Grid→Detail→Reel retains p12 in the captions and Grid tile, but its reel depicts a narrow central illustration plus flat pastel left/right blanks rather than the photographic multi-aperture spinning disc rendered in repaired X05. X06 uses yet another simplified multi-aperture paper disc with synthetic rainbow tiles. This could make the product appear to change hardware/visual semantics between navigation paths. **Second-pass source repair:** all three X07 reel scenes now use the actual X05 five-sector paper-disc paths, embedded fixture photographs, selected wedge outline and fixed pointer, cropped to their existing X07 phone viewport. The p12 hero, Grid selection and photo-detail full view now reference the **same embedded fixture1 JPEG bytes as X05**, verified in source via new semantic assertion. Other Grid thumbnails remain explicitly schematic, X06 still has synthetic placeholders, and the X07 new composition needs direct native-resolution raster inspection. This is a partial continuity repair, not a total optical/physical acceptance.

### VRC-05 | Finite reel edges appear directionally correct (source + sampled raster, conditional)

The reviewed X06 first/one/three/exact five/22/60/last frames show appropriate counts and blank vs occupied physical apertures in the phone crop; first/last previous/next buttons appear muted. The 22 middle example pictures p11/p12/p13 and the 60 example p38/p39/p40, with cropped/offscreen outer positions. **No claim yet** that the full circular five windows, 0-photo state, TalkBack order or actual gesture bounds pass; those require additional images/device tests.

### VRC-06 | Native/OS outcomes are still sketches (high, open)

Rendered X01 conveys 22 chosen, 7 verified/15 pending and 20 verified with one duplicate/one skipped without claiming provider-ready cloud items are already local. X03 distinguishes explicit Home request from pending, acknowledged, unknown, failed and Lock-unsupported branches; X04 active/paused/delayed/stopped distinct captions. However Android Photo Picker in X01, OS wallpaper apply, chooser/widget/Quick Settings and exact five-minute schedule remain **illustrations**, not captured host-owned results. They require native acceptance and accessible fallback design checks. The sampled P1 images show compact card controls, often far below 200% text, and many permission/support claims are represented only by short labels.


### VRC-07 | Original critical reel large-text reference clips content (high, source repair pending fresh raster)

Direct inspection of recovered `critical-reel-optic.png` showed `reel-large-text.svg` rendered the 28px collection subtitle beyond the 401px phone width and overlapped a 32px “Selected 12 of 22” caption with the adjacent Grid link. The revised source splits the subtitle into “22 photographs · Offline” and “collection”, reduces the oversized title and reduces the selected counter to 24px. This **source fix is committed but has not yet been raster verified from current PR HEAD**. Do not mark large-text accessibility as approved.

## Follow-up gates

1. Run `python3 scripts/validate-svg-photo-portability.py` and the original plus new structural/semantic validators in a checkout.
2. Generate fresh PNG contact sheets directly from PR #6 HEAD (not the older `74b5f23` archive); review **all** new ten state-refinement SVGs, corrected X05/X09/X11/X13, and source-hashed critical X01/X03/X04/X06/X07 families.
3. Establish exact bitwise image identity or deliberately different fixture sources when a photo ID appears in different storyboard families. Reel art and hero photo should agree within each journey.
4. Check the **15 newly dedicated same-surface drawings** plus 200% text, contrast, focus/insets and OS handoffs. The manifest's 34/34 'pictured' claim is structural, not optical proof. Do not promote drafts or call the 60-question UI/UX audit complete.

**Evidence status:** partial baseline optical reconnaissance + corrected local X05/X09/X11 illustrations, **not** independent end-to-end optical verification of the PR branch; no images approved.


### VRC-08 | Fifteen supplemental state drawings produced; NOT optical pass

Fifteen dedicated source-editable SVGs were added for the former 11 partial and four related-only core variants; the total supplemental count is now 25. Each illustrates its own phone screen and recovery action instead of borrowing notes from another surface. The structural manifest reads 34 pictured / 0 partial / 0 related-only. **The new 15 SVGs have NOT been independently reviewed at native raster resolution**, and the existing 10 plus originals are not comprehensively reviewed on this branch. Next check on-device text wrap, Android-owned controls, action boundaries and the complete optical corpus.


### VRC-09 | X07 photographic identity and reel-def portability (source repaired, raster pending)

Six X07 source frames revised: `01-reel-focus`, `02-grid-focus`, `03-photo-detail`, `04-back-reel`, `06-return-original`, `07-multi-select`. Reel screens reuse photographic five-sector X05 geometry with local clip, and each p12 hero/detail/selected tile reuses **exactly the same embedded fixture1 data**. The first edit attempt accidentally selected a `<rect>` inside a clipPath and removed image definitions; **all six files were immediately reconstructed from unchanged main copies, with explicit JPEG node count/byte-length checks and fresh commits**. The final branch files contain three embedded photo nodes in each Reel scene, and one in each Grid or Detail scene. This is a source-inspected correction; still no fresh branch raster signoff.

`scripts/validate-svg-photo-portability.py` now XML-parses proposals and checks local <use> and clip-path targets, valid embedded JPEG bytes, and expected X05/X07 image fixture counts, preventing the first assembly error from escaping again. `scripts/validate-storyboard-semantics.py` requires cross-frame p12 fixture byte identity rather than merely identical labels. **New script execution and CI results still must be recorded from checkout or workflow logs.**


### VRC-10 | Core Source Detail repeated Discover directory (source corrected, optical pending)

The original `design/proposals/plates/source-detail.svg` was a mislabeled multi-provider Discover phone body even though its right annotation described a single source. It now displays one Google Photos source card, explicit selected-item-only Android picker access, a non-user illustrative image, cautious rights/provenance copy, and separate Choose Photos / Back to Discover actions. No online provider integration, attribution validity or picker UI operation was verified; **new layout has not been independently raster-inspected**.


### VRC-11 | Prepared source-hashed native optical sheets (not executed)

The optional CairoSVG/Pillow helper now enumerates 34 named states, 25 supplemental drawings, the corrected original/offline Source Detail pair and large-type cross-surface comparisons, in addition to the existing journey groups. `--set` limits expensive bitmap decoding, and `--native` emits exact 401×890 phone crops and a JSON ledger of source SHA-256 hashes, making visual review reproducible. **The new renderer revision and its outputs have not been executed in a fresh branch checkout; still no optical signoff.** Reviewers should use `python3 scripts/render-visual-reference-sheets.py --list-sets` followed by `--set core-34-named-states --set source-detail-paired --native` first.


### VRC-12 | Distinct screen-state mechanisms in eight refinements (source inspected)

The second-pass generic state drawings proved too text-heavy to support a meaningful later interface audit. Eight were selectively enhanced with phone-visible mechanisms: `collections.large` now shows three explicit searchable/sortable collection rows and their counts; `destinations.lock` adds a small clock-safe crop visualization and `destinations.unsupported` an actual target capability matrix; `automation-editor.nested` depicts AND with an indented OR subgroup; `photo-detail.excluded` has a small representative preview and reversible status; `backup-and-restore.restore` offers explicit non-destructive conflict choices; `activity.failed` and `activity.uncertain` contrast current vs last-confirmed entries on a timeline. All eight source SVGs were refetched and scanned for missing phone-side evidence strings, duplicate ID hazards and approximate long-line overflow. **These are still draft illustrations, not complete input behaviour or pixel-accurate reviewed native screens.**
