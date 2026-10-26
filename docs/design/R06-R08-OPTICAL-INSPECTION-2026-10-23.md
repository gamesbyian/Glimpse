# R06–R08 targeted optical inspection and semantic correction · 2026-10-23

**Draft candidate. Five actual 401×890 phone pixels inspected. No native Android, TalkBack, human acceptance or release claims.**

## Provenance and method

The five exact remote SVGs were fetched from the draft PR #6 through the connected GitHub repository, reproduced locally from their deterministic source generator, and independently cross-checked by `git hash-object`: all **five local Git blob SHA-1 values exactly equalled GitHub's returned current file blob SHA**. Rendering used **CairoSVG 2.8.2** and **Pillow 12.3.0** on a disposable Linux container. Rendered each full proposal at 1160×920, cropped phone rectangle **(15,14) to (416,904)**, saved 401×890 RGB PNG and directly opened the five-panel composite at visible pixel size. This is **real source-equivalent raster inspection**, not native screenshot inspection or an automated optical pass.

| Phone source | Exact Git blob SHA-1 | Source SHA-256 | Reviewed phone PNG SHA-256 |
| --- | --- | --- | --- |
| `r06-share-dismissed.svg` | `e3acbb13e4111cad1ab4d26ad6d09bc6ca9fc160` | `ff4b900b877c7136edd6746fbfbd1dd9dad314b66bc122b67efb4082b7a282e4` | `202001fadffc83bff04b8de488ccc436f799d19da0cc18e28b72999d0bb2a539` |
| `r07-battery-restricted.svg` | `d014b73e01e81e523421474d6317af0bfbf765cf` | `203cd7a5ab8938fea15c5f2e53db755a00c4ba9a49af1fd3fc74fdcd6cc4856a` | `6b35c52d9bbce569d3808b5f7f7547f87922eeba034cd91d8174ca506bd4635b` |
| `r07-unknown-after-attempt.svg` | `e9c57d448db2af2e5a274114848ea94c7152fb6e` | `8eae212323629804960cf6ab753d46781830f6debdafe45f9d20b620691e4ad7` | `5062fba57f1fa3517a400f0313a848d70d058a3a6c323faa0d053318bd8f10df` |
| `r08-restore-conflict-choice.svg` | `9e813b14f71dd9af03952cc611b644f60c6e7295` | `8be4e307620c0129ee402976f1af19b0284c830dad7dd433be6f956d18d9f438` | `3167f7b5be17d4042c12b664958a3de289564b9943ce9f1d03663fc3024d5997` |
| `r08-source-revoked.svg` | `112021a04098d6bc45be44ddac7061e174c15bce` | `fbc6cf0bcf52d5d5889cc951545ff776fcd630ce8b3284cc118819dcb4498fc8` | `3306242e1aadf020b0305e7f43232644a048ebbf04b70477204cd352e8a1114c` |

For rerun in an equivalent environment, use `python3 scripts/validate-audit-r06-r08.py --render`. Different fonts, raster libraries or versions can yield different exact PNG checksums despite an unchanged SVG, so maintain both source hashes and renderer environment. The current viewed PNG bytes live in the active agent's local review artifact; the targeted GitHub Action may produce its own downloadable PNG review artifact in CI.

## First-pass actual-render findings, repaired

1. **R07 UNKNOWN: misleading certainty.** The original generic "State remains safe / Last confirmed wallpaper p04" callout could read as a present OS wallpaper assertion despite an unknown apply outcome. Replaced with **"Last Glimpse acknowledgement: p04 · current device state may differ"**, and directed user to activity review before repeating. Protects GUX-27/29.
2. **R06 share: false conceptual success.** Generic wallpaper callout on a share-return screen did not explain external handoff. Replaced with **"Recipient delivery: not confirmed"** and preserved p12/p12c state. Protects GUX-16/17.
3. **R07 restricted: irrelevant reassurance.** Replaced wallpaper-safe generic section with **"Next attempt: Android may delay"** and separate manual Change now path. No five-minute execution promise.
4. **R08 revoked: context and asset lifetime.** Replaced generic wallpaper state with local-copy preservation and clarified 17 saved as *illustrative example*, not a measured provider count. New import needs permission, existing verified copies do not.
5. **R08 restore: missing third choice.** Added **"Skip item / review later"** at 48px nominal height in addition to keep-existing/import-separate-copy. Reflowed buttons without occluding bottom navigation. Restore remains a preview with no automatic apply or overwrite.

## Second-pass optical inspection and limitations

All five post-fix 401×890 phone crops were visually opened in a side-by-side real PNG sheet. Titles, status cards, long explanatory sentences and primary/secondary buttons were legible without **obvious** clipping at this one font setting and exact crop. R08 conflict now displays three separate action targets. The prominent status is text-first and does not rely on color alone. Candidate shares a consistent warm cream/teal/nav visual language with the proposed support-state family.

**Still imperfect:** The five screens deliberately favor text-first adverse states, with repetitive cards; attractiveness and task comprehension require human judgment against the photographic Reel family. No automatic text-bound test, WCAG contrast calculation, actual 48dp touch geometry from Android, 200% text scale, RTL, screen reader route, OS picker return, battery restrictions or restore behavior was observed here. The 401×890 drawing is *not* evidence that Android will render the same layout or consume the same dp bounds. No measured timing or provider data was fabricated. Do not promote the five optical-review flags to source-level/native audit PASS without the question-specific evidence gate.

## Next proof and approval dependencies

- Run the focused validator and optical workflow from current branch, reconcile CI font/raster hashes without treating a different renderer's PNG checksum as a design regression by itself.
- Compare each altered draft screen with its prior reference using exact same 401×890 viewport; inspect revised X13/R02 actual photographic p12 → p12c output and the R03 target-aware wallpaper result chain. 
- For R07 build and exercise deterministic state-reducer fixtures; on Android collect eligibility vs worker invocation/OS acknowledgement, battery saver, reboot, deliberate delay and UNKNOWN recovery.
- For R08 verify provider-grant removal against retained *verified* local bytes, interrupted storage writes, restore collision policy and no unintended WallpaperManager call.
- Capture native 200% text / TalkBack / 48dp hit boxes; obtain **human approval of a single chosen whole-set source hash inventory**, not approval by implication from a successful mockup validator.

**Audit integrity:** Cycle-A unchanged: 18 source PASS / 8 ISSUE / 34 UNVERIFIED. Exactly five draft references now have source-equivalent, directly inspected 401×890 optical evidence; **zero native/owner proofs were added**.
