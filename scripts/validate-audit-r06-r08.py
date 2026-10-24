#!/usr/bin/env python3
"""Fast, offline source contract for the R06–R08 candidate visual deltas.

Validates source/metadata integrity only. Does not assert optical, native or owner approval.
"""
import json
from pathlib import Path
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
INDEX = ROOT / "design/proposals/audit-revisions/r06-r08-delta-evidence.v1.json"
SVG_NS = "{http://www.w3.org/2000/svg}"

def main():
    data = json.loads(INDEX.read_text(encoding="utf-8"))
    assert data["auditBaseline"] == {"pass": 18, "issue": 8, "unverified": 34, "changedScores": 0}
    scenarios = data["scenarios"]
    assert len(scenarios) == 5, "Five distinct targeted states expected"
    seen = set()
    for case in scenarios:
        path = ROOT / case["source"]
        prior = ROOT / case["before"]
        assert path.is_file(), path
        assert prior.is_file(), prior
        assert str(path) not in seen, f"Duplicated proposal {path}"
        seen.add(str(path))
        assert case["illustratedInDraft"] is True
        for gate in ("opticallyReviewedFromExactSourceSHA", "verifiedOnAndroid", "ownerApprovedExactSourceSHA"):
            assert case[gate] is False, f"Evidence gate {gate} requires a separate observed proof record"
        assert case["phoneCrop"] == {"x": 15, "y": 14, "width": 401, "height": 890}
        tree = ET.parse(path)
        root = tree.getroot()
        assert root.tag == SVG_NS + "svg"
        assert root.attrib.get("viewBox") == "0 0 1160 920"
        assert root.attrib.get("width") == "1160" and root.attrib.get("height") == "920"
        metadata = root.find(SVG_NS + "metadata")
        assert metadata is not None and metadata.text
        meta = json.loads(metadata.text)
        assert meta["auditLane"] == case["id"].split("-")[0]
        assert meta["proposalStatus"] == "draft-not-owner-approved"
        assert meta["wallpaperMutation"] is False and meta["recipientConfirmed"] is False
        assert meta["phoneCrop"] == [15, 14, 401, 890]
        assert set(case["questions"]).issubset(set(meta["auditQuestions"]))
        texts = [node.text or "" for node in root.iter(SVG_NS + "text")]
        assert "Native Android / TalkBack: unverified" in texts
        assert any("DRAFT" in t for t in texts)
        assert len(texts) >= 20
        print(f"source-only OK: {case['id']} -> {case['source']}")
    print("PASS: 5 R06–R08 draft source checks; 0 optical/native/owner gates assessed")

if __name__ == "__main__":
    main()
