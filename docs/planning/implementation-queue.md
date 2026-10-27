# Glimpse native implementation queue

## Live merged Android baseline · October 27

- **Merged [#27](https://github.com/gamesbyian/Glimpse/pull/27)** (main `5c9bcc21`): original five-window paper face, genuine bounded notch rotation, licensed editorial typography, collection-scoped persisted photo focus, stale-gesture rejection, 320dp/200%-type action wrapping. Branch native build and full screenshot atlas passed; owner visual/touch approval remains open.
- **Merged [#28](https://github.com/gamesbyian/Glimpse/pull/28)** (main `0af0a3b7`): independent API34 real Android OS Photo Picker tests for single/duplicate/two-photo imports and byte-identical offline copies after source deletion and restart. Both clean-branch picker workflows passed. Old stacked PRs #21/#24 closed as superseded. No Google Photos cloud-sync or physical-handset claim.
- **Merged [#29](https://github.com/gamesbyian/Glimpse/pull/29)** (main `50af4719`): an actual full-original-aspect photo-detail view, with Previous/Next, Back, accurate imported metadata and explicit existing Home apply. Its updated native build and full emulator atlas passed. No crop/editor/export simulation.
- **Merged [#30](https://github.com/gamesbyian/Glimpse/pull/30)** (main `42a519cf`): restore last collection or safe fallback after restart, with unit tests and native CI.
- **Merged [#31](https://github.com/gamesbyian/Glimpse/pull/31)** (main `a4589eb6`): original-only Android Sharesheet with private read-only FileProvider, SHA/size/path/MIME revalidation, metadata warning and emulator chooser/cancel tests. Both final-head native and atlas CI passed; a real external receiving app and physical handset remain unverified. Crop/sanitized sharing is still absent.
- **Merged [#32](https://github.com/gamesbyian/Glimpse/pull/32)** (main `1ce3b865`): separate last-acknowledged Home photo from preview, photographic collection covers and true Room membership counts; read-only queries. Native unit and emulator full atlas passed; subsequent independent screenshot inspection revealed a compact/200%-font reflow defect, corrected by #38.
- **Merged [#33](https://github.com/gamesbyian/Glimpse/pull/33)** (main `aaa0e6b6`): wallpaper-set errors/cancellations after entering the Android side-effect preserve uncertain pending tokens and pause automatic shuffle, with noncancellable pending persistence; native CI passed. Forced OS fault-injection still pending.
- **Merged [#34](https://github.com/gamesbyian/Glimpse/pull/34)** (main `cd2d6579`): native build workflow concurrency scoped per PR/branch; independent agents no longer cancel one another's CI.
- **Merged [#35](https://github.com/gamesbyian/Glimpse/pull/35)** (main `24f6dcb9`): visual-atlas workflow follows Home/Collections, model and Room query changes. Emulator atlas passed.
- **Merged [#36](https://github.com/gamesbyian/Glimpse/pull/36)** (main `9e24404c`): real saved shuffle source is always identified even while browsing another collection; two-collection fixture and emulator journey verified that browsing does not reassign rotation. Build/tests and full atlas passed.
- **Merged [#37](https://github.com/gamesbyian/Glimpse/pull/37)** (main `eb9b1e2c`): off-main-thread 24-hour stale importer `.partial` cleanup; verified actual temp-file deletion, finalized-original preservation and symlink rejection. Build/tests and emulator atlas passed. Full durable import journal remains pending.
- **Merged [#38](https://github.com/gamesbyian/Glimpse/pull/38)** (main `6a4b6cfb`): directly screenshot-observed 320dp/200%-type collection-title squeeze repaired with responsive stacked photographic covers and title/count; responsive Home with acknowledged image. Build/tests and atlas passed, and exact-size pre/post screenshot comparison was inspected. Owner/device signoff remains pending.
- **Open [#39](https://github.com/gamesbyian/Glimpse/pull/39)**: strengthen the 320dp/200% screenshot harness to **prove at least 100dp of real reel Canvas is visible**; a previous passing atlas mislabeled a screenshot of above-the-fold controls as a reel screenshot. Wait for current-head full emulator proof before merging.

**Immediate product gate:** accept #39 only after a new actual 320dp/200%-type screenshot shows the real reel (not just controls above it); independently inspect output, then put a current debug APK on the owner's Pixel 6 Pro and collect real tactile, accessibility and optical observations. Continue the approved **direction** toward physical lower-corner reels, but do not mistake proposed geometry/spacing or CI screenshots for owner approval. Master design PR #6 stays draft and unmerged. Crop, metadata-sanitized exports, platform lifecycle/background timing, widget/Quick Settings coverage and full operation journal are still open.


> **26 October 2026 current execution:** [R0–R6 build plan](BUILD-PLAN-2026-10-26.md); [native tactile reel PR #27](https://github.com/gamesbyian/Glimpse/pull/27). Existing import/apply/shuffle stays intact. First animated disc and baked paper face have passed Gradle/JVM/PNG tests; independent lower-corner geometry and actual Android optical review remain acceptance gates. **Prioritize physical/visual reel quality before broad unrelated features.** Draft design [PR #6](https://github.com/gamesbyian/Glimpse/pull/6) remains unapproved.


**Issue [#1](https://github.com/gamesbyian/Glimpse/issues/1) begins native development.** Historical Waltz specifications remain evidence, and [current decisions](current-decisions.md) retain authority.

## Priority update: real owner testing, 2026-10-23

A real installation successfully imported **22 Google Photos images** through the standard Android picker, set Home wallpaper and activated/observed five-minute shuffle with accurate visible status, with no reported crashes or sizing issues. Owner explicitly called the app generic and its present horizontal slot-strip **not a genuine Glimpse reel**. The owner expected to swipe across all 22 images, but the current selector is a **rounded rectangle with no circular edge**, exposes only five slots, and appears not to fill its visible space correctly until first interaction. Do not assume an observed preference for circular-edge dragging. See [actual owner observation](../quality/first-slice-evidence.md).

**Historical October 23 observation (superseded in code by merged #27):** owner saw a horizontal slot strip before the real circular paper face was implemented. Current screenshots show the five-window raster paper disc; user approval of its tactile/optical behavior remains open.

**Highest-priority next milestone:** validate/refine the **newly compiled** circular native reel prototype against the entire five-window design family. It already supports swipe/tap and a finite 22-photo identity mapping, but animated rotation, initial-render, tap ergonomics, reduced-motion behavior and on-device inspection remain unverified. Audit stable ID-to-aperture mapping on 0/1/3/5/22/60-item sets; validate boundary behavior, interrupted gestures, reduced motion, TalkBack/Previous/Next parity and Reel↔Grid selection identity. Do not broaden unrelated backend features first or solicit visual approval of an unimplemented design. Preserve the already-working import/apply/shuffle pathways.

## Native visual-inspection infrastructure

- [x] Debug-only deterministic test datasets feed **real Room and MainActivity**; six fixed artworks gain unique numbered indices.
- [x] Android screenshot/hierarchy collector produces PNGs, accessibility XML, control-size diagnostics, manifest and offline gallery.
- [x] Dedicated opt-in emulator workflow, modeled on ROYGBIV's targeted atlas rather than permanent expensive CI.
- [x] Independently inspected actual merged Android screenshots across standard and 320dp/200% typography: discovered and corrected #38's real caption squeeze. Further reel optical/physical proof is still explicitly required by #39.
- [ ] Use atlas whenever the actual circular reel, photo window sizing, navigation and accessibility change; keep independent human-device approval gate.

## Connected visual journey production (2026-10-23)

- [x] The [12 P0 packages X01–X12](../design/proposal-mockup-coverage-gaps-2026-10-23.md) now have **82 connected supplementary SVG frames**, separate from the 66 original proposals. See [storyboard source index](../../design/proposals/storyboard-coverage.v1.json).
- [x] [Searchable mobile P0 storyboard atlas](../../design/atlas/storyboards.html) with frame progression, photo/state identity, source paths and offline packaging.
- [x] [Run 39938085295](https://github.com/gamesbyian/Glimpse/actions/runs/39938085295): 82-frame XML integrity, complete manifest mapping, offline packaging, 36 representative PNG renders and artifact upload passed.
- [ ] Inspect the full **visual content** of X01–X12 against every required control, especially native Android picker/chooser fidelity, hero/wheel physical consistency, editable crop outcomes and 200% type reflow. Rendered SVGs are not an approved design.
- [ ] Map each of the [34 missing declared core state variants](../design/proposal-mockup-coverage-gaps-2026-10-23.md) to a *visually sufficient* storyboard frame or create targeted dedicated state art where reuse is inadequate. Do not close by filename matching.
- [ ] Produce/verify the 11 P1 storyboard groups X13–X23 (external shares, crops, large libraries, rules, rights, diagnostics, restore, Android integrations, Help, exact outgoing bytes and adverse input) before a truly comprehensive product-proposal audit.
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
- [x] Implemented basic real-notch rotation/snap, finite selection and reduced-motion fallback (#27); **velocity/interrupt tactile quality, TalkBack equivalence and Pixel timing are still not physically accepted.**
- [ ] Confirm native capture + physically inspect first-fill, small/large photo collections, type scaling, safe insets and zoom/cropping.

## P1: daily use beyond the bounded slice

- [ ] Persistent import journal and per-photo retry; cleanup of stranded private files after interrupted import.
- [ ] Screenshot rotate/crop and original-or-crop sharing (F071/F073) remain open. PR #31 separately implements original-only manual outbound Sharesheet; altered/cropped/sanitized copy, incoming ACTION_SEND, clipboard and batch share are still absent.
- [ ] Home/Lock/Both capability probes and target-isolated apply acknowledgments.
- [ ] Widgets, Quick Settings and more background policies through the *same* command gateway.
- [ ] Real on-device performance, Doze, OEM, large text, TalkBack, backups and upgrade testing.

## Later research

Only adopt secondary reels when uncoached evidence demonstrates added usefulness over conventional selectors. Keep owner-reviewed references separate from experimental layouts.

**Not a release:** current layout is a semantic prototype; there is a first-hand owner report from a real installed phone, but no comprehensive physical-device acceptance or signed release, and no guarantee of five-minute background updates.
