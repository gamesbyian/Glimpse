# Glimpse visual proposal set

**Status: internally routed draft source files, not a user-approved UI atlas.** This directory is the safe starting point for the next coordinated design pass. Do not cherry-pick multiple mutually exclusive historical comparison layouts into production.

## Where to look

| Need | Location | Authority |
| --- | --- | --- |
| Current user-facing original app screen families | [24 annotated SVG proposals](plates/) | Draft layout plus textual required/forbidden inventory; the annotations **do not** establish that all required controls have been drawn |
| Real-photo paper reel boundary states | [Eight reel proposals](critical-reel/) | Reconciled wheel semantics and stable identity examples; inspect empty, one, three, 22, last, reduced motion, large text, interrupted |
| About, Help, feedback, crash, updates, ratings, privacy, licences and other support | [34 support-state SVG proposals](support-plates/) | 16 distinct support surfaces, two or more alternatives each |
| All 73 original workflows → 24 surfaces | [Main manifest](visual-coverage.v1.json) | Machine-readable mapping, constraints and 63 remaining full-state-visual needs |
| 16 common-app extensions → 34 states | [Support manifest](support-coverage.v1.json) | S01–S16, separate from historical W/F IDs |
| Complete conflicts and missing exact artwork | [Bidirectional audit](../../docs/design/reference-gap-audit.md) | Current disposition and V0–V3 gates |
| Shared color/navigation draft | [tokens](../tokens/brand.json) | One coherent proposal palette; **not** visual approval |
| Root identity/reference photographic five-window disc | [paper reel](../reference/reel-wide-apertures.png) | User-approved direction of form and materials, **not** final phone layout |
| Alternative dual-wheel studies | [research experiments](../research/reel-layout/) | **Unapproved mutually exclusive comparisons only** |

## Initial review set

These images should be judged **together** as the earliest coherent proposed family:

- [Home](plates/home.svg), [Collections](plates/collections.svg), [Reel](plates/reel.svg), [Grid](plates/grid.svg)
- [Reel with 22 photographs](critical-reel/reel-twenty-two.svg) and [three photographs](critical-reel/reel-three.svg)
- [Last real photograph](critical-reel/reel-last.svg), [empty collection](critical-reel/reel-empty.svg), [reduced motion](critical-reel/reel-reduced-motion.svg)
- [Quick import](plates/quick-import.svg), [Share](plates/share.svg), [Automation](plates/automation.svg)
- [About (sideload)](support-plates/about--sideload.svg), [Help](support-plates/help--topics.svg), [Crash recovery](support-plates/crash-recovery--suspected-crash.svg), [Diagnostic preview](support-plates/diagnostic-preview--minimal.svg), [Rate unavailable](support-plates/rate-review--not-available.svg)

All images currently use the same paper/teal design proposal, system sans typography and four-tab navigation convention. The large disc has five **physical apertures**, but the 22-photo state shows **22** as the collection total. Numbers in the critical reel wedges are synthetic fixture labels, not data from a real user's library.

## What the proposals do not prove

- A full 420×900 SVG is an **illustration**, not a calibrated Android dp screenshot.
- The screen-family SVGs are *annotated design sketches*, not final complete high-fidelity pixel references; missing or hypothetical controls are itemized beside the phone drawing.
- The dramatic rim, photo crops, hub, and material realism still need owner-directed refinement and physical-device visual acceptance.
- Accessibility at 200% scale and swipe interruption require native, instrumented verification. A reduced-motion/large-text illustration only communicates intent.
- No user asset, unverified personal photo, user-report log or private screenshot is incorporated into this public repo.

## Review and promotion workflow

1. Choose a feature, inspect the contract and its proposed image **plus neighboring screens**.
2. Check task consequences, previous/next, accessibility and all materially different empty/failure/unsupported states.
3. Adjust the editable SVG and update any affected manifest/contract together. Don't leave old screenshots in the root reference directory as competing authority.
4. Run `python3 scripts/validate-visual-spec.py`, then **inspect the actual graphics**, preferably via the target Android screenshot workflow once functional.
5. When the owner explicitly approves a whole compatible set, freeze exact source hashes in a new `design/approved/` manifest. **Do not infer approval merely from these files existing.**

Source-backed proposals are versioned; optional screenshot/atlas artifacts live in Actions and do not replace source or a separate runtime evidence ledger.
