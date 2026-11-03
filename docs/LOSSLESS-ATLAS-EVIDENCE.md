# Lossless visual-atlas storage experiment (not the default CI artifact)

The ordinary Android visual atlas stays **PNG + Android UI XML**, with its existing workflow, screenshots, names, SHA-bound evidence and retention untouched. The optional `scripts/repack_atlas_lossless.py` tool experiments with a separately named *shadow ZIP* of the actual captured images. It does not generate screenshots, does not alter the installed Android app and must not be used to qualify native behavior.

## Source and actual measured context

- Original [Glimpse Android atlas Actions run #41405719069](https://github.com/gamesbyian/Glimpse/actions/runs/41405719069), artifact ID `13124561527`, produces **94 files** (46 real Android PNGs, 46 XML hierarchies and two index/metadata files).
- The original GitHub Actions artifact ZIP has **72,606,231 bytes**. Its PNG entries contain **73,188,146 bytes** of image payload before outer ZIP compression.
- A subsequent complete 94-file pack benchmark produced a verified **44,594,660-byte** shadow ZIP, versus **72,606,231 bytes** original: **28,011,571 bytes (38.58%) smaller**. Every member was read back successfully from the shadow ZIP and all encoded screenshot RGBA pixel digests were revalidated. This is a local artifact experiment, not an Actions upload measurement or production acceptance.
- An earlier *per-PNG* 46-image test using Pillow lossless WebP with `method=2` produced **45,322,584 bytes**, saving **38.07% of individual image-file bytes**. All 46 decoded WebPs matched their original screenshots **byte for byte at the RGBA pixel-plane level**, after independent decoding. This is not itself a final ZIP benchmark, and it does not preserve original PNG file bytes.

## Reproduce on a downloaded original Actions artifact

Use Python 3.10+ and a pinned compatible Pillow; the reference test used Pillow 12.3.0.

```bash
python -m pip install Pillow==12.3.0
python -m unittest discover -s scripts -p 'test_repack_atlas_lossless.py' -v
python scripts/repack_atlas_lossless.py --input original-atlas.zip --output shadow-lossless.zip --method 2
python scripts/repack_atlas_lossless.py --verify shadow-lossless.zip
```

The tool preserves XML and other non-PNG files verbatim in the shadow archive and maps every screenshot name to a `.webp` file. The manifest records original file paths, SHA-256 of the original PNGs, SHA-256 of the encoded WebPs, screenshot dimensions and SHA-256 of the canonical decoded RGBA bytes. It refuses ZIP path traversal, duplicate source members, collisions, missing files and mismatched pixels. It refuses overwriting its own input or an existing output. Conversion uses a private staging file and publish-only-on-success behavior, so a failed run does not leave a corrupted final output.

**Critical provenance distinction:** lossless WebP reconstructs identical pixels but cannot reconstruct the original encoded PNG bytes. The manifest's original PNG hash is a *claim about the input the converter observed*, not independent proof of those unavailable original bytes. To authenticate it, retain the original source artifact or separately recorded, independently trusted source hashes. Do not convert away the only copy of original/source-locked review evidence.

## Promotion gates

Before this could replace a default upload, validate all downstream image viewers, issue links, report consumers, filename/path expectations and evidence immutability requirements. Run independent exact-source comparisons, test failure-path screenshot capture, measure GitHub's **final uploaded artifact ZIP bytes** and actual runner CPU time, and demonstrate robust consumer recovery or dual publication. A successful conversion never establishes Android UI acceptance, physical-device parity, or user visual approval. [Storage investigation #191](https://github.com/gamesbyian/Glimpse/issues/191) owns further integration decisions.
