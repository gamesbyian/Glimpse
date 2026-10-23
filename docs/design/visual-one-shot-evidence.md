# Glimpse one-shot visual reference validation

**Automated tool result only. No visual, native, accessibility, or owner approval.**

- Workflow source commit: `29038a10f21ef34c5e2948d69e0343532d76922d`
- Workflow result before publishing: `failure`
- Branch: `agent/visual-state-reconciliation-20261023`
- Review sets: named 34, source detail, reel/grid/crop/share and large text
- CI artifact name: `glimpse-reference-draft-29038a10f21ef34c5e2948d69e0343532d76922d`

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
  "errors": [
    "home.paused: missing trace metadata",
    "home.uncertain: missing trace metadata",
    "Evidence-level totals differ from records"
  ],
  "approval": "NONE",
  "optical": "NOT TESTED; inspect browser-rendered contact sheets"
}
~~~

## Rendered evidence

- Missing: /tmp/glimpse-visual-phone/index.json
- Missing: /tmp/glimpse-visual-journeys/index.json

**Status describes automatic structural/render execution, NOT whether humans saw/read or accepted the PNG files.**
