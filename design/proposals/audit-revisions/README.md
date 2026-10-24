# R01: first audit-driven finite physical reel comparisons

**Draft, not approved.** Trace to GUX-02, GUX-03, GUX-06, GUX-07, GUX-09 and GUX-32. The purpose of these six drawings is to make **selection/index geometry** inspectable before stylistic design acceptance. The existing X05 photographic five-sector reel remains the stronger tactile surface reference; these new circle-aperture studies do **not** replace it or meet the owner’s final paper-reel expectations on their own.

## Source pairings

| Scenario | New vector proposal | Comparison reference | New evidence needed |
| --- | --- | --- | --- |
| Settled 22 | [idle-22](r01-idle-22.svg) | [X05 01 rest](../storyboards/x05/01-rest.svg) | Native 22-item reel and focus data |
| Mid-gesture 18° | [drag-18](r01-drag-18.svg) | [X05 03 drag](../storyboards/x05/03-drag-18.svg) | Gesture angle and source-ID continuity |
| Snapped item 13 | [settled-snap](r01-settled-snap.svg) | [X05 05 snap](../storyboards/x05/05-snap-72.svg) | Settled-ID and Grid parity |
| Final item 22 | [last-22](r01-last-22.svg) | [X05 08 last](../storyboards/x05/08-last.svg) | Next disabled, empty neighbours and p22 architecture continuity |
| Empty 0 | [empty-0](r01-empty-0.svg) | [critical empty reel](../critical-reel/reel-empty.svg) | No fake photos, distinct Add photos recovery |
| One item | [single-1](r01-single-1.svg) | [X06 02 single](../storyboards/x06/02-single.svg) | Four empty apertures and one reachable image |

**Position policy encoded:** fixed pointer identifies the top aperture, ordered clockwise: selected ID `s`, `s+1`, `s+2`, `s-2`, `s-1`. Any value outside 1..N becomes visibly empty, never an invented wrapped photo. Note the illustrative five round windows here remain provisional versus the wedge-shaped paper photo openings in X05: reconciling those two treatments is a visual choice for the next full proposal round and owner comparison, not a technical assumption to silently merge. At drag-18 the selected ID remains p12; after snap it becomes p13.

## Acceptance requirements before R01 closes

- Source-hashed native-sized PNGs for every new drawing, with no wholly empty or failed render; Android screenshots stay separate.
- Full ring/wedge treatment must visibly communicate a **circular paper disc** with tangible rim/hub, meaningful crop, fixed pointer and five large photo windows, rather than five isolated ordinary circular image chips.
- One stable ID model must work at N = 0, 1, 3, 5, 22, 60, with positive proof from native code and pixel inspection that p22 is accessible and Next is disabled at the actual last item.
- The selected photo in the stationary hero must be the same physical photo in the focused aperture, not merely the same caption ID.
- Gesture interruption, reduced-motion route, previous/next alternate controls and TalkBack focus must have independent observations.
- New native/reference comparison must be tested on handset before any audit findings may become source-level PASS for runtime behavior.

**Current disposition:** six source-editable first-pass state sketches committed; validator exists; optical and native equivalence remain separately gated.
