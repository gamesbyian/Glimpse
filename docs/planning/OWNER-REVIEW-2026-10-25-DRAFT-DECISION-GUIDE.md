# Glimpse owner review: draft decisions, source-exact choices

**October 25, 2026. This is NOT an approval, release clearance, or native compatibility verdict.** Draft integration: [PR #6](https://github.com/gamesbyian/Glimpse/pull/6). The proposed six-screen CC0 alternative is visually coordinated but **is not selected** in the shared review manifest. Do not silently mix historical rights-unverified photo fixtures with the new CC0 series.

## Open the references

- [Current selected whole-product reviewer](../../design/atlas/whole-product-review-selection.html) and [source selection manifest](../../design/proposals/whole-product-review-selection.v1.json). This contains 24 primary and 34 support candidate choices, not approvals.
- [Synchronized CC0 six-screen alternative manifest](../../design/proposals/whole-product-candidates/cc0-consistent-20261024/migration-manifest.v1.json). Consider **Home / Reel / 3-column Grid / Detail / Collections / Share together**, not by individually transplanting p12 placeholders.
- [Six-screen photographic contact](../../design/system/reviews/ci-renders/cc0-consistent-six-screen-contact.jpg) and [source/raster SHA ledger](../../design/system/reviews/PRODUCTION-A-CURRENT-HANDOFF-2026-10-24.md).
- [Full B journey optical report](../../design/reviews/support-agent-b-2026-10-24/full-journey-corpus/OPTICAL-REVIEW.md), [137-frame source/raster ledger](../../design/reviews/support-agent-b-2026-10-24/full-journey-corpus/full-journey-source-ledger.json), and [B contrast evidence](../../design/reviews/support-agent-b-2026-10-25/).
- [Photographic asset/provenance records](../../design/assets/production-c-provenance.v1.json) and [C strict six-screen JPEG validator](../../scripts/validate-agent-c-cc0-proposals.py).

## Eight owner decisions (answer separately; silence is not consent)

| Decision | Focus | Present status |
|---|---|---|
| D01 | Home crescent vs card collection entry | Not selected |
| D02 | Physical five-window finite Reel and gestures | Requires owner optical/usability judgment |
| D03 | Grid / Next / Previous / Set / Share action hierarchy at nominal and large text | Native/owner comparison still needed |
| D04 | Selected p12 vs last-app-acknowledged p04 vs unknown OS wallpaper | Distinct state claims must remain true |
| D05 | Offline/pending/revoked provider and storage representation | Device/provider variation needs scrutiny |
| D06 | Real Android picker, chooser, Lock capability and best-effort five-minute rotation | No OS/result/Doze guarantees |
| D07 | Restore review, reversible choice, commit and result | Owner comprehension check |
| D08 | Visual continuity across photo, support, recovery and native surfaces | Whole-package decision |

## Non-negotiable acceptance distinctions

The green source and Android CI runs mean checks passed for their **specific defined cases**, not that every physical handset or 200%-text/TalkBack journey is accepted. Static authored 48px targets are not automatic Android 48dp target proof. The visual proposal may use the **three-column Grid** candidate matching one observed native layout, but the older selected reviewer still uses a **four-column Grid**: this is an explicit choice, not an invisible correction. A source-cleared CC0 photograph does not by itself determine the preferred photo subject/crop or confer owner visual approval. Imported user photographs do not inherit sample-asset CC0 licensing.

Before any selection or release: run `scripts/validate-integration-source-freeze.py`, `scripts/validate-integrated-optical-ledger.py`, `scripts/validate-agent-c-cc0-proposals.py`, and `scripts/test-agent-b-contrast.py`; regenerate reviewer source hashes and independently open **exact 401×890 raster crops** of the final chosen source tree. Preserve native 320dp/fontScale2/TalkBack, focus-return, OS sharesheet delivery ambiguity, Home/Lock distinction and rotation limitations as explicit gates.

**Owner instruction:** review the six-screen CC0 set as an indivisible choice first, then the 34 support screens and 23 complete journeys. Record a decision for each D01–D08. If any is deferred, keep `ownerApproved=false`, PR #6 draft, and do not write `design/approved/`.
