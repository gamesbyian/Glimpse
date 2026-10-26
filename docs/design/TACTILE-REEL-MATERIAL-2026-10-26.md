# Tactile paper reel artwork contract
**26 October 2026 | R1/R2 asset handoff | generated draft, not approved physical object**

## Source and current integration

Initial source file: [`glimpse_paper_reel_face.png`](../../app/src/main/res/drawable-nodpi/glimpse_paper_reel_face.png), a **384×384 RGBA PNG** baked for the first rotating reel in [`CircularPhotoReel.kt`](../../app/src/main/java/io/github/gamesbyian/glimpse/CircularPhotoReel.kt). Original Git blob ID `41d5e4d04166a83737ed268bee1b0ed5156a45c1`. It is an original, digitally synthesized material sheet with uncompressed PNG data, not sampled from the owner's composite mockup or an imported user photograph. There are **five transparent, genuinely empty photo apertures** in the face. A source-integrity script verifies PNG CRC, exact geometry and window/hub/outer-ring opacity.

Photos are independent cached bitmaps painted underneath the face in the same coordinates. Their identities follow the finite ordered Room-backed selected ID. The paper surface and photographs rotate as one; a fixed pointer does not. Existing Grid, hero and WallpaperManager commands are untouched. The disk is deliberately larger than the display and clipped below the hero; this is a functional provisional viewport, not a final approved position.

**This resource is an intermediate material test, not an artist-approved photoreal paper reel.** It improves on the unshaded flat vector prototype but is still stylized. Do not claim it faithfully reproduces cardboard extrusion, fiber microgeometry, glossy film apertures, physical embossing or the exact late-October reference boards without pixel-level/device review.

## Production-quality art package to replace/refine it

1. **Authored surface maps:** photographed/scanned pale cardboard face, natural uneven grain and honest microscopic wear; soft inner cutout bevel and offset rim thickness; matte specular response. Avoid counterfeit/brand trade dress. Preserve the wide five-window silhouette and an uncluttered center hub.
2. **Separable components:** RGBA face with perfectly registered transparent windows, optional nonrotating soft shadow, optional moving edge occlusion/highlight overlay, and exact normalized angular/mask geometry. Do not bake arbitrary flower/mountain photos or UI labels into the face.
3. **Registration:** nominal 72° pitch, centered selected wedge at -90°, normalized center/radii; the rendered photo mask may exceed the paper window slightly for clean compositing. Cross-check against the actual Kotlin `aperture()` geometry and first/last/empty/recently-deleted/slow-photo states.
4. **Native scaling:** make masters sufficiently large for phone DPI/rotation and derive constrained variants only after real-device measurement. Avoid huge bitmaps that overwhelm thumbnail caches; measure memory, overdraw, jank and idle CPU on Pixel 6 Pro.
5. **Typography:** deliberate display serif/sans pair, licensed and versioned. Photo titles and vital actions legible at 320dp/200% text, with TalkBack/haptic-independent controls. Do not ship web-font hotlinks or unrestricted font binaries from unknown sources.
6. **Provenance:** commit rights/licence, artist/source, exact SHA256 and intended viewport/pose for each source and derivative. User images are never embedded in tests or issue attachments.
7. **Optical acceptance:** compare approved source/master with on-device Reel/Home/Detail/Collections captures, including 0/1/3/22/60 photos, animation mid-step, reduced motion, bounded memory, offscreen cropping, and clear focus marker. A green Gradle job or visual-snapshot test does not replace owner's aesthetic judgment.

## Known constraints

The two owner-provided October 26 boards establish **art direction** but include mock functions not in Glimpse's technical spec. Do not implement a reel for Sharesheet providers, arbitrary search facets or a Settings icon set simply because the pictures portray them. Two live reels may be used **only** in the opt-in collection→photo experiment with distinct IDs and actual counts; preserve a conventional collection chooser comparison and a single-reel compact/large-text alternative.

This document stays alongside the [build plan](../planning/BUILD-PLAN-2026-10-26.md) and [motion architecture](../architecture/reel-motion.md). The source face is replaceable independently of selection logic.
