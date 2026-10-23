# Glimpse optical verification delta after full 203-frame atlas

**Status: draft, source-level recovery.** This document prevents the first successful 203-frame atlas from being mistaken for verification of *later* artwork. It is not the 60-question UI/UX audit, Android behaviour acceptance or user design approval.

## Last independently verified corpus

- [Actions run 40023670567](https://github.com/gamesbyian/Glimpse/actions/runs/40023670567) at source commit `76829b700f1f952b94ce8e490b5aed5a406e15e5`.
- This job produced **203 source-hashed original/P0/P1 phone crops** and **59 additional targeted reference crops**, with no reported render failures. An 18-page contact gallery was inspected; some native crops also inspected.
- Structural and semantic checks passed at that specific source commit: **29** semantic cases, 228 proposal SVGs, 133 decodable images. Its evidence does not extend to the modifications below.

## Post-run modifications that invalidate selected earlier image crops

| Artwork | Current GitHub blob SHA | What was changed | Independent current artifact raster status |
| --- | --- | --- | --- |
| `design/proposals/storyboards/x05/08-last.svg` | `337c33cf9bfb53374c1be934c49e53029ff9e170` | Hero and selected aperture both now use the distinct architectural p22 public art4.jpg photo, rather than reusing p13 snow | **Pending final current-source CI PNG** |
| `design/proposals/storyboards/x08/01-home-active.svg` | `d69fcd8cdc15bd3442a8c4096886185a24ff5b94` | CURRENT HOME pale badge widened | **Pending** |
| `design/proposals/storyboards/x08/06-status-collapsed.svg` | `7bced15de949988c3bfae14f75ba3cff527fab2a` | CURRENT HOME pale badge widened | **Pending** |
| `design/proposals/storyboards/x10/06-rotation-delayed.svg` | `e359000436e1a047c8a1b21cf65a8a6d25092208` | LAST ACKNOWLEDGED pale badge widened | **Pending** |
| `design/proposals/advanced-storyboards/x13/02-edited.svg` | `3d88252232861e6ad85f70381a7990bc5665179f` | p12c artwork now visually differs from p12 through a clipped 150% crop | Locally reconstructed and viewed; **independent GitHub raster pending** |

The relevant validation sources also advanced: `scripts/validate-storyboard-semantics.py` blob `1fcca97d5920ce23069ac4587989fc6b52b2fa54` now covers p22 uniqueness, chip widths and transformed p12c; `scripts/validate-svg-photo-portability.py` blob `73349924ce6b32262695ff0075ff5d5315b1b912` requires four embedded X05 final-frame images.

## How to close precisely

1. Find an Actions run whose `head-sha.txt` is **newer than all five current SVG revisions**; the branch's [automated evidence ledger](visual-one-shot-evidence.md) records the source commit and run URL.
2. Require 5 successful source validation scripts, **34** semantic-source cases, and complete 66/82/55 galleries with 203 source-hashed native PNGs.
3. Open the **five current** phone PNGs at native 401×890. Compare X05 p22 hero with selected aperture and final-state critical reel architecture; compare X08 and X10 badge legibility; compare X13 original p12 versus cropped p12c geometry. Do not confuse a successful SVG render with crop/contrast sufficiency.
4. Update exact reviewed source SHAs, reviewer status and remaining defects in this ledger. Maintain **draft / not owner approved** status even after a positive optical check. Defer Android Photo Picker, OEM target/crop, gestures, TalkBack, font scaling, Doze, device testing and the 60-question audit to their own gates.

If the opt-in workflow does not run, leave all five independent-artifact entries **pending**. Do not relabel the earlier 203 images as current.
