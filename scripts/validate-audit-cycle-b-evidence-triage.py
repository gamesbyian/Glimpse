#!/usr/bin/env python3
"""Protect Glimpse Cycle B source-delta triage against premature UX rescoring."""
from pathlib import Path
from xml.etree import ElementTree as ET
import json

ROOT=Path(__file__).resolve().parents[1]
A=json.loads((ROOT/"docs/design/glimpse-ux-audit-cycle-a.v1.json").read_text())
B=json.loads((ROOT/"docs/design/glimpse-ux-audit-cycle-b-evidence-triage.v1.json").read_text())
assert B["schemaVersion"]==1 and B["status"]=="SOURCE_DELTA_TRIAGE_NOT_NEW_SCORE"
assert B["unchangedCycleA"]==A["summary"]["counts"]
assert A["summary"]["counts"]=={"PASS":18,"ISSUE":8,"UNVERIFIED":34,"NOT_APPLICABLE":0}
assert B["auditQuestions"]==len(A["results"])==60
rows=B["candidateEvidenceDeltas"]
assert len(rows)==18 and len(set(r["id"] for r in rows))==18
audit={r["id"]:r for r in A["results"]}
for r in rows:
    prior=audit[r["id"]]
    assert r["cycleA"]==prior["status"],r["id"]
    assert r["weight"]==prior["weight"],r["id"]
    assert r["cycleB"]=="EVIDENCE_ADDED_DO_NOT_RESCORE"
    assert r["referenceSources"]
    for p in r["referenceSources"]:
        assert p.startswith("design/proposals/audit-revisions/") and p.endswith(".svg"),p
        file=ROOT/p
        assert file.exists(),p
        svg=ET.parse(file).getroot()
        assert svg.tag=="{http://www.w3.org/2000/svg}svg"
        assert svg.get("viewBox")=="0 0 1160 920"
assert "source" in B["sourceRenderedEvidence"]
assert B["sourceRenderedEvidence"].endswith("/source-ledger.json")
print("PASS: 18 evidence changes tied to 60 original weighted questions, zero silent status changes")
