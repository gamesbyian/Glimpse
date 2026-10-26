# R05: Accessibility and mobile-proof gate

**Prepared after recovering the independently rendered R01 and R02 work, 2026-10-23.** Machine-readable case inventory: [r05-accessibility-evidence.v1.json](../../design/proposals/audit-revisions/r05-accessibility-evidence.v1.json). The reference set includes X09 large-text variants, R01 photographic reel and contrast illustrations. **No tests in this document have been run on an Android device.**

## Source gaps identified

- **Reduced-motion reel route:** no dedicated frame showing an accessible zero-motion way to advance from p12 to p13 with focus stable and index updated. Reusing a large-text reel image is **not** evidence of reduced-motion equivalence.
- **TalkBack reel flow:** no depicted focus or announcement sequence for `photo p12 of 22`, Previous, Next, Grid, Return, disabled Next at p22, no-selection empty state. X09 text layouts are not a substitute for a semantics tree.
- **Hit area/inset proof:** reference coordinates provide visual sketches, not Android measured `boundsInScreen`; must capture actual clickable bounds in dp at multiple widths/font scales.
- **Short-screen and keyboard:** compact error reference exists, but a native short-height viewport with keyboard displayed and action visibility is unverified.

## Native proof protocol

Capture device model, API version, display pixels, density, font scale, accessibility settings, app build SHA and dataset fixture identity for each screenshot. Record **native screenshot + UI hierarchy/accessibility tree + command trace** for each case in the manifest; do not use right-hand SVG annotations or mocked TalkBack captions.

At **N=0,1,3,5,22,60**, require keyboard or accessibility Next/Previous to reach exactly the same stable photo ID as drag/tap/Grid. For large collections, verify there is no five-slot navigational cap. At N=22, start p12, move to p13 and return via Grid; at the p22 boundary, Next must be disabled and not silently wrap. For reduced motion, do not spin/inertially animate, but preserve all operations and feedback. For TalkBack, verify focused item name, index, total, selected-versus-last-applied distinction, disabled controls and restored focus.

For 200% font scaling, inspect Home, Reel, Grid, Import, Error and action confirmations. Preserve essential labels, destinations, status confidence and input alternatives. Record any truncation as a specific failing crop and remediation task. The layout need not be pixel-identical to the reference if adapting prevents clipping.

For hit targets, measure **actual bounding rectangles**, density and resulting dp dimensions. Aim for comfortable 48dp+ interactive targets in the primary navigation and photography operations, while noting Android platform minimum and opt-in user ergonomics; do not infer targets from rendered ink alone. Check target spacing and overlap at small width.

## Audit handoff

Link the native artifact back to GUX-31–39 and the exact reference SHA. Mark each case PASS, ISSUE or UNVERIFIED independently; a missing test does not establish a defect. Then update candidate spec and source-editable mockups for observed failures; rerender revised source and repeat native comparison. **Owner approval is a separate gate.**


## Illustration-gap closure, separate from native testing

Two new **interaction-diagram references** are available: [reduced-motion p12→p13](../../design/proposals/audit-revisions/r05-reduced-motion.svg) and [TalkBack focus route](../../design/proposals/audit-revisions/r05-talkback-route.svg). Their purpose is to make focus/selection requirements visually reviewable; neither is a screenshot, simulated TalkBack log or tested implementation. The [manifest](../../design/proposals/audit-revisions/r05-accessibility-evidence.v1.json) now maps both dedicated sources without claiming any device result. The original source gaps above describe what was missing *before* this addition. Every R05 native proof remains **unverified**.
