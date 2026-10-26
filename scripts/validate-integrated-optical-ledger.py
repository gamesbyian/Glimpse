#!/usr/bin/env python3
"""Fail closed when the draft review snapshot disagrees with current A/B optical source ledgers.

Evidence hashes do not constitute owner approval, raster inspection, Android proof, or rights clearance.
"""
import json
from pathlib import Path

root = Path(__file__).resolve().parents[1]
def read(rel):
    return json.loads((root / rel).read_text(encoding="utf-8"))

snap = read("design/atlas/whole-product-review-snapshot.v1.json")
support = read("design/reviews/support-agent-b-2026-10-24/source-ledger.json")
journeys = read("design/reviews/support-agent-b-2026-10-24/full-journey-corpus/full-journey-source-ledger.json")
primary = read("design/system/reviews/ci-renders/ci-raster-ledger.json")

assert snap["status"] == "DRAFT_REVIEW_SNAPSHOT_NOT_APPROVED"
assert len(snap["sources"]) >= 258 and snap["uniqueReferencedSources"] == len(snap["sources"])
selected = {x["path"]: x for x in snap["sources"]}
assert len(selected) == len(snap["sources"]) and all(x["ownerApproved"] is False for x in selected.values())
expected = {}
def add(path, sha):
    assert path not in expected, f"Duplicate optical source: {path}"
    assert len(sha) == 64 and all(ch in "0123456789abcdef" for ch in sha), path
    expected[path] = sha
for x in support["sources"]:
    add(x["source"], x["sourceSHA256"])
for group in journeys["groups"]:
    for x in group["frames"]:
        add(x["source"], x["sha256"])
for x in primary["screens"]:
    add(x["sourcePath"], x["sourceSHA256"])
assert len(support["sources"]) == 34
assert sum(len(g["frames"]) for g in journeys["groups"]) == 137
assert len(primary["screens"]) == 18
assert len(expected) == 189
# Generated B contacts/ledgers are rebuilt from current sources by the atlas job.
# Source and JPEG SHA-256 parity is required; optical/human approval remains separate.
bad = [f"{p}: ledger={sha} snapshot={selected.get(p, {}).get('sourceSHA256')}"
       for p, sha in expected.items()
       if p not in selected or selected[p]["sourceSHA256"] != sha]
if bad:
    raise SystemExit("FAIL current source/raster input ledger mismatch:\\n" + "\\n".join(bad))
import hashlib
def sha(path):
    return hashlib.sha256((root / path).read_bytes()).hexdigest()
for card in support["contacts"]:
    rel = "design/reviews/support-agent-b-2026-10-24/" + card["name"]
    assert sha(rel) == card["SHA256"], rel
for group in journeys["groups"]:
    rel = "design/reviews/support-agent-b-2026-10-24/full-journey-corpus/" + group["contact"]
    assert sha(rel) == group["contactSHA256"], rel
    assert group["generatedOnly"] is True
    assert group["independentlyOpticallyInspected"] is False
print(f"PASS: {len(expected)}/{len(expected)} current source-hashed A/B raster input pairs "
      f"and 6+23 current contact JPEG digests; {len(selected)-len(expected)} other "
      "draft referenced source versions. Generated pixels only; no new owner/native/optical approval.")
