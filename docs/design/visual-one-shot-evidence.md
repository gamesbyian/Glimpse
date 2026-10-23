# Glimpse one-shot visual reference validation

**Automated tool result only. No visual, native, accessibility, or owner approval.**

- Workflow source commit: `9a67c7d2543a6e603a6d9c4fd6a3b0c4f25ca3d3`
- Workflow run ID: `40022471345`
- Workflow run URL: https://github.com/gamesbyian/Glimpse/actions/runs/40022471345
- Run attempt: `1`
- Workflow result before publishing: `success`
- Branch: `agent/visual-state-reconciliation-20261023`
- Review sets: named 34, source detail, reel/grid/crop/share and large text
- CI artifact name: `glimpse-reference-draft-9a67c7d2543a6e603a6d9c4fd6a3b0c4f25ca3d3`

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
  "cases": 24,
  "semanticSourceErrors": [],
  "opticalProof": false,
  "deviceProof": false,
  "ownerApproval": false
}
Scanned 228 proposal SVGs, 133 image nodes, 133 decoded embedded rasters.
Source-level SVG/photo portability PASS (not optical, device or owner PASS).
~~~

## Rendered evidence

- Raster ledger: /tmp/glimpse-visual-phone/index.json
  - Groups: core-34-named-states, source-detail-paired
  - Native PNG crops: 36
- Raster ledger: /tmp/glimpse-visual-journeys/index.json
  - Groups: reel-grid-crop-share, critical-type-comparison
  - Native PNG crops: 23

**Status describes automatic structural/render execution, NOT whether humans saw/read or accepted the PNG files.**
