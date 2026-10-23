# Reel layout research

This directory is **comparative research**, not the accepted production UI.

- [Research method and verdict](../../../docs/design/reel-layout-comparison.md).
- [Generator](build.py) and [fixture/variant manifest](manifest.json).
- SVG outputs and the six consistent source JPEGs are tracked in the original private archive; consult [asset inventory](../../../docs/assets.md) for current transfer status and image-rights review.

The generator requires `fixtures/source/art1.jpg` through `art6.jpg`, `Pillow`, `CairoSVG`, and `reportlab`; it cannot be run from a clean checkout until those fixtures have been cleared and transferred. Variants are deliberately alternative experiments and must not override the current single-reel baseline.
