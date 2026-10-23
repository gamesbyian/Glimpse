#!/usr/bin/env python3
"""Check traceability and structural consistency of *proposal* SVGs.

No screenshots or user acceptance are inferred from a passing XML/contract test.
Run: python3 scripts/validate-visual-spec.py
"""
from __future__ import annotations
import json
import re
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
SVG_NS = "{http://www.w3.org/2000/svg}"
errors: list[str] = []
warnings: list[str] = []


def read_json(path: str):
    return json.loads((ROOT / path).read_text(encoding="utf-8"))


def assert_equal(actual, expected, label):
    if actual != expected:
        errors.append(f"{label}: expected {expected!r}, got {actual!r}")


contract = read_json("docs/design/screen-contract.json")
main = read_json("design/proposals/visual-coverage.v1.json")
support = read_json("design/proposals/support-coverage.v1.json")
tokens = read_json("design/tokens/brand.json")
assert_equal(contract["destinations"], ["Home", "Discover", "Collections", "Automation"], "contract tabs")
assert_equal(tokens["navigation"], contract["destinations"], "token tabs")
assert_equal([s["id"] for s in main["screens"]], [s["id"] for s in contract["screens"]], "24 core surfaces")
assert_equal(len(main["screens"]), 24, "core count")
assert_equal(len(support["screens"]), 16, "support count")

for core, specified in zip(main["screens"], contract["screens"]):
    for field in ("required", "conditional", "forbidden"):
        assert_equal(core[field], specified[field], core["id"] + "/" + field)

ids = ["W%02d" % i for i in range(1, 71)] + ["F071", "F072", "F073"]
assert_equal(main["workflowIds"], ids, "original workflow IDs")
covered = {id_ for s in main["screens"] for id_ in s["workflows"]}
assert_equal(sorted(covered), sorted(ids), "73 user workflow routes")
assert_equal(len(set(s["id"] for s in support["screens"])), 16, "unique support IDs")
assert_equal(len(set(s["id"] for s in main["screens"])), 24, "unique core IDs")
assert_equal(len(set(x for s in support["screens"] for x in s["states"])), len(set(x for s in support["screens"] for x in s["states"])), "support state set")
assert_equal(len([x for s in support["screens"] for x in s["states"]]), 34, "support state variants")
assert_equal(sum(len(s["variants"]) for s in main["screens"]), 63, "core conditional variants")

# The exact subset of high-risk native states for which a photo-backed
# reference exists. These do not replace runtime motion evidence.
critical = ["empty", "one", "three", "twenty-two", "last",
            "reduced-motion", "large-text", "interrupted"]

checked = 0


def svg(path: str, label: str) -> ET.Element | None:
    global checked
    file = ROOT / path
    if not file.is_file():
        errors.append("Missing reference: " + path)
        return None
    data = file.read_text(encoding="utf-8")
    try:
        root = ET.fromstring(data)
    except ET.ParseError as exc:
        errors.append("Malformed SVG " + path + ": " + str(exc))
        return None
    if root.tag != SVG_NS + "svg":
        errors.append("Wrong SVG root: " + path)
    if root.attrib.get("viewBox") not in ("0 0 1160 920",):
        errors.append("Inconsistent frame dimensions: " + path)
    if any(x.tag in (SVG_NS + "script", SVG_NS + "foreignObject")
           for x in root.iter()):
        errors.append("Unexpected active content: " + path)
    if "PROPOSAL" not in data.upper() and "DRAFT" not in data.upper():
        errors.append("Missing explicit nonapproved status: " + path)
    if "#F6F0E6" not in data or "#153F46" not in data:
        errors.append("Missing shared paper/ink palette: " + path)
    if "Today     Explore" in data or "Today / Explore" in data:
        errors.append("Legacy tab names in active plate: " + path)
    if re.search(r"<text[^>]*>(?:Set now|Next)</text>", data):
        errors.append("Ambiguous actionable label: " + path)
    checked += 1
    return root


for s in main["screens"]:
    svg(f"design/proposals/plates/{s['id']}.svg", s["id"])
for s in support["screens"]:
    for state in s["states"]:
        svg(f"design/proposals/support-plates/{s['id']}--{state}.svg",
            s["id"] + "/" + state)
for state in critical:
    root = svg(f"design/proposals/critical-reel/reel-{state}.svg", state)
    if root is not None:
        n = sum(1 for element in root.iter() if element.tag == SVG_NS + "image")
        if state == "empty" and n:
            errors.append("Empty reference paints photo content")
        if state == "one" and n != 2:  # hero and one real wedge
            errors.append("One-image reference has non-unique photo slots")
        if state == "three" and n != 4:  # hero plus three real wedges
            errors.append("Three-image reference has misleading extra imagery")

# Explicit recorded exclusions: do not silently absorb exploratory dual-wheel
# or older Waltz art into the canonical proposal.
if "design/research/reel-layout/*.svg" not in main["existingArtworkClassification"]:
    errors.append("Research variants must remain marked as nonauthoritative")

print(json.dumps({
    "checkedSvgFiles": checked,
    "coreScreenFamilies": len(main["screens"]),
    "supportScreenFamilies": len(support["screens"]),
    "supportStatePlates": 34,
    "criticalReelStatePlates": len(critical),
    "routedHumanWorkflows": len(covered),
    "textTracedCoreRequired": sum(len(x["required"]) for x in main["screens"]),
    "draftCoreVariantsWithoutCompleteScreenPlates": 63,
    "warnings": warnings,
    "errors": errors,
    "evidenceStatement": "Static structural checks, not pixel/UI/device/owner acceptance",
}, indent=2))
if errors:
    sys.exit(1)
