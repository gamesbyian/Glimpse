#!/usr/bin/env python3
"""Validate A's unselected photographic Home candidate against a known C CC0 derivative.

Evidence scope: bytes, stable ID, metadata and proposal states. Not device or owner acceptance.
"""
from __future__ import annotations
import base64
import hashlib
import html
import json
import re
from pathlib import Path
from xml.etree import ElementTree as ET

ROOT = Path(__file__).resolve().parents[2]
FILE = ROOT / "design/proposals/whole-product-candidates/home-p04-photographic-cc0.svg"
EXPECTED = "20d6a8dab7e5eed0fbbbb0d98de799487512264aa7d0cf7ced701944683e2bd0"
SOURCE = "https://commons.wikimedia.org/wiki/File:Random_landscape_photo_I_took.jpg"
SVG = "{http://www.w3.org/2000/svg}"

def validate() -> dict:
    raw = FILE.read_bytes()
    root = ET.fromstring(raw)
    metadata = root.find(SVG + "metadata")
    assert metadata is not None and metadata.text, "Missing embedded provenance metadata"
    state = json.loads(html.unescape(metadata.text))
    assert state["lastConfirmedAppliedAssetId"] == "p04"
    assert state["selectedAssetId"] == "p12"
    assert state["presentOSWallpaperStatus"] == "not-read-back"
    assert state["p04PhotographAuthor"] == "Philsacor"
    assert state["p04PhotographLicense"] == "CC0-1.0"
    assert state["p04PhotographSource"] == SOURCE
    assert state["p04PhotographAssetSHA256"] == EXPECTED
    assert state["ownerApproved"] is False
    assert state["androidVerified"] is False
    assert state["wallpaperMutation"] is False
    assert "p04HeroArtwork" in state
    assert "illustrative vector" not in state["p04HeroArtwork"]
    assert not state.get("sourceSHA256"), "Stale self-hash mislabels a derivative candidate"
    images = [node for node in root.iter(SVG + "image")]
    assert len(images) == 2, "p04 hero plus separately selected p12 preview expected"
    by_region = {(x.get("x"), x.get("y")): x for x in images}
    hero = by_region[("52", "159")]
    href = hero.get("href") or hero.get("{http://www.w3.org/1999/xlink}href")
    assert href and href.startswith("data:image/jpeg;base64,")
    photo = base64.b64decode(href.partition(",")[2], validate=True)
    assert len(photo) == 68374, "p04 derivative bytes changed"
    assert hashlib.sha256(photo).hexdigest() == EXPECTED
    assert hero.get("clip-path") == "url(#p04-photo-cc0-crop)"
    selected = by_region[("53", "512")]
    selected_href = selected.get("href") or selected.get("{http://www.w3.org/1999/xlink}href")
    assert selected_href and selected_href != href, "p04 acknowledgement must not impersonate p12"
    data = raw.decode("utf-8")
    assert "Last acknowledged by Glimpse" in data
    assert "Device may have changed since" in data
    assert "UNAPPROVED SOURCE-EDITABLE CANDIDATE" in data
    return {"candidate": str(FILE.relative_to(ROOT)), "sourceSHA256": hashlib.sha256(raw).hexdigest(),
            "p04JPEG_SHA256": EXPECTED, "p04JPEG_bytes": len(photo),
            "p12Distinct": True, "AndroidVerified": False, "ownerApproved": False}

if __name__ == "__main__":
    print(json.dumps(validate(), indent=2))
