# Production C · photographic source acceptance ledger · 2026-10-24

**Scope:** true source-image identities, source rights metadata and exact bytes. **Provisional asset sourcing does not approve final screen aesthetics, native Android behavior or personal-photo rights.**

All 13 image identities required by the currently visible Home/Reel/Grid/Detail reference scenario (p04, p06–p17) have separately sourced **Wikimedia Commons uploader-labeled own-work CC0 1.0** JPEG candidates. Files below are metadata-stripped JPEG derivatives. C independently inspected actual rendered JPEG content; some sources, notably p04, are visually subdued. This asset availability should not force an owner aesthetic decision.

| Photo ID | Independent uploader-claimed CC0 source | Derived SHA-256 | Candidate role |
| --- | --- | --- | --- |
| p04 | [Philsacor](https://commons.wikimedia.org/wiki/File:Random_landscape_photo_I_took.jpg) | `20d6a8dab7e5eed0fbbbb0d98de799487512264aa7d0cf7ced701944683e2bd0` | source-verified and pending primary SVG insertion |
| p06 | [K.M.YOGESH](https://commons.wikimedia.org/wiki/File:Mountain_near_lake.jpg) | `c5902098629b2ce85686a94204ea011a88711ac6d65308aa48323d9052a8dc1a` | source-verified and pending primary SVG insertion |
| p07 | [Stefan Bellini](https://commons.wikimedia.org/wiki/File:Sunflower_in_the_garden.JPG) | `a64e072f62f72b5ebd167bea0ad44f64e6d51a7d07a7dc78d01c0c0fc0160e59` | source-verified and pending primary SVG insertion |
| p08 | [PagesAndPathsPH](https://commons.wikimedia.org/wiki/File:Sunset_in_the_City_01.jpg) | `68a2e5c556be1db5a3ac6abdf60a8e2c2eaa8773aae84f48a3bf600a9c73e656` | source-verified and pending primary SVG insertion |
| p09 | [Kanimozhi Vennila](https://commons.wikimedia.org/wiki/File:Purple_Night_sky.jpg) | `7a01f8b4f1ab6a6a28cf85b4b4387f0df9a8734186eb322568e273a5174cc00d` | source-verified and pending primary SVG insertion |
| p10 | [Pinguinguy](https://commons.wikimedia.org/wiki/File:Sunny_beach_ocean.jpg) | `8baff97c5dc73daa533bde70ac7bb3bd08e767a93d316894c9b9f1161d9331b9` | **source-verified alternative only; old SVG bytes retained** |
| p11 | [SpessiG](https://commons.wikimedia.org/wiki/File:Landscape_of_a_wild_forest.jpg) | `658b7e6b88eb1d28b922042427e7eceac7f7fbef88454e0919666980096d645a` | **source-verified alternative only; old SVG bytes retained** |
| p12 | [Sporlo](https://commons.wikimedia.org/wiki/File:Chive_flower_close-up.jpg) | `e781fb03f2fd56a97e409118009524fe79be4dd68f1b871108fe7073e36d15be` | **source-verified alternative only; old SVG bytes retained** |
| p13 | [Jdszekeres](https://commons.wikimedia.org/wiki/File:Still_and_clear_sea.jpg) | `c62038af15c3cc41b0ca607cf144fdb4069d1bbc8dc349014f87b6a63c14badf` | **source-verified alternative only; old SVG bytes retained** |
| p14 | [TBOE PC Ohm](https://commons.wikimedia.org/wiki/File:Starry_sky1.jpg) | `3fe5b70cfc9d7994a7ac0732413e4a19b9167d7f6ba5b47cda386208e0c6350e` | **source-verified alternative only; old SVG bytes retained** |
| p15 | [Villager3222](https://commons.wikimedia.org/wiki/File:Pinwheel_flower_close-up_shot.jpg) | `5b0f2e0e7afcfcae31f0641409819087fe3d69611912d1430ebdd804783ded74` | **source-verified alternative only; old SVG bytes retained** |
| p16 | [Drgage25](https://commons.wikimedia.org/wiki/File:Boston_Common_at_sunset.jpg) | `92d30c89130514f15bcb82539db6b31c9ae5f6175a73c13d02aa6c04161c4d48` | source-verified and pending primary SVG insertion |
| p17 | [Cristian Butacu](https://commons.wikimedia.org/wiki/File:Night_sky_and_trees.jpg) | `ab0010efc6ebed8ada9cefc3ea0f246ef0a4de4437ea9efc8a6ebf83b0524d8e` | source-verified and pending primary SVG insertion |

## Two different migration gates

**Missing source art:** p04 Home plus p06/p07/p08/p09/p16/p17 Grid now have real independent, source-authenticated bytes under `design/assets/cc0/`. Only Production A modifies selected primary SVG plates. p04's **last Glimpse acknowledgment** is an historical application state, not a readback of present Android wallpaper. Do not substitute p12's selected preview for p04.

**Legacy in-use art:** p10/p11/p12/p13/p14/p15 remain mapped to six original art1–art6 JPEGs whose reuse rights have **not** been established. Their source-byte continuity is real, yet their photographic/legal provenance is insufficient for approval-grade reuse. Replacement files are ready as `p10-replacement.jpg` through `p15-replacement.jpg`. The asset manifest's `crossSurfaceProofTarget.photoSourceMode` intentionally remains `legacy-research`. A coordinated switch to `rights-verified-replacements` is valid **only** when all four selected Home/Reel/Grid/Photo Detail proposal SVGs have been source-edited, rendered and independently inspected with the same p12 JPEG SHA and all six stable p10–p15 IDs. `scripts/validate-agent-c-asset-integrity.py` supports both states and rejects mixtures.

A selected user photo identity and an illustrative artwork source SHA are different concepts. 22 sample records created by the debug APK are 22 *numbered synthetic derivatives* assembled from three verified CC0 photographs. They are not 22 witnessed Google Photos imports and cannot prove provider/temporary grant behavior. The debug APK no longer packages six rights-unverified research originals.

The four historical non-photographic fallback source files p08/p09/p16/p17 remain editable and conspicuously labeled `NO SOURCE PHOTO`; they were honest alternatives while sourcing was blocked, not evidence of photographs. Keep for comparison, not preferred final art.

## Native evidence and remaining truth

- [Successful actual Android API34 native/CI run #40138084864](https://github.com/gamesbyian/Glimpse/actions/runs/40138084864) completed source-integrity validation, focused JVM tests, actual running MainActivity 0/1/3/5/22/60 screenshot/hierarchy capture, and **seven device instrumentation tests** (`PhotoImporterDeviceTest` five tests and `WallpaperGatewayDeviceTest` two).
- In the same API34 session, actual Android at 320dp/200% font reported Reel and Grid navigation target heights **48.0dp**, overcoming their previous measured 38.9dp shortfall. The New collection button has a compact `New` visible label and full semantics.
- The OS Home wallpaper ID test checks an **immediate readback after a user-intent command** and the uncertain-operation test blocks new mutation. Neither checks crop pixels after launcher scaling, future system wallpaper, Lock target, precise five-minute scheduling or Doze. The importer tests exercise real Room/ContentResolver with a controlled `file://` fixture, **not** genuine third-party `content://` Photo Picker grant revocation.
- Actual returned PNGs and accessibility XML remain in GHA artifacts, not immortalized as unsupported human/owner acceptance. Current proposal source and actual native UI are **not equivalent**; the 24+34 whole-set human review gate is still open.

## Required integration sequence

1. A selects suitable source/crop choices, replaces editable SVG image sources using verified bytes, and preserves the invariant p12 exact same photograph wherever p12 appears. p04 must use its different verified JPEG.
2. B checks imagery changes do not break state truth, label legibility and 320dp/200%-text semantic journeys.
3. Integration verifies one set of exact SVG SHA-256s and corresponding rendered phones, re-runs source validators and independent native acceptance where relevant, updates reviewer evidence/queue, and retains owner decisions. Do not mutate `design/approved/` or merge PR #6 without explicit owner signoff.

**Most authoritative per-image source record:** [`production-c-provenance.v1.json`](../production-c-provenance.v1.json). It carries original downloaded SHA-256, source URL, creator, license page, transformed dimensions and exact checked-in derivative SHA.