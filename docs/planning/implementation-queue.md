# Glimpse native implementation queue

**Issue [#1](https://github.com/gamesbyian/Glimpse/issues/1) begins native development.** Historical Waltz specifications remain evidence, and [current decisions](current-decisions.md) retain authority.

## Priority update: real owner testing, 2026-10-23

A real installation successfully imported **22 Google Photos images** through the standard Android picker, set Home wallpaper and activated/observed five-minute shuffle with accurate visible status, with no reported crashes or sizing issues. Owner explicitly called the app generic and its present horizontal slot-strip **not a genuine Glimpse reel**. The owner expected to swipe across all 22 images, but the current selector is a **rounded rectangle with no circular edge**, exposes only five slots, and appears not to fill its visible space correctly until first interaction. Do not assume an observed preference for circular-edge dragging. See [actual owner observation](../quality/first-slice-evidence.md).

**Highest-priority next milestone:** replace the placeholder selector with the proposed large-window continuous tactile offscreen photo reel and working drag/swipe/settle traversal for the entire finite collection. Audit stable ID-to-aperture mapping on 0/1/3/5/22/60-item sets; validate boundary behavior, interrupted gestures, reduced motion, TalkBack/Previous/Next parity and Reel↔Grid selection identity. Do not broaden unrelated backend features first or solicit visual approval of an unimplemented design. Preserve the already-working import/apply/shuffle pathways.

## Native visual-inspection infrastructure

- [x] Debug-only deterministic test datasets feed **real Room and MainActivity**; six fixed artworks gain unique numbered indices.
- [x] Android screenshot/hierarchy collector produces PNGs, accessibility XML, control-size diagnostics, manifest and offline gallery.
- [x] Dedicated opt-in emulator workflow, modeled on ROYGBIV's targeted atlas rather than permanent expensive CI.
- [ ] Independently verify first atlas run and inspect output screenshots against the selected reference geometry; repair tooling failures as needed.
- [ ] Use atlas whenever the actual circular reel, photo window sizing, navigation and accessibility change; keep independent human-device approval gate.

## P0: first real debug APK

- [x] Android project scaffold, Kotlin/Compose, Room/KSP, Gradle wrapper.
- [x] Photo Picker snapshot imports with SHA deduplication and immutable private copies.
- [x] Finite Reel and Grid with preview-only browsing, explicit Home apply.
- [x] Serialized WallpaperManager gateway, operation pending/uncertain status, active-use no-repeat shuffle.
- [x] Pure shuffle tests, narrow Android build workflow, pre-observation task ledger.
- [x] Android SDK 37.0 package workaround, compiled debug APK, passed JVM test task, and uploaded [debug artifact](https://github.com/gamesbyian/Glimpse/actions/runs/39904252446). Not a signed release or proof of device behavior.
- [ ] Test on Android phone, provider handling, process death, airplane mode and font/accessibility.
- [ ] Record actual uncoached actions, misunderstandings, error recovery and results against [predictions](../quality/first-slice-evidence.md).
- [ ] Implement the approved large-window paper reel's meaningful gestures/motion, preserving Grid and ordinary buttons.

## P1: daily use beyond the bounded slice

- [ ] Persistent import journal and per-photo retry; cleanup of stranded private files after interrupted import.
- [ ] Screenshot rotate/crop and original-or-crop sharing via Android Sharesheet/FileProvider (F071/F073).
- [ ] Home/Lock/Both capability probes and target-isolated apply acknowledgments.
- [ ] Widgets, Quick Settings and more background policies through the *same* command gateway.
- [ ] Real on-device performance, Doze, OEM, large text, TalkBack, backups and upgrade testing.

## Later research

Only adopt secondary reels when uncoached evidence demonstrates added usefulness over conventional selectors. Keep owner-reviewed references separate from experimental layouts.

**Not a release:** current layout is a semantic prototype; no signed APK, no device acceptance, no human observations and no guarantee of five-minute background updates.
