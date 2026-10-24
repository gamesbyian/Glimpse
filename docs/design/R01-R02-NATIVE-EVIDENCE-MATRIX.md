# Native reference comparison matrix for R01 and R02

**Purpose:** Turn the first scored audit findings into a finite, actionable Android review packet, rather than treating source SVG output as runtime evidence. This matrix does not claim a new, current-design acceptance test was performed. **A historical genuine Android screenshot/hierarchy gallery does exist**: [main emulator run 39926620822](https://github.com/gamesbyian/Glimpse/actions/runs/39926620822) captured seven 1440×3120 provisional first-slice screens (3-photo, 22-photo Reel/Grid, 200% text), but its screenshot content is not independently inspected here and its rectangular pseudo-reel predates the photographic design. See [provenance and limitations](../quality/NATIVE-ATLAS-DISCOVERY-2026-10-24.md).

| Dataset / starting state | Source reference | Native observation required | Must be true |
| --- | --- | --- | --- |
| N=0, no selection | `r01-photo-empty-0.svg` | First render, Add Photos, keyboard/focus | Five blank paper apertures; no selected/hero image; no phantom IDs; Add Photos is focusable and accessible |
| N=1, selected p01 | `r01-photo-single-1.svg` | Initial launch, Previous/Next, tap centre | One genuine photo used in hero and centre wedge; four empty openings, no wrap or duplicate collection entries |
| N=3, selected p02 | `r01-photo-three-3.svg` (source-derived photographed paper-wheel draft) | Drag both ways, select each ID, interrupt | p01–p03 all reachable; no synthesized image; finite boundaries |
| N=5, selected p03 | `r01-photo-five-5.svg` | Tap five apertures, swipe, restore focus | All five exact IDs reachable; no wrap unless user explicitly chooses it |
| N=22, first p01 | `r01-photo-first-22.svg` | Initial first boundary, Next and disabled Previous, Grid return | Three distinct valid paper openings at p01/p02/p03, two leading holes empty; no wrap; focus and hero p01 agree |\n| N=22, selected p12 | `r01-photo-rest-22.svg` | First draw before interaction, thumb hit bounds, TalkBack traversal | p12 hero/wedge same source; 5 nearby photos distinct; visible 12/22; no movement needed to fill disc |
| N=22, drag 18° | `r01-photo-drag-18.svg` | Touch and stop halfway / cancel pointer | Paper wheel moves; pointer and hero stay fixed; selected ID still p12 until settle |
| N=22, snapped p13 | `r01-photo-snap-13.svg` | Snap and capture settled frame | p13 art identity matches former neighbour p13; 13/22; previous/next and Grid choose same stable ID |
| N=22, last p22 | `r01-photo-last-22.svg` | Select last through swipe and Next, then attempt Next | Last image distinct from p13, no phantom p23/24; Next disabled; no automatic wrap |
| N=60, p01/p30/p60 | `r01-photo-sixty-60.svg` | Random seek, long drag, Grid roundtrip, background/resume | All 60 indices reachable; stable IDs; no five-image cap or O(N) rebuild on every wheel tick |
| 320×640 short phone | `r05-photo-reel-narrow-short-320.svg` | Native short/narrow capture incl real status/nav insets, keyboard/focus at default and 200% font | Hero/finite wheel, Grid/Previous/Next and bottom system navigation all usable without text clipping or off-screen touch targets |\n| R02 crop p12 | `r02-crop-pending.svg` | Open editor, crop, cancel and reopen | Preview transform visible; original source bytes intact; no p12c record before acknowledged Save |
| R02 saved p12c | `r02-copy-saved.svg` | Save a copy, foreground/restart, inspect source+copy | Both records survive; p12c is actual changed bitmap; source p12 unmodified; Home last-confirmed p04 unchanged |
| R02 Share choice | `r02-share-choice.svg` | Open chooser, select original and edited on separate passes | Different content URIs and image bytes; exact relevant photo thumbnails; truthful recipient outcome |
| R02 Return/Dismiss | `r02-return-dismiss.svg` | Dismiss Sharesheet, share to external provider, return with/without result | Focus returns consistently; no made-up delivered status, no wallpaper apply; saved copy persists |
| System differences | Actual Android screen recordings | Tested target/SDK, OEM, launcher, text at 200%, contrast, reduced motion, font/display size | Insets do not eat pointer or controls; buttons meet 48dp where native guidelines apply; TalkBack announces selected index/total and selected vs last applied |

## Evidence bundle contract

For each test record: GitHub build SHA, APK SHA-256, device manufacturer/model/API and display size, reduced-motion/font scale, photo dataset seed and stable asset map, expected state transition, actual screenshot PNG and accessibility hierarchy XML, recording if motion matters, observable result, and reviewer disposition. Store native screenshots separately from SVG proposal rasters and identify them unmistakably as runtime captures.

Re-use the repository's debug-only deterministic dataset and screenshot/hierarchy collector; do not fabricate test evidence by rendering a vector into a simulated Android bezel. Attach proof to the affected GUX question and R01/R02 revision entry, retaining the first audit baseline.

## Acceptance interpretation

A **source-level design** pass requires current-source-hashed visuals and the reviewer to inspect every comparison at mobile scale. A **native implementation** pass additionally requires Android proof for the corresponding behaviour and accessibility path. An **owner acceptance** pass requires an actual owner decision, separate from both automated checks. An unknown or failed native result must remain unverified, never silently relabelled successful.
