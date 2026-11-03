# Native Android atlas: whole-artifact lossless shadow benchmark

Date: 2026-11-02 UTC. This is **experimental P2 evidence**, not a change to
the standard Android atlas upload and not owner visual acceptance.

## Reproducible source

- Actual GitHub Actions [native atlas run #41405719069](https://github.com/gamesbyian/Glimpse/actions/runs/41405719069), artifact `glimpse-android-visual-atlas`, ID **13124561527**.
- ZIP SHA-256: `99a398451a6f490f97e5dd324415491a0b646c2042e9bf7c2045dcf0c095ec1a`.
- Source `manifest.json` identifies screenshot source SHA
  `b9ca263c3ac7d200394a6aee54ad48898f5df3cf`.
- Archive has **94 distinct entries:** 46 native emulator PNG screenshots,
  46 hierarchy XML files, `index.html`, and `manifest.json`.
- Original ZIP size: **72,606,231 bytes**, of which 72,536,503 bytes are
  deflate-compressed PNG entries.

## Complete independent encoding test

Read each original entry from the untouched artifact; encode every PNG as
Pillow 12.3.0 WebP `lossless=True, method=2`; decode it again and compare
**every RGBA pixel byte** and both dimensions with the original PNG. Preserve
all other archive members byte-for-byte and use ZIP deflate level 6. Add a
separate manifest with each original PNG name and pixel digest.

| Metric | Actual measured value |
| --- | ---: |
| Screenshots independently decoded/compared | **46 / 46 pass** |
| Original PNG member payload bytes | 73,188,146 |
| Lossless WebP member payload bytes | 45,322,584 |
| Original artifact ZIP bytes | **72,606,231** |
| Shadow ZIP bytes | **44,588,189** |
| Reduction, whole ZIP | **28,018,042 bytes (38.59%)** |
| Measured local encode+decode+ZIP wall time | **72.54 seconds** |
| Original screenshot identities omitted | **0** |
| Hierarchy XML / HTML / JSON entries omitted | **0** |

The output shadow ZIP contained 95 entries including the extra benchmark
manifest, and passed an independent full ZIP CRC traversal. The size is from
this complete one-off, source-matched benchmark and **does not claim**
a measured GitHub upload duration or savings for every future build. The
existing `scripts/repack_atlas_lossless.py` offers a corresponding opt-in
packer and verifier with a stricter provenance manifest.

## Consumer and provenance constraints

The actual captured `index.html` and `manifest.json` still reference
the original `.png` filenames. Simply substituting `.webp` in the uploaded
artifact would break normal human browsing and downstream filename consumers.

An opt-in **review ZIP restoration** capability therefore needs to reconstruct
the original PNG filenames, preserve the 46 XMLs, HTML and JSON, and
**verify identical decoded RGBA pixel bytes** before publishing the review
bundle. Regenerated PNG encoding is **not byte-identical** to the original
PNG artifact. Keep original per-image PNG SHA-256 identifiers in the
shadow manifest for traceability; do not claim that recreated PNG bytes match
those original hashes.

The authoritative standard atlas remains unchanged, retaining its original
PNG bytes and current duration. Production switch is deliberately deferred
until reviewer tooling is exercised on a full real source artifact, every
consumer is inventoried, a source-matched exact-artifact upload duration is
measured, and the owner decides whether pixel equivalence is acceptable for
the historical evidence class. Do not downsample, omit 200%-font states, drop
TalkBack hierarchy XML, silently delete semantic capture names or lower
reliability coverage to save storage.

Tracking: [issue #191](https://github.com/gamesbyian/Glimpse/issues/191).
