# Glimpse native product build plan
**Authoritative implementation sequence · 26 October 2026 · execution begun**

## Why this plan exists

**Canonical original reference images:** [owner reference archive](../../design/reference/owner-2026-10-26/README.md), [Board A](../../design/reference/owner-2026-10-26/glimpse-physical-reels-and-screen-concepts-a.jpg), [Board B](../../design/reference/owner-2026-10-26/glimpse-physical-reels-and-screen-concepts-b.jpg). Original JPG bytes and Git blob identities are preserved on `main`. These are the primary *aesthetic* comparator for the art/reel work. They do not supersede actual user-workflow contracts or grant approval to invented controls.



The owner likes the broad direction of the two October 26 concept boards: **convincingly physical pale paper photo reels; partially offscreen, preferably at a lower edge or corner; asymmetric compositions with room for more than one reel when they have distinct jobs; stronger editorial typography, richly photographed hero imagery, artful deliberate UI**. This is an **approved design direction for prototyping**, not permission to ship every pictured control or claim either board is a pixel-perfect implementation reference.

Previous exploratory SVGs are too generic and too diagrammatic. The existing Android prototype has real imported photos, offline copies, a five-aperture circular *schematic*, Reel/Grid browsing, and an explicit Home wallpaper gateway. The owner physically tested an earlier APK and imported 22 photos. **Do not replace or break this working vertical slice while building the new design.** The first job is to make the reel real.

Authority for semantics remains: [current decisions](current-decisions.md), [critical reel UX](../design/reel-ux-audit.md), [finite reel behavior](../design/reel-interaction.md), [motion/render contract](../architecture/reel-motion.md), [technical side-effect boundaries](../architecture/behavior-contracts.md), [screen contract](../design/screen-contract.json), and [73 workflow audit](../product/feature-audit.md). For implementation facts use live code/tests and [first-slice evidence](../quality/first-slice-evidence.md). The [draft review PR #6](https://github.com/gamesbyian/Glimpse/pull/6) is separate and remains **unmerged/unapproved**.

## Release philosophy: a working beautiful app, not 58 static paintings

1. Deliver **installable debug APKs** after each end-to-end milestone. Do not call a CI-built APK human-accepted or physically measured.
2. One canonical finite selection model (stable photo/collection IDs) and one serialized Android WallpaperManager command gateway. UI animations have **zero wallpaper side effects**. A glanceable display of *previewed*, *requested/pending*, *last Glimpse-acknowledged applied*, and *Android system state unknown* must never be conflated.
3. Original files immutable; provider grants may expire; importing makes verified durable private copies with SHA dedupe, then maintains collections offline. Android-native picker, Sharesheet and supported URI grants, no assumed Google Photos album sync.
4. Use Compose/HWUI/Skia, Room, DataStore as needed, Coil/bounded decode when justified; avoid introducing Flutter, a WebView-based app, arbitrary reel-per-setting behavior or a second scheduling engine.
5. Build **personality into the reusable components**, not just splash screens. Accessible labels/standard actions always accompany a reel. All critical functions must work with no gesture, haptics or animation.
6. The concept-board imagery is *visual reference, not source assets with proven licensing*; acquire or author actual versioned production **raster material maps / alpha-cutouts / optional rim occlusion** before claiming tactile fidelity. Do not crop a screenshot of a mockup with phone chrome into a runtime UI.

## Product UX contract: what the reel actually does

### Core, ship first

- **Home**: rich current/last-acknowledged wallpaper hero; partial **collection-entry crescent**, with a clear entry to the current collection and a named/tappable alternative. Small reel is not a precision drag-wheel. Separate **Change wallpaper**, **Pause/Resume**, **Share** and best-effort next eligibility. No fake current Android wallpaper readback.
- **Collection browser / photo reel**: large realistic five-aperture disc cropped below/side of the screen, selected image displayed in a **stationary hero**. The reel navigates a finite ordered list of **actual `PhotoRecord.id`**s; count/position, focus marker, Previous photo, Next photo, View all (Grid), Set as wallpaper and destination confirmation remain readable outside the moving reel. At 0/1/2/3/4 items, empty slots remain empty; no phantom duplicates.
- **Grid**: first-class ordinary performant grid with the same stable selected ID, filter/search/selection as that feature matures. Switching between Reel and Grid preserves focus and order. Never replace bulk management with ornamental paper disks.
- **Collections landing**: named cards with true counts, add/import actions and optionally partial **passive reel cover** of actual collection photos. If later promoted to a selector, make role/consequence explicit.
- **Photo detail / editor**: large undistorted image, explicit crop/rotate/share/apply actions; an optional *same-domain* photo reel can select adjacent assets while editing state remains independent. Reversible original-vs-recipe handling.
- **Sharing / settings / automation**: native semantic controls and platform Sheets. No fake provider reels, icon-only settings wheels or scheduling clocks.

### Meaningful simultaneous reels: conditional, not required in first APK

The preferred experiment is **small upper-right collection reel** (selects `CollectionId`) + **large cropped lower-left photo reel** (selects `PhotoRecord.id` in that collection), separated from the stable hero. Parent change rebases child domain and preserves/restores that collection's last selected photo. Neither selection applies a wallpaper or starts a schedule. A named collection chooser + one real photo reel is the control comparison. At 320dp/200% font scaling show **one** active full interactive reel; secondary becomes a labelled chooser. Test first-click comprehension and accidental parent changes before defaulting to two reels.

Home-vs-Lock dual reels, bounded recent-history reels and crop-variant reels are **optional hypotheses** after their corresponding domain/state/OS capabilities are implemented. Source/provider, search-facet, Settings, permissions and Share reels shown in the explorations are **not approved feature additions**.

## Art and interaction: physical object, not vector cosplay

**Physical material production** is a first-class workstream. Design an object-quality material package with:
- off-white compressed paper/card stock with physically convincing fiber/grain at native pixel densities, at least two local-lighting variants, recessed bevels around the five broad image windows, cast shadows, edge thickness, and restrained wear;
- a crisp, memorable hub/fastener and a single fixed indexing marker; no View-Master trademarks or trade dress copied verbatim;
- **large genuine-photo apertures**, separate from the static paper-face/rim material, correct natural aspect/crop and mask antialiasing, with stable identity across focus changes;
- a normalized geometry manifest for art dimensions, five mask window positions, safe interactive clip zones, angular direction/72° pitch, DPI/scale variants, embedded credits, actual provenance and SHA digests;
- expressive but legible licensed type: a distinctive editorial display face for titles and a high-legibility companion sans for status/action text. Ship font licence alongside exact binary; system fallback until font procurement.

No costly per-frame procedural grain, full-library bitmap wheel, false 3D physics, continual shader animation or unmeasured heavy blur. The rendered disc and inset image layer should rotate cohesively around an anchored hub, with fixed pointer and hero. Cache paths, paper material, downsampled nearby thumbnails, and use HWUI/Compose draw-phase transforms. Measure on a **real Pixel 6 Pro** before claiming smoothness.

**Motion states:** Idle → Dragging(pointer) → Settling(selected stable ID, legal notch) → Rebase hidden slots → Idle, with cancellation/re-targeting on fresh input and collection changes. Provisional ease-out/spring about 220–320ms, 72° per notch, one haptic per completed notch; force instant semantic-equivalent updates in reduced motion/Battery Saver. Drag must not steal Android back-edge/system gestures. Previous/Next/keyboard/Switch/TalkBack equivalent controls remain fixed.

## Ordered milestones and release gates

| Stage | Concrete deliverable | Verification/evidence | What's expressly not included yet |
|---|---|---|---|
| **R0 in progress** | Reconcile authoritative reference; create the canonical build plan, isolate an Android motion branch from draft review PR #6; pure bounded physical reel-step rules + tests | Docs/branch/PR, JVM tests, CI debug APK | Image-source art approval, physical motion acceptance |
| **R1 functional reel** | Correct index→slot mapping with visible *actual disc rotation*, interruption, drag/tap, stable hero and fixed actions; finite endpoints; no preview/apply side effect | Deterministic 0/1/2/3/5/22/60 photo tests; screenshot emulator; real phone swipe/reverse/resize and 200% text observations | Textured object-quality art if exact materials not yet signed off |
| **R2 reel material + typography** | Versioned rendered paper material/alpha maps with provenance, object-level depth, photographed/approved image-window treatment, editorial typography; actual source-to-device side-by-side | Full-resolution source hashes, 1x/2x/3x atlas, real phone screenshot + optical review, performance traces | Human whole-product approval |
| **R3 first polished vertical slice** | Home/Collections/Photo Reel/Grid/Detail share single IDs and richer art direction, persistent selection; active collection entry; import offline; explicit Home apply and honest visible rotation | Pixel owner flow: import real 22+ photos, Reel↔Grid↔Detail, preview/apply, pause/change, restart, offline, A11y non-gesture | Guaranteed background five-minute changes, blanket album sync |
| **R4 real photo tools** | Screenshot ACTION_SEND/ClipData/picker intake, rotate/crop reversible recipes, original/crop share via Android Sharesheet/FileProvider, clipboard fallback and rights/provenance | Crash/interruption/dedup/restart, exact outgoing bytes, no source destruction, external target tests | Undocumented Lens intent guaranteed |
| **R5 wallpaper automation + integrations** | Robust state journal and idempotent background eligibility, reboot/process recovery, Home/Lock/Both probes, Glance/Quick Settings/shortcut where supported, rules/sequence previews | Device-specific Doze and OEM trials, "eligible/attempted/applied/unknown" traces; Android system limits disclosed | Exact-time execution guarantee |
| **R6 daily-driver hardening** | Accessibility, 320dp/200% font, focus return, keyboard/Switch/TalkBack, safe-area/edge gestures, low-memory performance, storage and backup/recovery, controlled signing | Independent native/emulator and physical-device regression, current screenshots vs accepted target, distribution APK signature and provenance | Claim of Play Store publication without actually publishing |

**Priority order:** deliver a native **R1/R2 reel APK** to the owner before extending noncritical workflow breadth. Focus engineering on what failed the first physical test: generic aesthetic and nonphysical pseudo-reel, *while preserving successful import/apply*. Work ahead during CI, but don't conflate build green with a person approving tactility.

## Independent workstreams and integration policy

- **A · art and reel engineering:** owns the canonical renderer/motion state, physical source/mask package and typography/theme. Owns exact screenshots/object acceptance. No second reel implementations.
- **B · UX/journeys/accessibility:** owns navigation compositions, photo/Reel/Grid/collection continuity, real actions, support states and accessibility semantics, later reversible editing/share.
- **C · native reliability:** owns Room import/retention, Android wallpaper command, scheduling, chooser/Photo Picker, performance and real Android instrumentation.
- **Integration owner:** live `main` workboard, CI/APK artifacts, PR conflict resolution, authenticity/rights checks, freeze/evidence and release gating. No agent may declare owner approval or merge master design PR #6.

One agent owns each file/contract. Send domain/UI API changes across issue links before modifying another owner's code. Prefer small scoped PRs and focused CI. Keep native delivery distinct from the still-**draft** owner visual reviewer and its 8 decisions.

## Acceptance checklist for the very next APK

- Reel is recognizably circular/cropped, occupies a meaningful part of screen, initially fills all available real photo slots, and physically moves when browsing. Fixed pointer and true `x of N` are visible.
- User can select an arbitrary one of 22 images in a collection, go backward and forward, handle first/last and empty/singleton, and jump to Grid with **same selected photo**.
- User can interrupt a settle and issue a new request without double-advancing, visible slot changing photo identity mid-window, or losing the selected ID.
- No gesture, reel tap, Grid selection or preview changes Android wallpaper. Explicit labelled Apply uses **existing serialized gateway**.
- No invented repeat-photo fill, fictional Google Photos live sync, success toast before acknowledgement, impossible Android background guarantee, or UI overflow at large type.
- Pass bounded pure tests and native compile; publish installable debug APK. Follow with **physical Pixel observation**, not "approved" inferred from emulation.

## Sources, decisions and open design evaluation

The owner's two concept boards (October 26 conversation uploads) establish **artistic direction**. They also depict unsupported category/provider/tool reels and some false OS UI controls; those are **not requirements**. Transfer the physical reel craftsmanship, clipped composition and art-directed photography/typography, not every caption/icon/function. Actual source asset publication, font rights and exact viewport captures remain to be arranged. No repository design is marked owner-approved merely because it resembles these boards.

**Now:** commit this plan; implement and test a pure finite reel-motion decision layer and wire visual notch motion into the current circular prototype; open a scoped draft PR and preserve current Android workflow results. Then author/verify production material assets and deliver the revised installable APK for physical review.
