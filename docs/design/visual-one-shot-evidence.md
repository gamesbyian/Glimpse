# Glimpse one-shot visual reference validation

**Automated tool result only. No visual, native, accessibility, or owner approval.**

- Workflow source commit: `eeb82ebb4aac84de9266faeb3c6fa0be3e0b47b7`
- Workflow run ID: `40030545600`
- Workflow run URL: https://github.com/gamesbyian/Glimpse/actions/runs/40030545600
- Run attempt: `1`
- Workflow result before publishing: `success`
- Branch: `agent/visual-state-reconciliation-20261023`
- Review sets: 34 states, Source Detail, reel/grid/crop/share, large text, complete original 66 + P0 82 + P1 55
- CI artifact name: `glimpse-reference-draft-eeb82ebb4aac84de9266faeb3c6fa0be3e0b47b7`

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
Scanned 230 proposal SVGs, 134 image nodes, 134 decoded embedded rasters.
Source-level SVG/photo portability PASS (not optical, device or owner PASS).
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

**Status describes automatic structural/render execution, NOT whether humans saw/read or accepted the PNG files.**
