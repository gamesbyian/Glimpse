# Proposed specification amendment B: import transaction truth and recovery

**Status: audit-driven draft for review; not yet normative, native tested or owner-approved.** Trace GUX-12/13/14/25, [R04 evidence map](../../design/proposals/audit-revisions/r04-import-evidence.v1.json), X01 connected import storyboard and [R04 two recovery candidates](../../design/proposals/audit-revisions/r04-import-cancel.svg). Current normative lifecycle remains [behavior contracts](../architecture/behavior-contracts.md) and [image import/share plan](../product/image-import-sharing.md).

## Durable-copy semantics

Let `N=22` denote Android Picker *selected source URIs*, `V=17` independently verified and atomically published private local copies (with durable membership) and `P=5` not yet verified. These numbers describe an **illustrative test dataset**, not an actual Glimpse import result.

Every visual import state distinguishes:
- **Selected by provider** is a *requested source*, not a saved photo or an offline guarantee.
- **Copying / decoding / hashing / staging** is in progress and must not enable an action that requires a durable copy.
- **Verified local copy** is safe to keep and browse if byte verification, atomic promotion and membership commit succeeded.
- **Pending / inaccessible / duplicate / rejected** each count separately. A duplicate can be resolved through membership without inventing a second original.
- **Batch complete** means every requested selection has a disposition, not necessarily that `N` new files were created. E.g., 22 selected, 20 imported, one duplicate, one skipped is a reconciled batch, not “22 new copies.”
- **Progress percentage** must state its denominator and scope. For file-count `17/22`, the nearest integer is **77%**, not “78%”; 78% may be correct for verified *bytes* only with an explicitly different denominator. Do not confuse the screenshot quick-import 78% fixture with a 22-item batch.

## Interrupted import decision contract

When user presses **Stop**, present a real choice rather than losing 17 already durable copies:
1. **Stop and keep 17**: commit only verified existing records, dispose of uncommitted staging safely, retain provenance, and show the resulting partial collection;
2. **Continue copying**: return to the import progress state without duplicating already committed assets.

When storage is insufficient, the UI says **17 verified, five still waiting**; it cannot show five as offline. Offer **Keep verified** and **Free space then retry** (on supported devices a link to settings/free-space guidance may be offered, but never claim it actually freed bytes). A retry must first verify available space and re-check provider URI access. Do not imply source mutation, wallpaper apply, or invisible cloud access.

## Failure-recovery invariants

- Staged temporary files are not promoted or counted as local items until validated and atomically committed.
- Unexpected process death reconciles the journal and temporary files; a duplicate submission must be idempotent.
- Source URI access can expire; show required picker reselection or retry. Do not silently use placeholder/cached thumbnails as original source.
- The user retains control over partial successes; rollback of an unverified in-flight file does not remove previously committed copies.
- Android Photo Picker remains OS-owned; only post-selection Glimpse status and decisions can be approved from these draft screens.

## Minimum acceptance packet

At least 0/17/22 verified counts, duplicate/skipped distributions, cancellation during bytes streaming, out-of-space mid-file, lost source permission, provider offline, process death and re-entry, retry after sufficient space, invalid MIME and large-decode rejection, 200% text, accessibility labels and action bounds, with exact persisted Room membership/bytes evidence. Require **screenshots and UI hierarchy from the native build** in addition to state-machine tests. The two current SVG drawings are *visual hypotheses*, not successful transaction proof.

Source-evidence lineage: [R04 targeted run 40048067960](https://github.com/gamesbyian/Glimpse/actions/runs/40048067960) passed structural checks and rendered both new cases at 401×890. The user/Android/device approval gate remains open.


## New reference completeness checkpoint

The R04 [case map](../../design/proposals/audit-revisions/r04-import-evidence.v1.json) now includes four dedicated draft SVGs for 0 selected, 17/22 verified, cancellation with 17 retained, and insufficient storage. The two original X01 sources for accounted completion and cloud-provider availability remain linked but require flow review. These four dedicated phone rasters were independently inspected from [successful opt-in Actions #40049428896](https://github.com/gamesbyian/Glimpse/actions/runs/40049428896). Their **structural and optical presence** must never be cited as successful Android storage-recovery or Photo Picker behaviour.
