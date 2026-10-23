#!/usr/bin/env python3
"""Deterministic semantic/identity invariants for key Glimpse design storyboard fixtures.

This checks XML/phone-side text/metadata consistency, never pixel layout,
visibility behind clip paths, Android functionality, or owner acceptance.
"""
import base64
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
    # X07 p12 persists across reel -> grid -> detail -> reel. Compare the
    # actual JPEG data URI with X05, not a text caption or a file name.
    x05root = ET.parse(P0 / "x05/01-rest.svg").getroot()
    x05fixture = next((e.get("href") for e in x05root.iter()
                      if e.get("id") == "fixture1"), None)
    if not x05fixture or not x05fixture.startswith("data:image/jpeg;base64,"):
        ERRORS.append("x05.01-rest: original p12 embedded photo missing")
    x07 = [
        ("01-reel-focus", "Photo 12 of 22"),
        ("02-grid-focus", "Selected p12 of 22"),
        ("03-photo-detail", "Local photo p12"),
        ("04-back-reel", "Photo 12 of 22"),
        ("06-return-original", "Photo 12 of 22"),
        ("07-multi-select", "3 selected"),
    ]
    for frame, label in x07:
        path = P0 / "x07" / (frame + ".svg")
        check(path, "p12", (label,), group="x07")
        root = ET.parse(path).getroot()
        matching = [e.get("href") for e in root.iter()
                    if e.get("id") == "fixture1"]
        if matching != [x05fixture]:
            ERRORS.append("x07." + frame + ": p12 photographic pixels diverge from X05")
    # Finite p22 must be a different real photographic asset from the p13
    # snowy fixture, and both its hero and selected disc wedge use it.
    final_root = ET.parse(P0 / "x05/08-last.svg").getroot()
    final_img = next((n.get("href") for n in final_root.iter(SVG + "image")
                      if n.get("id") == "fixture4"), None)
    original_p22 = (ROOT / "design/research/reel-layout/fixtures/source/art4.jpg").read_bytes()
    if not final_img or not final_img.startswith("data:image/jpeg;base64,"):
        ERRORS.append("x05.08-last: distinct photographic fixture4 for p22 missing")
    elif base64.b64decode(final_img.split(",", 1)[1]) != original_p22:
        ERRORS.append("x05.08-last: selected p22 JPEG differs from public art4 fixture")
    final_uses = [n.get("href") for n in final_root.iter(SVG + "use")]
    if len(final_uses) < 4 or final_uses[0] != "#fixture4" or final_uses[3] != "#fixture4":
        ERRORS.append("x05.08-last: p22 hero and selected aperture disagree")
    # Native raster sweep found status captions outside their pale badges.
    # Enforce enough width for the known short status strings.
    badge_fixture = {
        "x08/01-home-active.svg": ('<rect x="49" y="494" width="116"', 'CURRENT HOME'),
        "x08/06-status-collapsed.svg": ('<rect x="49" y="494" width="116"', 'CURRENT HOME'),
        "x10/06-rotation-delayed.svg": ('<rect x="49" y="374" width="167"', 'LAST ACKNOWLEDGED'),
    }
    for frame, (chip, label) in badge_fixture.items():
        payload = (P0 / frame).read_text(encoding="utf-8")
        if chip not in payload or label not in payload:
            ERRORS.append(frame + ": current/confirmed status badge would clip")
    # Optical defects discovered in the first current-branch raster sheets.
    # These are protective source assertions, not independent pixel proof.
    optical_fixtures = {
        "accessibility-high-contrast": ("High contrast   ON", "M320 272 L326 279 L341 261"),
        "automation-paused": ("p12 · confirmed", 'x="110" y="466" width="145"'),
        "automation-delayed": ("p12 · confirmed", 'x="115" y="485" width="145"'),
        "quick-import-progress": ("Continue disabled until 100% verified", 'fill="#D9DEDA"'),
        "image-editor-blur-extend": ('radialGradient id="extension-red"', 'radialGradient id="extension-blue"'),
    }
    for name, terms in optical_fixtures.items():
        path = ROOT / "design/proposals/state-refinements" / (name + ".svg")
        body = path.read_text(encoding="utf-8")
        for term in terms:
            if term not in body:
                ERRORS.append(name + ": lost optical source invariant " + term)
    blur = (ROOT / "design/proposals/state-refinements/image-editor-blur-extend.svg").read_text(encoding="utf-8")
    if "feGaussianBlur" in blur:
        ERRORS.append("image-editor-blur-extend: unsupported CairoSVG filter reintroduced")
    print(json.dumps({"cases": len(expected)+len(scale)+3+len(x07)+len(optical_fixtures)+1+len(badge_fixture),
                      "semanticSourceErrors": ERRORS,
                      "opticalProof": False, "deviceProof": False,
                      "ownerApproval": False}, indent=2))
    return 1 if ERRORS else 0

if __name__ == "__main__":
    sys.exit(main())
