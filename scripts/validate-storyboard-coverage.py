#!/usr/bin/env python3
"""Independent P0 storyboard traceability checks. This is not visual approval."""
import json
import re
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
manifest = json.loads((ROOT / "design/proposals/storyboard-coverage.v1.json").read_text())
groups = manifest["groups"]
errors = []
assert [g["id"] for g in groups] == [f"x{i:02d}" for i in range(1,13)]
seen = set()
for group in groups:
    frames = group["frames"]
    if len(frames) < 5:
        errors.append(group["id"] + ": fewer than 5 connected frames")
    for i, f in enumerate(frames, 1):
        path = f["path"]
        if path in seen:
            errors.append("Duplicate path " + path)
        seen.add(path)
        if f["ordinal"] != i or not path.startswith("design/proposals/storyboards/" + group["id"] + "/"):
            errors.append("Invalid order/path for " + path)
        asset = ROOT / path
        if not asset.is_file():
            errors.append("Missing " + path)
            continue
        try:
            svg = ET.parse(asset).getroot()
        except ET.ParseError as e:
            errors.append("Malformed SVG " + path + ": " + str(e))
            continue
        tag = "{http://www.w3.org/2000/svg}"
        if svg.tag != tag + "svg" or svg.get("viewBox") != "0 0 1160 920":
            errors.append("Invalid drawing frame " + path)
        title = svg.find(tag + "title")
        desc = svg.find(tag + "desc")
        if title is None or desc is None or not (desc.text or "").strip():
            errors.append("Missing accessible title or description " + path)
        if any(n.tag in (tag + "script", tag + "foreignObject") for n in svg.iter()):
            errors.append("Unexpected active SVG content " + path)
        content = asset.read_text(encoding="utf-8")
        if "NOT APPROVED" not in content.upper() and "UNAPPROVED" not in content.upper() and "NOT USER" not in content.upper() and "DRAFT" not in content.upper():
            errors.append("Missing explicit unapproved status " + path)
        # Keep user imports, local file paths and arbitrary external network
        # references out of public reference art.
        safe_text = content.replace("http://www.w3.org/2000/svg", "").replace("http://www.w3.org/1999/xlink", "")
        if re.search(r"(?:file://|content://|/data/user/|https?://[^\s\x22]+)", safe_text):
            errors.append("Disallowed external/private path in " + path)
    valid = {f["id"] for f in frames}
    for edge in group.get("transitions", []):
        if edge["from"] not in valid or edge["to"] not in valid:
            errors.append("Broken transition " + group["id"])
# The advanced source files have a separate manifest so their additional
# evidence is not confused with the initial P0 82-frame inventory.
advanced = json.loads(
    (ROOT / "design/proposals/advanced-storyboard-coverage.v1.json").read_text()
)
advanced_groups = advanced["groups"]
if [g["id"] for g in advanced_groups] != [f"x{i:02d}" for i in range(13, 24)]:
    errors.append("P1 groups must cover X13-X23 exactly")
advanced_paths = set()
for g in advanced_groups:
    for i, f in enumerate(g["frames"], 1):
        p = f["path"]
        if f["ordinal"] != i or p in advanced_paths:
            errors.append("P1 duplicate or out-of-order: " + p)
        advanced_paths.add(p)
        if not p.startswith("design/proposals/advanced-storyboards/" + g["id"] + "/"):
            errors.append("P1 scope mismatch: " + p)
        asset = ROOT / p
        if not asset.is_file():
            errors.append("P1 missing: " + p)
            continue
        try:
            svg = ET.parse(asset).getroot()
            if svg.tag != "{http://www.w3.org/2000/svg}svg" or svg.get("viewBox") != "0 0 1160 920":
                errors.append("P1 dimensions/root: " + p)
            if svg.find("{http://www.w3.org/2000/svg}title") is None:
                errors.append("P1 accessible title missing: " + p)
        except ET.ParseError as exc:
            errors.append("P1 XML invalid: " + p + ": " + str(exc))
actual_p1 = set(str(p.relative_to(ROOT)) for p in
                (ROOT / "design/proposals/advanced-storyboards").glob("x??/*.svg"))
if actual_p1 != advanced_paths or len(actual_p1) != 55:
    errors.append("P1 file registry differs from 55 checked SVGs")
files = sorted(str(p.relative_to(ROOT)) for p in (ROOT / "design/proposals/storyboards").glob("x??/*.svg"))
if sorted(seen) != files:
    errors.append("Manifest does not exactly match storyboard files")
if len(seen) != manifest["counts"]["additionalStoryboardSvg"]:
    errors.append("Manifest count differs from storyboard files")
print(json.dumps({"p0Packages":len(groups),"storyboardFrames":len(seen),"advancedStoryboardFrames":len(advanced_paths),"errors":errors,
"status":"structural_validation_only_not_design_approval"},indent=2))
if errors: sys.exit(1)
