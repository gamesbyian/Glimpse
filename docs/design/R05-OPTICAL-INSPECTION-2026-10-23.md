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
