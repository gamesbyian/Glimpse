# R02 selection identity: pre-audit continuity packet

**Draft source study · 2026-10-23.** [Exact eight-step manifest](r02-identity-chain.v1.json), existing [X07 reel/grid/detail](../storyboards/x07/01-reel-focus.svg), [X11 editor](../storyboards/x11/01-inbound-original.svg) and [X13 sharing](../advanced-storyboards/x13/01-original.svg).

## Precisely defined transitions

1. Reel selects **p12** from 22, without changing last-confirmed Home **p04**.
2. Grid opens with p12 selected; returning from Grid or Detail retains p12 and its collection identity.
3. Crop editor begins with source p12. Cropping, rotating and adjusting its preview **does not destroy p12 or silently replace the selection ID**.
4. Explicit **Save edited copy** creates a distinguishable derivative **p12c**, with persisted source/derivative link and actual image pixels transformed. The source must still be available independently.
5. Share original uses p12; share edited copy uses p12c. A successful handoff to Android Sharesheet does not confirm that the recipient received or kept the asset.
6. Return to Glimpse retains a valid selected image and does not infer Home was changed. Applied result remains separately labelled.

## Important evidence limit

The existing X07 source drawings encode matching `selectedAssetId=p12`, X11 crop also encodes `p12`, and X13 edited-share specifies `p12c` with a clipped crop transformation. This is a **cross-document logical trace, not a continuous real Android sequence**, and by itself does not show a concrete persisted Save transaction between the X11 and X13 scenes. That missing transition is the next required R02 revision mockup. Native Photo Picker/Share handlers need independent real proof.

## Required new proposal sequence before R02 scores may change

| Order | Planned mockup | User-visible action | Evidence requirement |
| --- | --- | --- | --- |
| 01 | p12 selected Reel | Enter Grid | Hero and focused aperture same source photo and selected label |
| 02 | p12 selected Grid | Open photo | Exact same image content and count |
| 03 | p12 Detail | Edit | Retain p12 and stable collection ID |
| 04 | p12 live Crop | Save edited copy | Source preserved; in-flight edit not presented as permanent |
| 05 | Save acknowledged | Offer original p12 and edited p12c side-by-side | Transform and derived ID must be genuine |
| 06 | Android Sharesheet original | Choose original | Real Android handoff represented honestly; never fake third-party destination |
| 07 | Android Sharesheet edited | Choose edited copy | Distinct actual URI and pixels |
| 08 | Return/Dismiss | Back to exact photo | No wallpaper apply, no false delivery confirmation |

**Priorities:** Native screenshot/selection traces, optical paired comparison, successful and failed save, URI lifetime/offline outcome, interrupted intent return, keyboard and TalkBack focus. New drafts remain unapproved; avoid raising audit PASS counts merely for manifest completion.
