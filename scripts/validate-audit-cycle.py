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
counts = {x: sum(v["status"] == x for v in cycle["results"])
          for x in ("PASS", "ISSUE", "UNVERIFIED", "NOT_APPLICABLE")}
assert counts == cycle["summary"]["counts"]
assert sum(counts.values()) == 60
print(json.dumps({"questions": 60, "categories": 6, "revisionLanes": 8,
                  "scenarioTargets": sum(len(x["requiredScenarios"]) for x in plan["lanes"]),
                  "results": counts, "type": "document_trace_validation_only"}, indent=2))
