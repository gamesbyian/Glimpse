# Glimpse native implementation queue

**Issue [#1](https://github.com/gamesbyian/Glimpse/issues/1) begins native development.** Historical Waltz specifications remain evidence, and [current decisions](current-decisions.md) retain authority.

## Priority update: real owner testing, 2026-10-23

A real installation successfully imported **22 Google Photos images** through the standard Android picker, set Home wallpaper and activated/observed five-minute shuffle with accurate visible status, with no reported crashes or sizing issues. Owner explicitly called the app generic and its present horizontal slot-strip **not a genuine Glimpse reel**. The owner expected to swipe across all 22 images, but the current selector is a **rounded rectangle with no circular edge**, exposes only five slots, and appears not to fill its visible space correctly until first interaction. Do not assume an observed preference for circular-edge dragging. See [actual owner observation](../quality/first-slice-evidence.md).

**Highest-priority next milestone:** validate/refine the **newly compiled** circular native reel prototype against the entire five-window design family. It already supports swipe/tap and a finite 22-photo identity mapping, but animated rotation, initial-render, tap ergonomics, reduced-motion behavior and on-device inspection remain unverified. Audit stable ID-to-aperture mapping on 0/1/3/5/22/60-item sets; validate boundary behavior, interrupted gestures, reduced motion, TalkBack/Previous/Next parity and Reel↔Grid selection identity. Do not broaden unrelated backend features first or solicit visual approval of an unimplemented design. Preserve the already-working import/apply/shuffle pathways.

## Native visual-inspection infrastructure

- [x] Debug-only deterministic test datasets feed **real Room and MainActivity**; six fixed artworks gain unique numbered indices.
- [x] Android screenshot/hierarchy collector produces PNGs, accessibility XML, control-size diagnostics, manifest and offline gallery.
- [x] Dedicated opt-in emulator workflow, modeled on ROYGBIV's targeted atlas rather than permanent expensive CI.
- [ ] Independently verify first atlas run and inspect output screenshots against the selected reference geometry; repair tooling failures as needed.
- [ ] Use atlas whenever the actual circular reel, photo window sizing, navigation and accessibility change; keep independent human-device approval gate.

## Connected visual journey production (2026-10-23)

- [x] The [12 P0 packages X01–X12](../design/proposal-mockup-coverage-gaps-2026-10-23.md) now have **82 connected supplementary SVG frames**, separate from the 66 original proposals. See [storyboard source index](../../design/proposals/storyboard-coverage.v1.json).
- [x] [Searchable mobile P0 storyboard atlas](../../design/atlas/storyboards.html) with frame progression, photo/state identity, source paths and offline packaging.
- [x] [Run 39938085295](https://github.com/gamesbyian/Glimpse/actions/runs/39938085295): 82-frame XML integrity, complete manifest mapping, offline packaging, 36 representative PNG renders and artifact upload passed.
- [x] Add six representative [browser-rendered flow sheets](../../design/atlas/representative-flows.html) (60 source-frame entries), eight X05 pose identities/transitions, source-level tests and an optional CairoSVG PNG contact-sheet generator. **Do not call this completed optical inspection.**
- [ ] Inspect the full **visual content** of X01–X12 against every required control, especially native Android picker/chooser fidelity, hero/wheel physical consistency, editable crop outcomes and 200% type reflow. Rendered SVGs are not an approved design.
- [x] Map exact SVG source paths for all [34 originally missing named core state variants](../../design/proposals/core-state-evidence.v1.json), distinguishing 34 structurally phone-pictured, 0 partial and 0 related-only, with twenty-five targeted supplemental refinements and a [visual contact sheet](../../design/atlas/core-state-contact-sheet.html). This is **source evidence only**, not 34 optical acceptances.
- [x] Draw the 15 formerly partial/related-only named states on the correct phone surface, with local status and user action. No optical signoff implied.
- [ ] Optically inspect the 25 supplemental state drawings at native scale and judge their action outcomes, fit and distinctness.
- [ ] Confirm each of the [34 named state variants](../../design/proposals/core-state-evidence.v1.json) is **visually sufficient** after direct raster inspection, including 15 newly illustrated formerly partial/related-only states; evidence-path assignment alone is already done and is not a signoff.
- [x] Produce the 11 P1 storyboard groups X13–X23: **55 source-editable draft frames** covering external shares, crops, large libraries, rules, rights, diagnostics, restore, Android integrations, Help, outgoing data and adverse input. Not optically approved.
- [x] Replace X07's mismatched p12 reel/hero/detail reference with the same public photograph and five-window X05 disc geometry (source correction, optical pending).
- [x] Correct original Source Detail baseline phone (formerly duplicated Discover listing) with a specific provider detail and explicit source-scoped next steps. Still draft.
- [ ] Raster-check revised Source Detail and its dedicated offline variant.
- [ ] Raster-check revised X07, X06 photo/window continuity and run the new source-level fixture integrity checks.
- [ ] Inspect all 55 P1 phone drawings and Android-owned handoffs, correct contradictions and prove flow outcomes before any comprehensive product-proposal audit.
- [ ] Preserve four conditional P2 scenario families as future/explicitly gated.
- [ ] Freeze explicit owner-approved whole-set hashes (V2) before full application visual conformity testing (V3).

## Visual release dependencies

- [x] [Two-way reference audit](../design/reference-gap-audit.md): original 73 workflows, 24 surfaces, 127 mandatory and 59 conditional elements, 16 added everyday-app surfaces, classified conflicts.
- [x] [Unified draft proposal family](../../design/proposals/README.md): 24 screen families, 34 support-state plates and eight real-image reel boundary references with consistent navigation/tokens.
- [x] [Fast static validation](../../scripts/validate-visual-spec.py): XML/contract/palette completeness, distinct from visual or user signoff.
- [x] [Mobile review atlas](../../design/atlas/README.md): 66 SVG proposals searchable with precise copyable references; packaged as an offline CI artifact (run [39922564218](https://github.com/gamesbyian/Glimpse/actions/runs/39922564218)).
- [ ] **V1:** complete and visually inspect a high-fidelity coherent Home/Collections/Reel/Grid/Import/Apply/Paused/Error core family, including legitimate state transitions and 200% font layout.
- [ ] **V2:** obtain an owner-reviewed exact approved *set*, not single disconnected images; freeze hashes and prohibit contradictory targets.
- [ ] **V3:** compare against actual Android screenshots, native semantics and physically tested swipe/settle/interrupt behavior.

## Everyday app experience features

- [x] [S01–S16 planned](../product/everyday-app-experience.md): About, Help, contacts, bug reports, crash recovery/diagnostics, updates, reviews, privacy, local storage/reset, permissions, licences, beta feedback, notifications and no-handler fallback.
- [x] [34 annotated proposed support-state references](../../design/proposals/README.md), with an opt-in privacy posture and channel-specific Play/sideload variants.
- [ ] **M1:** build an unobtrusive About/Help/Privacy/Storage/Capabilities entry tree; real version/build; link to genuine issues; external chooser fallback; offline task help.
- [ ] **M1:** locally captured minimal failure history and user-previewed diagnostic export; no automatic photo/crash upload or nagging.
- [ ] **M2:** Credits/Licences, What's New, beta feedback and notification preferences once the matching functionality exists.
- [ ] **M3:** Google Play review/update integration **only if** the app is actually Play-distributed and verified there; never show false Play prompts in the sideload app.

## P0: first real debug APK

- [x] Android project scaffold, Kotlin/Compose, Room/KSP, Gradle wrapper.
- [x] Photo Picker snapshot imports with SHA deduplication and immutable private copies.
- [x] Finite Reel and Grid with preview-only browsing, explicit Home apply.
- [x] Serialized WallpaperManager gateway, operation pending/uncertain status, active-use no-repeat shuffle.
- [x] Pure shuffle tests, narrow Android build workflow, pre-observation task ledger.
- [x] Android SDK 37.0 package workaround, compiled debug APK, passed JVM test task, and uploaded [debug artifact](https://github.com/gamesbyian/Glimpse/actions/runs/39904252446). Not a signed release or proof of device behavior.
- [ ] Test on Android phone, provider handling, process death, airplane mode and font/accessibility.
- [ ] Record actual uncoached actions, misunderstandings, error recovery and results against [predictions](../quality/first-slice-evidence.md).
- [x] First real circular paper disc with 5 wedges, true photo IDs across 0/1/3/22 cases, swipe/tap selection, stationary hero, grid fallback and unit tests. **Visual acceptance and animated wheel motion remain open.**
- [ ] Improve circular reel response: actually rotate/snap/reverse the disc with interruption and accessibility parity rather than instantaneous rebinding.
- [ ] Confirm native capture + physically inspect first-fill, small/large photo collections, type scaling, safe insets and zoom/cropping.

## P1: daily use beyond the bounded slice

- [ ] Persistent import journal and per-photo retry; cleanup of stranded private files after interrupted import.
- [ ] Screenshot rotate/crop and original-or-crop sharing via Android Sharesheet/FileProvider (F071/F073).
- [ ] Home/Lock/Both capability probes and target-isolated apply acknowledgments.
- [ ] Widgets, Quick Settings and more background policies through the *same* command gateway.
- [ ] Real on-device performance, Doze, OEM, large text, TalkBack, backups and upgrade testing.

## Later research

Only adopt secondary reels when uncoached evidence demonstrates added usefulness over conventional selectors. Keep owner-reviewed references separate from experimental layouts.

**Not a release:** current layout is a semantic prototype; there is a first-hand owner report from a real installed phone, but no comprehensive physical-device acceptance or signed release, and no guarantee of five-minute background updates.

- [x] Extend opt-in raster tool with source-hashed native phone crops, 25 state refinements, 34 named state sheets and corrected Source Detail/type groups. **Prepared only**, no fresh branch raster test.

- [x] Add distinct visual mechanisms to eight structurally pictured states (large collections, supported/unsupported Lock, nested conditions, per-photo exclusions, restore conflicts, failed and uncertain activity). Source illustrations only.

- [x] Render and contact-inspect **all 203 baseline SVG phone previews** (66 original, 82 P0, 55 P1) using source SHA-256 provenance in Actions run 40023670567. No wholesale blank layouts, but not individual native DPI or 60-question UX audit signoff.
- [x] Repair final-reel p22 photograph duplicate (distinct public architectural fixture4 in hero and selected aperture) and three small X08/X10 status chip overflow defects; add specific source/identity regressions.
- [ ] Confirm final p22 and status chip changes in fresh CI PNG and inspect new source-hashed native crops.

- [x] Visually differentiate cropped share p12c from unedited source p12 in X13's phone image geometry; assert the transform in storyboard-semantic regression checks. Final branch rerasterization requested.

- [ ] Close the five precise post-atlas asset verification gaps listed in [POST-ATLAS-VERIFICATION-2026-10-23.md](../design/POST-ATLAS-VERIFICATION-2026-10-23.md) using source-sha-matched native phone PNGs. Do not substitute the earlier 203-render success.


## Audit-driven next wave (owner authorized)

- [x] Author [60-question instrument](../design/glimpse-ux-audit-60.v1.json), perform honest [cycle-A source-level grading](../design/glimpse-ux-audit-cycle-a.v1.json) and document [findings](../design/GLIMPSE-AUDIT-CYCLE-A-REPORT.md). 18 draft source PASS / 8 ISSUE / 34 UNVERIFIED, no native acceptance inferred.
- [x] Draft [finite-reel and wallpaper state-truth candidate A1](AUDIT-CANDIDATE-A1-REEL-AND-STATE-TRUTH.md).
- [x] Specify [R01–R08 exact mockup revision and proof packet](../../design/proposals/audit-driven-revision-plan.v1.json) with 56 named scenarios.
- [ ] Capture/verify post-atlas five updated source images, native reel screenshots, 200%-text and focus hierarchy evidence before promoting unresolved questions.
- [ ] Implement and raster-inspect next-round R01–R08 comparison mockups in separate unapproved source category; attach before/after and source hash.
- [ ] Rerun all 60 audit questions on matching updated sources/native traces, document changed scores and unresolved gaps, request owner approval separately.


## Latest audit delta (2026-10-23, R01/R04/R05)

- [x] Independently render and inspect regenerated R01 photo-backed reel with **portable Grid vector arrow**. Source regeneration guard and [successful six-frame Actions #40048643795](https://github.com/gamesbyian/Glimpse/actions/runs/40048643795), actual phone crop inspected.
- [x] Add R04 [bulk import evidence matrix](../../design/proposals/audit-revisions/r04-import-evidence.v1.json), explicit cancellation and low-storage proposal sources, [candidate B specification](AUDIT-CANDIDATE-B-IMPORT-RECOVERY.md), [source-hashed successful targeted Actions #40048067960](https://github.com/gamesbyian/Glimpse/actions/runs/40048067960) and [optical review](../design/R04-IMPORT-OPTICAL-INSPECTION-2026-10-23.md).
- [x] Complete R05 [two draft diagrams plus photograph-backed focus overlay](../../design/proposals/audit-revisions/r05-accessibility-evidence.v1.json), source-fixture parity, proposed 48dp Grid touch outline and [successful optical Actions #40049026265](https://github.com/gamesbyian/Glimpse/actions/runs/40049026265).
- [x] Illustrate R04 dedicated zero-item and 17-of-22 verified-progress/cancel contexts; [run 40049428896](https://github.com/gamesbyian/Glimpse/actions/runs/40049428896) rendered all four R04 draft cases, independently inspected. Other X01 offline/completion reference and real native import behaviour remain to be verified.
- [ ] Capture real native R01 finite reel and R05 focus/200% text/target bounds/TalkBack traces using the existing emulator/hierarchy collector; compare exact actual screenshots against the stronger photo-backed paper reel, then re-score only evidence-supported GUX items.
- [ ] Resolve R04 native cancellation/storage/retained-byte behaviour, source URI expiry, and partial Room memberships with interrupted transaction checks; keep original provider images untouched.


## Safe interrupted-session handoff (2026-10-23)

The newest recovered priority/evidence snapshot is [HANDOFF-2026-10-23-SESSION-RECOVERY-AND-NEXT-AGENT.md](HANDOFF-2026-10-23-SESSION-RECOVERY-AND-NEXT-AGENT.md). Treat full one-shot 40041023803 and later targeted R01/R04/R05 checks separately. The next agent should prioritize genuine Android screenshots/hierarchy and native trace collection, not more unchecked reference counts. Cycle-A audit remains 18 PASS / 8 ISSUE / 34 UNVERIFIED; all visual work remains draft.


## R06–R08 follow-through (2026-10-23; still draft)

- [x] Commit [candidate C](AUDIT-CANDIDATE-C-SHARE-SCHEDULE-RECOVERY.md) covering normative share-return, scheduling uncertainty, local-copy retention and non-destructive restore conflict decisions.
- [x] Add five distinct draft phone-screen sources for share dismissal, battery restriction, unknown apply, revoked provider access and restore conflict choice. See [source-evidence manifest](../../design/proposals/audit-revisions/r06-r08-delta-evidence.v1.json); source-readback checks succeeded, **no optical acceptance inferred**.
- [x] Source-hash reproduce, rasterize and directly inspect **all five** R06–R08 native-sized 401×890 phone crops; corrected five semantic/copy defects and three-choice restore control. Exact SHA256 evidence and remaining limitations: [optical review](../design/R06-R08-OPTICAL-INSPECTION-2026-10-23.md). Previous same-surface candidates need additional side-by-side optical inspection.
- [ ] Add the smallest viable transition and accessibility-equivalence checks; confirm actual 48dp touch bounds, 200% layout, TalkBack order and external activity returns on Android.
- [ ] Examine remaining R03 pending/partial per-target apply and R06 share preparation/no-handler state before declaring a complete human-review candidate.
- [ ] Run comprehensive cross-screen optical and command-semantic review, then arrange **explicit human whole-set approval** with version/hash freeze. Cycle-A scores remain 18/8/34 until new audit evidence supports a change.

- [x] Cross-audit existing P1 X13/X18/X19 against the new R06–R08 states; fix false categorical recipient-delivery copy in `x13/04-cancel.svg` and contradictory no-action-committed text in `x19/05-restore-result.svg`. These two modified older frames still need freshly raster-inspected phone crops.
- [x] Add an opt-in, **five-source only** [R06–R08 optical CI workflow](../../.github/workflows/glimpse-audit-r06-r08-optical.yml) with strict source-SHA contract and optional renderer-specific pixel-hash comparison. Do not infer CI execution from the workflow's presence.
- [ ] Confirm updated X13 and X19 actual pixels are readable, align X19's restore review/choice/result route with the dedicated R08 three-decision screen, then close only question-specific visual evidence gaps.

## Whole-set human-review spine and cross-storyboard truth (2026-10-23 late)

- [x] Assemble [39 source-linked candidate phone frames](../../design/proposals/canonical-review-spine.v1.json) in eight task-order groups with a [phone-only, annotation-clipped HTML review spine](../../design/atlas/canonical-review-spine.html). **All 39 GitHub source paths independently existed at check; this is still a partial tour, not 39 approvals or the whole 203-image acceptance gate.**
- [x] Add [spine source/HTML validator](../../scripts/validate-canonical-review-spine.py) with opt-in source-hash snapshot for later whole-set human signoff. Validator committed, not yet executed on an intact checkout.
- [x] Reconcile P1 X13 share return and X19 restore result against R06 and R08 and repair two materially false/contradictory copy claims. See [cross-storyboard audit](../design/CROSS-STORYBOARD-TRUTH-RECONCILIATION-2026-10-23.md).
- [x] Extend the opt-in seven-source optical CI workflow to produce the five R06–R08 changed-phone crops plus the two corrected P1 original phone crops, without the expensive 203-image render. Workflow execution outcome remains unverified.
- [ ] Complete a reviewer-readable **full reference selection** beyond this 39-frame spine, covering every mandatory screen/control/error state, and distinguish optional variants and obsolete sketches in a versioned reference decision ledger.
- [ ] Independently inspect revised X13/X19 phone pixels, review full 203 baseline and 25 refinements at actual rendered scale for cross-screen typography, and freeze final whole-set source hashes only after explicit human approval.
