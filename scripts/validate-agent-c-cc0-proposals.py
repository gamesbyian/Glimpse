#!/usr/bin/env python3
"""Independently check exact embedded photo bytes in A's six unselected SVGs.

This is source/rights-provenance acceptance, not optical, native or owner approval.
"""
import base64
import hashlib
import json
from pathlib import Path
import sys
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
A = "design/proposals/whole-product-candidates/cc0-consistent-20261024/migration-manifest.v1.json"
C = "design/assets/production-c-provenance.v1.json"
XLINK = "{http://www.w3.org/1999/xlink}href"
REQUIRED = {
    "home": {"p04", "p12"},
    "reel": {"p10", "p11", "p12", "p13", "p14", "p15"},
    "grid": {f"p{n:02d}" for n in range(6, 18)},
    "photo-detail": {"p12"},
    "collections": {"p08", "p12", "p15"},
    "share": {"p12"},
}

def check(condition, message):
    if not condition:
        raise ValueError(message)

def digest(data):
    return hashlib.sha256(data).hexdigest()

def embedded_hashes(path):
    result = []
    for tag in ET.parse(path).getroot().iter():
        if tag.tag.rsplit("}", 1)[-1] != "image":
            continue
        href = tag.get("href") or tag.get(XLINK)
        check(href and href.startswith("data:image/jpeg;base64,"),
              f"{path}: non-portable image reference")
        image = base64.b64decode(href.partition(",")[2], validate=True)
        check(image[:3] == bytes([0xff, 0xd8, 0xff]),
              f"{path}: embedded JPEG magic missing")
        result.append(digest(image))
    check(result, f"{path}: no images")
    return set(result)

def validate(root=ROOT):
    ca = json.loads((root / C).read_text())
    aa = json.loads((root / A).read_text())
    check(aa.get("status") == "UNSELECTED_SOURCE_EDITABLE_MIGRATION"
          and aa.get("ownerApproved") is False
          and aa.get("androidWallpaperCurrentVerified") is False
          and aa.get("sourceMode") == "rights-verified-replacements",
          "Unselected A candidate claims acceptance or lacks rights mode")
    check(aa.get("selectedId") == "p12"
          and aa.get("lastGlimpseAcknowledgedId") == "p04",
          "Selected photo and historical applied photo conflate identities")
    cr = {p["photoId"]: p for p in ca["requestedIndependentPhotos"] +
          ca["verifiedReplacementPhotos"]}
    ar = {p["id"]: p for p in aa["verifiedIndependentPhotos"]}
    check(set(cr) == set(ar) == {"p04"} | {f"p{n:02d}" for n in range(6, 18)},
          "A and C have inconsistent 13-photo identity inventories")
    hashes = {}
    for name, record in cr.items():
        check(record["rightsStatus"] == "verified" and
              record["sha256"] == ar[name]["derivedSHA256"] and
              record["assetPath"] == ar[name]["assetPath"],
              f"{name}: source rights/digest contradiction")
        data = (root / record["assetPath"]).read_bytes()
        check(len(data) == record["derivativeBytes"] and
              digest(data) == record["sha256"],
              f"{name}: missing or mutated exact image bytes")
        hashes[name] = record["sha256"]
    check(len(set(hashes.values())) == 13, "Two photo IDs alias same JPEG")
    old = {p["sha256"] for p in ca["priorFixtureSet"]["assets"]}
    check(not set(hashes.values()).intersection(old),
          "Licensed originals alias old rights-unverified research art")
    check(set(aa["sourcePaths"]) == set(REQUIRED), "Six-screen role inventory drift")
    accepted = 0
    for role, ids in REQUIRED.items():
        images = embedded_hashes(root / aa["sourcePaths"][role])
        for name in sorted(ids):
            check(hashes[name] in images, f"{role} lacks complete genuine {name} JPEG")
            accepted += 1
        check(not images.intersection(old), f"{role} embeds old rights-blocked art")
    print(f"PASS: {accepted} exact photo-role checks in six UNSELECTED SVGs; "
          "13 uniquely SHA-verified CC0 sample identities; p12 continuous. "
          "Not visual, Android or human acceptance.")
    return accepted

if __name__ == "__main__":
    try:
        validate()
    except (OSError, ValueError, KeyError, ET.ParseError) as exc:
        print("FAIL: cross-surface CC0 actual photo bytes:", exc, file=sys.stderr)
        raise SystemExit(1)
