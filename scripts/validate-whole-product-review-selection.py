#!/usr/bin/env python3
"""Validate complete *candidate* Glimpse review selection and optional SHA snapshot.

No GUI or user approval can be inferred from structural source inventory. Source
hash snapshots capture a mutable draft selection, not design signoff.
"""
import argparse
import hashlib
import json
from pathlib import Path
import xml.etree.ElementTree as ET

ROOT=Path(__file__).resolve().parents[1]
SRC="design/proposals/whole-product-review-selection.v1.json"
SVG="{http://www.w3.org/2000/svg}svg"
def load(path):
    return json.loads((ROOT/path).read_text(encoding="utf-8"))
def valid_source(p):
    assert p.startswith("design/proposals/") and p.endswith(".svg"),p
    file=ROOT/p
    assert file.exists(),p
    root=ET.parse(file).getroot()
    assert root.tag==SVG and root.get("viewBox")=="0 0 1160 920",p
    return hashlib.sha256(file.read_bytes()).hexdigest()

def main():
    ap=argparse.ArgumentParser()
    ap.add_argument("--emit",type=Path,help="emit source-hashed candidate snapshot, NOT owner-approved")
    args=ap.parse_args()
    d=load(SRC)
    assert d["schemaVersion"]==1
    assert d["approval"]=={"ownerApproved":False,"ownerApprovalDate":None,"approvedSourceInventoryHash":None}
    assert d["status"]=="PRELIMINARY_WHOLE_PRODUCT_REVIEW_SELECTION_NO_OWNER_APPROVAL"
    core=load("docs/design/screen-contract.json")
    support=load("design/proposals/support-coverage.v1.json")
    p0=load("design/proposals/storyboard-coverage.v1.json")
    p1=load("design/proposals/advanced-storyboard-coverage.v1.json")
    trace=load("design/proposals/audit-scenario-evidence.v1.json")
    named=load("design/proposals/core-state-evidence.v1.json")
    assert d["scopes"]=={"primarySurfaces":24,"supportSurfaces":16,
                         "supportVariantStates":34,"journeyGroups":23,
                         "p0Frames":82,"p1Frames":55,
                         "requiredNamedCoreStates":34,"auditComparisonScenarios":56}
    assert len(d["primarySurfaces"])==24 and len(d["supportSurfaces"])==16
    assert len(d["journeys"])==23 and len(d["explicitOwnerQuestions"])==8
    assert len(named["states"])==34
    assert trace["counts"]["totalRequired"]==56
    assert all(s["evidenceState"]=="PICTURED" for s in trace["scenarios"]),trace["counts"]
    all_sources={}
    def register(p):
        if p not in all_sources:
            all_sources[p]=valid_source(p)
    assert [s["surfaceId"] for s in d["primarySurfaces"]]==[s["id"] for s in core["screens"]]
    for proposal,actual in zip(d["primarySurfaces"],core["screens"]):
        assert proposal["ownerChoice"] is None and proposal["exactOwnerApprovalSHA256"] is None
        assert proposal["requiredSemanticElements"]==actual["required"]
        assert proposal["forbiddenSemanticElements"]==actual["forbidden"]
        assert proposal["candidateState"].startswith("UNAPPROVED_")
        register(proposal["reviewCandidatePath"])
        assert len(proposal["alternativesToDecide"])==len(set(proposal["alternativesToDecide"]))
        for path in proposal["alternativesToDecide"]:register(path)
    assert [s["surfaceId"] for s in d["supportSurfaces"]]==[s["id"] for s in support["screens"]]
    count=0
    for proposal,actual in zip(d["supportSurfaces"],support["screens"]):
        assert [v["stateId"] for v in proposal["variants"]]==actual["states"]
        for v in proposal["variants"]:
            count+=1
            assert v["ownerChoice"] is None and v["exactOwnerApprovalSHA256"] is None
            assert v["reviewCandidatePath"]==f"design/proposals/support-plates/{actual['id']}--{v['stateId']}.svg"
            register(v["reviewCandidatePath"])
    assert count==34
    joined=p0["groups"]+p1["groups"]
    count_p0=count_p1=0
    for g,s in zip(d["journeys"],joined):
        assert g["id"]==s["id"] and g["ownerChoice"] is None
        assert g["status"]=="CONNECTED_SOURCE_DRAFT"
        assert g["frames"]==[x["path"] for x in s["frames"]]
        for path in g["frames"]:register(path)
        if g["id"] in {x["id"] for x in p0["groups"]}:count_p0+=len(g["frames"])
        else:count_p1+=len(g["frames"])
    assert (count_p0,count_p1)==(82,55)
    for x in named["states"]:register(x["assetPath"])
    for x in trace["scenarios"]:
        for path in x["sourcePaths"]:register(path)
    assert len({s["reviewCandidatePath"] for s in d["primarySurfaces"]})==24
    assert len({q["id"] for q in d["explicitOwnerQuestions"]})==8
    summary={"status":"DRAFT_REVIEW_SNAPSHOT_NOT_APPROVED",
             "scope":{"primary":24,"support":34,"journeys":23,
                      "storyboardFrames":137,"named":34,"revisionScenarios":56},
             "referenceManifest":SRC,
             "uniqueReferencedSources":len(all_sources),
             "sources":[{"path":p,"sourceSHA256":sha,"ownerApproved":False}
                        for p,sha in sorted(all_sources.items())]}
    if args.emit:
        out=args.emit if args.emit.is_absolute() else ROOT/args.emit
        out.parent.mkdir(parents=True,exist_ok=True)
        out.write_text(json.dumps(summary,indent=2)+"\n",encoding="utf-8")
    print("PASS: complete 24 primary / 34 support state / 23 flow packages / "
          "137 storyboard frames / 34 named states / 56 pictured audit scenarios; "
          f"{len(all_sources)} unique source-hashed references, ZERO owner approvals")

if __name__=="__main__":main()
