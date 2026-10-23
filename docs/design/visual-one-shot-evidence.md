# Glimpse one-shot visual reference validation

**Automated tool result only. No visual, native, accessibility, or owner approval.**

- Workflow source commit: `c7fc5556f53e4a8b66a9dccdd0e150bfae96e2e8`
- Workflow run ID: `40032396530`
- Workflow run URL: https://github.com/gamesbyian/Glimpse/actions/runs/40032396530
- Run attempt: `1`
- Workflow result before publishing: `failure`
- Branch: `agent/visual-state-reconciliation-20261023`
- Review sets: 34 states, Source Detail, reel/grid/crop/share, large text, complete original 66 + P0 82 + P1 55
- CI artifact name: `glimpse-reference-draft-c7fc5556f53e4a8b66a9dccdd0e150bfae96e2e8`

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
Scanned 236 proposal SVGs, 134 image nodes, 134 decoded embedded rasters.
Source-level SVG/photo portability PASS (not optical, device or owner PASS).
Traceback (most recent call last):
  File "/home/runner/work/Glimpse/Glimpse/scripts/validate-audit-cycle.py", line 43, in <module>
    assert ids == expected, (state, ids, expected)
           ^^^^^^^^^^^^^^^
AssertionError: ('idle-22', [], [12, 13, 14, 10, 11])
~~~

## Rendered evidence

- Missing: /tmp/glimpse-visual-phone/index.json
- Missing: /tmp/glimpse-visual-journeys/index.json
- Missing: /tmp/glimpse-visual-complete/index.json
- Missing: /tmp/glimpse-visual-audit-r03/index.json
- Missing: /tmp/glimpse-visual-audit-r01/index.json

**Status describes automatic structural/render execution, NOT whether humans saw/read or accepted the PNG files.**
