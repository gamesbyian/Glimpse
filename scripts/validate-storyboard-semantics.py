#!/usr/bin/env python3
"""Deterministic semantic/identity invariants for key Glimpse design storyboard fixtures.

This checks XML/phone-side text/metadata consistency, never pixel layout,
visibility behind clip paths, Android functionality, or owner acceptance.
"""
import html
import json
import re
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
P0 = ROOT / "design/proposals/storyboards"
P1 = ROOT / "design/proposals/advanced-storyboards"
SVG = "{http://www.w3.org/2000/svg}"
ERRORS = []

def check(path, identity=None, terms=(), applied="unchanged", group=None):
    try:
        root = ET.parse(path).getroot()
    except (FileNotFoundError, ET.ParseError) as exc:
        ERRORS.append(str(path.relative_to(ROOT)) + ": " + str(exc))
        return
    metanode = root.find(SVG + "metadata")
    try:
        meta = json.loads(metanode.text) if metanode is not None else {}
    except (ValueError, TypeError):
        meta = {}
    phone_text = [html.unescape("".join(n.itertext())).strip() for n in
                  root.iter(SVG + "text") if float(n.attrib.get("x", "9999")) < 420]
    data = " | ".join(phone_text)
    tag = str(path.relative_to(ROOT))
    if identity is not None and meta.get("selectedAssetId") != identity:
        ERRORS.append(tag + ": selectedAssetId not " + identity)
    if group and meta.get("journeyId") != group:
        ERRORS.append(tag + ": journeyId mismatch")
    if applied is not None and meta.get("appliedStatus") != applied:
        ERRORS.append(tag + ": appliedStatus mismatch")
    for term in terms:
        if term not in data:
            ERRORS.append(tag + ": missing PHONE-side text: " + term)
    return meta

def main():
    expected = [
        ("01-rest", "p12", "rest", 0), ("02-touch-down", "p12", "pointer-down", 0),
        ("03-drag-18", "p12", "mid-drag", 18),
        ("04-drag-36", "p12", "mid-drag", 36),
        ("05-snap-72", "p13", "snapped-next", 72),
        ("06-reverse-18", "p13", "reverse-drag", -18),
        ("07-interrupted", "p13", "interrupted-settle", None),
        ("08-last", "p22", "finite-last", None),
    ]
    for frame, asset, phase, delta in expected:
        meta = check(P0 / "x05" / (frame + ".svg"), identity=asset,
                     terms=("SELECTED " + asset,), group="x05")
        if meta and (meta.get("motionPhase") != phase or
                     meta.get("gestureDeltaDegrees") != delta):
            ERRORS.append("x05." + frame + ": inaccurate pose trace metadata")
    scale = [
        ("01-first", "p01", "Photo 1 of 22"),
        ("02-single", "p01", "Photo 1 of 1"),
        ("03-three", "p02", "Photo 2 of 3"),
        ("04-exact-five", "p03", "Photo 3 of 5"),
        ("05-twenty-two", "p12", "Photo 12 of 22"),
        ("06-sixty", "p39", "Photo 39 of 60"),
        ("07-last", "p22", "Photo 22 of 22"),
    ]
    for frame, asset, term in scale:
        check(P0 / "x06" / (frame + ".svg"), identity=asset,
              terms=(term, asset), group="x06")
    check(P0 / "x11/04-save-only.svg", "p12c",
          ("Saved p12c to Rainbows", "Device wallpaper has not changed."), group="x11")
    check(P0 / "x11/06-use-now.svg", "p12c",
          ("Edited copy p12c ready", "Set Home wallpaper"), group="x11")
    # P1's original/edited distinction must agree with metadata. P1 does
    # not currently encode wallpaper outcomes, hence applied=None.
    check(P1 / "x13/02-edited.svg", "p12c",
          ("CROPPED COPY p12c", "Share edited copy"), applied=None, group="x13")
    print(json.dumps({"cases": len(expected)+len(scale)+3,
                      "semanticSourceErrors": ERRORS,
                      "opticalProof": False, "deviceProof": False,
                      "ownerApproval": False}, indent=2))
    return 1 if ERRORS else 0

if __name__ == "__main__":
    sys.exit(main())
