# Glimpse owner-supplied visual reference boards · 26 October 2026

These are the **two original JPG reference boards supplied by the owner** and moved here **without re-encoding or editing a pixel**. They originally appeared as automatically named files at the repository root. The simple A/B naming follows the root filename order, not an assertion about which conversation attachment corresponds to either board.

| Board | Current asset | Original root filename | Git blob SHA | Bytes |
| --- | --- | --- | --- | ---: |
| A | [Physical reels and screen concepts A](glimpse-physical-reels-and-screen-concepts-a.jpg) | `file_00000000b87481fb95f13fc5b502db89.jpg` | `a2db8b2f1d5abc5a617c0dbadde3aa04b73d44cd` | 514,564 |
| B | [Physical reels and screen concepts B](glimpse-physical-reels-and-screen-concepts-b.jpg) | `file_00000000eeec81f58f289880d667c79c.jpg` | `92f9cc8e4bb45de7df405c5b778b8542c3e9e809` | 540,117 |

## What these images mean

The owner accepted the **art direction**, including a convincingly physical light-cream reel, textured paper/card/plastic, recessed photo windows, rim and hub detail, natural lighting and shadows, stylish editorial type, rich imagery, partially off-screen reels docked toward a corner or lower edge, and **multiple simultaneously visible reels only when each is genuinely functional**.

**They are not a screen-by-screen implementation specification or pixel-perfect owner approval.** Illustration captions/controls are subordinate to the technical app contracts. In particular, do not implement a provider reel in an Android-owned Sharesheet, decorative Settings/category/tool wheels, uncommitted places/people/time recognition features, or claims of cloud-provider capabilities that the native spec does not support.

The first independently meaningful two-reel **experiment** is collection selection (a small reel or conventional chooser) plus finite photo selection (a large reel). The same real `CollectionId` and `PhotoRecord.id` must drive ordinary accessible equivalents. Changing either selection must never silently apply wallpaper.

## Use and asset rights

Treat the JPGs as **owner-supplied concept references** for art direction, composition, comparative review, and documented implementation decisions. Do **not** extract copyrighted-looking photos, unlicensed fonts, icons, or illustrations directly into the shipped APK. Create/obtain separately traceable and licensed production resources with their own provenance, SHA-256 and device acceptance.

Further authority: [native build plan](../../../docs/planning/BUILD-PLAN-2026-10-26.md) (currently under development in PR #27), [reel UX contract](../../../docs/design/reel-ux-audit.md), [reel interaction contract](../../../docs/design/reel-interaction.md), and the [current product decisions](../../../docs/planning/current-decisions.md). The design reviewer in PR #6 remains unapproved.
