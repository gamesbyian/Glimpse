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
# A single B-owned diagnostic disclosure source was corrected after the prior optical
# ledger. Its newer automated 401x890 raster/source hash is known, but has not been
# independently signed off against the historical contact. Do not silently score it.
optical_pending = {
    "design/proposals/support-plates/diagnostic-preview--expanded.svg":
        "6d9b0c31377f18fcddb5f66452ab507f34f20b55212db7719bc92f9f0c248e85"
}
bad = []
pending = []
for p, sha in expected.items():
    actual = selected.get(p, {}).get("sourceSHA256")
    if actual == sha:
        continue
    if p in optical_pending and actual == optical_pending[p]:
        pending.append(p)
    else:
        bad.append(f"{p}: ledger={sha} snapshot={actual}")
if bad or set(pending) != set(optical_pending):
    raise SystemExit("FAIL integrated optical source ledger mismatch or unacknowledged optical delta:\\n" +
                     "\\n".join(bad + ["Pending mismatch set: " + ", ".join(pending)]))
print(f"PASS {len(expected)-len(pending)}/{len(expected)} current A/B optical SHA pairs match; "
      f"{len(pending)} specifically pinned support update has automated raster evidence but "
      "independent current-source optical inspection remains PENDING; "
      f"{len(selected)-len(expected)} extra draft reference sources outside this historical ledger")
