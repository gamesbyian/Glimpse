# Agent B: support and journey visual review, 24 October 2026

**Status: source-editable visual proposal, independently optically examined, not human-approved or natively accepted.** Production B issue [#8](https://github.com/gamesbyian/Glimpse/issues/8). Unmerged stacked draft branch: `agent/support-journey-a11y-20261024`.

## Exact evidence and reproducibility

- The **34 source SVGs** at `design/proposals/support-plates/*.svg` were revised by `scripts/agent-b-modernize-support.py`, keeping all 34 original IDs. These are edits, not 34 new alternatives. Generator is idempotent, with A's **provisional**, not frozen, design 0.1 secondary `#566861` and nominal >=48px support Back/nav source regions. The marker `data-control` annotates paper-study geometry and focus intent, **not native Android semantics**.
- `source-ledger.json` carries SHA-256 for each exact SVG and all six 401×890 JPEG contact sheets. Existing historical baseline sheets remain in `design/reviews/support-2026-10-24/`; they were **not overwritten**. Every file in this new directory is a separate B-owned derived corpus.
- `source-journey-a11y-audit.json` reports 34 support states, **23 P0/P1 journey groups**, 82+55=137 frames and **56/56 linked revision scenarios** with zero broken references. It does not prove transition runtime behavior or consent delivery.
- `compact-fontscale-stress.jpg` and `.json` contain six exactly sourced phone cases shown at **320×710 uniform scale** and **font-size doubled without reflow**. These are deliberate redlines; no Android `fontScale=2`, 320dp, TalkBack, responsive viewport or safe-inset proof is claimed.
- Focused [successful CI #40131451599](https://github.com/gamesbyian/Glimpse/actions/runs/40131451599): repeatable idempotent source upgrade, existing support-truth/visual/storyboard validators, new 48px geometry, 23 journey/137 storyboard/56 scenario integrity, six raster sheets, and compact/doubled-type stress artifact. Earlier [CI #40130784827](https://github.com/gamesbyian/Glimpse/actions/runs/40130784827) established the baseline 34-state B edit and source-equivalent optics.

## Actual direct pixel observations

All **six new** 3×2 contact JPEGs were fetched from current branch and directly opened as images (34 distinct screens). **No gross contact-scale overflow** visible after source transformation. Back/navigation are quieter compared with original baseline support sheet; removal of the non-app lavender “PROPOSAL” chip returns roughly 24 units of useful vertical space to action content. Photo-reel ornament has not been introduced into Settings.

- **Storage deletion (contact 04, sixth row in 34-state stream, entry #23):** the old card-style confirmation left surrounding Storage cards/navigation looking independently actionable; B now draws a shaded blocking backdrop and a modal, with one unambiguous Cancel and destructive 54-unit button, a clearly labeled **example 22-copy count**, provider-original preservation and active-rotation consequence. Source SHA from the ledger. This image remains a proposal; Android must compute counts and enforce modal focus trapping and state mutation semantics.
- **Outbound feedback (contact 05, entry #30):** previous “share preview” was optically almost identical to invitation except an on toggle; now visibly shows **draft, not sent**, **no screenshot selected**, **diagnostics off**, and a distinct **Open Android Sharesheet…** action. The actual native Sharesheet and recipient delivery are unverified.
- **Diagnostics (contact 02, entries #11–12):** minimal and expanded payload examples remain distinct; technical-detail source row has an expanded hit-region proposal. Samples are deliberately marked examples, never a promise of exact current runtime payload. No photographs, source URI or account token should be sent.
- **Report problem (contact 02, entries #7–8):** source-visible “Nothing sent; diagnostics off” versus “Diagnostics selected, not sent” now differ in both text and toggle state.
- **Support family states (contacts 01,03,05,06):** checkable separation of Play/sideload, Help tutorial, optional notifications vs unimplemented, rights provenance warnings, restricted Lock and Android chooser-cancel vs missing-handler. Existing explicit warning texts maintained.

All six **static compact-font stress cases** were independently opened. The uniformly scaled 320-wide render already yields very small control text. Artificial source-level doubled fonts cause **visible right-edge clipping, collapsed 4-tab labels, overlapping controls and report fields** on six representative screens, especially the expanded report preview and modal. Therefore this set **fails a naive 200%-font layout policy**; it does *not* establish that a real native layout also fails. Source-only geometry cannot measure 48 **dp** hit areas, semantic focus or OS-owned views. See cross-lane issue [#13](https://github.com/gamesbyian/Glimpse/issues/13).

## Journey continuity and blocking gates

- Edited P1 X13 cancellation `design/proposals/advanced-storyboards/x13/04-cancel.svg` to say delivery **unverified** rather than “no image delivered”; mere system chooser closure is not a verified receipt or proof of a particular recipient action.
- Edited P1 X22 no-handler `design/proposals/advanced-storyboards/x22/05-absent.svg` to distinguish **no compatible target** from deliberate chooser cancellation, and keep draft for copy/save. Neither supplies an Android screenshot.
- New B validator covers all 137 referenced storyboard paths/ordinals and all 56 scenario paths, but cannot independently judge each of 137 static frames as optically current generation; those still require a connected human/product integration pass.
- **Not qualified:** native 320dp/200%-font/TalkBack, default focus and return after OS handoff, dynamic contact/diagnostic byte-for-byte payload preview, real Play channel and Android result, user comprehension, design set owner choice. Native/system-owned UI and all owner questions remain unresolved. PR #6 must not be merged or treated as approved.

## Further independently inspected P0/P1 journey hardening (current B branch)

The B source-only audit now also outputs **six exact-SVG, 401×890 two-phone transition/branch contact sheets covering one explicitly classified pair per all 23 journey groups** (46 optical samples, not all 137 frames). The [paired contact ledger](journey-pair-source-ledger.json) captures every image SHA-256, each of the 46 exact source SHA-256s, pair types, and evidence exclusions; [successful CI #40137297609](https://github.com/gamesbyian/Glimpse/actions/runs/40137297609) refreshed and revalidated this entire corpus. The six contacts are `journey-pair-contact-01.jpg` through `06.jpg`. **All six pairsheets were directly opened and visually checked once before the final fixes; sheets 01, 05 and 06 were directly reopened at their final source hashes after the fixes.** No blanket claim of 137 direct optical acceptances.

High-value current-source findings and repairs, anchored to the current ledger:

- **X02 collection removal, source pair SHA-256s** `38ead1cdd2ef4fb0c819bdd0aaecd83ad07bb0310ce0cd943c9f10c6934d74d4` and `2e37d4ecb3a22e91cc4b771043de9c6406b202fb8955243f85a153bc3644343d`. Formerly the only action was `Review removal` and the following frame still showed the *deleted Rainbows collection* as a live empty collection. Now a genuine shaded modal study disables underlying page/nav geometry and presents Cancel/Remove Rainbows, an explicit 22-membership scope, preserved originals, and safe shuffle pause. The continuation shows **Collections / Last collection removed**, with no ghost Rainbows header and a Create collection recovery. Current pixel sheet 01 directly inspected: complete modal, no visible clipped labels, clear absent-collection outcome at normal size. The frame assumes Rainbows was the last collection; collections with other survivors require a separate list return, **not implied by this example**.
- **X19 restore conflict, source SHA-256** `b9acde4b856fc34b282b35e6ba52b6ae0bb80e786c18e6eb26d3d829e2533dac`. Original “Choose safe merge” jumped straight to a completed restore without an explicit final commit. Now it exposes 1,284 proposed entries/2 skipped, existing Rainbows ID and 22 local photos preserved, four missing provider connections, the no-change-until-confirm contract, and independent 51-unit Cancel/Confirm safe merge controls. Current sheet 05 directly opened: content readable, action hierarchy clear, continuation visibly “Restore completed”. Numbers are fixture examples, not real restore results.
- **X22 diagnostics handoff, consent-frame SHA-256** `3ef34cf5a4dd7a9a90c889a0e170bb06283811bcae1cf0ae498bf56599fefcdd` and handoff-frame SHA-256 `3b0a9bdaac22ddc71d6566d6b6cf84bde8aaf29d7edf482d450db137f85d72f0`. Previously the frame had no actual choice affordances and jumped to an alleged Android chooser with an ambiguous single `Copy / Save / Share` call to action. It now depicts two full-row, **default-OFF** field inclusion controls, a separate Review outgoing example step, clearly sample-only redacted fields, no selected recipient/no sent assertion, distinct **Copy report / Save locally / Open Android Sharesheet** actions. Current sheet 06 directly reopened; controls and status fit without visible clipping at 401×890. **The intended native acceptance is to serialize exactly the final outgoing bytes after selections, show those exact bytes, recheck consent, then invoke real Android sharing. This SVG example is intentionally not falsely labeled exact runtime data.**

The [source validator](../../../../scripts/validate-agent-b-support-journeys.py) now rejects regressions where X02 still shows the deleted collection, a destructive choice is unguarded, or X19/X22 lack explicit consequences and recognizable 48-unit design hit areas. The scoped workflow passed source+existing storyboard semantics and rendered all updated evidence; no shared viewer/selection manifests have been modified by B. **Integrator must reconcile shared advanced-storyboard action captions** for X19 `Confirm safe merge` (formerly `Choose safe merge`) and X22 `Review outgoing example` / `Open Android Sharesheet…` (formerly `Enable selected fields` / `Copy / Save / Share`). The manifest owns catalog captions, and B deliberately did not touch it.

## Companion 320dp / 200%-type support reflow study (unselected)

Production A's unselected two-state [Sections sheet primary concept](https://github.com/gamesbyian/Glimpse/blob/agent/production-a-primary-system-20261024/design/proposals/whole-product-candidates/compact-320dp-200pct-sections-sheet.svg) has a matching, independently source-editable **Report a problem** support-form companion at [`compact-report-sections.v0.1.svg`](compact-report-sections.v0.1.svg), with directly inspected [PNG](compact-report-sections.v0.1.png) and [machine-readable SHA/evidence record](compact-report-sections.v0.1.json). This stays **OUTSIDE** the selected 34 support paths and is not an owner IA decision.

The left visual study has a two-line 36-unit heading, 94/91-unit form fields, separate 85-unit diagnostics OFF row, reachable 54-unit Review report and 55-unit Sections controls; scrollable form content is an intended **native** behavior, not implemented by the SVG. The right study shows four **86-unit** section rows and a 54-unit Close sections action inside a modal layer, with focus trap/return rules described rather than falsely simulated as TalkBack. Script `scripts/render-agent-b-compact-support-study.py` generates both the editable SVG and 1080×800 raster and hashes all evidence; its own structural gate checks **12 explicit >=54-unit source targets**, which are **not Android dp measurements**.

**Direct pixel inspection:** the first PNG exposed a clipped right-hand notes heading and a tofu/missing-glyph dropdown arrow. Both were corrected in source generator, [focused CI #40137946540](https://github.com/gamesbyian/Glimpse/actions/runs/40137946540) passed, and the exact final PNG was reopened. The compact heading, complete panel labels, 4 destination rows and path-based Sections chevron now all fit without visible nominal-size clipping. Current editable SVG SHA-256 `58c2561a65563b649d8e136b24af3e16d9857446429e61b1a9fb4fd11274d9b7`; current PNG SHA-256 `81ee37a46315e9e9896cc4cdfdf3a44bee270529b7b583c14bce07817c786d20`. **No real 320dp/Android fontScale=2/IME/TalkBack/inset/native scrolling or device approval follows.** See [blocker #13](https://github.com/gamesbyian/Glimpse/issues/13).

## Focused developer replay

```sh
python3 scripts/agent-b-modernize-support.py --apply
python3 scripts/agent-b-modernize-support.py
python3 scripts/validate-agent-b-support-journeys.py
python3 scripts/validate-support-state-truth.py
python3 scripts/validate-visual-spec.py
python3 scripts/validate-storyboard-coverage.py
python3 scripts/validate-storyboard-semantics.py
python3 scripts/validate-audit-scenario-trace.py
python3 -m pip install cairosvg pillow
python3 scripts/render-support-review-corpus.py
python3 scripts/render-agent-b-accessibility-probes.py
python3 scripts/render-agent-b-journey-pairs.py
python3 scripts/render-agent-b-compact-support-study.py
```

The old `render-support-review-corpus.py` itself targets the historical support contact directory; the B-owned lean Actions wrapper moves generated copies into this versioned folder, deliberately avoiding historical evidence replacement. Read SHA-256s from the current ledger, not this prose.
