# Glimpse design assets: current inventory

**Updated 2026-10-22.** This inventory describes the actual contents of `gamesbyian/Glimpse`. The old private source folder was preserved, but is not the destination for future work.

## Verified files in this repository

**Tactile reel reference**
- [Five-window paper reel (vector)](../design/reference/five-window-reel.svg)
- [Offscreen browser concept (vector)](../design/reference/reel-browser-concept.svg)
- [Wide-aperture visual exploration (PNG)](../design/reference/reel-wide-apertures.png)
- [Provisional brand tokens](../design/tokens/brand.json)

**Reel layout experiment**
- [Study and verdict](design/reel-layout-comparison.md)
- [Six editable SVG variants and exact photo fixtures](../design/research/reel-layout/README.md)
- [Reproducible generator](../design/research/reel-layout/build.py), [variant/fixture SHA-256 manifest](../design/research/reel-layout/manifest.json)
- [Eight-page comparison review PDF](../design/research/reel-layout/comparison-review.pdf)

**Motion study, not accepted implementation**
- [Virtualization example](../design/research/motion/virtualization.png)
- [Next-frame example](../design/research/motion/next-frame.png)
- [Last-frame example](../design/research/motion/last-frame.png)

The **six SVGs, six fixture JPGs, four PNG reference images and PDF were migrated with exact Git blob SHA parity** to the original files in the private `general-purpose/Glimpse` directory. These files are visual research, not screenshots from an implemented Android app. The wide-aperture and paper aesthetic are accepted direction; exact geometry, copy and all dual-reel variants remain provisional.

## Only one shortlisted asset still outside Glimpse

The tactile identity board is preserved at [`general-purpose/Glimpse/artifacts/glimpse_tactile_wallpaper_identity_board.png`](https://github.com/gamesbyian/general-purpose/blob/main/Glimpse/artifacts/glimpse_tactile_wallpaper_identity_board.png) (the archive is **private**). Git blob SHA `0a53b73ef54fe54815f9ff63bce521dbeef91fd2`, 2,066,345 bytes. The available file reader returned empty content for this larger binary, so it has **not** been published to the new repo. Do not create an empty or synthetic substitute. Earlier visual direction can still be studied using the preserved smaller reference PNG and editable schematics.

## Deliberately excluded historical artifacts

The source archive holds older Waltz v2/v3 decks, the v4 57-slide deck, old screen mockups and exploratory PDFs, 15 transfer chunks, an orphaned deck generator and split ZIP files. Those are **not current design authority**. Do not import them wholesale.

The redundant `Glimpse_Reel_Comparison_Reproducible_Source.zip` is unnecessary because the generator, fixtures and outputs are now tracked individually.

## Image provenance and publication

The six small photo fixtures originated with the initial Glimpse design work; the project record identifies them by SHA-256 in [the manifest](../design/research/reel-layout/manifest.json). No explicit standalone image-use licence has been established. Verify their provenance and licensing before redistribution or making an open-source licence grant over the artwork. Public repository visibility does not itself settle third-party image rights.

For future assets, record source, SHA-256, usage, approval level and license. **Accepted style direction is not approval of every mockup pixel.** Keep production-approved references in `design/reference/`, experiments in `design/research/`, and link them through this page.
