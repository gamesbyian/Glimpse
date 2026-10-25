# Production A · Primary visual evidence ledger · October 24, 2026
**DRAFT.** [Issue #7](https://github.com/gamesbyian/Glimpse/issues/7), dependent [draft PR #11](https://github.com/gamesbyian/Glimpse/pull/11), base draft PR #6 head `b58de11`; current A HEAD at ledger creation `7dcce54a586740dea25e9977c42d06f478379e5b`. This ledger is source + seventeen static optical inspections only. **Zero Android/user/owner approvals.**

## Actual source edits, not just coverage reports
- **17** `design/proposals/plates/*.svg` primary phone surfaces structurally revised with task-first treatment, explicit route/action/outcome, state truth and expanded 48px back/overflow affordances. [Working design tokens](../tokens.v0.1.json), [grammar](../README.md) and new [focused source/raster validator](../validate-production-a.py).
- New distinct editable [photographic Home p04 candidate](../../proposals/whole-product-candidates/home-p04-photographic-cc0.svg) uses Agent C's independently verified **Philsacor CC0 1.0** image, **derived JPEG SHA-256 `20d6a8dab7e5eed0fbbbb0d98de799487512264aa7d0cf7ced701944683e2bd0`**, source [Wikimedia Commons](https://commons.wikimedia.org/wiki/File:Random_landscape_photo_I_took.jpg), provenance in C [PR #12](https://github.com/gamesbyian/Glimpse/pull/12). This alternate embeds the exact source JPEG and retains selected p12 independent of last-acknowledged p04, with *current* OS wallpaper unknown. **Candidate not source-raster inspected or selected/owner approved**; integration must independently inspect the crop and reconcile C's asset before considering selection. No cross-lane modifications made.
- Before source Git blobs refer to PR #6's initial `b58de11` tree; after blobs refer to this A snapshot. Git blob SHA-1 fingerprints are labeled as such, never passed off as SHA-256. All sources remain editable.

| Primary plate | Before Git blob SHA-1 | After Git blob SHA-1 | Optical disposition |
| --- | --- | --- | --- |
| settings | `0c86fd59d77434e3b068f845571124e6d30c825f` | `ee473d39a7819f7682082d85fb5a2d992bb21a2f` | Opened 401×890 pixels |
| quickstart | `79dea99512372b64bef5f360bb0cc327af3b1e16` | `e5295e553273512772ee1feac3c16402a9598da4` | Opened 401×890 pixels |
| discover | `5cfa397dbcf5209307bbff2febb579b83dbbda15` | `c6a505ac7fc1f0340a040d16fc4dc770ac55441b` | Opened 401×890 pixels |
| destinations | `3f082d79be03f6294315ab0f582c8b51fc4505b3` | `d99c4e2e94640afc95d61fb789634c5682d1101f` | Opened 401×890 pixels; exact source hash matched |
| automation | `11dd53b67df6a6abfb9b4e1ee1f35b0ca0ffca09` | `55e080c34f42419f6b137f23b7cced3ea10accc0` | Opened 401×890 pixels |
| activity | `3230a5408d315af60845de3f3d08af1f21c9558a` | `d3f0322ff625e5112f2624651ea686c859a87b17` | Opened 401×890 pixels; exact source hash matched |
| image-editor | `49e882b403ec70f5d8332d9bff82f298242548d1` | `c587ab578336d6a481594255072f164308281375` | Opened 401×890 pixels; exact source hash matched |
| automation-editor | `39ccc23fa8e42a9defb58e713f06076eb1ec5d77` | `7551955f2d480b64139c1349aa825b3330e58153` | Opened 401×890 pixels; exact source hash matched |
| source-detail | `d677e2b55790b7ddc0160c8dc7d72a60ff7f4d2f` | `1aab880a4e7c4f363a90770400a8e374ce060e04` | Opened 401×890 pixels; exact source hash matched |
| schedule-simulator | `653dce4609142d6f76c2a65de7e234932f62e3f7` | `33039d08037368d95054fd578086245fc6a06e42` | Opened 401×890 pixels; exact source hash matched |
| diagnostics | `22cae9aed6a69a8e1ac8ed23d5d90f09e73568a2` | `d4383a053feb89a992b0a35de89e5013ccf8ebff` | Opened 401×890 pixels |
| widgets | `a908564d5c5e25fbe60816127c88152f1ac18d96` | `d208cf2d451d5f318cd984e1310bcbf91f3109ef` | Opened 401×890 pixels; exact source hash matched |
| quick-settings-and-shortcuts | `fb9a89294ab9aa5fcdae968fe9ca26846b45debc` | `e3f337cc5538b3743e99846ffd9e789fc28119c4` | Opened 401×890 pixels; exact source hash matched |
| backup-and-restore | `87f4f5d794e13dcb32eb3b731e6edd550c3a3164` | `80a48e112c466952d868afa9624f52385dc2f68b` | Opened 401×890 pixels; exact source hash matched |
| onboarding | `a499b0666d4fa3398661e4109502da236221c48f` | `2d2d3aaf2c1bb4179b22fb5a120b301b8e1cbb00` | Opened 401×890 pixels; exact source hash matched |
| error-empty-permission | `87329647ff8a45b2498e04ec448cf71b647350ab` | `da0a1286df72271e3f4024774a23db2e7c872ab2` | Opened 401×890 pixels; exact source hash matched |
| accessibility-preferences | `bd1f7f8ae43186bbc480137f7fb326ef5eba2b8d` | `0ad810ce315800e710e331006ccabbbae135fa16` | Opened 401×890 pixels; exact source hash matched |

## Five exact-source, actually viewed phone renders

An editable [five-phone contact SVG](primary-five-exact-source-contact.v0.1.svg) is compiled **directly from the five exact committed current phone regions** (x=15,y=14,w=401,h=890). The same region markup was independently reconstructed, run through CairoSVG 401×890 and **directly opened** at phone scale as PNGs plus contact JPEG. SHA-256 equality of **all five** phone-region strings between current GitHub source and locally rendered source was explicitly confirmed; `sha256("abc")` was sanity checked. The JPEG contact's bytes in this session are SHA-256 `3dfe7937f82dc72488a14af46d20f5e6a4cc94f5a5f5252b0dad99c582d54c0d`. The JPEG exists as a conversation artifact, while the GitHub SVG contact is a persistent **source-editable** reconstruction; no binary PNG claims are made about GitHub-hosted artifacts.

| Surface | Exact committed phone-region SHA-256 | Opened PNG SHA-256 | Direct optical observation |
| --- | --- | --- | --- |
| settings | `79573438dfd38f363386a7c2c078d261ed1cd1a6b861a65e912a9a58f077e455` | `c0fff09a09103b573d814a9291e8a851fb8025b0009f930278e89320c8f97139` | Legible 7-route task grouping, including newly added network/privacy; no nav overlap; clear 48px overflow. |
| quickstart | `ebd11c6afdca9635620e046498bfe4ad1df24defb56b703a476d356081f21296` | `2497052acd1818ecf95c9d66e9ee5123d698413e94a57b61d45d2ba269253f33` | Selected Home and conditional Lock distinguishable; explicit best-effort interval and call to action; cancellation copy clears action area. |
| discover | `b36a915df3853f0f8481175a5d588ff4d349193f1d4c847a879b9a4375398db0` | `a9b46a3d50f6e2ea11cd660de32048b525de360ad50e15bb10d208c15d4f507d` | Search control, three provenance-labelled sources and rights warning legible; unsupported search glyph replaced with vector. |
| automation | `5f7dcedfe083dfc57b040ab8a276d6ecf165b5bb7df288cfeccda2a51e25917c` | `e7d9c9d69b3e9535864447f811bd4a55d13d4b23b1d14f1a8c39f63de89a5d72` | Foreground active state, independent Change Home / Pause actions and schedule caveat legible; rule route clearly subordinate. |
| diagnostics | `7ca7c7d1432aa0f659b3bb8c47622341a2013a2642d066170792fbd7ad3a8c9d` | `67d4b68ad104f339ef64928bd07375daf5a0502648470f20659c66f4e80d6f99` | Unverified Android state unambiguously separate from app acknowledgements; event chronology, failures and retry path legible. |

### Remaining 12 exact-source optical inspections

Three more persistent editable contacts [four-phone contact 1](primary-four-contact-1.v0.1.svg), [contact 2](primary-four-contact-2.v0.1.svg), [contact 3](primary-four-contact-3.v0.1.svg) contain the remaining twelve **current exact GitHub phone-region source fragments**. Their local CairoSVG 401×890 PNG crops were all opened and visually inspected on this production pass, and every one was independently SHA-256 compared byte-for-byte against the repository source after the final correction. Local JPEG contact SHA-256: contact 1 `1729efa171165593ac8f3882812b992ddfd677017b75a00d4450bef4bb681f8e`, contact 2 `1d54cc87b4f40f6f6d9fd952edecb1b7a1a448f1c92fb225fe21402f941a3fc9`, contact 3 `42417cef3c396790e4481e437368b699ba227844be7ccc79dd2a6fe5b2120aae`. Locally produced JPEGs are **conversation outputs**, not checked-in binary artifacts.

| Surface | Exact live current phone-region SHA-256 | Opened crop PNG SHA-256 | Direct optical observation |
| --- | --- | --- | --- |
| destinations | `1dab3f1cf77471988a6b650b20aca4b9e751496053f1ffa5f5126717e9503496` | `1715c6c94ff58186c42703c0e2454891156f5f525a9ada04802cdd54624d977a` | Home selected, Lock unsupported explicitly, preview state distinct and Android handoff label readable. |
| activity | `5b5a96ac0fa72042d11aa90b1dd159743cd25803c9535ce90f82395304860f7f` | `033361235ed64a09771627e4347590ac805d9ac682d75e708e640a4fa5c3e73e` | Event timestamps ordered, app acknowledgement vs Android status clearly separated, retry requires review. |
| image-editor | `9678ea5afc82c1dd4e007fe2fdef3810ff786a91b33d95d8383e0ed216e8e547` | `95723b9fc192c1fb9d37f03779578c2cb2313af2d7d84320da6beb6164b57717` | Illustrative art labeled, copy-only semantics prominent, Save and Cancel visible. |
| automation-editor | `d202b2aed28ca87abc11fb881934249d6fbab7c9acef0d945834d27c4174d654` | `0b4a09e36e6d3823f21e3250847b7359d5d76b4170749192671558a0968145de` | Caught input glyph tofu; replaced with labeled rule-name field and vector pencil; reopened after fix, no overlap. |
| source-detail | `464e2bc0c888b5d0c2a52f73c4f526bcc11e7b247205fae63ff05d6876dc1432` | `683740539ec5e49c22cc98821945636a3ee6730c569099399514707b1e6b630c` | Illustrative art honestly flagged; provenance, rights and picker route grouped clearly. |
| schedule-simulator | `6ed38e839328b5d1620407359df7220834129f9d32f4ef335e41c41f604acdbe` | `8d418f1408f09d0a2901db7d19c98706965b48bb6df972c7d6a5898e212f7b91` | Simulation-not-execution banner and 00:00/00:05/00:10 hypotheticals readable. |
| widgets | `11dde7c40519924d2c9bb9e3fdc17d15348304ce502c145b05c11205da6c3c31` | `48ea37c889b37dd6bb8d871daeea83f19d84eab651497aecb5c41e0124381003` | Compact widget example distinct from native picker, state provenance and launcher action legible. |
| quick-settings-and-shortcuts | `96a6a8303ee78e3268076ee1b63956258b9d86ffbe9ea8f77bcfe60a0e0bd676` | `6a5c22e784799768e0e4af31f72daf96b4ec57c18ca0e2058af439694272b174` | Explicit three shortcuts; static Quick Settings example and OS placement caveat visible. |
| backup-and-restore | `55cb91cb6026c27869eeb39fa29362bf4f1069b1a3a1898f48877782d7ff4879` | `ad5a84583b6dee94e9acf9230c2808eb205dcf0c9b7c8ace8490560869fdad64` | Export/restore separated, photos inclusion off by default, conflict/rollback caution in readable area. |
| onboarding | `103ab6e3f819791ee4b022fdc14b9abea4c634867705910349092a0fadbc91c8` | `ac2d16138b971e520ed3f6ab667aac662393e4e3c3697a28e0de4ead1a0d313e` | Illustration labeled sample, reel vs Grid choice plus explicit non-apply and source picker. |
| error-empty-permission | `ed1b2857f1aae942d1eb0c5c13948bde8519559be451036d55719fa80a322194` | `291124d762f701ace5698684a4392e4f632c5043db84ba50c00b34ad77aaff49` | Empty-library state distinct, import first step and separate picker recovery visible. |
| accessibility-preferences | `afcad6193ae4a70192630d19848553e7f66e29d858ae53dfcd22183bc2214367` | `8f501394d8e583dceb446c087b3bfbee4a94dce3a6ba06c833fa63137fddc3d0` | Reduce motion, Grid, Prev/Next and native-200% caveat readable without crowding at reference size. |

The new [extreme 320dp/200%-type Sections-sheet responsive candidate](../../proposals/whole-product-candidates/compact-320dp-200pct-sections-sheet.svg) is a **separate unselected static alternative** for [issue #13](https://github.com/gamesbyian/Glimpse/issues/13): it does not prove native reflow or replace the normal 4-destination model without owner/integration consent.

CairoSVG plus Pillow targeted test command when checking out these sources:
```sh
python3 design/system/validate-production-a.py
python3 design/system/validate-production-a.py --render-output /tmp/glimpse-a-phone-crops
```
The validator checks XML/palette/unapproved status, phone geometry, expected live phone text and 48px header affordances, then optionally rasterizes actual sources and emits per-file SHA-256. It does **not** automatically promote rendered files to `independentlyOpticallyInspected=true`. The focused local equivalent rendered **17/17 successful**, and **17/17 live repository phone-region hashes** matched exact local CairoSVG source before direct optical inspection. Full checkout 17/17 validator execution and actual GitHub Actions run remain **unperformed** here because the available connector cannot dispatch a workflow, and existing scoped workflows only push on the integration branch; do not imply CI-green from committed code.

## Unresolved decision/acceptance blockers
1. All **17 of 17 revised plates were rendered, source-hash matched and directly inspected at 401×890**. Full 320dp and 200% font stress still need native reflow and TalkBack inspection, and no static pixel proof establishes hitbox/focus behavior.
2. Resolve selected seven core source choices not rewritten here (Home, Collections, Reel, Grid, Quick Import, Share, Photo Detail). The four action-complete core sources stay in PR #6; C p04 alternate and other photographic fixtures need rights/stable identity before changing the shared selection.
3. Agent B's [320dp/200%-text block #13](https://github.com/gamesbyian/Glimpse/issues/13) is material; Android runtime, TalkBack, focus return, picker/chooser transaction, 0/1/3/5/22/60 parity and OS scheduling still **unverified**. Do not score the 60-question audit upward because of SVGs.
4. Agent C's remaining Grid images p08/p09/p16/p17 not cleared; art1–6 original research rights unresolved. No speculative license acceptance.
5. Integration #10 alone edits 24+34 selection, SHA freeze, reviewer atlas, #6 branch, workflow orchestration or merges. Retain all eight human choice questions, no edits to `design/approved/`.

**Disposition: production implementation substantial; all seventeen new plates source-matched, independently optically viewed at 401×890; full checkout CI and all final owner/native gates pending.**
