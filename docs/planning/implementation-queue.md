> **P0 owner device acceptance correction (2026-10-28):** [#81](https://github.com/gamesbyian/Glimpse/issues/81) is the next visual architecture gate: fixed viewport-edge/corner reels independent of scrolling, scroll-behind content, no lower footer, and drag initiation from *all* photo apertures as well as paper. Current APK's reel motion feels promising but its placement and interaction are wrong; visual fidelity to owner reference remains the leading priority, feature breadth a close second. No crashes were reported in this initial test, but functional journeys have not received a complete owner review.

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
- **Merged [#39](https://github.com/gamesbyian/Glimpse/pull/39)** (main `c49955e4`): 320dp/200%-text atlas now refuses a falsely labeled reel capture and locates a genuine visible reel Canvas before accepting the screenshot. Full emulator CI passed; not owner tactile signoff.
- **Merged [#40](https://github.com/gamesbyian/Glimpse/pull/40)** (main `e6f018c2`): verify the actual existing SHA-named offline original before attaching another membership, rather than trusting a filename.
- **Merged [#41](https://github.com/gamesbyian/Glimpse/pull/41)** (main `94a8b515`): dedicated real photographic reel browsing screen beyond the Home summary; on-device optical and touch acceptance remains pending.
- **Merged [#42](https://github.com/gamesbyian/Glimpse/pull/42)** (main `f4305c04`): EXIF-aware bounded decode for Home wallpaper application.
- **Merged [#43](https://github.com/gamesbyian/Glimpse/pull/43)** (main `d9326523`): shared EXIF-aware photo previews, Grid and paper reel; compile and full emulator atlas passed.
- **Merged [#44](https://github.com/gamesbyian/Glimpse/pull/44)** (main `0924b62b`): tactile acknowledgement of settled photo selection, pending real handheld vibration/touch validation.
- **Merged [#45](https://github.com/gamesbyian/Glimpse/pull/45)** (main `c636383d`): prevent stale previously decoded bitmaps from showing under newly selected photo identity during asynchronous load; Native Android CI and emulator atlas passed.
- **Merged [#47](https://github.com/gamesbyian/Glimpse/pull/47)** (main `e4021f99`): bounded local-only, per-photo import START/IMPORTED/DUPLICATE/FAILED/INTERRUPTED journal and restart reconciliation, with no provider URIs, image content, filenames or exception details retained; unit and native emulator CI passed. Full OS process-death injection and per-item retry UI remain open.
- **Merged [#46](https://github.com/gamesbyian/Glimpse/pull/46)** (main `2702b5ad`): native offline Help/Privacy/About with real build identity, truthful picker/shuffle/privacy/sharing disclosures and Back navigation. Two XML-escaped ampersand atlas assertions were corrected; final branch native build and full emulator atlas passed. Storage/reset, redacted diagnostics export and physical accessibility checks remain open.
- **Merged [#48](https://github.com/gamesbyian/Glimpse/pull/48)** (main `eca6e2b4`): retry only failed Photo Picker inputs in the same in-process session, scoped to the originating collection, with provider URIs never persisted; branch native build/JVM and full emulator atlas passed. Provider revocation still requires a fresh picker selection.
- **Merged [#50](https://github.com/gamesbyian/Glimpse/pull/50)** (main `b3e8cfcc`): Help & Privacy now reports a read-only app-private SHA-original and temporary staging file count/byte estimate, using off-main-thread metadata-only scanning, without following symlinks or deleting photos. Four focused JVM inventory tests and native build passed. This is not membership-aware storage cleanup.
- **Merged [#51](https://github.com/gamesbyian/Glimpse/pull/51)** (main `533a46c8`): stale-import cleanup refuses symlinked originals directories as well as symlinked child files, with a real filesystem preservation regression test; branch native compile/JVM passed.
- **Merged [#49](https://github.com/gamesbyian/Glimpse/pull/49)** (main `00dacc09`): Photo Picker destination ID is now captured at launch and restored through Activity recreation; collection existence is verified on return so images cannot silently attach to whichever collection is being browsed later. Former stacked history was rebuilt into one conflict-free main-based commit. Final source head passed native build/JVM and full emulator atlas; real Android picker/process-death acceptance remains open.
- **Merged [#55](https://github.com/gamesbyian/Glimpse/pull/55)** (main `1cad653a`): a shared exact-byte private-original verifier now checks canonical path, expected MIME/filename, byte length and SHA-256 before either outgoing FileProvider share or Android Home wallpaper apply. Four JVM regression tests and branch native build passed; corrupt sources fail before the OS mutation gateway.
- **Merged [#53](https://github.com/gamesbyian/Glimpse/pull/53)** (main `0e320e57`): exported Android ACTION_SEND and ACTION_SEND_MULTIPLE image URI intake with deliberate destination/consent, bounded import and no implicit wallpaper change; native/JVM, full atlas and dedicated API34 real Android intent + exact-byte/restart tests passed. Crop/straighten/edit and real OEM screenshot-toolbar handoff remain open.
- **Merged [#54](https://github.com/gamesbyian/Glimpse/pull/54)** (main `2d72a67c`): Help & Privacy now offers a local-only allowlisted numerical/support diagnostic report, explicit editable preview, Copy/Android Share, and Close; no automatic upload or photo/URI/ID inclusion. Native/JVM and full emulator atlas passed. External recipient and device-accessibility checks remain.
- **Merged [#56](https://github.com/gamesbyian/Glimpse/pull/56)** (main `18c87945`): user-tapped clipboard image URI paste into the selected collection, without background reading or persisted grants. Exact head passed native/JVM, full emulator atlas, actual Android clipboard image/text scenario, incoming-share regression and two real OS Photo Picker workflows. On-device OEM clip grants/200%-font acceptance remain open.

- **Merged [#58](https://github.com/gamesbyian/Glimpse/pull/58)** (main `14327096`): Photo Detail saves a SHA-verified, intact single original to a user-chosen Android DocumentsProvider through native Save As; no private path leakage, no wallpaper mutation. Native/JVM and real emulator document-picker launch/cancel atlas passed; independent third-party provider durability and physical handsets still need acceptance. This is not a library backup.
- **Merged [#59](https://github.com/gamesbyian/Glimpse/pull/59)** (main `d0da9250`): `:app:packageGlimpseDebugApk` creates `app/build/distributions/glimpse-debug.apk` and uploads it in the Android CI artifact, keeping AGP's internal `app-debug.apk` for emulator tools. Native build passed; debug packaging does not imply release signing.
- **Merged [#61](https://github.com/gamesbyian/Glimpse/pull/61)** (main `101e8672`): validated in-place collection rename leaves IDs, photo memberships, wallpaper acknowledgement, active shuffle source and browsing selection unchanged. Native/JVM and full emulator atlas passed.
- **Merged [#62](https://github.com/gamesbyian/Glimpse/pull/62)** (main `bd25e8d6`): user-tapped read-only original-integrity audit in Help & Privacy checks actual Room originals off-main-thread using the shared SHA/size/path/MIME verifier. Only aggregate totals are displayed, no repair/upload/delete; JVM, native, clipboard, incoming-share and branch emulator atlas passed. A subsequent main atlas's offscreen assertion was corrected separately by #65.
- **Merged [#65](https://github.com/gamesbyian/Glimpse/pull/65)** (main `d0a24422`): full emulator atlas scrolls to the actual rendered 22/22 integrity-audit result instead of treating UIAutomator's below-fold omission as source corruption. Corrected full atlas passed.
- **Merged [#63](https://github.com/gamesbyian/Glimpse/pull/63)** (main `a204b5e0`): Photo Detail can add a verified original to another existing collection by attaching only its Room membership. Main/source collection and saved shuffle stay unchanged; native build and earlier source-faithful 22-to-3-photo emulator journey passed. Later combined atlas inherited the unrelated offscreen-audit failure before #65.
- **Merged [#64](https://github.com/gamesbyian/Glimpse/pull/64)** (main `7369ef35`): user-initiated Android collection ZIP Save As with versioned manifest, per-original SHA-256 checks, bounded streaming and exact-byte provider readback. Final branch native/JVM, clipboard, incoming-share and full Android atlas passed; not a complete library/settings backup and no native ZIP restore yet.
- **Merged [#67](https://github.com/gamesbyian/Glimpse/pull/67)** (main `06b2bc7b`): explicit Photo Detail removal from only one collection when another membership preserves the immutable source. Room transactional last-membership guard, no file deletion and no wallpaper/shuffle mutation. Native/JVM and source-faithful full Android atlas passed.
- **Merged [#68](https://github.com/gamesbyian/Glimpse/pull/68)** (main `6e2dd7ab`): Android OpenDocument restores exact-byte validated originals-only collection ZIP into a **new separate Room collection**, with bounded manifest/entries/bytes/pixels, staging cleanup, no ZIP-controlled filesystem paths and atomic membership transaction. Final-head native/JVM and real Android emulator export→restore test passed. This cannot restore historical app settings or guarantee third-party cloud-provider durability.
- **Merged [#69](https://github.com/gamesbyian/Glimpse/pull/69)** (main `0b5aa296`): share one to 25 verified originals from a collection through real Android ACTION_SEND_MULTIPLE Sharesheet, scoped read grants and no wallpaper mutation. Native/JVM, full atlas, incoming-share and clipboard regressions passed. Third-party recipients and owner handset require separate testing.
- **Merged [#73](https://github.com/gamesbyian/Glimpse/pull/73)** (main `08d1964`): Photo Detail on-screen reversible crop-edge and clockwise quarter-turn preview plus Android Save As edited, metadata-free bounded PNG.
- **Merged [#74](https://github.com/gamesbyian/Glimpse/pull/74)** (main `2face50`): guarded removal of a redundant/empty collection container only, preserving all original bytes, memberships elsewhere and the saved shuffle source.
- **Merged [#75](https://github.com/gamesbyian/Glimpse/pull/75)** (main `712557b`): save a rotated/cropped PNG as a separate SHA original in the collection, explicit edited Home Use Now through the existing wallpaper gateway, and durable editable crop recipes in private preferences.
- **Merged [#77](https://github.com/gamesbyian/Glimpse/pull/77)** / **[#78](https://github.com/gamesbyian/Glimpse/pull/78)**: paused same-collection shuffle resumes without discarding the no-repeat bag; symlinked originals root is refused before Photo Picker staging. Real-device/emulator acceptance of the editor and removal flows is still open.

**Immediate product gate:** the 320dp/200%-text reel visibility check is integrated (#39). Independently inspect captured imagery, then put a current debug APK on the owner's Pixel 6 Pro and collect tactile, accessibility and optical observations. Continue the approved **direction** toward physical lower-corner reels, but do not mistake proposed geometry/spacing or CI screenshots for owner approval. Master design PR #6 stays draft and unmerged. Crop, metadata-sanitized exports, platform lifecycle/background timing, widget/Quick Settings coverage and full operation journal are still open.


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

- [x] Bounded privacy-safe per-photo import operation journal and restart reconciliation (#47), plus stale staging cleanup (#37); [ ] real provider/process-death injection, richer diagnostics and per-photo retry UX.
- [ ] Screenshot rotate/crop and original-or-crop sharing (F071/F073) remain partly open. Incoming ACTION_SEND, explicit clipboard paste, single original sharing, collection batch sharing, original Save As and originals-only ZIP export/restore are now implemented. Edited PNG export/editor and edited in-collection copies shipped in #73/#75 (emulator/device acceptance open); full in-app edited sharing, fine straighten, wallpaper safe-area choices and complete screenshot quick finish remain open.
- [ ] Home/Lock/Both capability probes and target-isolated apply acknowledgments.
- [ ] Widgets, Quick Settings and more background policies through the *same* command gateway.
- [ ] Real on-device performance, Doze, OEM, large text, TalkBack, backups and upgrade testing.

## Later research

Only adopt secondary reels when uncoached evidence demonstrates added usefulness over conventional selectors. Keep owner-reviewed references separate from experimental layouts.

**Not a release:** current layout is a semantic prototype; there is a first-hand owner report from a real installed phone, but no comprehensive physical-device acceptance or signed release, and no guarantee of five-minute background updates.
