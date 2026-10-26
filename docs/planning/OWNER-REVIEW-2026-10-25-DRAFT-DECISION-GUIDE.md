# Glimpse: owner visual review

**October 25, 2026 · editorial review candidate · NOT APPROVED.**
The single integration proposal remains [draft PR #6](https://github.com/gamesbyian/Glimpse/pull/6), unmerged into `main`. No file in `design/approved/` is changed. Every candidate remains subject to your choice.

## Interactive Pages review studio (recommended first)

**Live reviewer:** https://gamesbyian.github.io/Glimpse/review/ . The [Pages publisher workflow](https://github.com/gamesbyian/Glimpse/actions/workflows/glimpse-pages-publisher.yml) runs from trusted `main` while checking out the **unmerged review candidate branch**. Its build metadata records the exact draft source commit. This publishing path does **not** approve a design or merge PR #6.

- **Large screen / fast comparisons:** a phone-cropped maximized canvas, variant chips, hold-to-peek A/B, side-by-side mode, full annotated SVG, browser fullscreen. Use Left/Right to change options, J/K to step screens, Space to peek, F to maximize. The control panels become drawers on mobile.
- **Browse and judge everything:** 24 primary screens; 34 distinct support states; 23 sequential journeys (137 storyboard frames); exhaustive source artwork; and D01–D08 decisions. Source hash and file path follow each displayed version.
- **Individual review:** choose a preferred screen source, Accept / Revise / Defer, add general notes, or pin an issue with priority directly on the phone crop. Each pin records its exact SVG path and SHA-256. Changes to a source invalidate stale visual acceptance records.
- **Whole-set review:** live counts, eight distinct decision prompts, source-choice compatibility safeguards across the six photographic screens, outstanding feedback and blocking pins. Whole-set approval *request* is a separate explicit action that cannot be activated with unresolved revisions, incompatible design selections, or missing acceptance.
- **Data retention:** review input is automatically stored **in this browser's localStorage only**. It is not sent to GitHub, its authors or a server. Browser storage is not a cross-device backup. **Export the JSON review file** to preserve/share it, and import it on another browser or attach it to a GitHub issue / the chat. The GitHub issue action pre-fills a short summary only. No actual repository approval is claimed by clicking a browser button.
- **Boundaries:** browser proposals are static SVG studies; not native 320dp/200%-font acceptance, TalkBack/navigation verification, Android picker/Doze result proof, or a signed app release.

[Independent desktop + mobile Chromium interaction acceptance](https://github.com/gamesbyian/Glimpse/actions/runs/40254307142) exercised option switching, 2-up comparison, screen pins, locally saved choices, D01, a source-bound JSON export and a 393px mobile drawer, with screenshot artifacts. Passing this browser test does not imply design approval.

## Open these first

1. [Curated whole-product reviewer](../../design/atlas/whole-product-review-selection.html): **24 primary screens**, **34 support states**, **23 linked journeys (137 storyboard frames)** and **eight questions**. Each selected source has its SHA-256 printed beside its card. **Open a generated reviewer only after its validation workflow has regenerated the exact current source snapshot.**
2. [Six-screen photographic comparison](../../design/system/reviews/ci-renders/cc0-consistent-six-screen-contact.jpg) and [seven-screen expanded comparison](../../design/system/reviews/ci-renders/cc0-consistent-seven-flow-contact.jpg): the provisional, *source-verified CC0 sample* Home / Reel / **three-column Grid** / Detail / Collections / Share family, preferred as **one atomic draft editorial candidate**, not chosen by you. Open the [six selected editable SVGs](../../design/proposals/whole-product-candidates/cc0-consistent-20261024/migration-manifest.v1.json) and inspect each at phone scale.
3. [Full interaction-journey optical report](../../design/reviews/support-agent-b-2026-10-24/full-journey-corpus/OPTICAL-REVIEW.md), [support-state review](../../docs/design/SUPPORT-STATE-OPTICAL-REVIEW-2026-10-24.md), and [all 276 source alternatives](../../design/atlas/whole-set-evidence-atlas.html). The 276 figure is the historical inventoried draft corpus, not a completed native acceptance result.
4. [Photo provenance (13 distinct original/derived identities)](../../design/assets/production-c-provenance.v1.json) and [native testing notes](../../docs/quality/first-slice-evidence.md). CC0 refers to **supplied sample fixtures only**, never automatically to imported user photos.

The previous six-screen choices are **retained as alternatives** on the corresponding reviewer cards. The all-photo proposal resolves the old illustrative p04 hero and eight schematic Grid photo gaps, but the subjects, crop, paper-reel feel, three-versus-four Grid columns and action hierarchy remain human decisions. The alternate `source-p12.svg` source-and-rights detail view is supplementary, not silently added as a twenty-fifth primary screen.

## Eight decisions to make, independently

| ID | Inspect | Decide |
|---|---|---|
| **D01** | Home collection entry: paper crescent versus ordinary card | Does the crescent improve navigation, or should a conventional card lead? |
| **D02** | Five-window finite Reel, photo aperture edges, settled pose, arrow parity | Is the physical reel compelling and understandable enough to become Glimpse's core interaction? |
| **D03** | Three-column sample Grid versus older four-column Grid; previous/next, apply/share, focus affordances | Which density/action hierarchy should be preferred? What should change before approval? |
| **D04** | p04 last Glimpse Home acknowledgement, p12 selected preview, OS wallpaper unknown | Are all three visually and verbally distinct, with no false current-wallpaper claim? |
| **D05** | Source/provider permission loss, pending copies, offline collection and storage recovery | Are uncertainty, safe actions and persistence understandable? |
| **D06** | Android Photo Picker and Sharesheet boundaries, Home/Lock support, best-effort five-minute rotation | Is the promise truthful and the unsupported/unknown state obvious? |
| **D07** | Restore preview, conflict resolution, deliberate commit, undo/recovery | Can the owner understand what changes only on final confirmation? |
| **D08** | All 24 primary + 34 support + 23 flow groups | Does it feel like one current-generation app, with acceptable hierarchy and accessibility? |

A **preferred draft candidate** is simply what the reviewer leads with. A response to one D-question does not automatically approve another. For each question, record *accept / change / defer* and optionally name the source path and visible defect. Final owner approval, if given, must identify an exact consistent SHA inventory after revisions.

## Evidence boundaries and outstanding engineering

- **Source**: reproducible SVGs, visible controls, source SHA-256, original CC0 JPEG bytes, and the atomic six-screen guard. The all-CC0 family has a documented seven-screen optical comparison, but a fully independent optical pass on the exact final **generated** 58-screen selection must still be confirmed.
- **Native emulator**: focused Android Photo Picker imports, duplicate detection, offline copies, restart and foreground timing have credible API34 evidence. These do not prove Google Photos album auto-sync, other provider behavior, actual wallpaper readback, background Doze timing, or physical-device compatibility.
- **Accessibility**: native 320dp/200% font, TalkBack focus return, true 48dp click targets and interrupted OS chooser semantics remain separately gated. A successful static 401×890 SVG render cannot settle these.
- **Human**: the observed reaction to the older generic native reel is not approval of this new photographic design. All eight explicit decisions and `ownerApproved=false` remain untouched.

## Freeze protocol for the integrator

After the final source changes, regenerate `design/atlas/whole-product-review-selection.html` and `whole-product-review-snapshot.v1.json` together, then verify the **58 primary/support selected raw-source SHA-256 values**, the **137-frame/189-ledger continuity**, `scripts/validate-agent-c-cc0-proposals.py`, `scripts/validate-agent-c-review-selection.py`, `scripts/validate-owner-review-readiness.py`, `scripts/test-agent-b-contrast.py`, and genuine current-head CI. Include the exact integration commit in the handoff.

**Do not merge PR #6, set `ownerApproved=true`, edit `design/approved/`, or describe pending physical-device tests as passed.** The goal of this packet is a meaningful visual decision, not an Android release certificate.
