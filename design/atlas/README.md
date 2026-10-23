# Glimpse mobile visual review atlas

Open `design/atlas/index.html` from a locally served repository checkout:

```sh
python3 -m http.server 8000
# Visit http://localhost:8000/design/atlas/
```

Alternatively, download and unzip the `glimpse-visual-proposals` artifact from the **Validate Glimpse proposal-reference structure** GitHub Action. Open `design/atlas/index.html` in a browser. The standalone artifact retains image paths and works without the internet, except GitHub source links.

The **original-screen tab contains 66 references**: 24 original screens, eight critical reel states, 34 support-state variants, searchable by ID, route, workflow or title. The complete image and a copyable reference are available from every card. This is a **draft design atlas** and does not claim the images have been approved, inspected on a device, or implemented in the native app. See [audit](../../docs/design/reference-gap-audit.md).

The companion [**connected storyboard viewer**](storyboards.html) includes **82 new supplementary P0 frames** (X01–X12), with per-journey timeline, source paths and frame navigation. The offline CI ZIP includes both viewers, the storyboard JSON coverage manifest and the public research fixtures needed by existing SVGs. See the [P0 production record](../../docs/design/proposal-mockup-coverage-gaps-2026-10-23.md); these remain proposals, not user-approved or runtime-captured UI.

**Maintenance:** entries are generated from the two JSON coverage manifests; regenerate the file when manifests change. A single canonical path is used in documentation, copied references and GitHub. Keep the separate Android emulator screenshot atlas distinct: those are actual implementation screenshots, not design targets.
