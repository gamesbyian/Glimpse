#!/usr/bin/env python3
"""Fail closed on external/repo-relative raster assets in Glimpse proposal SVGs.

Each illustrated SVG should be self-contained to render identically in source,
GitHub's offline atlas artifact and local optical reference sheets. Only image
data URIs and local SVG fragment references are allowed. Metadata, annotations
and blank placeholders cannot hide broken image paths. Not a visual signoff.
"""
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1] / "design/proposals"
BAD = []
ASSETS = 0

for svg in ROOT.rglob("*.svg"):
    text = svg.read_text(encoding="utf-8")
    for match in re.finditer(r"<image\b[^>]*?(?<![\w:-])(?:href|xlink:href)=[\"']([^\"']+)[\"']", text, re.S):
        value = match.group(1)
        ASSETS += 1
        if value.startswith("#") or value.startswith("data:image/jpeg;base64,") or value.startswith("data:image/png;base64,") or value.startswith("data:image/webp;base64,"):
            continue
        BAD.append((str(svg.relative_to(ROOT)), value[:100]))

print(f"Scanned {len(list(ROOT.rglob('*.svg')))} proposal SVGs and {ASSETS} embedded/linked image nodes.")
if BAD:
    for path, url in BAD:
        print(f"ERROR: {path} has non-portable photo reference {url}")
    sys.exit(1)
print("Self-contained photo references: source-level PASS (not an optical PASS).")
