#!/usr/bin/env python3
"""Validate 34 original core-state slots against depicted PHONE evidence.

Structural/text evidence only. This is deliberately not raster, device, TalkBack,
OS, or owner approval. Keep every failure visible and do not use annotation text.
"""
import html
import json
import re
import sys
import xml.etree.ElementTree as ET
from collections import Counter
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
PREFIX = "design/proposals/"
EVIDENCE = ROOT / (PREFIX + "core-state-evidence.v1.json")
VISUAL = ROOT / (PREFIX + "visual-coverage.v1.json")
S = "{http://www.w3.org/2000/svg}"
EXPECTED_BASELINE = {
    "home": "active", "collections": "three", "grid": "selected",
    "quick-import": "screenshot", "share": "original",
    "automation": "running", "discover": "sources",
    "destinations": "home", "image-editor": "home",
    "activity": "acknowledged", "automation-editor": "simple",
    "source-detail": "online", "photo-detail": "details",
    "quickstart": "ready", "schedule-simulator": "forecast",
    "diagnostics": "all-clear", "widgets": "preview",
    "quick-settings-and-shortcuts": "available", "settings": "general",
    "backup-and-restore": "export", "onboarding": "welcome",
    "error-empty-permission": "permission",
    "accessibility-preferences": "standard",
}
REEL_REPRESENTED = {"middle-22", "three", "one", "empty", "last", "reduced-motion"}
LEVELS = {"pictured-state", "partial-pictured", "related-only"}

def main():
    errors, warnings = [], []
    data = json.loads(EVIDENCE.read_text(encoding="utf-8"))
    visual = json.loads(VISUAL.read_text(encoding="utf-8"))
    screens = {s["id"]: s for s in visual["screens"]}
    slots = {(s["id"], v) for s in screens.values() for v in s["variants"]}
    accounted = {(k, v) for k, v in EXPECTED_BASELINE.items()}
    accounted |= {("reel", v) for v in REEL_REPRESENTED}
    expected = slots - accounted
    actual = [(s["surfaceId"], s["stateId"]) for s in data["states"]]
    if len(expected) != 34 or len(slots) != 63:
        errors.append(f"contract drift: {len(slots)} declared; {len(expected)} unpictured slots")
    if Counter(actual) != Counter(expected):
        errors.append("34-slot identity mismatch: " + str(sorted(set(actual) ^ expected)))
    if len(data["states"]) != data["requiredStates"] or len(data["states"]) != 34:
        errors.append("Expected exactly 34 unique state records")
    if data["sourceCounts"] != {
        "originalCurrentGenerationSvg": 66, "p0Storyboards": 82,
        "p1AdvancedStoryboards": 55, "newTargetedRefinements": 25
    }:
        errors.append("Asset provenance count labels drifted; reconcile, do not inflate baselines")
    counted = Counter()
    new_paths = set()
    for state in data["states"]:
        key = state["surfaceId"] + "." + state["stateId"]
        path = state["assetPath"]
        level = state["evidenceLevel"]
        counted[level] += 1
        if level not in LEVELS:
            errors.append(key + ": unexpected evidence level")
        if not path.startswith(PREFIX) or not path.endswith(".svg"):
            errors.append(key + ": invalid asset path")
            continue
        if path.startswith(PREFIX + "state-refinements/"):
            new_paths.add(path)
        asset = ROOT / path
        if not asset.is_file():
            errors.append(key + ": missing " + path)
            continue
        try:
            svg = ET.parse(asset).getroot()
        except ET.ParseError as exc:
            errors.append(key + ": malformed SVG " + str(exc))
            continue
        if svg.tag != S + "svg" or svg.get("viewBox") != "0 0 1160 920":
            errors.append(key + ": unexpected SVG root/viewport")
        if svg.find(S + "title") is None or svg.find(S + "desc") is None:
            errors.append(key + ": missing SVG title/desc")
        if svg.find(S + "metadata") is None:
            errors.append(key + ": missing trace metadata")
        # A text string is credited only when a visible SVG text element in
        # the PHONE left side contains it. An annotation x>=439 is irrelevant.
        labels = [html.unescape("".join(t.itertext())).strip() for t in
                  svg.iter(S + "text") if float(t.get("x", "9999")) < 420]
        for claimed in state["phoneEvidenceLabels"]:
            if not any(claimed in label for label in labels):
                errors.append(key + ": evidence label not pictured on phone: " + claimed)
        if state["approvalStatus"] != "draft-not-owner-approved":
            errors.append(key + ": illegal approval promotion")
        if state["opticalStatus"] != "needs-raster-and-human-review":
            errors.append(key + ": unwarranted optical signoff")
        if level == "related-only":
            warnings.append(key + " is NOT covered (related surface only)")
        if level == "partial-pictured":
            warnings.append(key + " requires additional complete surface/transition evidence")
    if dict(counted) != data["evidenceCounts"]:
        errors.append("Evidence-level totals differ from records")
    actual_new = set(str(p.relative_to(ROOT)) for p in
                     (ROOT / PREFIX / "state-refinements").glob("*.svg"))
    if new_paths != actual_new or len(actual_new) != data['sourceCounts']['newTargetedRefinements']:
        errors.append("The supplemental refinements have an incomplete manifest registry")
    print(json.dumps({"declaredStates": len(slots), "baselineCredited": len(accounted),
                      "unpicturedOriginalSlots": len(expected), "evidenceLevels": counted,
                      "newRefinements": len(new_paths), "warnings": warnings,
                      "errors": errors, "approval": "NONE",
                      "optical": "NOT TESTED; inspect browser-rendered contact sheets"}, indent=2))
    return bool(errors)

if __name__ == "__main__":
    sys.exit(int(main()))
