#!/usr/bin/env python3
"""Validate exhaustive 56-scenario audit source mapping, not UX acceptance."""
import json
import re
from pathlib import Path
from xml.etree import ElementTree as ET

ROOT=Path(__file__).resolve().parents[1]
def load(p):
    return json.loads((ROOT/p).read_text(encoding="utf-8"))

plan=load("design/proposals/audit-driven-revision-plan.v1.json")
trace=load("design/proposals/audit-scenario-evidence.v1.json")
required={(lane["id"], name) for lane in plan["lanes"] for name in lane["requiredScenarios"]}
seen={(s["lane"], s["scenario"]) for s in trace["scenarios"]}
assert len(required)==56 and len(seen)==len(trace["scenarios"])==56, "Scenario keys duplicated or missing"
assert seen==required, f"Missing {sorted(required-seen)}, unexpected {sorted(seen-required)}"
states={"PICTURED":0,"PARTIAL":0,"MISSING_SPECIFIC":0}
svg_ns="{http://www.w3.org/2000/svg}svg"
for scenario in trace["scenarios"]:
    state=scenario["evidenceState"]
    assert state in states, scenario
    states[state]+=1
    paths=scenario["sourcePaths"]
    assert paths and len(set(paths))==len(paths), scenario
    for rel in paths:
        assert rel.startswith("design/proposals/") and rel.endswith(".svg"), rel
        file=ROOT/rel
        assert file.is_file(), rel
        root=ET.parse(file).getroot()
        assert root.tag==svg_ns and root.get("viewBox")=="0 0 1160 920", rel
    if state!="PICTURED":
        assert len(scenario["nextProof"])>=30, scenario
assert states=={key:trace["counts"][key] for key in states}
assert states["PICTURED"]>=50, "Core revision scenario art unexpectedly regressed"
# All 56 pictures can exist while native usability, optical acceptance and owner signoff remain unverified.
assert trace["auditCycleA"]=={"PASS":18,"ISSUE":8,"UNVERIFIED":34}

size_examples=trace["finiteCountExamples"]
assert [p["photoTotal"] for p in size_examples]==[0,1,3,5,22,60]
for photo in size_examples:
    n=photo["photoTotal"]
    file=ROOT/photo["path"]
    assert file.is_file(), file
    root=ET.parse(file).getroot()
    metadata=root.find("{http://www.w3.org/2000/svg}metadata")
    assert metadata is not None, file
    info=json.loads(metadata.text)
    assert info.get("photoTotal")==n, (n,info.get("photoTotal"))
    if n in (3,5,60):
        visible=info["fixtureArtworkByPhotoId"]
        assert len(visible)==min(n,5)
        assert len(set(visible.values()))==min(n,5)
        assert len(info["photoFixtureSHA256"])==min(n,5)
        src=file.read_text(encoding="utf-8")
        assert len(re.findall(r'<g clip-path="url\(#c[0-4]\)"><use href="#fixture[1-6]"',src))==min(n,5)

offline_path=ROOT/"design/proposals/audit-revisions/r04-import-offline-17-of-22.svg"
offline=ET.parse(offline_path).getroot()
offline_meta=json.loads(offline.find("{http://www.w3.org/2000/svg}metadata").text)
assert offline_meta["networkStatus"]=="offline"
assert offline_meta["verifiedLocalCopies"]==17
assert offline_meta["pendingPhotos"]==5
assert offline_meta["hasFullBatchSuccess"] is False
assert offline_meta["resumeEligibility"]=="requires-network-and-valid-provider-permission"
phone_labels=[
    e.text or "" for e in offline.iter()
    if e.tag=="{http://www.w3.org/2000/svg}text"
    and float(e.attrib.get("x",1000))<420
]
joined=" | ".join(phone_labels)
assert "17 of 22 saved locally" in joined
assert "Resume unavailable" in joined
assert "Review 17 saved photos" in joined
assert "No wallpaper was applied by importing" in joined
assert "22 of 22" not in joined, "No all-verified claim when offline"

print(f"PASS: 56 required audit scenarios traced; {states['PICTURED']} pictured, "
      f"{states['PARTIAL']} partial, {states['MISSING_SPECIFIC']} missing-specific. "
      "Six finite-size source cases; ZERO native/owner acceptance claims.")
