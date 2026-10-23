# Asset inventory and migration policy

Updated 2026-10-22 for **gamesbyian/Glimpse**. This file replaces two stale asset/publication documents from the earlier `general-purpose` directory. It records *actual repository presence* rather than earlier intentions.

## Verified in this repository

- [Brand tokens](../design/tokens/brand.json): exploratory visual values
- [Five-window reel drawing](../design/reference/five-window-reel.svg): editable simplified schematic
- [Reel browser concept drawing](../design/reference/reel-browser-concept.svg): editable simplified schematic
- [Reel comparison manifest](../design/research/reel-layout/manifest.json) and [generator](../design/research/reel-layout/build.py): research and build source; generator requires photo fixtures and additional Python dependencies to operate
- [Screen contract](design/screen-contract.json): machine-readable planned UI semantics
- All current specification and decision Markdown listed in [docs index](README.md)

The design reference SVGs are **not approved exact UI screenshots**, and none demonstrate a working Android application.

## Approved direction versus unapproved alternatives

Accepted directional guidance: tactile warm paper material, five **large** photo windows, cropped reel extending offscreen, a full-size stationary image preview, real finite counts and a Grid alternative. A single photo reel is the *recommended* baseline. Two-reel designs and specific placements remain comparison hypotheses.

## Source retained elsewhere

The private archive [`gamesbyian/general-purpose/Glimpse`](https://github.com/gamesbyian/general-purpose/tree/main/Glimpse) is the original preservation copy. This public repository deliberately excludes:
- Fifteen `.transfer/v4` upload chunks.
- Older Waltz mockup SVGs, obsolete v4 generator and superseded deck outline.
- 57-slide v4 pitch deck, earlier exploratory PDFs, archive parts, and other publication-era binaries whose current authority is superseded or redundant.

**Do not copy wholesale** from the archive. Verify file provenance, authorship, image rights, usefulness and SHA before bringing in additional binary assets.

## Remaining useful reference candidates

These were selected for eventual migration, but are **not to be described as present until their paths actually exist**:
- Six photographic test fixtures `design/reel-comparison/fixtures/source/art1.jpg` to `art6.jpg` (rights review first).
- Six comparison SVGs `A_single_photo_reel.svg`, `B_asymmetric_collection_photo.svg`, `B_compact_fallback.svg`, `C_destinations_compact.svg`, `C_destinations_tabs_baseline.svg`, and `D_destinations_wide_dual.svg`.
- The tactile identity board `artifacts/glimpse_tactile_wallpaper_identity_board.png`.
- Compact reel layout comparison PDF `artifacts/Glimpse_Reel_Layout_Comparison_Review.pdf`.
- Four motion/still-frame reference images in `design/v4-source/`: five-wide-windows, virtualization-sequence, next-frame, last-frame-derived.

Do not embed JPEGs or other nonfree or unverified imagery merely to make an attractive repository README. Keep the current reference drawings and acceptance text authoritative for build decisions until source assets have been properly approved.

## Canonicalization requirements

For any transferred file: list source repo and path, SHA-256, usage and license, whether a user approved *the specific asset* versus its general direction, and a stable URL. Resolve remaining stale links to original private files as **archive references**, not local new-repo assets. Once a prototype is accepted, maintain a small `design/reference/` set and retain research under `design/research/`.
