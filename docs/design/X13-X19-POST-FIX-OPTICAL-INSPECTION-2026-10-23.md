# X13 / X19 corrected phone optical review, 2026-10-23

Both existing P1 SVGs were source-matched to the live draft GitHub blobs, rendered with CairoSVG 2.8.2 at 1160×920, cropped to phone rectangle (15,14)-(416,904) with Pillow 12.3.0, and directly inspected as 401×890 PNGs. This is design evidence, not an Android screenshot or product approval.

| File | Current Git blob | Source SHA256 | Reviewed 401×890 PNG SHA256 |
| --- | --- | --- | --- |
| x13/04-cancel.svg | a09c5d7f05006ed58be36c56fab1e6befe26e562 | 2d6564fc9f9cbb94d70c96700841ff6a10b0245b0ab179dc860ddd41280f4dab | 3312712e1059bddd19365aded5d324b5199b0b3a8fcecb4635c6c42d139085a2 |
| x19/05-restore-result.svg | d49e714d36731d5154485d69db0c56d164a1e829 | 6bd12f9ac5886b542510cadc1ae0ae61233f4d9bf48385c5d089e4349c11dd52 | 4c1c0902754d7fe682e92eaafc8bdbe63d12ea07f19d02f176335d0c182e7b29 |

**Observed in actual pixels:** corrected X13 copy avoids pretending a Sharesheet return proves recipient delivery or unchanged external Android wallpaper. Corrected X19 summary consistently describes a completed, approved-choice restoration. At default preview scale, both titles, status cards and action labels are visible, without obvious clipping. Layouts remain sparse and should be compared to the richer R06/R08 alternatives for final visual coherence.

**Not observed:** real Sharesheet or restore transactions, 200% font rendering, TalkBack hierarchy, actual device touch bounds or human approval. Values in the result mockup are illustrative, not real restore measurements. Cycle A remains 18 source PASS, 8 ISSUE, 34 UNVERIFIED.
