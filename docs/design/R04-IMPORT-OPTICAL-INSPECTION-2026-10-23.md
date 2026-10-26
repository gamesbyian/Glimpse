# R04 import recovery: independent draft optical findings

**Date:** 2026-10-23. **Review:** two source-hashed 401×890 actual rendered phone PNGs and a two-panel atlas inspected directly. **Automated CI:** [successful targeted run 40048067960](https://github.com/gamesbyian/Glimpse/actions/runs/40048067960), source commit `b64da453503ad0e7823913de7b856ddf9eeeeee5`.

| Phone source | SHA-256 | Optical disposition |
| --- | --- | --- |
| `design/proposals/audit-revisions/r04-import-cancel.svg` | `262eba90a66b4b38f7fed6efb13bb83716e5712ae2599c4f5d06460e28123c88` | Readable draft with separate Stop/Continue choices; **not owner-approved** |
| `design/proposals/audit-revisions/r04-import-no-space.svg` | `acd6d5cff270dc015051875a14f8152fa5b61c18ac18a22fb098644c7c418d4a` | Storage-specific draft with Keep/Retry paths; **not owner-approved** |

## Positive observations

The source states show 17/22 verified local copies, 5 pending; they do not announce batch success, never claim pending files are available offline, explicitly retain original provider assets, and do not imply a wallpaper apply operation. Cancel and storage-full are distinguishable by title, status and choices. The five small **symbolic example** thumbnails are annotated as examples, not represented as the actual 22 selected photos. Main actions are visually substantial.

## Open deficiencies

1. **The storage-full primary action has warning-red prominence even though “Keep 17 verified” is the safe, non-destructive option.** Compare its emphasis with a retry decision, especially under colour-vision/high-contrast options, before approval.
2. **Secondary and bottom-navigation labels are small on a full 401×890 phone**, and the design retains considerable unused lower vertical space. At fontScale=2.0 this may need reflow into a sheet or larger recovery actions rather than reducing text.
3. **The two phone images reuse only three stylized local thumbnails and two grey pending tiles**. This is suitable as an abstract truth schematic, not photographic quality acceptance or an actual 22-item screenshot.
4. The current reference has no actual low-storage Android diagnostic, filesystem quota readback, interrupted transaction trace, provider reauthorization or retained Room membership evidence. Device/runtime proof remains absent.
5. **17/22 corresponds to roughly 77.3% by item count**. Keep the displayed ratio authoritative and do not copy “78%” from a separate quick-screenshot bytes-progress illustration without declaring a different denominator.

## Disposition

**R04 cancellation and storage recovery now have source-specific draft illustrations plus independently viewed native-sized renders.** Their absence as a named mockup is closed at the *reference-availability* level. Android behaviour, visual owner approval, TalkBack, 200% text and real Photo Picker/cloud scenarios remain unverified. The wider R04 matrix still has zero-entry and multi-item in-progress gaps; baseline GUX scoring unchanged.

Next contract: [candidate B bulk-import transaction specification](../planning/AUDIT-CANDIDATE-B-IMPORT-RECOVERY.md).


## Four-state R04 draft gallery: independent recheck

The dedicated empty and 17-of-22 bulk-progress illustrations are now authored and passed the expanded [targeted Actions run 40049428896](https://github.com/gamesbyian/Glimpse/actions/runs/40049428896), source commit `603bf9d0872914d66d938a9fe0c56da7b48d5107`. A **four-scene contact sheet was downloaded and directly opened**. Its exact source-hashed native phone crops now cover empty, cancel, storage-full and in-progress:

- `r04-import-empty.svg`: SHA256 `888df0a1777f3bbbf43f8062e8c74dc170979c7a4f4eb0250e88e3f5b3cadf46`. **0 selected / 0 offline copies**, five genuinely empty illustration apertures, explicit Choose photos handoff to system Picker; no provider content invented.
- `r04-import-17-of-22.svg`: SHA256 `cff3bd1d864515df906e3ea69c7d8e9659241a82e1e5dc2478276dc75841964e`. **17 verified / five pending**, muted disabled Continue action, explicitly available Stop and review. The source records `fileCountProgressPercentRounded=77`, corresponding to the truthful 17/22 item ratio instead of copying an unrelated screenshot 78% bytes-progress illustration.
- `r04-import-cancel.svg` and `r04-import-no-space.svg` were rerendered without source change, and the previous independent optical findings continue to apply.

**Scope clarification:** Four of six R04 audit scenarios now have distinct dedicated, source-editable, optically inspected draft phone illustrations. The other two, accounted-completion and provider offline, have existing related X01 storyboard sources whose exact task completeness requires flow-level optical review. None of the six has newly captured Android import transaction evidence. The empty film-doodle and local thumbnail examples are reference art, not copied private source photos. At 200% font scale, responsive reflow remains an explicit open check.
