#!/usr/bin/env python3
"""Check proposal SVG XML and image/reference portability, not optical quality.

Scan all image sources and local <use> and clipping/mask/paint references.
X05 and X07 require embedded public fixture photographs on the exact source
files, since otherwise a page may *claim* photographic continuity while
silently rendering empty windows in the offline atlas.
"""
import base64
import binascii
import re
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1] / "design/proposals"
SVG = "{http://www.w3.org/2000/svg}"
XLINK = "{http://www.w3.org/1999/xlink}href"
GOOD_MEDIA = ("image/jpeg", "image/png", "image/webp")
EXPECTED_FIXTURES = {
    **{f"storyboards/x05/{name}.svg": 3 for name in (
        "01-rest", "02-touch-down", "03-drag-18", "04-drag-36",
        "05-snap-72", "06-reverse-18", "07-interrupted", "08-last")},
    **{f"storyboards/x07/{name}.svg": 3 for name in (
        "01-reel-focus", "04-back-reel", "06-return-original")},
    **{f"storyboards/x07/{name}.svg": 1 for name in (
        "02-grid-focus", "03-photo-detail", "07-multi-select")},
}
BAD, embedded, image_nodes, files = [], 0, 0, 0

for svg in sorted(ROOT.rglob("*.svg")):
    files += 1
    rel = str(svg.relative_to(ROOT))
    try:
        root = ET.parse(svg).getroot()
    except ET.ParseError as exc:
        BAD.append(f"{rel}: SVG XML parse failure: {exc}")
        continue
    ids = [e.get("id") for e in root.iter() if e.get("id")]
    idset = set(ids)
    if len(ids) != len(idset):
        BAD.append(f"{rel}: duplicated SVG ID (ambiguous fixture or clip target)")
    fixture_count = 0
    for e in root.iter():
        local = e.tag.removeprefix(SVG)
        href = e.get("href") or e.get(XLINK)
        if local in ("image", "use"):
            if local == "image":
                image_nodes += 1
                if e.get("id", "").startswith("fixture"):
                    fixture_count += 1
                    if rel in EXPECTED_FIXTURES and e.get("preserveAspectRatio") != "xMidYMid slice":
                        BAD.append(f"{rel}: fixture must use crop-to-fill xMidYMid slice; default meet letterboxes the hero/reel")
            if not href:
                BAD.append(f"{rel}: {local} node has no href")
            elif href.startswith("#"):
                if href[1:] not in idset:
                    BAD.append(f"{rel}: unresolved {local} local target {href}")
            elif local == "image" and href.startswith("data:"):
                m = re.fullmatch(r"data:([^;]+);base64,([A-Za-z0-9+/=]+)", href, re.S)
                if not m or m.group(1) not in GOOD_MEDIA:
                    BAD.append(f"{rel}: unsupported or malformed image data URI")
                else:
                    try:
                        raw = base64.b64decode(m.group(2), validate=True)
                        if not raw or (m.group(1) == "image/jpeg" and not raw.startswith(b"\xff\xd8")):
                            raise ValueError("empty or missing JPEG magic")
                        embedded += 1
                    except (binascii.Error, ValueError) as exc:
                        BAD.append(f"{rel}: corrupt embedded raster: {exc}")
            else:
                BAD.append(f"{rel}: non-portable {local} href {href[:100]}")
        for attr in ("clip-path", "mask", "filter", "fill", "stroke"):
            val = e.get(attr, "")
            for target in re.findall(r"url\(#([^)]+)\)", val):
                if target not in idset:
                    BAD.append(f"{rel}: unresolved {attr} target #{target}")
    if rel in EXPECTED_FIXTURES and fixture_count != EXPECTED_FIXTURES[rel]:
        BAD.append(f"{rel}: expected {EXPECTED_FIXTURES[rel]} embedded photo fixtures, got {fixture_count}")

print(f"Scanned {files} proposal SVGs, {image_nodes} image nodes, {embedded} decoded embedded rasters.")
for line in BAD[:100]:
    print("ERROR:", line)
if len(BAD) > 100:
    print(f"... and {len(BAD)-100} further reference errors")
if BAD:
    sys.exit(1)
print("Source-level SVG/photo portability PASS (not optical, device or owner PASS).")
