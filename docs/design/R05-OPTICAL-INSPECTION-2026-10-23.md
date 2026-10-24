# R05 focused optical inspection · 2026-10-23

**Reference type:** draft interaction diagrams, not Glimpse Android screenshots or demonstrated TalkBack output.

## Independently verified source/raster artifact

- Source commit: `5b94cf01e105bbee0224a50e287ebcb3db24e3e8`
- Targeted CI run: [40047595980](https://github.com/gamesbyian/Glimpse/actions/runs/40047595980), automated validation and rendering **passed**.
- Local artifact: two SHA-provenance **401×890** phone PNGs, corresponding to the two exact SVG inputs:
  - `r05-reduced-motion.svg`: SHA256 `33350a2f5eea98df3bb7d371a092040fca479b19a95493ae5990e0e31fcf2d75`
  - `r05-talkback-route.svg`: SHA256 `01f137c9e5bf5a3c1db0fc780b010b7ef8a7d9d194dfde167e6bb9bd2d0a44ee`
- The full two-panel contact sheet and the **native-resolution reduced-motion phone** were opened and inspected, not just checked for nonblank pixels.

## Positive observations

The dedicated reduced-motion route names p12 and a Next action that selects p13 without a spinning wheel. Its Previous, Next and Grid alternatives have large drawn horizontal rectangles. The TalkBack concept separately names photo **12 of 22**, marks it selected and distinguishes **last confirmed Home p04** from selected p12. A four-step focus route is visible and numbered.

## Actual deficiencies and remaining evidence

1. **The reduced-motion illustration simultaneously shows p12 in a large hero and p13 in a smaller highlighted card.** Without unambiguous “before” and “after” qualifiers, this is ambiguous as a *single phone UI state*. Repair the labels or depict two separate stages. The source metadata still says selectedAssetId=p12, so the p13 element must be explicitly a **proposed after-Next state**, not a current selection. Severity: P1 draft-diagram clarity.
2. **The TalkBack illustration draws numbered focus-route controls as if the numbers are on-screen button labels.** Clarify that the numbering is an audit overlay, not expected product UI; the actual spoken output, focus order, disabled-state announcements and focus restoration still require Android capture. Severity: P1 representation hygiene.
3. Both mockups are **semantic diagrams without a photographic reel**. They do not prove equality of the route with the real five-aperture geometry or stable p12 photographic content. Their standalone screens should not be promoted as full visual targets.
4. No native 200% font-scale screenshots, actual TalkBack tree, measured target bounds, reduced-motion settings screenshots or real click-to-selection traces exist in this artifact. GUX-31–39 remain **unverified where they require Android behaviour**.

## Next closure criteria

Preserve these two diagrams as *illustrative accessibility interaction references*. Clarify before/after and numbering in their own draft sources. Then render a separate source-hashed **photo-backed R05 focus-overlay** and obtain matched Android screenshots, hierarchy bounds in dp, UI selection traces for 0/1/3/5/22/60, scale 2.0 and TalkBack focus order. Do not upgrade the first 60-question audit's 18/8/34 scoring from automatic or source-level rendering alone.

### Related CI bug fixed

Initial targeted run [40047475006](https://github.com/gamesbyian/Glimpse/actions/runs/40047475006) failed because the audit validator overwrote the 60 GUX question IDs with R01's numeric photo IDs. The validator now uses `visible_photo_ids` separately; the re-run passed and rendered both candidates. **This was a test variable-shadowing bug, not invalid R05 source references.**


## Targeted correction and second optical review

The draft illustrations were edited after the first run: reduced-motion now labels `BEFORE · p12 (12 of 22)` and `AFTER NEXT · p13 (13 of 22)` and declares the transition target `p13` in its metadata; TalkBack's diagram explicitly says **audit overlay, not UI button text**. [Targeted run 40047904810](https://github.com/gamesbyian/Glimpse/actions/runs/40047904810) passed and emitted 401×890 phone images with source hashes `1d031b2ea995915bb951231fa3bc95a0fe8aa37d50e324cd224a7891d8c9dc87` (motion) and `7165688761aa02cff0d1192b04a3f587001680b2cf70b6a040a05e82adc5f7be` (focus route). **I directly re-opened the revised two-panel PNG at rendered size**: the before/after labels and audit-versus-button distinction are visible and have resolved the initial *annotation-clarity* defects.

**Still missing:** photography-backed focus overlay showing these semantics on the genuine R01 paper reel, uncoached user comprehension, native target bounds, actual Android accessibility tree, TalkBack speech/focus restoration, reduced-motion settings and fontScale=2.0 captures. Do not reinterpret a corrected caption as native proof or change any GUX-31–39 result without independent behaviour evidence.


## Photographic reel focus-overlay extension

A third, image-backed R05 reference now overlays proposed focus order directly on the [R01 photographic paper reel](../../design/proposals/audit-revisions/r01-photo-rest-22.svg): [R05 photo reel focus overlay](../../design/proposals/audit-revisions/r05-photo-reel-focus-overlay.svg). It preserves **all six base64 JPEG fixture payloads identically** to R01, with fixed hero p12, last-confirmed Home p04 and number labels explicitly identified as an **audit overlay, not native UI**. The [targeted three-frame run 40048816204](https://github.com/gamesbyian/Glimpse/actions/runs/40048816204) passed and its native-sized overlay PNG was **opened directly**: the four proposed route regions clearly mark selected photo, Previous, Next and Grid without obscuring the actual wheel. This is substantially more actionable than the earlier two abstract diagrams, but still cannot prove Android accessibility.

That inspection found the Grid highlight was only 36px high, a poor *proposed* touch area. The source was revised to a **48dp-high illustrative target** and guarded by the audit validator; it is explicitly tagged `targetBoundsProposedNotMeasured=true`. **The final adjusted overlay needs its own exact-source re-render before calling that adjustment optically reviewed.** Measured native click bounds and focus restoration still have no proof.


## Final 48dp proposed Grid focus box: source-hash optical recheck

The enlarged target outline in the R05 photographic paper-reel focus overlay passed [targeted run 40049026265](https://github.com/gamesbyian/Glimpse/actions/runs/40049026265), source commit `ccebfa104365acb81bc853f0cc9cbe88723462ae`. A 401×890 crop generated from exact SVG SHA-256 `80bef8638ed0c18d6e91e67fb66a5f89336458ff592738bc9ebd5c3922135f07` was **downloaded and opened**. The enlarged dashed proposed Grid target is clearly visible, positioned between the hero and disc pointer and does not obscure the genuine photo. The proposed targets for Preview, Previous and Next remain visible. The photo fixture bytes match the R01 source, enforced by the audit script; photo content was not substituted or recoloured.

**Disposition: visual proposal optical check passed at that exact source**, not an actual Android hitbox check. `targetBoundsProposedNotMeasured=true` remains compulsory metadata. Real tappable bounds in dp, TalkBack route, keyboard focus, fontScale=2.0 and uncoached use remain **unverified**. No GUX baseline score upgrade.
