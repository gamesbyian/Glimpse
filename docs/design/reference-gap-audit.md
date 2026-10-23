# Glimpse: bidirectional specification-to-visual-reference audit

**Audited:** 2026-10-23. **Result:** comprehensive *traceable proposals*, **not** a complete owner-approved or pixel-locked reference set. The source of truth is [current decisions](../planning/current-decisions.md) plus [24 original semantic screen contracts](screen-contract.json), [73 original workflows](../product/feature-audit.md), and [16 new support-feature contracts](../../design/proposals/support-coverage.v1.json). A single annotated plate can describe missing elements without depicting them, so the existence of a plate is **not sufficient** for an approved UI.

> **Follow-up to the original coverage audit:** the [exact 34-slot evidence manifest](../../design/proposals/core-state-evidence.v1.json) records 34 structurally phone-pictured, 0 partial and 0 related-only cases after a second same-surface illustration pass. This is a deliberately weaker claim than visual completeness: all require raster/owner review and the 15 formerly partial/related slots still require independent optical and whole-flow sufficiency inspection. See the [browser contact sheet](../../design/atlas/core-state-contact-sheet.html). Supplemental SVGs are a separate class, not added to the 66 baseline.

## Exact coverage counts

| Dimension | Evidence in this repository | Status |
| --- | --- | --- |
| Original human-facing workflows | W01–W70, F071–F073 mapped in [manifest](../../design/proposals/visual-coverage.v1.json) to at least one of 24 main screens | **73/73 routed**, not separately photographed |
| Contracted UI surface families | 24 entries in screen-contract; [24 unified annotated proposals](../../design/proposals/plates/) | **24/24 have a draft screen-family image** |
| Contracted required elements | 127 required elements across those 24 surfaces, encoded and enumerated beside proposed layouts | **127/127 text-traced**, but **not all visibly realized in the phone portion** |
| Optional/error/conditional contract elements | 59 conditional elements, separate per-surface forbidden behaviors | **59/59 text-traced**, conditional illustrations incomplete |
| Core alternate screen states | 63 variants recorded in visual-coverage manifest | **63 declared**; 8 high-priority photo-reel state plates, while the rest still need targeted production-quality visuals |
| New common app experience cases | S01–S16 described in [everyday app specification](../product/everyday-app-experience.md) | **16/16 screen-family contracts added** |
| Support-screen alternatives | 34 explicit states (sideload vs Play, privacy, error, diagnostics, etc.) | **34/34 have distinct annotated draft plates** |
| Critical photo-reel states | Empty/one/three/22/last/reduced-motion/large-text/interrupted | **8/8 photographed sample-fixture layouts drafted**, not acceptance-tested |
| Actual APK screenshots | Opt-in [Android capture harness](../quality/android-visual-inspection.md) | Separate implementation evidence, never authoritative target artwork |
| Owner-approved exact images | Only overall warm paper, five large windows, clipped physical disc, stationary hero, Grid and meaningful finite operation **direction** is approved | **No complete, specific pixel-accurate screen set yet signed off** |

**Do not report “100% visual completeness” from the mapping numbers.** All W journeys are routed but complex interaction branches, physical animation frames, screen heights and accessibility may still lack explicit rendered references. The 24 plates are first-pass coherent compositions with annotated contracts; their right-hand text is not proof of a physical control on the phone.

## Corrections and precedence decisions

| Conflict / ambiguity found | Governing decision | Replacement or quarantine |
| --- | --- | --- |
| Historical `Today / Explore / Collections / Rules` vs newer `Home / Discover / Collections / Automation` | **Home / Discover / Collections / Automation**, exactly four destinations | [Unified proposed tokens](../../design/tokens/brand.json); both [core proposals](../../design/proposals/plates/) and [support proposals](../../design/proposals/support-plates/) use canonical labels |
| Prior warm greys / apricot / sea green / purple versus several old token palettes | One proposed warm-paper, deep-ink, tidal-teal, coral-accent, muted-purple selection grammar; check final contrast/accessibility with real screen | Brand token baseline is provisional, **not** user pixel approval |
| Full five-photo identity disc mistaken for only five collection assets | Five are *apertures*, not an image cap; virtualization uses true image IDs from 0 to N−1 | [Photo-backed boundary/motion proposals](../../design/proposals/critical-reel/) show 0/1/3/22/last cases |
| Flat horizontal thumbnail strip from first real app | Rejected as the intended reel. Genuine cropped **round paper wheel** with five large openings, fixed pointer, real drag/swipe, selected hero separate from applied wallpaper | [Reel state sketches](../../design/proposals/critical-reel/) and [reel interaction contract](reel-interaction.md); real touch and initial-fill measurements still open |
| Historical `Next / Set now / Grid` toolbar | Explicit **Previous photo**, **Next photo**, **View all / Grid**, and target-qualified **Set Home wallpaper**; action never happens while simply browsing | New annotated plate / critical state drawings; old [reel browser schematic](../../design/reference/reel-browser-concept.svg) is **directional only, not a copy target** |
| Some drawings show `Set wallpaper`, `Set now`, or bare `Next` while state can mean preview, queue or apply | Exact action labels must include target and operation. Unqualified legacy text is prohibited as implementation authority | Semantic [screen contract](screen-contract.json) wins; new plate captions with inconsistent verb are annotations to be replaced, **not final controls** |
| Five photo apertures in a full disc versus bottom cropped reel on phone | Full disc is brand/physical component reference; phone layout deliberately cuts a much larger disc beneath the hero and within safe insets | [Full photographic identity](../../design/reference/reel-wide-apertures.png) + [phone-state reel studies](../../design/proposals/critical-reel/) are complementary views, not interchangeable screens |
| Home decoration mistaken for a second draggable reel | Home may have no wheel, or a passive/clearly linked collection crescent. Do not place an unlabeled miniature gesture target | Home proposal, screen-contract Home guardrails |
| Parent/child and Home/Lock dual reels contradict compact single-reel baseline | Single main reel on phone; ordinary collection selector and Home/Lock tabs baseline. Two independent reels need later comparative task evidence | [Six variants](../../design/research/reel-layout/) are labelled **research alternatives**, never all simultaneously implemented |
| W57 and W60 used for unrelated browsing and rotation tests | W57 is schedule simulation; W60 is time-bounded hold; generic browsing/Start shuffle use semantic action names, not invented W IDs | [Corrected observation ledger](../quality/first-slice-evidence.md) and [human-expectation walkthroughs](human-expectations.md) |
| Historical diagrams suggest an unlimited cyclic physical wheel without boundary semantics | Rotate the physical object; collection remains finite. Non-loop baseline; explicit loop, if ever enabled, must be visually labelled and never faked by duplicate IDs | 0/1/3/22/last sample-state visual proposals |
| Proposed reel visuals don't communicate focus, user error or disabled states | Same selected asset ID and action semantics in Grid, buttons and reduced motion; blank apertures for absent real photos; no action hidden under system bars | Screen contract, critical reel states, actual native screenshot/semantics acceptance still required |
| Developer/help/about labels not represented in 24 original proposals | Keep four tabs. Support and disclosure live in Settings/About, relevant contextual sheets, Android chooser and offline help | [S01–S16 support screens](../../design/proposals/support-plates/) |
| Review, update, email and bug submission implied to work everywhere | Show channel-specific UI; no Play review in sideload; no unverified contact mailbox; off-device diagnostic share only after preview and choice | S03–S09 / S14–S16 |
| Old 16-screen Waltz deck and v4 slides overlap new proposals | Valuable historical design rationale, **not** competing authority | Kept in private source archive, not imported as Glimpse canonical screens |

## Visual completeness by screen-family group

| Family | Draft plates | What must still be explicitly resolved or drawn before claiming final reference |
| --- | --- | --- |
| Home / Collections / Reel / Grid | [Core 24](../../design/proposals/plates/), [8 reel states](../../design/proposals/critical-reel/) | Precise visual geometry and hero:wheel balance, collection covers and counts, default Home actions/status, metadata and first/last/initial-fill; exact focus transition and finger affordances; 200% text clipping |
| Quick import / Share / Photo detail / Crop editor | Core 24 | Android photo-picker and Sharesheet handoff anatomy, screenshot rotate/crop/save distinction, edit before/after, Undo and batch failure; original-vs-edited explicit preview |
| Automation / Rule editor / Schedule simulator / Activity / Diagnostics | Core 24 | Full rule priority/precedence, nested-source simulator variants, paused/deferred/unknown/failed screens and recovery choices; mobile density/readability |
| Discover / Source detail | Core 24 | Provider-rights labels, real empty/offline/unsupported states, error consent and source provenance; do not claim all source providers exist |
| Home / Lock / Quickstart / Onboarding | Core 24 | Per-destination crop, partially unsupported targets, truthful active-use timing message, orientation variants |
| Widgets / Shortcuts / Settings / Backup / Accessibility | Core 24 | Native Android launcher widget/tile vs in-app preview, restore-conflict and wrong-key states, large text/keyboard/TalkBack, data deletion and privacy |
| About / Help / Contact / Crash / Report / Updates / Rate / Privacy / Storage / Permissions / Credits / Beta / Notifications / Handler fallback | [34 support plates](../../design/proposals/support-plates/) | Final copy, configured contact route, actual release channel, exact anonymization fields, installed browser/share handlers, Android OS screen appearance and Play listing availability |

**Support cases are now distinct from core features**: S01–S16 never change W01–W70 or F071–F073 meanings. Further implementation may reuse existing Settings and Diagnostics rather than introduce 16 heavyweight Activity screens.

## Consistency contract for all **working** proposals

The ***working proposal set*** comprises `design/proposals/plates/*.svg`, `design/proposals/critical-reel/*.svg`, and `design/proposals/support-plates/*.svg`. All are source-editable SVGs with text, declared reference status and shared screen vocabulary. They should be evaluated as a family, not individually optimized decorative posters.

1. **Chrome:** warm cream canvas, deep-teal ink, teal primary controls; one four-destination navigation sequence, same heading hierarchy and insets. Support pages include Back and don't add a fifth tab.
2. **Photographic UI:** one prominent stationary, rectangular preview; the wheel is a paper disc with five cutouts and a single fixed pointer. Hero and wheel selection must represent the **same** item ID. Elsewhere, no ornamental spinning controls.
3. **Consequences:** browse/preview, Home/Lock apply, queue/schedule, share/export and simulated future previews have explicit distinct labels, paths and outcomes.
4. **Motion:** one snap increments one real photo identity; direction/reversal/interruption preserve stable ID. No automatic apply when turning; reduced-motion gets same commands through buttons or Grid.
5. **Accessibility:** regular 48dp-class touch comfort, native TalkBack semantics, font scaling and sufficient contrast; do not force circular gestures. Layouts of 420×900 **design coordinates** are illustrative, not a claim of real dp or pixel dimensions on a particular phone.
6. **Evidence status:** these are **unapproved proposals** unless the user approves particular reference files. Existing prior art is labeled directional/experimental; the implementation screenshot atlas is observational evidence, not a design source.

## Remaining visual and product decisions worth explicit owner review

These are **not** silently resolved by uniform styling:

- Actual paper material treatment, rim thickness, hub details and photographic apertures at running phone dimensions; icon silhouette and final brand mark.
- Whether wheel rotation uses both direct drag and horizontal swipe/flick on a circular surface; exact snap and finite-edge feedback.
- Home hero visual vs collection-detail preview, pinned/paused/unknown state hierarchy, primary action placement, and whether the active-collection crescent is useful at all.
- Crop/editor presentation (especially safe areas and independent Home/Lock), support popovers vs full pages, compact/large type, landscape/tablet/foldable.
- Asset image licences and missing 2 MB tactile board in preserved private repo.
- Exact contact channel and publication route. Google Play presence and a public review destination are **not** assumed.
- Motion frames and 63 state variants: 8 photo-reel states plus 34 support states are concrete proposals, but the original set still needs a selected minimum of high-fidelity final assets.

## Definition of visual-set readiness

**Gate V0 (achieved as draft infrastructure):** 73 W/F IDs mapped, 24 original screen-family proposals, 16 support contracts with 34 state plates, 8 high-risk reel plates, shared proposed tokens, conflicts classified.

**Gate V1 (still open):** one production-grade coherent candidate per shipped screen and materially different state (especially Home, Collection, Reel, Grid, Import, Apply, Pause/Error), complete visual controls, stable typography/spacing and documented responsive variants; inspect SVGs optically rather than just reading XML.

**Gate V2 (still open):** owner review of the *whole* image set together, explicit approval or change requests and checksums; no leftover ambiguous toolbar or contradictory selection feedback.

**Gate V3 (still open):** actual Android captures compared with approved references and semantic tests for actions, real 22-photo swipe traversal, no empty-start underfill, larger fonts, reduced motion and physical-device interactions. Only then can a reference be called implemented.

Continuing implementation before V1 is complete is possible for nonvisual data work, but agents **must not treat the present proposal drawings as definitive UI instructions**. The next UI milestone is to refine and get approval on the high-priority Home/Reel/Grid/Collection family **together**, not to separately polish a random screen.

## Further references

- [First owner/device observation](../quality/first-slice-evidence.md)
- [Reference acceptance](../quality/reference-acceptance.md)
- [UX surface inventory](ui-surface-inventory.md)
- [The support spec and M1–M4 staging](../product/everyday-app-experience.md)
