# Glimpse one-shot visual reference validation

**Automated tool result only. No visual, native, accessibility, or owner approval.**

- Workflow source commit: `9eab98d1916b93f26bd1d999ff80e227e79d6fdd`
- Workflow run ID: `40041023803`
- Workflow run URL: https://github.com/gamesbyian/Glimpse/actions/runs/40041023803
- Run attempt: `1`
- Workflow result before publishing: `success`
- Branch: `agent/visual-state-reconciliation-20261023`
- Review sets: 34 states, Source Detail, reel/grid/crop/share, large text, complete original 66 + P0 82 + P1 55
- CI artifact name: `glimpse-reference-draft-9eab98d1916b93f26bd1d999ff80e227e79d6fdd`

## Structural script outputs

~~~text
{
  "checkedSvgFiles": 66,
  "coreScreenFamilies": 24,
  "supportScreenFamilies": 16,
  "supportStatePlates": 34,
  "criticalReelStatePlates": 8,
  "routedHumanWorkflows": 73,
  "textTracedCoreRequired": 127,
  "draftCoreVariantsWithoutCompleteScreenPlates": 63,
  "warnings": [],
  "errors": [],
  "evidenceStatement": "Static structural checks, not pixel/UI/device/owner acceptance"
}
{
  "p0Packages": 12,
  "storyboardFrames": 82,
  "advancedStoryboardFrames": 55,
  "errors": [],
  "status": "structural_validation_only_not_design_approval"
}
{
  "declaredStates": 63,
  "baselineCredited": 29,
  "unpicturedOriginalSlots": 34,
  "evidenceLevels": {
    "pictured-state": 34
  },
  "newRefinements": 25,
  "warnings": [],
  "errors": [],
  "approval": "NONE",
  "optical": "NOT TESTED; inspect browser-rendered contact sheets"
}
{
  "cases": 34,
  "semanticSourceErrors": [],
  "opticalProof": false,
  "deviceProof": false,
  "ownerApproval": false
}
Scanned 246 proposal SVGs, 165 image nodes, 165 decoded embedded rasters.
Source-level SVG/photo portability PASS (not optical, device or owner PASS).
{"state": "photo-rest-22", "file": "design/proposals/audit-revisions/r01-photo-rest-22.svg", "sourceBytes": 632420, "sha256": "6d29de6ae0114a4d20c47a5d547a51b7f9cedf7ca4878985f8de2dc8290807b7", "mode": "check"}
{"state": "photo-drag-18", "file": "design/proposals/audit-revisions/r01-photo-drag-18.svg", "sourceBytes": 632552, "sha256": "ea02a5a400ffed1b93c5aeb7f5b2874b57bfe9742c4e3b0b9e80b7c2ef75d8a0", "mode": "check"}
{"state": "photo-snap-13", "file": "design/proposals/audit-revisions/r01-photo-snap-13.svg", "sourceBytes": 632455, "sha256": "b6db89904847b00d6097387906c6671a30397f5cd60c0ccb72443fae621e1d33", "mode": "check"}
{"state": "photo-last-22", "file": "design/proposals/audit-revisions/r01-photo-last-22.svg", "sourceBytes": 631067, "sha256": "4c54486c39a5146f0e6bec002bb5075c7474dd4f5ead2edcc9ac9e1137f75d3e", "mode": "check"}
{"state": "photo-empty-0", "file": "design/proposals/audit-revisions/r01-photo-empty-0.svg", "sourceBytes": 10039, "sha256": "bd38ecc804d06a1ef08682aa314ab6d14d93a80049eab325a98c912189da1056", "mode": "check"}
{"state": "photo-single-1", "file": "design/proposals/audit-revisions/r01-photo-single-1.svg", "sourceBytes": 96495, "sha256": "a5c19f89c8466998b2a9afdc0fbf3a0020253bb8363b49f7a1dd75f32a2706b7", "mode": "check"}
{
  "questions": 60,
  "categories": 6,
  "revisionLanes": 8,
  "scenarioTargets": 56,
  "results": {
    "PASS": 18,
    "ISSUE": 8,
    "UNVERIFIED": 34,
    "NOT_APPLICABLE": 0
  },
  "type": "document_trace_validation_only"
}
{"pairScenes": 12, "r01": 6, "r02": 4, "r03": 2, "assertions": "all links resolved, 401x890 screen window", "opticalOrNativeApproval": false}
~~~

## Rendered evidence

- Raster ledger: /tmp/glimpse-visual-phone/index.json
  - Groups: core-34-named-states, source-detail-paired
  - Native PNG crops: 36
- Raster ledger: /tmp/glimpse-visual-journeys/index.json
  - Groups: reel-grid-crop-share, critical-type-comparison
  - Native PNG crops: 23
- Raster ledger: /tmp/glimpse-visual-complete/index.json
  - Groups: entire-original-66, entire-p0-82, entire-p1-55
  - Native PNG crops: 203
- Raster ledger: /tmp/glimpse-visual-audit-r03/index.json
  - Groups: audit-r03-truth-candidates
  - Native PNG crops: 2
- Raster ledger: /tmp/glimpse-visual-audit-r01/index.json
  - Groups: audit-r01-finite-motion
  - Native PNG crops: 6
- Raster ledger: /tmp/glimpse-visual-audit-r01-photo/index.json
  - Groups: audit-r01-photo-backed
  - Native PNG crops: 6
- Raster ledger: /tmp/glimpse-visual-audit-r02/index.json
  - Groups: audit-r02-copy-transition
  - Native PNG crops: 4

**Status describes automatic structural/render execution, NOT whether humans saw/read or accepted the PNG files.**
