# Production C: independent source-vs-native discrepancy handoff (2026-10-25)

**Scope:** evidence reconciliation only, no promotion of source candidates, no native/physical acceptance or owner approval. Agent C owns this report. Latest refreshed integration PR #6 head `58087cf05a3455ca9c9e4f5a71dddbb42c974d60`; source manifest blob remains the separately recorded fetched evidence from the older selection. Verify exact current blob again at integration before freeze. All paths below are *branch-specific*, not necessarily on `main`.

## Major integration discrepancy: photographic selection

1. Integration's `design/proposals/whole-product-review-selection.v1.json` (blob `44872431ee6ec2c4236aacd3ae0210ba2bab0205`) still selects **older six** references, not A's new rights-audited coordinated alternative:
   - Home `audit-revisions/r03-home-primary-actions-p04.svg`, explicitly warns p04 hero is an illustrative vector.
   - Reel `audit-revisions/r01-photo-primary-wallpaper-action.svg`
   - Grid `audit-revisions/r02-grid-primary-actions-p12.svg`
   - Detail `audit-revisions/r02-photo-detail-actions-p12.svg`
   - Collections `whole-product-candidates/collections-photo-covers.svg`
   - Share `audit-revisions/r02-share-choice.svg`
2. A's independent *unselected* replacement contract `design/proposals/whole-product-candidates/cc0-consistent-20261024/migration-manifest.v1.json` (on PR #16, blob `82522ca26e5d73d5947e174a30dfa5ce03470c8c`) requires the six paths `home.svg`, `reel.svg`, `grid-3column.svg`, `detail.svg`, `collections.svg`, `share.svg` to be treated as **one coordinated photographic alternative**: 13 different independently source-hashed CC0 sample photographs, p04 last app acknowledgement distinct from selected p12, and `androidWallpaperCurrentVerified:false`. Its Grid candidate is three columns, while the selected source remains four columns. **Owner's 3-versus-4 choice is open.**
3. A post-integration PR [#16](https://github.com/gamesbyian/Glimpse/pull/16) latest head `f3c3b9188f28c4b0ea0ee63db0d213dc098e1930` is presently **mergeable:true** against its stale base; **this does not guarantee conflict-free integration onto PR #6's newer head**. Its direct seven-screen optical ledger describes readable 12 image IDs, removal of misleading tile-level CC0 licensing, actual p12 Detail thumbnail and one *unselected* p12 rights-source screen. Seven 401×890 pairs are catalogued in `design/system/reviews/PRODUCTION-A-POSTMERGE-OPTICAL-2026-10-24.md`. Reconcile divergence with today's integration head and revalidate precise latest SHA before integration. Do not silently select a seventh route or assert real source-link navigation in native Android.
4. C's integration-branch provenance `design/assets/production-c-provenance.v1.json` (blob `440d98703151c12f7213397cc64fab28a556c3fa`) contains several older `available-not-integrated` and `primary Home drawing NOT updated` statuses. These remain **accurate about the currently selected older proposal**, but could be misunderstood as stating that **no A alternate SVG** uses the C assets. Revise those status fields *only after integration changes the selection*, preserving prior fixture rights caveats. `scripts/validate-agent-c-cc0-proposals.py` tests six **unselected** coordinated SVGs, not currently selected screens.

### Direct corrective contract for integrator

- Independently reopen and compare #16 latest seven-source PNG/raster ledger against source bytes; resolve #16 nonmergeability before considering it.
- Present the older selected six and A's six-screen CC0 alternative as **alternatives** in the owner review. Do not quietly change the authoritative selection/owner choices. A's Source & Rights page remains supplemental, independently selectable.
- If switching six sources later, update manifest paths and exact SHA snapshot atomically, re-run C image-byte and A photographic validators against the same commit, then repeat all six optical crops. Verify p04/p12 distinction, p06–p17 Grid photo IDs and label legibility, rights metadata only on sample-specific detail routes.
- Maintain `preview != applied != last-app-acknowledged != Android-current-verified`; source art and CI do not certify OS state.

## Native reality, exact latest successful runs

| Evidence | Exact head and runs | What it does prove | What it does **not** prove |
| --- | --- | --- | --- |
| One-image system picker and repeat duplicate, removal and restart | C PR #21 `cbd9abef`; [source/native #40234544360](https://github.com/gamesbyian/Glimpse/actions/runs/40234544360) and [real-picker #40234544366](https://github.com/gamesbyian/Glimpse/actions/runs/40234544366), success | Actual API34 OS picker; original/private JPEG byte identity; duplicate reported without extra row; private photo survives deleting synthetic MediaStore original and app restart | Google Photos cloud album auto-sync, arbitrary providers/OEMs, persistent original provider grant |
| Stale debug collection-name truth | C PR #23 `1257a8c6`; [#40235164580](https://github.com/gamesbyian/Glimpse/actions/runs/40235164580), success | Debug label no longer falsely freezes initial photo count; native 22-case instrumentation reported successful | A production Room count error or full live accessibility acceptance |
| Two-photo picker batch | C PR #24 `619d845f`; [#40236080807](https://github.com/gamesbyian/Glimpse/actions/runs/40236080807), success; artifact `12615176166` | Actual API34 Add(2), Photos:2, two different original/private SHA pairs after public original deletion and process restart; independent artifact gate step succeeded | Physical phone, all URI/provider combinations, wallpaper targets, Doze or owner approval |
| Native C broad suite | [#40236080816](https://github.com/gamesbyian/Glimpse/actions/runs/40236080816), source and emulator jobs both successful; artifacts `12615815252`, `12615571058`, `12615236847` | Completed configured test suite and retained emulator evidence | Physical 320dp/2× TalkBack, keyboard/D-pad focus return, system wallpaper readback or five-minute wake reliability |

Native **320dp/fontScale=2** and touch/focus acceptance in [issue #13](https://github.com/gamesbyian/Glimpse/issues/13) stays **OPEN**. Existing successful instrumentation does not settle TalkBack, IME, interrupted chooser return or real-world device focus. Unselected Sections sheets are source studies only. Foreground five-minute rotation is an *eligible in-app behavior*, not an Android background/Doze timing promise. Google Photos Photo Picker selection is manual, not arbitrary album synchronization.

**Release disposition:** The owner visual package can advance with explicit caveats, but no source selection freeze or native/physical signoff is justified by these results. Integration PR #6 must remain draft; all eight owner choices remain open. No edits to `design/approved/`.

## Later live PR dependency refresh (October 25)

GitHub's direct PR metadata now identifies **#21 mergeable:false** at `cbd9abef` and **#23 mergeable:false** at `1257a8c6` against their stale integration bases `864de860`; do not call either directly mergeable without restack/conflict resolution. #24 remains **mergeable:true against child #21** at `619d845f`; this says nothing about landing the stack on current PR #6. PR #16 is newly **mergeable:true** at `f3c3b918` against its *earlier* integration base. PR #25 is mergeable:true. The integration branch is `58087cf0` and still draft. Exact previous native CI success belongs to original C HEADs, not a future restack; new-head CI is required after substantive restacks. Source-level selection/owner questions remain unresolved. Updated PR metadata is different from visual validation.
