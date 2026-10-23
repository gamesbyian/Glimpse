# Reel layout comparison: reproducible design experiment

This folder contains **six controlled alternative layouts**, a generator, six identical source photo fixtures across variants, the machine-readable manifest, and a compact eight-page review PDF. They are **design research, not Android screenshots**.

## Key files

- [A: single photo reel](A_single_photo_reel.svg), recommended compact baseline
- [B: nested collection / photo reels](B_asymmetric_collection_photo.svg), exploratory
- [B: compact fallback](B_compact_fallback.svg), reference for accessibility and smaller viewports
- [C: compact Home / Lock preview](C_destinations_compact.svg), exploratory
- [C: conventional destination tabs](C_destinations_tabs_baseline.svg), recommended compact baseline
- [D: wide dual destination reels](D_destinations_wide_dual.svg), exploratory wide layout
- [Comparison review PDF](comparison-review.pdf), [source builder](build.py), [fixture manifest](manifest.json), [six JPEGs](fixtures/source/)

To regenerate, install the Python packages `Pillow`, `CairoSVG`, and `reportlab`, then run `python design/research/reel-layout/build.py` from the repository root. Original output may depend on fonts present on the executing system, so resulting raster typography can differ. This is not an Android build command.

All existing variant SVG, JPEG and PDF files have been verified against their exact source Git blob identities. No live-app usability or accessibility acceptance is claimed. The [decision analysis](../../../docs/design/reel-layout-comparison.md) explains purpose, navigation semantics and experimental conclusions. Use these as alternatives to evaluate, not mandated UI.
