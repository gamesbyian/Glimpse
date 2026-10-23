# Mobile app UI/UX condensed-wisdom synthesis, weighted for Glimpse (2026)

**Date:** 2026-10-23. **Status:** External research synthesis and **audit preparation only**. No Glimpse design proposals were scored, accepted or rejected in this document. Read alongside [current decisions](../planning/current-decisions.md), [existing visual gap audit](../design/reference-gap-audit.md) and [human expectations](../design/human-expectations.md); this inventory is advisory, not a competing product contract.

## Research purpose and method

Collected **public checklists, top-10 lists, do/don't guidance, platform specifications, heuristics and government accessibility assessment frameworks** across English, French, Spanish, Portuguese, German, Japanese, Korean and Chinese sources. Compared primary Android guidance and international accessibility standards against practitioner lists; then deduplicated advice into discrete, falsifiable review principles. Some practitioner pages concern responsive websites rather than native apps and were only used for transferable principles. Apple guidance is comparative, not an Android requirement. MDN translations are **not** counted as independent corroboration. Individual numerical UX marketing claims are not adopted without credible validation.

This is a broad *targeted search*, not a formal systematic literature review or a guarantee of exhaustive retrieval. The sources and any future later additions should be rechecked when the audit is actually run.

## Weighting (relevance, not quality or pass/fail)

| Score | Meaning | Treatment at the future proposal audit |
|---|---|---|
| **5** | Central to Glimpse's distinctive core, user safety, accessibility or platform truth | Mandatory design review, high priority evidence |
| **4** | Strongly relevant to routinely used or supported journeys | Include in core audit, conditional evidence as relevant |
| **3** | Useful with qualifying workflows or greater product maturity | Review if supported or proposed, otherwise defer clearly |
| **2** | Low-frequency, future form factor or marginal polish | Optional, cannot displace core issues |
| **1** | Speculative future product changes outside current remit | Record as horizon item only |
| **0** | Inapplicable or contrary to current Glimpse product boundaries | Explicitly exclude from audit |

Weights express the **relative importance for Glimpse**, not probability, design acceptance, compliance, or strength of source evidence. A score-5 item cannot be considered satisfied without seeing the eventual proposal and appropriate behavior/evidence. Items with conditional provider or OS features remain conditional even at score 5.

**Coverage:** 174 distinct suggestions. Weight distribution: 0: 3, 1: 3, 2: 8, 3: 24, 4: 57, 5: 79. Reference labels link to the bibliography below.

## Generalized principle inventory

### 01. Goals, agency and information hierarchy

| ID | Glimpse weight | Generalized rule or goal | Why it matters specifically to Glimpse | Sources |
|---|---:|---|---|---|
| A01 | **5** | Keep the primary user job recognizable at first glance. | Glimpse should communicate browse, apply and rotate without needing knowledge of the reel metaphor. | [H1](#h1), [P11](#p11) |
| A02 | **5** | Choose hierarchy from user intent, not subsystem boundaries. | Image, active collection and rotation state should outrank internal command mechanics. | [P11](#p11), [H1](#h1) |
| A03 | **5** | Place the next sensible action beside the object it affects. | Collection-specific actions should stay near that collection or selection. | [P02](#p02), [H1](#h1) |
| A04 | **5** | Distinguish exploration from commitment. | Looking through photos must be safe before any wallpaper-changing action. | [P11](#p11), [H1](#h1) |
| A05 | **5** | Offer clear escape, backtracking and reversal. | Users should feel free to explore without fear of changing the device. | [H1](#h1), [P11](#p11) |
| A06 | **5** | Reserve strongest visual emphasis for the consequential action. | Apply and start/pause deserve clarity; decorative accents do not. | [P02](#p02), [P11](#p11) |
| A07 | **4** | Present a quiet default and progressive complexity. | Daily shuffle should not demand automation rule authoring. | [H1](#h1), [M5](#m5) |
| A08 | **4** | Prefer recognition over memory of mode and action meanings. | All modes and current selections need on-screen context. | [H1](#h1), [M2](#m2) |
| A09 | **4** | Make meaningful actions fast for returning users. | The most frequent wallpaper operations should be promptly reachable. | [H1](#h1), [P11](#p11) |
| A10 | **3** | Let experienced users customize efficiency without requiring customization. | Shortcuts, widgets and presets remain optional accelerators. | [H1](#h1), [P02](#p02) |

### 02. Information architecture and navigation

| ID | Glimpse weight | Generalized rule or goal | Why it matters specifically to Glimpse | Sources |
|---|---:|---|---|---|
| B01 | **5** | Use stable primary destinations with intelligible names. | Home, Discover, Collections and Automation have distinct purposes. | [P02](#p02), [H1](#h1) |
| B02 | **5** | Preserve current image and scroll/context when switching views. | Reel/Grid and detail return should not reset a user's place. | [P11](#p11), [H1](#h1) |
| B03 | **5** | Provide an obvious, reliable system Back path. | Deep links, dialogs, picker returns and detail screens must unwind correctly. | [P02](#p02), [P11](#p11) |
| B04 | **5** | Reveal rather than conceal frequently needed navigation. | Grid/View all and selection navigation should be easy to spot. | [H1](#h1), [P02](#p02) |
| B05 | **5** | Avoid making a gesture the sole route to any essential action. | Reel browsing must also work through buttons and Grid. | [P01](#p01), [P08](#p08) |
| B06 | **4** | Keep navigation hierarchy relatively shallow where practical. | Do not force a multi-step trip to start a collection shuffle. | [M4](#m4), [H1](#h1) |
| B07 | **4** | Use location, labels and selection state as orientation cues. | People should always know which collection and photo they are viewing. | [H1](#h1), [M2](#m2) |
| B08 | **4** | Place rare commands in a consistent, discoverable secondary area. | About, diagnostic details and destructive settings can be secondary. | [P02](#p02), [P11](#p11) |
| B09 | **4** | Give destinations distinct content rather than duplicate dashboards. | Collection curation, browsing and current wallpaper status have separate goals. | [P02](#p02), [H1](#h1) |
| B10 | **3** | Provide search, filtering and sorting when scale makes browsing cumbersome. | Critical as libraries grow, less critical for the first 22-photo scenario. | [M1](#m1), [M4](#m4) |
| B11 | **3** | Explain navigation transitions with continuity rather than arbitrary resets. | Reel to detail, crop and back should preserve image identity. | [P11](#p11), [H1](#h1) |
| B12 | **2** | Adapt the number and format of nav affordances to form factor. | Tablet navigation rail may matter later; handset baseline comes first. | [P02](#p02), [P03](#p03) |

### 03. Gestures, direct manipulation and tactile metaphor

| ID | Glimpse weight | Generalized rule or goal | Why it matters specifically to Glimpse | Sources |
|---|---:|---|---|---|
| C01 | **5** | Every unusual control needs an obvious actionable affordance. | A reel must look draggable/selectable rather than static decoration. | [P04](#p04), [H1](#h1), [M7](#m7) |
| C02 | **5** | Keep a one-to-one mental model between visual selection and underlying item. | The fixed pointer, hero and selected photo ID must agree. | [H1](#h1), [P11](#p11) |
| C03 | **5** | Never let browsing implicitly apply a wallpaper. | Different state transitions require different, explicit controls. | [P11](#p11), [H1](#h1) |
| C04 | **5** | Use a genuine finite position and meaningful collection ends. | Five apertures do not mean five items or an infinite library. | [H1](#h1), [M2](#m2) |
| C05 | **5** | Support drag-free equivalent operations. | Previous, Next, Grid and accessibility actions are necessary. | [P01](#p01), [P08](#p08), [P09](#p09) |
| C06 | **5** | Handle interruption, reversal and rapid repeated input predictably. | Motion should settle without skipping, jumping or queuing unintended actions. | [P04](#p04), [H1](#h1) |
| C07 | **5** | Avoid input collisions with system edge-back and vertical scrolling. | The large clipped disc cannot claim gesture navigation insets. | [P03](#p03), [P04](#p04) |
| C08 | **5** | Make tap-to-select versus drag-to-rotate unambiguous. | A finger action should have one predictable consequence. | [P04](#p04), [H1](#h1) |
| C09 | **5** | Make the reel earn its space and mechanical complexity. | It must improve image exploration, not merely display branding. | [P11](#p11), [H1](#h1) |
| C10 | **4** | Communicate the focus point, direction and result after each gesture. | Stationary hero and photo count can reduce uncertainty. | [H1](#h1), [M7](#m7) |
| C11 | **4** | Provide appropriately restrained tactile/visual response. | Haptics can mark notches, but cannot be sole information. | [P01](#p01), [M7](#m7) |
| C12 | **4** | Keep multiple browsing modalities behaviorally equivalent. | Grid, controls, TalkBack and reel should select the same photo. | [P01](#p01), [P10](#p10) |
| C13 | **3** | Do not force a literal physical metaphor where ordinary controls are clearer. | Settings and permissions need no novelty reel. | [P11](#p11), [H1](#h1) |
| C14 | **3** | Ensure the metaphor works at small and large collection sizes. | Empty, 1, 3, 5, 22 and large-count conditions need understandable states. | [M1](#m1), [H1](#h1) |

### 04. Visibility of state and truthful feedback

| ID | Glimpse weight | Generalized rule or goal | Why it matters specifically to Glimpse | Sources |
|---|---:|---|---|---|
| D01 | **5** | Differentiate selected preview from installed wallpaper. | Glimpse has at least two image identities with different consequences. | [H1](#h1), [P11](#p11) |
| D02 | **5** | Distinguish request, pending, confirmed, failed and unknown outcomes. | Android wallpaper commands may outlive the immediate gesture. | [H1](#h1), [P07](#p07) |
| D03 | **5** | Make active, paused, stopped and delayed rotation visually distinct. | People should not infer that a schedule is active from a selected interval. | [H1](#h1), [P11](#p11) |
| D04 | **5** | Express timing as eligible versus actually applied where appropriate. | Five-minute preference is not an exact sleeping-device guarantee. | [P07](#p07) |
| D05 | **5** | Keep count and position truthful. | Show real item totals and focused position even when the wheel is clipped. | [H1](#h1), [M1](#m1) |
| D06 | **5** | Give feedback quickly without claiming success prematurely. | A transient pending state is preferable to false confirmation. | [H1](#h1), [P11](#p11) |
| D07 | **4** | Separate persistent status from short-lived acknowledgments. | Rotation state belongs somewhere stable; snackbars suit minor events. | [P02](#p02), [H1](#h1) |
| D08 | **4** | State what changed and what did not change after important actions. | Importing photos does not apply or activate them automatically. | [P11](#p11), [H1](#h1) |
| D09 | **4** | Show progress for work whose duration is noticeable. | Cloud-backed import/copy, crop and bulk work should show progress. | [M1](#m1), [P05](#p05) |
| D10 | **4** | Prevent status from becoming obsolete across navigation and process restart. | Reopen should show actual collection and apply state. | [H1](#h1), [P07](#p07) |
| D11 | **4** | Report partial success honestly. | Imported, duplicate, failed and skipped counts should be explicit. | [H1](#h1), [M1](#m1) |
| D12 | **3** | Use unobtrusive confirmations for low-risk, reversible actions. | Avoid blocking dialog chains for every selection. | [P02](#p02), [H1](#h1) |

### 05. Prevention, failure and recovery

| ID | Glimpse weight | Generalized rule or goal | Why it matters specifically to Glimpse | Sources |
|---|---:|---|---|---|
| E01 | **5** | Prevent accidental destructive consequences. | Deleting private snapshots or changing device state demands clear intent. | [H1](#h1), [P11](#p11) |
| E02 | **5** | Protect original photos by default. | Crop, rotation and export must not silently overwrite source media. | [P05](#p05), [P11](#p11) |
| E03 | **5** | Keep the last known-good wallpaper when a change fails. | Failed apply should not blank the device or corrupt sequence state. | [H1](#h1), [P07](#p07) |
| E04 | **5** | Offer a meaningful retry for recoverable failures. | Picker/network/import/apply interruptions should have a next step. | [H4](#h4), [H1](#h1) |
| E05 | **5** | Name the actual failure rather than showing an opaque code. | Users need to distinguish denied permission, missing source and OS refusal. | [H4](#h4), [H1](#h1) |
| E06 | **5** | Do not silently swallow unknown device outcomes. | An uncertain wallpaper result should say uncertain, not successful. | [H1](#h1), [P07](#p07) |
| E07 | **4** | Provide Undo when safe, and explicit confirmation when Undo is impossible. | Removing from Glimpse differs from deleting the user's original. | [H1](#h1), [P11](#p11) |
| E08 | **4** | Keep cancellation safe during multi-item work. | No phantom imported items or orphan status should remain visible. | [H4](#h4), [P05](#p05) |
| E09 | **4** | Retain in-progress context following recoverable interruptions. | Unexpected Activity death should not throw away a collection setup. | [H1](#h1), [H4](#h4) |
| E10 | **4** | Design success, failure, empty, offline and partial-result states together. | Happy-path-only drawings leave the interface underspecified. | [M1](#m1), [M3](#m3) |
| E11 | **3** | Use specific, local error text near the affected control. | The problem and correction should not require a separate search. | [H4](#h4) |
| E12 | **3** | Avoid repeated confirmation dialogs for already reversible micro-actions. | Excess interruption raises friction without protecting anything. | [H1](#h1), [P11](#p11) |

### 06. Photographs, collections and image transformation

| ID | Glimpse weight | Generalized rule or goal | Why it matters specifically to Glimpse | Sources |
|---|---:|---|---|---|
| F01 | **5** | Keep source, imported copy, crop and applied result conceptually separate. | These may be four different assets or versions. | [P05](#p05), [H1](#h1) |
| F02 | **5** | Provide a large faithful preview of the exact outcome being chosen. | Wallpaper decisions depend on seeing the actual composition. | [P11](#p11), [M1](#m1) |
| F03 | **5** | Preserve image identity through Reel, Grid, crop, sharing and history. | A user should not lose which photo is being modified. | [H1](#h1), [P11](#p11) |
| F04 | **5** | Explain original-versus-edited consequences before sharing or exporting. | Android share targets should receive the version visibly chosen. | [P05](#p05), [P11](#p11) |
| F05 | **5** | Show the result of import and durable-offline availability. | Cloud-provider selection alone must not imply local persistence. | [P05](#p05), [H1](#h1) |
| F06 | **5** | Make Home and Lock targets explicit where supported. | Destination affects actual device wallpaper, not just thumbnail styling. | [P11](#p11), [H1](#h1) |
| F07 | **4** | Give album and collection names, counts and useful representative imagery. | Users need to distinguish sets at a glance. | [H1](#h1), [M1](#m1) |
| F08 | **4** | Support sensible bulk actions with clear selection scope. | Thousands of images need batch handling without accidental broad edits. | [M1](#m1), [H1](#h1) |
| F09 | **4** | Surface storage consequences at the right time. | Offline snapshots consume device space; estimates should be intelligible. | [P05](#p05), [P11](#p11) |
| F10 | **4** | Separate intentional exclusion from deletion and active-set membership. | A skipped photo may still belong to its saved collection. | [H1](#h1), [P11](#p11) |
| F11 | **3** | Respect aspect ratios, crop safe areas and screen differences. | Home/Lock images may be cropped differently than previews. | [P03](#p03), [M1](#m1) |
| F12 | **3** | Keep discoverable provenance and source/licence context when applicable. | Third-party sources require attribution and provider limitations. | [P11](#p11), [P05](#p05) |

### 07. Accessibility, inclusive interaction and language

| ID | Glimpse weight | Generalized rule or goal | Why it matters specifically to Glimpse | Sources |
|---|---:|---|---|---|
| G01 | **5** | Make every essential flow operable without a complex gesture. | TalkBack, Switch Access and motor-limited users need equivalents. | [P01](#p01), [P08](#p08), [P10](#p10) |
| G02 | **5** | Aim for Android's 48dp touch targets with practical spacing. | Controls near the cropped reel should remain reliably hittable. | [P01](#p01), [P10](#p10) |
| G03 | **5** | Meet legibility and contrast expectations in all states. | Treat 4.5:1 ordinary text and 3:1 meaningful graphics as useful targets. | [P01](#p01), [P09](#p09) |
| G04 | **5** | Scale text without clipping content or obscuring actions. | Reel, cards, dialogs and labels must tolerate increased font settings. | [P01](#p01), [P10](#p10) |
| G05 | **5** | Provide proper accessible names, roles and state descriptions. | Selected/current/applying/paused must be announced correctly. | [P01](#p01), [P10](#p10) |
| G06 | **5** | Make focus order logical and visible after navigation or dialog closure. | Screen-reader and keyboard users need to know where they returned. | [P10](#p10), [M3](#m3) |
| G07 | **5** | Offer reduced-motion mode with equivalent function. | The physical reel cannot require animated rotation to work. | [P12](#p12), [P01](#p01) |
| G08 | **5** | Never encode selection, errors or destination in colour alone. | Selected and applied photo need textual/shape distinctions. | [P01](#p01), [P09](#p09) |
| G09 | **5** | Avoid hiding essential actions behind timed transient affordances. | Users must have time to inspect and act. | [P12](#p12), [P10](#p10) |
| G10 | **5** | Keep clipped and decorative reel elements out of the accessibility tree. | Only meaningful photographs and controls should receive focus. | [P10](#p10), [P01](#p01) |
| G11 | **4** | Ensure dynamic changes are announced without focus hijacking. | Completed imports and wallpaper results need accessible status messages. | [P10](#p10), [P08](#p08) |
| G12 | **4** | Support keyboard, hardware switches, voice and alternate pointer input. | An Android app may be used without direct touch. | [P01](#p01), [P12](#p12) |
| G13 | **4** | Offer meaningful image labels beyond filename when possible. | An unlabeled sequence of identical Photo buttons frustrates TalkBack. | [P10](#p10), [M3](#m3) |
| G14 | **4** | Allow text and controls to reflow rather than shrink indefinitely. | Small displays and large font settings both matter. | [P03](#p03), [P01](#p01) |
| G15 | **3** | Prepare for localization and right-to-left layout even before translations. | Rigid control widths and directions create future barriers. | [P12](#p12), [M1](#m1) |
| G16 | **3** | Avoid relying on auditory or haptic feedback by itself. | Signal important changes through accessible visual/text semantics. | [P01](#p01), [P12](#p12) |

### 08. Layout, ergonomics and adaptability

| ID | Glimpse weight | Generalized rule or goal | Why it matters specifically to Glimpse | Sources |
|---|---:|---|---|---|
| H01 | **5** | Design the actual phone viewport rather than stretching a desktop mockup. | Reel, hero and actions must coexist in usable handset space. | [P02](#p02), [P03](#p03) |
| H02 | **5** | Respect display cutouts, system bars and gesture insets. | The clipped wheel is particularly vulnerable at the screen edge. | [P03](#p03) |
| H03 | **5** | Give primary actions comfortable reach and size. | Set wallpaper and rotation controls must be practical one-handed. | [M5](#m5), [M7](#m7) |
| H04 | **5** | Keep photo content prominent relative to controls and decoration. | Large images are the reason to use a wallpaper application. | [P11](#p11), [P02](#p02) |
| H05 | **5** | Preserve a clear hierarchy when space becomes constrained. | The reel should not crowd out labels and safe target sizes. | [P11](#p11), [M1](#m1) |
| H06 | **4** | Keep fixed navigation from obscuring scrollable content. | Bottom bars, sheets and keyboard must not hide the last actionable row. | [P03](#p03), [M2](#m2) |
| H07 | **4** | Use grouping, whitespace and alignment to explain relationships. | Wallpaper target controls and collection selectors have separate scopes. | [P11](#p11), [M1](#m1) |
| H08 | **4** | Use reflow, max widths or alternate compositions on larger devices. | Do not mechanically enlarge a phone layout for tablets. | [P02](#p02) |
| H09 | **4** | Check both portrait and landscape where platform use permits. | Crop and rotation views may encounter landscape configurations. | [P02](#p02), [P03](#p03) |
| H10 | **3** | Test smaller and larger screens, font scales and display density. | A single Pixel screenshot is insufficient reference coverage. | [P03](#p03), [M1](#m1) |
| H11 | **3** | Prefer ordinary Android surfaces for settings and forms. | Bottom sheets and dialogs should serve tasks, not thematic novelty. | [P02](#p02), [P11](#p11) |
| H12 | **2** | Consider foldable posture and hinge avoidance if targeted. | Not essential to the first handset release, but architecture should adapt. | [P02](#p02) |

### 09. Motion, loading and performance

| ID | Glimpse weight | Generalized rule or goal | Why it matters specifically to Glimpse | Sources |
|---|---:|---|---|---|
| I01 | **5** | Make animation explain state change or spatial relationship. | Wheel rotation should communicate movement through photographs. | [P11](#p11), [H1](#h1) |
| I02 | **5** | Make animations interruptible and synchronized with final selection. | A moving wheel must never visually disagree with the hero. | [P04](#p04), [H1](#h1) |
| I03 | **5** | Prefer responsive input to ornamental effects. | Large photo decoding must not freeze drag or actions. | [M5](#m5), [P04](#p04) |
| I04 | **5** | Use bounded image memory and progressive thumbnail loading. | Collections may include many high-resolution local photos. | [P05](#p05), [M1](#m1) |
| I05 | **5** | Respect reduced-motion preferences without disabling navigation. | Alternative controls must maintain identical selected ID and actions. | [P01](#p01), [P12](#p12) |
| I06 | **4** | Avoid unnecessary full-screen spinners for background tasks. | Maintain orientation and show scoped progress during imports. | [P02](#p02), [M1](#m1) |
| I07 | **4** | Reserve haptics for meaningful tactile landmarks. | Notch feedback should reinforce, not overwhelm, the reel. | [P01](#p01), [M7](#m7) |
| I08 | **4** | Prevent layout jumps when media or labels load. | Fixed preview geometry reduces accidental taps and disorientation. | [M5](#m5), [M1](#m1) |
| I09 | **4** | Make loading, waiting and applying visibly distinct. | Network photo fetch differs from Android wallpaper apply. | [H1](#h1), [M1](#m1) |
| I10 | **4** | Maintain usable interfaces on midrange devices and power-saving states. | Performance isn't established by desktop rendering or emulator alone. | [M5](#m5), [P07](#p07) |
| I11 | **3** | Use appropriate motion restraint in support and settings screens. | Paper-reel animation does not need to permeate every dialogue. | [P11](#p11), [H1](#h1) |
| I12 | **3** | Measure latency, dropped frames and memory on real Android hardware. | Metrics ground later judgment but are not inferred from drawings. | [P04](#p04), [M5](#m5) |

### 10. Onboarding, language and guidance

| ID | Glimpse weight | Generalized rule or goal | Why it matters specifically to Glimpse | Sources |
|---|---:|---|---|---|
| J01 | **5** | Offer a short direct route to first useful output. | Photo Picker to collection to apply/rotate is the essential loop. | [P11](#p11), [H3](#h3) |
| J02 | **5** | Use action labels that name object and consequence. | Next photo, Set Home wallpaper and Start shuffle beat ambiguous verbs. | [H3](#h3), [H1](#h1) |
| J03 | **5** | Explain five-minute timing without misleading guarantees. | Describe eligible interval and delayed Android background execution. | [P07](#p07), [H3](#h3) |
| J04 | **5** | Tell users what imported photos mean and what remains on device. | Local copy versus provider-linked album is a consequential distinction. | [P05](#p05), [H3](#h3) |
| J05 | **4** | Ask for permissions only when the related benefit is understood. | Contextual picker and import beats startup permission walls. | [P06](#p06), [P11](#p11) |
| J06 | **4** | Build self-explaining controls rather than lengthy tutorials. | People rarely read manuals before trying a photo picker. | [H3](#h3), [H1](#h1) |
| J07 | **4** | Make every empty state actionable with a useful next step. | No photos should direct to Add photos; no rules to Create rule. | [M1](#m1), [H1](#h1) |
| J08 | **4** | Be consistent in verbs, terminology and capitalization. | Apply, preview, schedule, import and sync must mean different things. | [H3](#h3), [M1](#m1) |
| J09 | **3** | Offer Help at the point of confusion, with offline availability. | Explain failures or supported providers without internet dependency. | [H3](#h3), [P11](#p11) |
| J10 | **2** | Offer delight and a coherent voice without compromising task clarity. | Friendly wording should never bury the action itself. | [P11](#p11), [M6](#m6) |

### 11. Privacy, consent, agency and trust

| ID | Glimpse weight | Generalized rule or goal | Why it matters specifically to Glimpse | Sources |
|---|---:|---|---|---|
| K01 | **5** | Collect and expose only data necessary for the user's task. | A wallpaper manager should not require an account or photo telemetry. | [P06](#p06), [E1](#e1) |
| K02 | **5** | Prefer narrow photo access through native selection interfaces. | Avoid requesting broad device media permissions unnecessarily. | [P05](#p05), [P06](#p06) |
| K03 | **5** | Clearly distinguish local copies from externally shared material. | A sharesheet or bug report should never leak photos unexpectedly. | [P05](#p05), [E1](#e1) |
| K04 | **5** | Give an explicit preview before exporting diagnostics. | A crash report may expose sensitive metadata unless scrubbed. | [E2](#e2), [E1](#e1) |
| K05 | **5** | Never equate opening a chooser with a completed share. | External handlers may be cancelled or unavailable. | [H1](#h1), [P11](#p11) |
| K06 | **4** | Keep optional diagnostics, surveys and analytics opt-in. | No forced behavioral monitoring or nagging in a personal app. | [E1](#e1), [E2](#e2) |
| K07 | **4** | Make deletion and retention controls understandable. | Users should know whether removing a snapshot touches originals. | [E1](#e1), [H1](#h1) |
| K08 | **4** | Make opting out as clear as opting in. | No manipulative review, notification or diagnostic prompts. | [E1](#e1), [E2](#e2) |
| K09 | **4** | Explain permissions, background limits and source access in plain language. | Trust depends on an accurate capabilities description. | [P06](#p06), [H3](#h3) |
| K10 | **2** | Allow transparent account data portability if accounts ever exist. | No proprietary account is part of current Glimpse scope. | [E2](#e2), [P11](#p11) |

### 12. Native Android integration and device truth

| ID | Glimpse weight | Generalized rule or goal | Why it matters specifically to Glimpse | Sources |
|---|---:|---|---|---|
| L01 | **5** | Use the Android Photo Picker/SAF where they fit user intent. | Avoid custom imitations of system image selection. | [P05](#p05) |
| L02 | **5** | Treat provider selection as distinct from verified durable import. | Remote and revoked-provider assets cannot be assumed permanent. | [P05](#p05) |
| L03 | **5** | Use the system Sharesheet and safe content URIs for outbound sharing. | Do not promise direct integration to every target app. | [P05](#p05), [P06](#p06) |
| L04 | **5** | Respect Android scheduling limits instead of marketing exact timing. | Periodic WorkManager work has a 15-minute minimum repeat interval. | [P07](#p07) |
| L05 | **5** | Make wallpaper target capability conditional on real device support. | Home/Lock/Both options may differ by device and version. | [P11](#p11), [P07](#p07) |
| L06 | **5** | Do not overstate OS-level apply confirmations or capability probes. | The UX must accommodate uncertain outcomes. | [H1](#h1), [P07](#p07) |
| L07 | **4** | Provide recovery paths when a picker/provider is cancelled or absent. | System integration is inherently contingent. | [P05](#p05), [H4](#h4) |
| L08 | **4** | Respect hardware Back and Android edge gestures. | A custom circular reel must not break native navigation. | [P03](#p03), [P04](#p04) |
| L09 | **4** | Keep system notifications proportionate and user-controllable. | Do not interrupt for every successful shuffle. | [P11](#p11), [E1](#e1) |
| L10 | **4** | Label Play-only review/update features by actual distribution channel. | Sideload builds must not impersonate Play Store integration. | [P11](#p11), [E1](#e1) |
| L11 | **3** | Keep platform form controls and sheets familiar by default. | Use Compose semantics and Material interaction patterns sensibly. | [P02](#p02), [P01](#p01) |
| L12 | **3** | Ensure images, settings and local metadata survive ordinary lifecycle events. | Reboots, rotations and process loss must not silently reset user intent. | [P07](#p07), [H1](#h1) |

### 13. Everyday completeness, polish and credibility

| ID | Glimpse weight | Generalized rule or goal | Why it matters specifically to Glimpse | Sources |
|---|---:|---|---|---|
| M01 | **4** | Include discoverable About, Help, version/build and genuine contact routes. | A credible utility needs an understandable support surface. | [H1](#h1), [P11](#p11) |
| M02 | **4** | Show honest empty/offline/error states for support channels. | No configured email or browser should not become a dead end. | [H4](#h4), [M1](#m1) |
| M03 | **4** | Keep destructive storage/reset operations clearly scoped. | Tell users exact local content affected before deleting. | [H1](#h1), [E1](#e1) |
| M04 | **4** | Keep consistent component states and terminology across every surface. | Disabled, selected, loading, focus and error tokens are a system. | [P02](#p02), [M1](#m1) |
| M05 | **4** | Do not use a visual reference as proof of functioning behavior. | Design intent, native semantics and actual device evidence differ. | [H2](#h2), [M1](#m1) |
| M06 | **3** | Provide nonintrusive options for review, feedback and crash reports. | Support prompts should not hijack the core photo workflow. | [E1](#e1), [P11](#p11) |
| M07 | **3** | Establish an icon, type, spacing and color vocabulary. | The warm-paper visual language should remain coherent. | [P11](#p11), [M1](#m1) |
| M08 | **3** | Test long names, duplicated titles and missing metadata. | Real photo/collection data is irregular and sometimes absent. | [M1](#m1), [H1](#h1) |
| M09 | **2** | Support graceful dark appearance if appropriate to the chosen brand. | Worth exploring, but no requirement to sacrifice approved paper aesthetic. | [P11](#p11), [M1](#m1) |
| M10 | **2** | Make credits, licences and updates truthful and accessible. | Important for distribution polish, but secondary to wallpaper workflows. | [P11](#p11), [E1](#e1) |

### 14. Evaluation principles and evidence hygiene

| ID | Glimpse weight | Generalized rule or goal | Why it matters specifically to Glimpse | Sources |
|---|---:|---|---|---|
| N01 | **5** | Audit task journeys, not just static screens. | A perfect image cannot prove import to rotate actually makes sense. | [H2](#h2), [M3](#m3) |
| N02 | **5** | Test what someone expects each control to do before explaining it. | The reel invites assumptions worth explicitly checking. | [H2](#h2), [H1](#h1) |
| N03 | **5** | Keep design intent, behavior contract and device observations separate. | No screenshot alone certifies touch, OS outcome or accessibility. | [H2](#h2), [M3](#m3) |
| N04 | **5** | Inspect first/last/empty/loading/denied/failed/interrupted states. | One happy-path mockup systematically misses serious problems. | [M1](#m1), [M3](#m3) |
| N05 | **5** | Evaluate critical actions with native TalkBack and real touch. | Custom mechanisms can pass static checks while failing interaction. | [P10](#p10), [M3](#m3) |
| N06 | **5** | Record uncertain/not tested rather than quietly passing an item. | No human preference or hardware result should be fabricated. | [H2](#h2), [M3](#m3) |
| N07 | **4** | Trace findings to a specific screen, state, task and evidence. | An actionable review links issues to exact proposals and observations. | [H2](#h2), [M4](#m4) |
| N08 | **4** | Check consistency across the whole image reference family. | Conflicting screens create false implementation requirements. | [M1](#m1), [H2](#h2) |
| N09 | **4** | Use multiple complementary inspection approaches. | Heuristics, cognitive walkthroughs, accessibility and device tests differ. | [H2](#h2), [M3](#m3) |
| N10 | **4** | Prioritize failures by severity, exposure and confidence. | A deceptive Apply result outranks a subtle shadow mismatch. | [H2](#h2), [M4](#m4) |
| N11 | **3** | Test real humans rather than treating agent hypotheses as user research. | Owner walkthroughs are real evidence, albeit a small sample. | [H2](#h2), [M3](#m3) |
| N12 | **3** | Use representative source photographs and adverse device states. | Mock pictures alone may conceal crop, contrast and loading issues. | [M1](#m1), [M3](#m3) |

### 15. Usually low relevance or conditional

| ID | Glimpse weight | Generalized rule or goal | Why it matters specifically to Glimpse | Sources |
|---|---:|---|---|---|
| O01 | **2** | Provide data search and sophisticated filters only when library scale demands them. | Small-photo initial workflow shouldn't inherit enterprise complexity. | [M1](#m1), [M4](#m4) |
| O02 | **1** | Optimize signup/login flows if an account becomes necessary. | Glimpse intentionally has no account system. | [P11](#p11), [M4](#m4) |
| O03 | **1** | Add web-to-app funnel continuity if there is a real funnel. | No web commerce onboarding is currently relevant. | [M5](#m5) |
| O04 | **0** | Add cart, checkout, payment or subscription UX patterns. | No commerce exists in the product remit. | [E3](#e3) |
| O05 | **0** | Use retention nudges, streaks, social proof and aggressive notifications. | These conflict with a quiet personal wallpaper utility. | [E1](#e1), [E3](#e3) |
| O06 | **0** | Optimize advertising placement and conversion-rate dark patterns. | Glimpse has no ad business model. | [E1](#e1) |
| O07 | **1** | Implement elaborate multi-user role management only if needed later. | No team collaboration, multi-user workspace or accounts in scope. | [H1](#h1) |
| O08 | **2** | Add elaborate global localization infrastructure before first release. | Prepare for translation structurally; production translations can follow demand. | [P12](#p12) |

## How to use the inventory without prematurely auditing

1. Start with the separate [refined audit-ready instrument](../quality/mobile-ux-proposal-audit-instrument-2026-10-23.md), which groups high-value requirements into observable review questions and permits **Not yet designed**, **Not testable in a mockup**, **N/A**, **Unknown** and **Needs human/device test** outcomes. Do not convert those to passes.
2. Record the specific Glimpse reference image/version/state, the interaction it claims, the applicable principle IDs, consequence severity and evidence class. Grade the **whole reference family**, not a favorite single illustration.
3. Keep these independent axes: design legibility; semantic expectations; workflow completeness; real device behavior; accessibility; real human interpretation; and provenance/approval.
4. Resolve conflicts by distinguishing a **core requirement** from a **conditional convention**. The reel may be unconventional, but must not silently change wallpaper, conceal actions, or defeat non-gesture alternatives. Branding does not waive accessibility.
5. Never mark an unsurfaced or unimplemented feature as a failing mockup unless the approved product contract actually requires it on that surface. Evaluate applicable state variants and release-channel differences.

## Source register and confidence

Platform and standard references carry greater normative weight for their respective environments than a blog's checklist. Research/heuristics supply evaluation methods, not evidence that Glimpse passed a user test. Multilingual practitioner references are useful for discovering categories but may paraphrase each other. In particular, purported universal rules such as “three taps”, “3 seconds” and precise thumb-zone coordinates are **context-dependent hypotheses**.

| Ref | Source and language | Class | Link |
|---|---|---|---|
| <a id="p01"></a>P01 | Android Developers, Accessibility (native Android guidance) | platform | [Read source](https://developer.android.com/design/ui/mobile/guides/foundations/accessibility) |
| <a id="p02"></a>P02 | Android Developers, Layouts and navigation patterns / Material components | platform | [Read source](https://developer.android.com/design/ui/mobile/guides/layout-and-content/layout-and-nav-patterns) |
| <a id="p03"></a>P03 | Android Developers, Edge-to-edge design | platform | [Read source](https://developer.android.com/design/ui/mobile/guides/layout-and-content/edge-to-edge) |
| <a id="p04"></a>P04 | Android Developers, Understand gestures in Compose | platform | [Read source](https://developer.android.com/develop/ui/compose/touch-input/pointer-input/understand-gestures) |
| <a id="p05"></a>P05 | Android Developers, Photo picker | platform | [Read source](https://developer.android.com/training/data-storage/shared/photo-picker) |
| <a id="p06"></a>P06 | Android Developers, Minimize permission requests | platform | [Read source](https://developer.android.com/privacy-and-security/minimize-permission-requests) |
| <a id="p07"></a>P07 | Android Developers, Work requests and periodic work limits | platform | [Read source](https://developer.android.com/develop/background-work/background-tasks/persistent/getting-started/define-work) |
| <a id="p08"></a>P08 | W3C, WCAG 2.2 (2023/2025 edits) | standard | [Read source](https://www.w3.org/TR/WCAG22/) |
| <a id="p09"></a>P09 | W3C, What's New in WCAG 2.2 | standard | [Read source](https://www.w3.org/WAI/standards-guidelines/wcag/new-in-22/) |
| <a id="p10"></a>P10 | GitHub Primer, native Mobile Accessibility Checklist | platform checklist | [Read source](https://primer.style/accessibility/tools-and-resources/checklists/mobile-checklist/) |
| <a id="p11"></a>P11 | Apple Human Interface Guidelines, Design principles (cross-platform conceptual source, not Android authority) | platform comparative | [Read source](https://developer.apple.com/design/human-interface-guidelines/design-principles/) |
| <a id="p12"></a>P12 | Apple Human Interface Guidelines, Accessibility (cross-platform comparative source) | platform comparative | [Read source](https://developer.apple.com/design/human-interface-guidelines/accessibility) |
| <a id="h1"></a>H1 | Nielsen Norman Group, Ten usability heuristics | research/heuristic | [Read source](https://www.nngroup.com/articles/ten-usability-heuristics/) |
| <a id="h2"></a>H2 | Nielsen Norman Group, How to conduct a heuristic evaluation | research/method | [Read source](https://www.nngroup.com/articles/how-to-conduct-a-heuristic-evaluation/) |
| <a id="h3"></a>H3 | UK Government Service Manual, Writing for user interfaces | public service design | [Read source](https://www.gov.uk/service-manual/design/writing-for-user-interfaces) |
| <a id="h4"></a>H4 | UK Home Office, Accessible error messages | public service design | [Read source](https://design.homeoffice.gov.uk/accessibility/interactivity/error-messages) |
| <a id="m1"></a>M1 | UI ZUKAN (Japanese), 100-point mobile UI review checklist (2026-03) | practitioner / JA | [Read source](https://ui-zukan.com/blog/mobile-ui-design-checklist) |
| <a id="m2"></a>M2 | Web担当者のガイドブック (Japanese), 30 mobile UI/UX review questions | practitioner / JA | [Read source](https://www.lab-ry-works.com/study/lesson/chapter-04-design/04-mobile-ui.html) |
| <a id="m3"></a>M3 | French government DINUM, native mobile accessibility audit guide (2025-02) | government / FR | [Read source](https://www.info.gouv.fr/accessibilite/applications-mobiles/guide-daudit-daccessibilite-des-applications-mobiles) |
| <a id="m4"></a>M4 | CorsoUX (Spanish), 50-item UX audit checklist (2026-04) | practitioner / ES | [Read source](https://euleinstitute.com/es/blog/auditoria-ux-checklist/) |
| <a id="m5"></a>M5 | UXCam (Portuguese), 10 mobile UX principles and 14 patterns (2026) | practitioner / PT | [Read source](https://uxcam.com/br/blog/mobile-ux/) |
| <a id="m6"></a>M6 | Justin Schmitz (German), mobile app UX checklist | practitioner / DE | [Read source](https://justinschmitz.de/blog/checklist-mobile/) |
| <a id="m7"></a>M7 | Pixelart Works (Korean), mobile touch/gesture launch checklist (2026) | practitioner / KO | [Read source](https://pixelart-works.com/ui-ux-design-column/?bmode=view&idx=172475307) |
| <a id="e1"></a>E1 | Government of Canada, Office of Consumer Affairs, Dark patterns | public agency | [Read source](https://ised-isde.canada.ca/site/office-consumer-affairs/en/dark-patterns) |
| <a id="e2"></a>E2 | Maloku et al., Privacy Starts with UI, pattern-catalog study (2026) | preprint research | [Read source](https://arxiv.org/abs/2601.13342) |
| <a id="e3"></a>E3 | Baymard, UX audit checklist and mobile review (2026) | commerce research, partly inapplicable | [Read source](https://baymard.com/blog/ux-audit-checklist-template) |
| <a id="x01"></a>X01 | MDN (Chinese), Mobile accessibility checklist; translation of a shared original | translation / ZH | [Read source](https://developer.mozilla.org/zh-CN/docs/Web/Accessibility/Guides/Mobile_accessibility_checklist) |
| <a id="x02"></a>X02 | MDN (Japanese), Mobile accessibility checklist; translation of a shared original | translation / JA | [Read source](https://developer.mozilla.org/ja/docs/Web/Accessibility/Guides/Mobile_accessibility_checklist) |
| <a id="x03"></a>X03 | Xinniyun (Chinese), 2025 mobile interaction and IA checklist, numerical claims unverified | practitioner / ZH | [Read source](https://www.xinniyun.com/8282d3fe/article-ux-design-principles-mobile-app) |
| <a id="x04"></a>X04 | Fivenine Design (Japanese), mobile UX practical checklist (2025) | practitioner / JA | [Read source](https://fivenine-design.com/blog/mobile-ux-improvement-checklist) |

## What this research deliberately does **not** conclude

- It does **not** score current Glimpse drawings, actual native screens or code.
- It does **not** claim user approval, WCAG conformance, Play distribution, direct Google Photos album sync or guaranteed five-minute background changes.
- It does **not** treat website checklists as authoritative Android component specifications.
- It does **not** prescribe arbitrary counts of taps, animation timings or button positions without task and device evidence.
- It does **not** convert all 0–5 relevance scores into a single misleading “UX score”.

**Source-of-truth precedence:** Current Glimpse decisions, technical contracts and later observed evidence take precedence over generic external advice. Any future audit must document exceptions explicitly instead of altering requirements covertly.
