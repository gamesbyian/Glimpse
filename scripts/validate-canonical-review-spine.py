#!/usr/bin/env python3
"""Validate a provisional human-review spine and optionally emit a hash snapshot.

A generated hash snapshot is a review candidate, NEVER owner approval.
"""
import argparse
import hashlib
import json
from pathlib import Path
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
INDEX = ROOT / "design/proposals/canonical-review-spine.v1.json"
ATLAS = ROOT / "design/atlas/canonical-review-spine.html"

def main():
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument("--emit-hashes", type=Path, help="Optional JSON path for review-time source SHA256 snapshot, not approval")
    args = ap.parse_args()
    manifest = json.loads(INDEX.read_text(encoding="utf-8"))
    assert manifest["schemaVersion"] == 1
    assert manifest["approved"] is False
    assert manifest["auditBaseline"] == {"PASS": 18, "ISSUE": 8, "UNVERIFIED": 34}
    assert manifest["checks"]["totalLinkedSVGs"] == 39
    assert len(manifest["groups"]) == 8
    page = ATLAS.read_text(encoding="utf-8")
    assert "39 selected" in page
    seen = set()
    out = []
    for group in manifest["groups"]:
        assert group["items"], group["title"]
        for item in group["items"]:
            rel = item["path"]
            assert rel.startswith("design/proposals/") and rel.endswith(".svg"), rel
            assert rel not in seen, f"Duplicated whole-set review source: {rel}"
            seen.add(rel)
            path = ROOT / rel
            assert path.is_file(), f"Missing review source: {rel}"
            root = ET.parse(path).getroot()
            assert root.tag == "{http://www.w3.org/2000/svg}svg", rel
            assert root.attrib.get("viewBox") == "0 0 1160 920", rel
            assert "../proposals/" + rel.removeprefix("design/proposals/") in page, rel
            assert item["exactOwnerApprovedSourceSHA"] is None, rel
            out.append({"group": group["title"], "source": rel, "sourceSHA256": hashlib.sha256(path.read_bytes()).hexdigest(), "approved": False})
    assert len(seen) == 39, len(seen)
    if args.emit_hashes:
        args.emit_hashes.parent.mkdir(parents=True, exist_ok=True)
        args.emit_hashes.write_text(json.dumps({"status": "REVIEW_SNAPSHOT_NOT_APPROVED", "count": 39, "sources": out}, indent=2) + "\n", encoding="utf-8")
    print("PASS: 39 unique existing 1160×920 draft SVGs, 8 review chapters and intact phone-only atlas links; no owner approval")

if __name__ == "__main__":
    main()
