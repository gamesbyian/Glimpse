# Production A · Source-exact integration handoff · October 24, 2026

## Current post-migration handoff · 13 distinct CC0 photo identities, 6 surfaces

Agent A's newest **unselected** all-CC0 candidate package consists of six source-editable, directly rendered 401×890 primary screens: **Home, Reel, three-column Grid, Photo Detail, Collections, and Share**. These preserve thirteen distinct source-identified p04 and p06–p17 JPEG photographs obtained and independently recorded by C #12 as uploader-declared CC0, with exact derived SHA-256 and original-source URLs. The inherited selected six-screen reference set still embeds rights-unverified art1–art6 in some places; **do not mix old selected files and new replacements while claiming continuity**.

[Atomic selection manifest](../../proposals/whole-product-candidates/cc0-consistent-20261024/migration-manifest.v1.json) maps all six new-to-old candidate paths and includes all thirteen source identifiers, bytes hashes, authors and declared reuse-rights sources. The [fail-closed six-screen validator](../validate-cross-surface-cc0-migration.py) decodes **all 27 embedded JPEG instances**, verifies correct photo IDs and source hashes, ensures p12 is unchanged across Home, Reel, Grid, Detail, Collection cover, and all three Share displays, preserves the original p12 / distinct derived p12c choice, p04 last-app-acknowledged versus *Android-current unknown*, and explicitly requires no owner/native approval.

**Actual successful focused CI:** [run #40141035276](https://github.com/gamesbyian/Glimpse/actions/runs/40141035276), plus earlier [#40140765700](https://github.com/gamesbyian/Glimpse/actions/runs/40140765700) including the first six-screen [401×890 direct optical contact](ci-renders/cc0-consistent-six-screen-contact.jpg). Corrected the Share candidate after direct inspection: real 48px vector Close, clean title reflow and explicit OS Sharesheet-delivery uncertainty. Its subsequent current PNG was also directly inspected and shows no visible action/nav overlap.

**Exact CI-produced SHA-256 source/raster pairs** from GitHub machine ledger blob `47fde5502fba852a881eff6b6dd0bf852bd19fb8`:

| Six synchronized proposal sources | Source SHA-256 | PNG 401×890 SHA-256 |
| --- | --- | --- |
| [home.svg](../../proposals/whole-product-candidates/cc0-consistent-20261024/home.svg) | `b436a27959c0b78e81d2d66c342d8bca605595904b5ff97530ea9331062f33d8` | `342d81ce3d5396e49feb3a9b5ba1b331441d79e327fb2d4992114f92bcd50ff9` |
| [reel.svg](../../proposals/whole-product-candidates/cc0-consistent-20261024/reel.svg) | `5d562b3f8420170e537ac27e5d10416414747028f2af2924f4ff87ceaa51400e` | `ef6111ff9cd660bb4dae5b954e5d95bcdaaf0e4ab6c5ac779ab50fa14070224c` |
| [grid-3column.svg](../../proposals/whole-product-candidates/cc0-consistent-20261024/grid-3column.svg) | `59a22728243ef80b3f1393927b8cca12f4bbbe7559bc937ce514e789a1d098af` | `f90a6938e58fd6f7b9e38710dc9b96b1d51e703b540c77c4093486657f38fb02` |
| [detail.svg](../../proposals/whole-product-candidates/cc0-consistent-20261024/detail.svg) | `a2958974c25a520b3f3574866432671ad44379cb35ce976dd1cbcb568be38303` | `f7fe91443fde13dbe3017f8eb870c843ab96fa6d4dd2b23e56fe2f521d82a482` |
| [collections.svg](../../proposals/whole-product-candidates/cc0-consistent-20261024/collections.svg) | `a738f6cc22cee1ed40f30d0efb1703cecefe771bf519d28da205d15049e54339` | `5a5d19efc765768847e023f570d3f24c2758c110f64ccba79af9bf14510b32c9` |
| [share.svg](../../proposals/whole-product-candidates/cc0-consistent-20261024/share.svg) | `e379e1e5941ca293a4e6a776b2682b3470d2cd6f912b8a07cc6d23d4b78e491c` | `5cca4ab2d837a4fa3670393cf2143d4c0beb12290da1f627e636ac54494e9d9b` |

**Integration-only atomic action:** Evaluate the six-screen package as one migration choice. If owner/integrator prefers source-cleared new photos, update all six selected paths, exact reviewer thumbnails and SHA source snapshot, align Agent C's cross-surface proof mode to `rights-verified-replacements`, and rerun native/source/optical checks. Do not independently swap one p12 display for its old research image. Decide 3 vs 4 Grid columns and the photographic aesthetic with the owner. All eight owner questions, real physical Android, 320dp/fontScale2, TalkBack, OS picker and apply readback are still pending. No PR #6 merge and no `design/approved/` edits.

The sections below retain the earlier 18-primary / four-initial-candidate audit history. They remain valid as historical observations but are **superseded for the latest six-photo-migration SHA hashes by the table above**.

## Actual source deliverables
- **18 editable primary surface phone plates** revised, including Quick Import: >=50px Rotate/Crop/Original and Show next/Apply, 52px Save copy, 48px vector Close, and saving emphatically does NOT apply. Settings, automation, destinations, Activity, diagnostics, recovery, onboarding, source details, editor, accessibility, widgets, shortcuts, backup and more are task-specific. This is source work, not merely a plan or screen count.
- Working source-editable grammar at `design/system/README.md`, tokens at `tokens.v0.1.json`, source/raster validator at `validate-production-a.py` and targeted byte/identity validators at `validate-home-photo-candidate.py`, `validate-grid-identity-candidate.py`, `validate-grid-three-column.py`.
- Four **unselected** alternatives: genuine C-verified Philsacor CC0 photo p04 Home with p12 selection separate; source-integrity Grid in existing 4-column layout; 3-column Grid aligned to actual C-observed API34 adaptive layout; 320dp / large-text Sections-sheet proposal. All three photo candidates use actual p06/p07/p04 source bytes from C #12, not claimed rights for p10–p15 research images. Four slots p08/p09/p16/p17 explicitly illustrate blocked source photographs.
- All eighteen 401×890 crops and four alternate candidate images were rasterized through A's dedicated workflow and preserved at `design/system/reviews/ci-renders/`. Independent direct pixel inspection opened all three latest 6-phone JPEG contact sheets, Home p04, both Grid candidates, and compact alternative. Fixed non-font icon tofu, nav selection overlap, Quick Import missing Close, redundant Automation control, Grid fallback duplicate tiny captions, and letter-crowding in its rights labels. This is *static optical* evidence, never emulator/native proof.

## Exact current 18 source SHA-256 / raster SHA-256

All eighteen rows are copied from the generated machine index `ci-renders/ci-raster-ledger.json`, which records **SHA-256**, not Git blob SHA-1. Every crop is 401×890. The index intentionally says `independentlyOpticallyInspected: false` until a separate human review protocol records an independent acceptance, even though A has personally inspected pixels.

| Editable source (design/proposals/plates/) | Source SHA-256 | 401×890 PNG SHA-256 |
| --- | --- | --- |
| accessibility-preferences | `9804c614aab05177dcba76c1a688ebd7da80dda3c368702cae16834fc7081bed` | `08c454c740bf56c3539d2814f5ce04dfe5c1a0d4061e729166d089f8864d67ca` |
| activity | `286377bb558373e01240f67a55c45e3c36f7652efafa77b617f7b225a3d33cd3` | `3c5414cadc474c615fea27e3692a21cba388f3ec882213e3123fb4a8ffd96b95` |
| automation-editor | `bd9a4bdedb0d4457412a4a426bb7cd844bc0e3e7c98f1b785f031155435d3cf8` | `244ca03074f8cabc16172d995a78dfbe005016dcc2712a01b09b1ced057dbd27` |
| automation | `cc5964d646fed03c39abc337582d2b0f53cfeccfeecfe375f4d78dcd6b80f97f` | `f73afe2b1c8d688624e42b2c2c9814bb0b594a5fe3c14164cd0c23a2080f2156` |
| backup-and-restore | `3a306d36c9c1b124939f06a8f96d33bf54b7918bc76007b98477156bfff1d377` | `c8323444888652ac0dd7e3955f69f6022e93565506ffc037717f44a080616fe6` |
| destinations | `bc2da412df3c3ca53607c483a0757e1498585d3b9fbdb759e338b038be67c107` | `d5e92e03119d2f5b2dd644e4c016f3d490b7e85d889798936be9cd97d0411fd5` |
| diagnostics | `2ec27e25e4375039f6ce7a729674cf889b269f357caf87abe2f8ec0f338ffc76` | `dcbf2c4ea940cb1df96d70148f4bd006c9edc226ed27dd3b40ad4f8eafd0980f` |
| discover | `b2b92b6ae7cdaeb2c2eb7052ecc0579f203dd52a33d30fb8f5b88e03105a0e68` | `37e6f8122b257777e47ff700012951d7c91bf6d6f3d63919ff85cfab924bfc34` |
| error-empty-permission | `7e0a79132c292352464d85e50f0a32bfe22c47d22a5eabf5917e3f04db67880d` | `677b731159c44d416af783d02fb648eb946100e4380bd94c24d9f70562adce18` |
| image-editor | `04cd81ac63c5ab92098af17f62dc2cb9e49b5c9f155051e75ab32f6bdb323f48` | `a3ca82f93ec5646b4f5a78f54d653ca6039d1d967af205bc695acf3990adb7de` |
| onboarding | `ba6d900665e19405ce3ba725b3c578bd6b0e2f084c8d314b563a951d90ef937a` | `c5f9c90282049c9a6e8a6eca8cdc7f9c915f3df46c98e0c62995bdb1a7bffabd` |
| quick-import | `48670018420d33aa7de4b33348fd0032cf20e00a1e52ffeb26950c9bf24d763a` | `8e2e16bf5447f72bd040b9384006f477e1ac5bcecdd1887db724c7111fb20575` |
| quick-settings-and-shortcuts | `db3e585bd076538f191c682d87f7f3c0889cca80d4d3f9ec5945c9dafc54336b` | `78b60a00bc760399b17e34e07e55ec5ef34e4d4f2d0cd3219d2980925e9b59a3` |
| quickstart | `c703b20c763f663151ebadaa0fa14b29b0241afbc688a3818b216415a3e5910c` | `4765ecf634a79bf46e81f8b661b9fae7fc75ab21078ad4156dab9ca828ca175b` |
| schedule-simulator | `67f4aba1c3f2a6c42a4337a10a44df9931b06fbdb5c26294431ccc0b39cf5a5b` | `a62cf9a481c27a79a9f6023e12866dc38b6bb8db7cb636053c8e76505f6326c7` |
| settings | `38759466ffd4a7c5d0b8534a536db8b46e495507ce73e123760f273b692e9b95` | `0ba3cae029dd8eec35c097340a1f1644b486dc4997c16abc608d90028af2c040` |
| source-detail | `5e6dec1cf8cb8503b5d33a59e13956c63182269ae249bae6085a6ca0c8d84bf3` | `392b935f68b561aa74acd57cb042e9dcfb9233ca5ec5fd7a7aba356de5b4893c` |
| widgets | `76063da0380c74d7f659d3cda0d4fe44bdb81a066368d97b648c1a77813f78a2` | `e8d2e66a1c3df9474abb9fed824a2399f85874182004313883aa8bcbe0d531c5` |

## Exact four unselected alternative source / raster SHA-256

| Candidate (whole-product-candidates/) | Source SHA-256 | Raster SHA-256 |
| --- | --- | --- |
| home-p04-photographic-cc0.svg | `ad0c7e3f66e3a2253c56a93338fd21c332f72d4b84e7917bf4348fb7eed12cdc` | `3714e960ea9963387efcbcec611c83ad51a553f41e29b71d544cc9964bda1c52` |
| grid-photography-provenance-p12.svg | `d13dc3374af934a48aecd5fafb7f0d3ef2cdf4be398671d08bd71b52b6566144` | `b01bc473c784de3a0f70be0737342be52fe37c5bcaa0d58fdf3b68200add8be8` |
| grid-3column-native-aligned-p12.svg | `f8b5a5edf3c84f594a03b432825c431bfcf56db2d094402b70baecede5cd6f83` | `d1cd722a54a98c940f166f5db8904c056c3e592aa04a77a6ff08a034b0f69f44` |
| compact-320dp-200pct-sections-sheet.svg | `b4a0b5419b3860ea9176ae28e3f2d4589f964a113c8debcd8c6feab18b46157a` | `90a99cb2ad4187576cb989cd5d4270fb5ad67a27375d6bda2d5894c3fba3de85` |

The source/raster manifest is generated by a real CI job, not by manually invented digest values. A earlier committed [optical ledger](PRIMARY-A-EVIDENCE-2026-10-24.md) documents the 17-source initial before/after SHA-1 and direct 401 crop observations; **that is a historical snapshot**, superseded for current post-edit SHA-256 by this machine index.

## CI execution and limits
- First verified successful targeted pull-request CI run: [#40136905984](https://github.com/gamesbyian/Glimpse/actions/runs/40136905984). Its steps passed source XML/contract, 18 direct CairoSVG/Pillow crop renders, exact p04 JPEG identity and raster packaging. Follow-up A workflow code/source updates preserved additional candidate PNGs and source-hashed machine index in GitHub; a later PR check may display `action_required` with no jobs due GitHub permission/approval policy, so **do not reclassify that as code-green or code-red**. The source-indexed checked-in binary outputs and completed prior run are inspectable.
- Reproduce: `python3 design/system/validate-production-a.py --render-output /tmp/glimpse-a-crops`; `python3 design/system/validate-home-photo-candidate.py`; `python3 design/system/validate-grid-identity-candidate.py`; `python3 design/system/validate-grid-three-column.py`.
- Source-only full `scripts/validate-visual-spec.py` passed in initial A CI. An older shared test `scripts/validate-primary-context-truth.py` hardcodes the superseded 44px Quick Import action rectangle; **integration must update that test** for 50px controls rather than regress the design.

## Integration-only decisions and remaining blockers
1. **Grid 3 versus 4 columns**: A preserves both source-editable image-truth candidates and reports C's real API34 native **three adaptive columns**. Integration and owner must decide the design and align implementation, with 320dp/200%-font, TalkBack and hitbox evidence. Neither image variation is selected in the shared manifest.
2. **Photo clearance**: verified source bytes for CC0 p04/p06/p07. p08/p09/p16/p17 original distinct SVG fallbacks are visually honest but still lack source photographs. p10–p15 pre-existing research JPEGs have unverified reuse rights; do not elevate rights status or declare photographic sample completeness.
3. **Exact OS/application truth**: p04 = last acknowledged by Glimpse, p12 = selected preview, Android's current wallpaper = *not read back*. Saving, selection, preview or Sharesheet launching never constitute applying/delivery. Five-minute timing is best effort.
4. **Native accessibility blocker #13**: C's real prior API34 320dp/fontScale=2 UIAutomator measurement found Reel/Grid filter chips **38.9dp intrinsically high**, below A's 48dp intent; this is distinct from partially viewport-clipped buttons. C subsequently proposed native FilterChip min-height 48dp and compact New collection; a new API34 pass and actual TalkBack focus evidence are still needed. 320dp/fontScale=2 full scroll reachability, actual hit targets, system chooser focus return and screen-reader semantics remain unaccepted despite source geometry. A's sections-sheet alternative has been pixel-inspected, but is NOT native proof or a unilateral nav model choice.
5. **Six previously selected core families remain outside the 18 revised primary plates**: Home, Collections, Reel, Grid, Share, and Photo Detail. These have older distinct action/identity sources that require independent reconciliation by Integration #10. Quick Import is now in A's changed plate set (18), and its previous 44px action test is outdated.
6. **Human owner questions**: all eight stay unanswered, with `design/approved/` untouched. PR #6 remains draft, unmerged. Integration alone controls reviewer manifest, source snapshot, CI orchestration on its branch, and owner-review status.

**Handoff request:** Run independent checkout validators, compare all four candidates in the reviewer, reconcile rights and native evidence, repair shared Quick Import assertion, explicitly select 3/4 Grid path only after evidence, and preserve no-approval labels. Do not silently merge draft PR #6 or claim final acceptance.
