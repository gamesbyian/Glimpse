#!/usr/bin/env python3
"""Fail closed if the draft reviewer snapshot drifts from selected SVG bytes.

This is a source-integrity gate only. It never asserts optical, native or human
acceptance. Run after regenerating the snapshot, before offering an owner review.
"""
import hashlib
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
MANIFEST = ROOT / "design/proposals/whole-product-review-selection.v1.json"
SNAPSHOT = ROOT / "design/atlas/whole-product-review-snapshot.v1.json"


def main():
    proposal = json.loads(MANIFEST.read_text(encoding="utf-8"))
    frozen = json.loads(SNAPSHOT.read_text(encoding="utf-8"))
    assert proposal["approval"]["ownerApproved"] is False
    assert frozen["status"] == "DRAFT_REVIEW_SNAPSHOT_NOT_APPROVED"
    assert frozen["referenceManifest"] == str(MANIFEST.relative_to(ROOT))
    expected = []
    expected += [x["reviewCandidatePath"] for x in proposal["primarySurfaces"]]
    expected += [v["reviewCandidatePath"] for s in proposal["supportSurfaces"] for v in s["variants"]]
    assert len(expected) == 58 and len(set(expected)) == 58, "expected 24 unique primary and 34 support sources"
    by_path = {e["path"]: e for e in frozen["sources"]}
    assert len(by_path) == len(frozen["sources"]), "duplicate snapshot source path"
    assert frozen["uniqueReferencedSources"] == len(by_path), "snapshot inventory count mismatch"
    failures = []
    for path in expected:
        entry = by_path.get(path)
        target = ROOT / path
        if entry is None or not target.is_file():
            failures.append(f"{path}: missing snapshot entry/source file")
            continue
        actual = hashlib.sha256(target.read_bytes()).hexdigest()
        if actual != entry["sourceSHA256"]:
            failures.append(f"{path}: source SHA256 {actual} != snapshot {entry['sourceSHA256']}")
        if entry.get("ownerApproved") is not False:
            failures.append(f"{path}: snapshot falsely signals approval")
    if failures:
        raise SystemExit("FAIL: selected candidate source integrity\n" + "\n".join(failures))
    print("PASS: 24 primary + 34 support selected exact SHA-256 bytes match draft snapshot; no owner approval")


if __name__ == "__main__":
    main()
