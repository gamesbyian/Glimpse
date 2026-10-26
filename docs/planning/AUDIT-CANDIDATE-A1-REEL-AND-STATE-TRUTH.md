# Proposed spec amendment A1: photographic reel, state truth and reference acceptance

**Status: audit-driven candidate, not yet normative/owner-approved.** Trace: [GUX-02,03,04,06,07,08,09,22,23,30,32,33,34,35,54,59](../design/glimpse-ux-audit-cycle-a.v1.json) and [R01–R03/R05](../design/GLIMPSE-AUDIT-CYCLE-A-REPORT.md). Existing [current decisions](current-decisions.md) and [screen contract](../design/screen-contract.json) remain authoritative until reviewed.

## The one-reel invariant

A Glimpse photographic reel has **exactly five physical aperture positions**, a recognizably **circular outer rim**, an internal hub, and a meaningful portion of its wheel extending beyond the display boundary. It does not mean five photos. A collection has a finite total `N`, of which `0..N-1` are stable saved photo IDs. The central selected index `s` is either absent if `N=0`, otherwise in `[0,N-1]`. The fixed focus window is not a moving hero; virtual aperture offset `k∈{-2,-1,0,1,2}` shows `photo[s+k]` when in range and a visibly empty hole otherwise. The reel may animate while dragging, but commit a new `s` only at a settled boundary. No duplicated photo may be inserted to fill empty windows. **No wraparound** unless a separately specified explicit mode is approved.

When `N=0` the photo surface offers a clear **Add photos** action and five unfilled apertures; `N=1` shows one real image and four empty apertures; `N=3` shows three true images; `N=5` five; `N=22` and `N=60` each permit reaching every index, not only the first visible five. The previous/next buttons, swipe gesture, a tapped aperture and Grid selection must all resolve to exact same stable ID. Interrupted drag returns a valid settled index; screen rotations and background/foreground transitions cannot silently swap selection or wallpaper. A visible count `s+1 of N` must agree with the hero and selected Grid tile.

## What the reel is NOT

It is not the owner-rejected rounded rectangular five-slot strip, a generic settings carousel, an applied wallpaper status indicator, a control for permissions or a cosmetic infinite thumbnail cover flow. No image is applied merely because it was selected or the wheel spun. Separate user action and Home/Lock target must request an apply.

## State labels and irreversible interpretations

- `selectedAssetId`: a UI choice; show *Preview* or *Selected* with count. Never say *Current wallpaper* unless device state has appropriate observed confirmation.
- `lastConfirmedAppliedAssetId`: the wallpaper set acknowledged by the system's request. Display *Last confirmed* if OS read-back is not possible. It is not guaranteed to remain currently applied when external launchers or other apps act.
- `pendingRequestedAssetId`: an in-flight wallpaper request and specific destination. Display pending with meaningful cancellation/exit behaviour; never mark applied before acknowledgment.
- `unknownOutcomeAssetId`: a request for which the outcome cannot be established. Display *Outcome unknown* and a human follow-up; block silent automatic retry or shuffle consumption.
- `nextEligibleAt`: earliest scheduling eligibility, not a guarantee that Android will execute precisely at that time. Distinguish pause, eligible, background-delayed and battery-limited states.

**Visual truth rule:** the main hero may preview a selected photo while a smaller independently labelled status card shows last confirmed Home p04; the two may differ, and the UI should visibly explain why.

## Acceptance packet required for R01/R02/R03

The owning PR must attach a source SHA, device/build ID, and a **single 0/1/3/5/22/60 image atlas** containing (for each applicable N) idle, tapping an off-centre aperture, 18° drag, 36° drag, settled snap, first/last boundary, Grid entry and return, interruption, reduced motion, and 200% text. Include a deterministic photo ID test ledger and accessibility hierarchy. Capture failures and unknown outcomes as separate fixtures. Screenshots generated solely from SVGs are reference *targets*, not Android observations.

For a valid scored `PASS`, show actual image pixels and state mapping where visual; show UI testing or semantic accessibility logs where behavioral. A passing build, a stubbed screen or a right-hand explanatory annotation is insufficient. Record owner impression of the genuine physical reel separately and without leading comparison language.
