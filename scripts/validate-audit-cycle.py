#!/usr/bin/env python3
"""Validate evidence-led Glimpse audit inventory and follow-up mockup traceability.

This is document/asset integrity, never UI or owner acceptance.
"""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
instrument = json.loads((ROOT / "docs/design/glimpse-ux-audit-60.v1.json").read_text())
cycle = json.loads((ROOT / "docs/design/glimpse-ux-audit-cycle-a.v1.json").read_text())
plan = json.loads((ROOT / "design/proposals/audit-driven-revision-plan.v1.json").read_text())
ids = [q["id"] for q in instrument["questions"]]
assert ids == [f"GUX-{i:02d}" for i in range(1, 61)], "60 sequential question IDs required"
assert len(cycle["results"]) == len(ids)
assert [x["id"] for x in cycle["results"]] == ids, "same ordered audit IDs"
assert len(instrument["categories"]) == 6
assert len(plan["lanes"]) == 8
for q, v in zip(instrument["questions"], cycle["results"]):
    assert q["question"] == v["question"] and q["weight"] == v["weight"]
    assert v["status"] in {"PASS", "ISSUE", "UNVERIFIED", "NOT_APPLICABLE"}
    if v["status"] in {"PASS", "ISSUE"}:
        assert v["evidence"] and (ROOT / v["evidence"]).exists(), (v["id"], v["evidence"])
    assert v["finding"] and v["nextAction"], v["id"]
for lane in plan["lanes"]:
    assert lane["questions"] and lane["requiredScenarios"]
    assert all(x in ids for x in lane["questions"])
    for path in lane.get("initialSourceDrafts", []):
        assert (ROOT / path).exists(), path
# Verify the illustrative finite disc does not fake photographs or change the
# selected stable ID while an 18-degree drag is still in progress.
import re
for state, chosen, total in (
    ("idle-22", 12, 22), ("drag-18", 12, 22),
    ("settled-snap", 13, 22), ("last-22", 22, 22),
    ("empty-0", 0, 0), ("single-1", 1, 1),
):
    path = ROOT / "design/proposals/audit-revisions" / f"r01-{state}.svg"
    src = path.read_text()
    ids = [int(x) for x in re.findall(r">p(\\d\d)</text>", src)]
    expected = [chosen + delta for delta in (0, 1, 2, -2, -1)
                if 1 <= chosen + delta <= total]
    assert ids == expected, (state, ids, expected)
    assert f'"photoTotal":{total}' in src, state
    assert (f'"selectedAssetId":"p{chosen:02d}"' if chosen else
            '"selectedAssetId":null') in src, state
    assert "lastConfirmedAppliedAssetId" in src
assert "Glimpse" in (ROOT / "design/proposals/audit-revisions/r01-idle-22.svg").read_text()
counts = {x: sum(v["status"] == x for v in cycle["results"])
          for x in ("PASS", "ISSUE", "UNVERIFIED", "NOT_APPLICABLE")}
assert counts == cycle["summary"]["counts"]
assert sum(counts.values()) == 60
print(json.dumps({"questions": 60, "categories": 6, "revisionLanes": 8,
                  "scenarioTargets": sum(len(x["requiredScenarios"]) for x in plan["lanes"]),
                  "results": counts, "type": "document_trace_validation_only"}, indent=2))
