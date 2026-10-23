# Glimpse UI/UX proposal audit instrument (prepared, NOT run)

**Date:** 2026-10-23. **Status:** Ready-to-use review criteria only. **Zero current proposal verdicts.** This instrument is distilled from the [174-item multilingual weighted principle register](../research/mobile-ux-weighted-principles-2026-10-23.md). Its purpose is to audit the unified reference family *after* that family is ready, without prematurely revising the art or declaring unsupported passes. Consult [current decisions](../planning/current-decisions.md), [screen contracts](../design/screen-contract.json), [visual coverage](../../design/proposals/visual-coverage.v1.json), [existing visual gap audit](../design/reference-gap-audit.md) and [reference acceptance](reference-acceptance.md) at execution time.

## Compact executive criteria

A defensible Glimpse UI must: (1) make images the star; (2) make the real paper reel genuinely useful; (3) distinguish browse/preview, apply and rotate; (4) keep every device or data consequence honest; (5) permit conventional navigation and accessible equivalents; (6) survive real data, denied permissions, OS constraints and interruptions; (7) remain consistent as a *family* of surfaces; and (8) preserve evidence and approval boundaries.

## Review scope and sequencing

- **Stage 0, preparation only (this document):** collate criteria, weights, relevant states, input contracts and proposed evidence. **Do not grade mockups yet.**
- **Stage 1, proposal review (future):** review exact versioned drawings, full screen family, annotated transitions, compact/large-font, empty/failure and support variants. Assess visual clarity and stated semantics; flag anything that cannot be inferred from a drawing.
- **Stage 2, implementation verification (future):** test real Compose views, semantics/focus, pointer behavior, device insets, cloud-provider handoffs, OS acknowledgements, performance and recovery; static visuals cannot prove these.
- **Stage 3, human expectation test (future):** uncoached, small sample/owner walkthroughs with recorded expectations and observed actions, separated from agent hypotheses.

### Outcome codes to be used **later**

| Code | Meaning |
|---|---|
| PASS | Specific evidence supports this criterion at the stage being audited |
| ISSUE | Specific counterevidence demonstrates a relevant flaw |
| OPEN | Reference/state exists but decision or evidence is incomplete |
| UNDRAWN | Required relevant state has not yet been pictured; no UI judgment implied |
| RUNTIME | Cannot be verified from static proposal; requires functioning Android build |
| HUMAN | Interpretation or preference awaits a real uncoached person |
| N/A | Contractually not applicable; record a reason, e.g. Play-only feature in sideload build |
| UNKNOWN | Eligibility or existence is not yet established; **never count as PASS** |

**Important:** This is not a 60-point UX scorecard. Relevance priority is only one axis. Once an audit is authorized, record **severity** (blocker/major/minor), **confidence** (observed/inferred), and **evidence class** independently. A score-5 requirement with UNKNOWN evidence remains unknown, never a pass or an automatic failure. Avoid turning all 60 conditions into mandatory single-screen clutter.

## Refined review questions (future audit input)

### Q1. Meaning, user intent and navigation

| Check | Relevance | Review question | Parent principles | Evidence needed in future |
|---|---:|---|---|---|
| Q01 | **5** | Can a new user name the screen's purpose and the next useful action without being coached? | [A01](../research/mobile-ux-weighted-principles-2026-10-23.md), [A02](../research/mobile-ux-weighted-principles-2026-10-23.md), [J01](../research/mobile-ux-weighted-principles-2026-10-23.md) | uncoached comprehension |
| Q02 | **5** | Are preview, apply, active set and scheduling semantically distinct across labels and affordances? | [A04](../research/mobile-ux-weighted-principles-2026-10-23.md), [C03](../research/mobile-ux-weighted-principles-2026-10-23.md), [D01](../research/mobile-ux-weighted-principles-2026-10-23.md), [D08](../research/mobile-ux-weighted-principles-2026-10-23.md) | contract plus state-dependent visual |
| Q03 | **5** | Does every consequential control name what will change and which target it affects? | [A06](../research/mobile-ux-weighted-principles-2026-10-23.md), [J02](../research/mobile-ux-weighted-principles-2026-10-23.md), [F06](../research/mobile-ux-weighted-principles-2026-10-23.md) | visual plus behavior contract |
| Q04 | **5** | Can users safely explore, go Back or cancel without side effects? | [A05](../research/mobile-ux-weighted-principles-2026-10-23.md), [B03](../research/mobile-ux-weighted-principles-2026-10-23.md), [E01](../research/mobile-ux-weighted-principles-2026-10-23.md) | interaction contract |
| Q05 | **5** | Are four primary destinations clear, stable and consistently named? | [B01](../research/mobile-ux-weighted-principles-2026-10-23.md), [B09](../research/mobile-ux-weighted-principles-2026-10-23.md) | whole-set visual |
| Q06 | **5** | Does navigation preserve selected photo, collection and meaningful position? | [B02](../research/mobile-ux-weighted-principles-2026-10-23.md), [B07](../research/mobile-ux-weighted-principles-2026-10-23.md), [B11](../research/mobile-ux-weighted-principles-2026-10-23.md) | transition/behavior contract |
| Q07 | **4** | Are routine actions discoverable without hunting through secondary menus? | [A09](../research/mobile-ux-weighted-principles-2026-10-23.md), [B04](../research/mobile-ux-weighted-principles-2026-10-23.md), [B08](../research/mobile-ux-weighted-principles-2026-10-23.md) | whole-set visual |

### Q2. Reel as a functional photo-navigation instrument

| Check | Relevance | Review question | Parent principles | Evidence needed in future |
|---|---:|---|---|---|
| Q08 | **5** | Do users recognize the paper object as a genuine interactive reel with photo apertures? | [C01](../research/mobile-ux-weighted-principles-2026-10-23.md), [C09](../research/mobile-ux-weighted-principles-2026-10-23.md) | actual visual reference and human interpretation |
| Q09 | **5** | Does the fixed pointer refer to exactly the photo visible in the hero, regardless of motion state? | [C02](../research/mobile-ux-weighted-principles-2026-10-23.md), [I02](../research/mobile-ux-weighted-principles-2026-10-23.md), [F03](../research/mobile-ux-weighted-principles-2026-10-23.md) | sequence of reference frames and state contract |
| Q10 | **5** | Is the finite total and actual index visible and truthful for all collection sizes? | [C04](../research/mobile-ux-weighted-principles-2026-10-23.md), [D05](../research/mobile-ux-weighted-principles-2026-10-23.md), [C14](../research/mobile-ux-weighted-principles-2026-10-23.md) | 0/1/3/5/22/large fixtures |
| Q11 | **5** | Are tap, drag, swipe, fling, reverse, settle and interruption consequences explicit? | [C06](../research/mobile-ux-weighted-principles-2026-10-23.md), [C08](../research/mobile-ux-weighted-principles-2026-10-23.md), [C10](../research/mobile-ux-weighted-principles-2026-10-23.md) | gesture timeline and state contract |
| Q12 | **5** | Do Previous, Next, Grid and accessibility controls produce the same selection as the reel? | [C05](../research/mobile-ux-weighted-principles-2026-10-23.md), [C12](../research/mobile-ux-weighted-principles-2026-10-23.md), [G01](../research/mobile-ux-weighted-principles-2026-10-23.md) | cross-mode semantics and controls |
| Q13 | **5** | Can selection be moved without accidentally applying, starting rotation or editing membership? | [C03](../research/mobile-ux-weighted-principles-2026-10-23.md), [D01](../research/mobile-ux-weighted-principles-2026-10-23.md) | state transition contract |
| Q14 | **5** | Do the wheel and photographs occupy enough useful space to justify the metaphor? | [C09](../research/mobile-ux-weighted-principles-2026-10-23.md), [H04](../research/mobile-ux-weighted-principles-2026-10-23.md), [H05](../research/mobile-ux-weighted-principles-2026-10-23.md) | viewport compositions and owner assessment |
| Q15 | **4** | Are the first, last, empty and few-photo cases honest, discoverable and visually coherent? | [C14](../research/mobile-ux-weighted-principles-2026-10-23.md), [N04](../research/mobile-ux-weighted-principles-2026-10-23.md) | state variants, not only main mockup |
| Q16 | **4** | Do controls and touch areas avoid conflicts with Android Back and system insets? | [C07](../research/mobile-ux-weighted-principles-2026-10-23.md), [H02](../research/mobile-ux-weighted-principles-2026-10-23.md), [L08](../research/mobile-ux-weighted-principles-2026-10-23.md) | measured phone insets plus eventual device trial |

### Q3. Import, collections, crop, sharing and shuffle

| Check | Relevance | Review question | Parent principles | Evidence needed in future |
|---|---:|---|---|---|
| Q17 | **5** | Can someone complete photo selection to first applied/shuffling wallpaper without entering advanced rules? | [J01](../research/mobile-ux-weighted-principles-2026-10-23.md), [A07](../research/mobile-ux-weighted-principles-2026-10-23.md), [F05](../research/mobile-ux-weighted-principles-2026-10-23.md) | end-to-end user journey |
| Q18 | **5** | Does the import surface communicate selected, imported, duplicate, failed and saved-offline counts? | [D11](../research/mobile-ux-weighted-principles-2026-10-23.md), [F05](../research/mobile-ux-weighted-principles-2026-10-23.md) | partial-result variants |
| Q19 | **5** | Are source photo, saved snapshot, edited variant and wallpaper result distinguished at choice points? | [F01](../research/mobile-ux-weighted-principles-2026-10-23.md), [F04](../research/mobile-ux-weighted-principles-2026-10-23.md), [E02](../research/mobile-ux-weighted-principles-2026-10-23.md) | visual plus media provenance contract |
| Q20 | **5** | Is the actual crop/fit and Home/Lock target previewed accurately before commitment? | [F02](../research/mobile-ux-weighted-principles-2026-10-23.md), [F06](../research/mobile-ux-weighted-principles-2026-10-23.md), [F11](../research/mobile-ux-weighted-principles-2026-10-23.md) | preview and variant references |
| Q21 | **5** | Does sharing clearly distinguish original from edited copy and use truthful external handoff messages? | [F04](../research/mobile-ux-weighted-principles-2026-10-23.md), [K03](../research/mobile-ux-weighted-principles-2026-10-23.md), [K05](../research/mobile-ux-weighted-principles-2026-10-23.md) | share variant plus OS handoff description |
| Q22 | **5** | Do active-set changes, exclusions and permanent deletion remain separate choices? | [F10](../research/mobile-ux-weighted-principles-2026-10-23.md), [E01](../research/mobile-ux-weighted-principles-2026-10-23.md), [E07](../research/mobile-ux-weighted-principles-2026-10-23.md) | editing/confirmation/Undo variants |
| Q23 | **4** | Can large libraries and multi-item operations be managed without hidden broad scope or lost selection? | [F07](../research/mobile-ux-weighted-principles-2026-10-23.md), [F08](../research/mobile-ux-weighted-principles-2026-10-23.md), [F03](../research/mobile-ux-weighted-principles-2026-10-23.md) | library-size scenarios |

### Q4. Status, failure, recovery and scheduling truth

| Check | Relevance | Review question | Parent principles | Evidence needed in future |
|---|---:|---|---|---|
| Q24 | **5** | Are selected photo, installed wallpaper, last applied item and next eligible shuffle unambiguous? | [D01](../research/mobile-ux-weighted-principles-2026-10-23.md), [D03](../research/mobile-ux-weighted-principles-2026-10-23.md), [D04](../research/mobile-ux-weighted-principles-2026-10-23.md) | cross-screen state matrix |
| Q25 | **5** | Are requested, pending, confirmed, unknown and failed wallpaper outcomes explicitly differentiated? | [D02](../research/mobile-ux-weighted-principles-2026-10-23.md), [D06](../research/mobile-ux-weighted-principles-2026-10-23.md), [E06](../research/mobile-ux-weighted-principles-2026-10-23.md) | result state variants |
| Q26 | **5** | Do Start, Pause, Resume, Stop and manual Next communicate distinct consequences? | [D03](../research/mobile-ux-weighted-principles-2026-10-23.md), [D08](../research/mobile-ux-weighted-principles-2026-10-23.md), [J02](../research/mobile-ux-weighted-principles-2026-10-23.md) | lifecycle transition reference |
| Q27 | **5** | Is five-minute timing described honestly when the app sleeps or Android defers work? | [D04](../research/mobile-ux-weighted-principles-2026-10-23.md), [J03](../research/mobile-ux-weighted-principles-2026-10-23.md), [L04](../research/mobile-ux-weighted-principles-2026-10-23.md) | schedule copy and late/deferred variants |
| Q28 | **5** | Do failures explain what happened, what remained safe and a useful next action? | [E03](../research/mobile-ux-weighted-principles-2026-10-23.md), [E04](../research/mobile-ux-weighted-principles-2026-10-23.md), [E05](../research/mobile-ux-weighted-principles-2026-10-23.md) | failed and recovery states |
| Q29 | **5** | Are permission denied, provider absent, offline, app restart and partial import separately recoverable? | [E08](../research/mobile-ux-weighted-principles-2026-10-23.md), [E09](../research/mobile-ux-weighted-principles-2026-10-23.md), [E10](../research/mobile-ux-weighted-principles-2026-10-23.md), [L07](../research/mobile-ux-weighted-principles-2026-10-23.md) | adverse state coverage |
| Q30 | **4** | Are progress, completion and persistent status displayed at appropriate time and location? | [D07](../research/mobile-ux-weighted-principles-2026-10-23.md), [D09](../research/mobile-ux-weighted-principles-2026-10-23.md), [D10](../research/mobile-ux-weighted-principles-2026-10-23.md) | async state progression |
| Q31 | **4** | Can destructive actions be cancelled or undone safely without repeated unnecessary dialogs? | [E01](../research/mobile-ux-weighted-principles-2026-10-23.md), [E07](../research/mobile-ux-weighted-principles-2026-10-23.md), [E12](../research/mobile-ux-weighted-principles-2026-10-23.md) | confirmation/Undo behavior |

### Q5. Accessibility and readable content

| Check | Relevance | Review question | Parent principles | Evidence needed in future |
|---|---:|---|---|---|
| Q32 | **5** | Is every essential action possible without relying on drag, rotation, timed disclosure or haptics? | [G01](../research/mobile-ux-weighted-principles-2026-10-23.md), [G09](../research/mobile-ux-weighted-principles-2026-10-23.md), [G16](../research/mobile-ux-weighted-principles-2026-10-23.md) | accessible alternative map |
| Q33 | **5** | Do all controls have Android-sized touch targets, enough separation and safe hit regions? | [G02](../research/mobile-ux-weighted-principles-2026-10-23.md), [H03](../research/mobile-ux-weighted-principles-2026-10-23.md) | real dp geometry, eventually touch trial |
| Q34 | **5** | Are useful text and control contrasts legible over paper and photographs in all states? | [G03](../research/mobile-ux-weighted-principles-2026-10-23.md), [G08](../research/mobile-ux-weighted-principles-2026-10-23.md) | sampled palette plus overlay samples |
| Q35 | **5** | Can increased system font settings reflow all UI without truncating actions or content? | [G04](../research/mobile-ux-weighted-principles-2026-10-23.md), [G14](../research/mobile-ux-weighted-principles-2026-10-23.md) | large-font variants and eventual app capture |
| Q36 | **5** | Are labels, roles, selected states, counts and apply outcomes semantically announced? | [G05](../research/mobile-ux-weighted-principles-2026-10-23.md), [G11](../research/mobile-ux-weighted-principles-2026-10-23.md), [G13](../research/mobile-ux-weighted-principles-2026-10-23.md) | Compose semantics/TalkBack evidence |
| Q37 | **5** | Does keyboard/switch focus follow a logical order and return predictably from overlays? | [G06](../research/mobile-ux-weighted-principles-2026-10-23.md), [G12](../research/mobile-ux-weighted-principles-2026-10-23.md) | focus traversal contract |
| Q38 | **5** | Does reduced motion retain exactly the same commands, photo selection and status? | [G07](../research/mobile-ux-weighted-principles-2026-10-23.md), [C12](../research/mobile-ux-weighted-principles-2026-10-23.md), [I05](../research/mobile-ux-weighted-principles-2026-10-23.md) | reduced-motion states and interactions |
| Q39 | **5** | Are visual decorations and offscreen reel children omitted from assistive focus appropriately? | [G10](../research/mobile-ux-weighted-principles-2026-10-23.md), [G05](../research/mobile-ux-weighted-principles-2026-10-23.md) | semantics tree |
| Q40 | **4** | Do long or translated strings, RTL and high display scaling have future-safe layouts? | [G15](../research/mobile-ux-weighted-principles-2026-10-23.md), [M08](../research/mobile-ux-weighted-principles-2026-10-23.md), [H10](../research/mobile-ux-weighted-principles-2026-10-23.md) | text stress fixtures and RTL review |

### Q6. Layout, performance, animation and consistency

| Check | Relevance | Review question | Parent principles | Evidence needed in future |
|---|---:|---|---|---|
| Q41 | **5** | Do hero, reel, navigation and important buttons coexist on a real portrait phone without crowding? | [H01](../research/mobile-ux-weighted-principles-2026-10-23.md), [H04](../research/mobile-ux-weighted-principles-2026-10-23.md), [H05](../research/mobile-ux-weighted-principles-2026-10-23.md) | same-scale viewport reference |
| Q42 | **5** | Are all touch controls, popups and photographs properly protected from bars, cutouts and gesture zones? | [H02](../research/mobile-ux-weighted-principles-2026-10-23.md), [H06](../research/mobile-ux-weighted-principles-2026-10-23.md) | system inset overlays |
| Q43 | **5** | Does wheel motion communicate real selection movement rather than purely decorate? | [I01](../research/mobile-ux-weighted-principles-2026-10-23.md), [C09](../research/mobile-ux-weighted-principles-2026-10-23.md) | motion keyframes and intended behavior |
| Q44 | **5** | Do interruptions, image loading and app latency preserve continuity and responsiveness? | [I02](../research/mobile-ux-weighted-principles-2026-10-23.md), [I03](../research/mobile-ux-weighted-principles-2026-10-23.md), [I08](../research/mobile-ux-weighted-principles-2026-10-23.md) | timeline and eventual hardware result |
| Q45 | **4** | Do all components share a single vocabulary for type, spacing, selection, focus, disabled and error? | [M04](../research/mobile-ux-weighted-principles-2026-10-23.md), [M07](../research/mobile-ux-weighted-principles-2026-10-23.md) | whole reference family |
| Q46 | **4** | Do long labels and small screens reflow without hiding user controls? | [H07](../research/mobile-ux-weighted-principles-2026-10-23.md), [H10](../research/mobile-ux-weighted-principles-2026-10-23.md), [G14](../research/mobile-ux-weighted-principles-2026-10-23.md) | compact and high-scale variants |
| Q47 | **4** | Are loading/decode/storage operations planned so large images do not stall the experience? | [I03](../research/mobile-ux-weighted-principles-2026-10-23.md), [I04](../research/mobile-ux-weighted-principles-2026-10-23.md), [I09](../research/mobile-ux-weighted-principles-2026-10-23.md) | performance architecture and device evidence |
| Q48 | **3** | Does the design have intentional adaptations for landscape, tablet or folding devices if supported? | [H08](../research/mobile-ux-weighted-principles-2026-10-23.md), [H09](../research/mobile-ux-weighted-principles-2026-10-23.md), [H12](../research/mobile-ux-weighted-principles-2026-10-23.md) | conditional form-factor states |

### Q7. Permissions, support, privacy and platform honesty

| Check | Relevance | Review question | Parent principles | Evidence needed in future |
|---|---:|---|---|---|
| Q49 | **5** | Does choosing photos use scoped native interfaces without unnecessary blanket access? | [K01](../research/mobile-ux-weighted-principles-2026-10-23.md), [K02](../research/mobile-ux-weighted-principles-2026-10-23.md), [L01](../research/mobile-ux-weighted-principles-2026-10-23.md) | Android permission and picker contract |
| Q50 | **5** | Are offline-copy durability and external provider limits described accurately? | [F05](../research/mobile-ux-weighted-principles-2026-10-23.md), [J04](../research/mobile-ux-weighted-principles-2026-10-23.md), [L02](../research/mobile-ux-weighted-principles-2026-10-23.md) | copy/provenance/denied states |
| Q51 | **5** | Does diagnostics export expose exactly what leaves the phone and require meaningful user approval? | [K03](../research/mobile-ux-weighted-principles-2026-10-23.md), [K04](../research/mobile-ux-weighted-principles-2026-10-23.md), [K06](../research/mobile-ux-weighted-principles-2026-10-23.md) | privacy/preview/consent variant |
| Q52 | **5** | Are external share, contact and handler-failure states truthful rather than fake success? | [K05](../research/mobile-ux-weighted-principles-2026-10-23.md), [L03](../research/mobile-ux-weighted-principles-2026-10-23.md), [M02](../research/mobile-ux-weighted-principles-2026-10-23.md) | platform chooser and no-handler variants |
| Q53 | **4** | Can users find About, Help, version/build, privacy and local storage/reset functions? | [M01](../research/mobile-ux-weighted-principles-2026-10-23.md), [M03](../research/mobile-ux-weighted-principles-2026-10-23.md), [J09](../research/mobile-ux-weighted-principles-2026-10-23.md) | cross-surface routes |
| Q54 | **4** | Are review, updates, notifications and feedback opt-in, non-nagging and distribution-channel appropriate? | [K08](../research/mobile-ux-weighted-principles-2026-10-23.md), [L09](../research/mobile-ux-weighted-principles-2026-10-23.md), [L10](../research/mobile-ux-weighted-principles-2026-10-23.md), [M06](../research/mobile-ux-weighted-principles-2026-10-23.md) | sideload-vs-Play support variants |

### Q8. Reference-family coherence and evidence

| Check | Relevance | Review question | Parent principles | Evidence needed in future |
|---|---:|---|---|---|
| Q55 | **5** | Does every required task/action have an identifiable relevant surface and state variant? | [N01](../research/mobile-ux-weighted-principles-2026-10-23.md), [N04](../research/mobile-ux-weighted-principles-2026-10-23.md) | coverage manifest, not visual verdict |
| Q56 | **5** | Are all illustrated labels, behaviors, visual states and tokens consistent across the proposed set? | [N08](../research/mobile-ux-weighted-principles-2026-10-23.md), [M04](../research/mobile-ux-weighted-principles-2026-10-23.md) | whole-set cross-reference |
| Q57 | **5** | Are approved direction, draft schematic, final reference, runtime screenshot and user observation clearly separated? | [N03](../research/mobile-ux-weighted-principles-2026-10-23.md), [M05](../research/mobile-ux-weighted-principles-2026-10-23.md), [N06](../research/mobile-ux-weighted-principles-2026-10-23.md) | provenance and status registry |
| Q58 | **5** | Can a future reviewer connect each finding to exact image IDs, states, conditions and requirement IDs? | [N07](../research/mobile-ux-weighted-principles-2026-10-23.md), [N10](../research/mobile-ux-weighted-principles-2026-10-23.md) | traceable audit log |
| Q59 | **5** | Do the planned tests separately address expectation, appearance, behavior, TalkBack and real hardware? | [N02](../research/mobile-ux-weighted-principles-2026-10-23.md), [N05](../research/mobile-ux-weighted-principles-2026-10-23.md), [N09](../research/mobile-ux-weighted-principles-2026-10-23.md) | evidence plan, never synthetic pass |
| Q60 | **4** | Are every N/A, deferred, unresolved and untested check explicitly labelled with reason and owner? | [N06](../research/mobile-ux-weighted-principles-2026-10-23.md), [N10](../research/mobile-ux-weighted-principles-2026-10-23.md) | review worksheet |

## Suggested evidence/coverage worksheet for the **future** audit

| Field | Record |
|---|---|
| Check ID | Q01–Q60 (from above) |
| Glimpse workflow/contract | Existing W/F/S ID, screen contract, or approved product decision |
| Exact reference(s) | Relative path + content SHA/version + screen/state name |
| Intended human action/result | In everyday language, not internal API terms |
| Consequence class | Preview / Apply / Rotation / Collection / Import / Edit / Share / Settings / Support |
| Applies? | Yes / Conditional / N/A, with supporting contract |
| Stage | Proposal / Runtime / Accessibility / Human |
| Outcome | PASS / ISSUE / OPEN / UNDRAWN / RUNTIME / HUMAN / N/A / UNKNOWN |
| Evidence | Frame comparison, text/label, motion plan, Compose state, physical observation, human quote, etc. |
| Severity and confidence | Independently classified; never conflated with principle relevance |
| Proposed resolution | Specific change, affected reference family and contract owner, **only once audit begins** |

### Representative future state matrix (planning only)

**Reel:** empty, one, three, five, twenty-two, large set, first, middle, last, during drag, snap, reverse, interrupted motion, image unavailable, reduced motion, large text, Grid round-trip.

**Import/collection:** picker cancelled, cloud photo loading, offline, duplicate, partial success, copy verified, no storage, provider revoked, process interrupted, source removed.

**Wallpaper/scheduler:** chosen preview, apply requested, pending, success, failed, uncertain, Home/Lock/Both supported and unsupported, rotation active/paused/stopped, delayed eligibility, reboot/recovery.

**Editor/share:** source vs crop, screen aspect, rotation, unsaved changes, discard/undo, share chooser completed/cancelled/no handler, no unwarranted “sent” confirmation.

**Support/distribution:** About/help/offline, permission granted/denied, diagnostics opt-in and preview, retention/delete, configured/unconfigured contact, update unavailable, sideload versus Play review eligibility.

**Device/environment:** representative Pixel-class compact portrait; extra-small/tall phones; increased system font and display size; screen reader/focus; reduced motion; gesture-nav/three-button; system bars/cutouts; supported landscape/tablet only if within scope.

This matrix is a **test-input inventory**, not an assertion that these have all been rendered, built or approved.

## Evidence-order and conflict resolution

1. Read current product decisions and product-specific behavioral contracts. No external list may silently override them.
2. Classify each question's applicability to the relevant exact screen, state, channel and future stage.
3. Review the *whole proposed family* for semantic and visual conflicts before isolated pixel judgments. A component may be correct alone but contradictory across scenes.
4. Keep visual reference approval, software behavior, accessibility and physical user testing as distinct verdicts. Missing tests should be scheduled, not fabricated.
5. Use the parent inventory to recover nuance when a compact question invites overgeneralization (e.g. 48dp Android targets versus the **different** WCAG 2.2 web minimum/exception rules; reasonable short paths versus arbitrary 'three taps').
6. Report uncertainty and irreducible trade-offs. If a choice makes the reel more delightful but less accessible, demand an equivalent alternative rather than quietly assuming one objective replaces the other.

## Anti-goals

No critique or alteration of current images in this research PR; no execution of a visual, heuristic, device or human audit; no code changes to Compose; no hardcoded approval of the already proposed state plates; and no invented first-click, satisfaction or performance results.

## Audit readiness checkpoint (unfilled)

The later auditor must explicitly establish: exact proposal set and hashes; current screen and workflow contracts; applicability flags; state/viewport coverage; the actual audited build if any; a stable evidence store; owner confirmation of audit scope; and what remains untestable without a working handset. **This checklist is intentionally not marked complete.**

**Total:** 60 review questions: 1 at weight 3, 13 at weight 4, 46 at weight 5. The weighted inventory retains lower-priority concepts that were dropped from this condensed audit basis.
