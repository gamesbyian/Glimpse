# Cross-storyboard command-truth reconciliation · 2026-10-23

**PR #6 draft; source audit only except the separately documented exact-source R06–R08 five-phone raster review. Native and human acceptance remain unverified.**

The comprehensive reference set must convey one coherent app state across original plates, P0 storyboards, P1 storyboards, supplemental recovery references and audit revisions. A repeated scenario is not automatically redundant: it may be a correction, a different command boundary or a competing alternative that must be retired before final owner approval.

## Concrete contradictions found and repaired

| Source | Before defect | Source correction (committed) | Remaining proof |
|---|---|---|---|
| `design/proposals/advanced-storyboards/x13/04-cancel.svg` | “No image delivered” treated Sharesheet return as proof about third-party delivery; “Sharing cancelled” assumed a known external result. | “Back from sharing”; “Delivery not confirmed”; “Sharing closed”. Resulting Git blob `fd4835ddaf3ece8a0fe7ed187c39e728b2ba43d5`. | Exact updated native phone crop and Android real Sharesheet dismissal/return |
| `design/proposals/advanced-storyboards/x19/05-restore-result.svg` | “Restore completed” simultaneously displayed “No action committed yet”, an impossible end state. | “Restore committed · review log”; “Only approved restore choices committed”. Resulting Git blob `d49e714d36731d5154485d69db0c56d164a1e829`. | Updated 401×890 optical check; native atomic/partial restore and conflict policy |

These changes correct **draft copy consistency only**. They do not assert restored data actually persisted or an Android recipient received anything.

## Deliberate overlap, not duplicate required screens

- **R06 vs X13/R02:** X13 explains original/edited chooser and no-handler branches. R02 supplies source-matched photo bytes and explicit p12 → p12c save boundary. R06's separate post-return decision exists because Android Sharesheet outcomes do not establish delivery. Prefer the R02 photographic payload and R06 neutral return copy during whole-set selection. Keep X13 progression as a predecessor and test its newly corrected cancellation message.
- **R07 vs X04/X18:** X04 establishes enabled/delayed/paused/resumed rotation sequence. X18 02 illustrates an *individual* apply whose outcome is unknown. The R07 restricted/unknown views explain scheduling eligibility, policy restriction and uncertain follow-up. Do not merge last-worker-run and last-confirmed-applied timestamps, and never interpret an OS restriction as evidence of an attempted wallpaper operation.
- **R08 vs X17/X19:** X17 04 illustrates *network offline* with verified local copies, while R08 revoked illustrates lost provider permission. X19 04 has only a generic “Choose safe merge” command; R08 conflict explicitly surfaces Keep / Import separate / Skip. X19 05 remains the subsequent committed result, not the same decision surface.
- **R03 vs X03/X14:** X03 pending/unknown and R03 last-acknowledged distinctions belong to one target-aware wallpaper gateway. X14 05 pictures Home acknowledged and Lock uncertain separately, which is preferable to a blanket “Both applied” claim.

## Audit integrity and remaining integration work

Keep Cycle A **18 PASS (draft-source), 8 ISSUE, 34 UNVERIFIED**. No score changes without evidence appropriate to each GUX question. Specifically GUX-18, GUX-27 and GUX-46 have stronger *draft visual evidence*, but Android/owner observations remain absent.

Priority next:
1. Obtain source-hash-matched full and phone-only rasters for modified X13 04 and X19 05; inspect at 401×890.
2. Verify side-by-side that R06 and the rest of X13/R02 use one stable selected/variant ID after chooser return; that X19 restore conflict choice really transitions into its result screen; that X04/R07 labels eligible/delayed/restricted/unknown distinctly.
3. Ensure the final human review atlas highlights preferred sources and clearly marks old alternatives for retirement. Never silently count both as approved.
4. Record per-frame Android accessibility bounds, 200% text and real OS outcomes before conformity claims. Real owner approval is a separate exact hash set.

**Human-review route:** [39-frame preliminary phone-only spine](../../design/atlas/canonical-review-spine.html), backed by [manifest](../../design/proposals/canonical-review-spine.v1.json). This is a selected tour, **not yet the complete approved proposal set**.
