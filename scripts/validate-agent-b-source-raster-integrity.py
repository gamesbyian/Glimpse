#!/usr/bin/env python3
"""Fail-closed source/render integrity check for Production B draft evidence.

Requires existing exact generated files. Checks bytes, not visual appearance,
device behavior, semantic TalkBack announcements, or owner approval.
Run after the B render scripts, on a clean source-equivalent checkout.
"""
from __future__ import annotations
import hashlib
import json
from pathlib import Path
from xml.etree import ElementTree as ET

ROOT=Path(__file__).resolve().parents[1]
DIR=ROOT/"design/reviews/support-agent-b-2026-10-24"
errors=[]

def digest(path:Path)->str:
    if not path.is_file():
        errors.append(f"missing {path.relative_to(ROOT)}")
        return ""
    return hashlib.sha256(path.read_bytes()).hexdigest()

def load(name:str):
    return json.loads((DIR/name).read_text(encoding="utf-8"))

support=load("source-ledger.json")
semantic=load("source-journey-a11y-audit.json")
pairs=load("journey-pair-source-ledger.json")
compact=load("compact-report-sections.v0.1.json")
stress=load("compact-fontscale-stress.json")
assert support["total"]==34 and len(support["sources"])==34
assert semantic["supportStates"]==34 and semantic["journeys"]==23
assert semantic["storyboardFrames"]==137 and semantic["illustratedScenarios"]==56
assert semantic["supportMinimumBodyFontBelow14"]==0 and semantic["errors"]==[]
assert pairs["pairs"]==23 and pairs["frames"]==46
assert len(pairs["groups"])==23 and len(pairs["contacts"])==6
assert compact["status"]=="STATIC_UNSELECTED_NOT_NATIVE_OR_OWNER_ACCEPTANCE"
assert stress["kind"]=="STATIC_RISK_SIMULATION_NOT_NATIVE_ANDROID"

all_paths=[p["source"] for p in support["sources"]]
assert len(set(all_paths))==34
audit=semantic["sourceSHA256"]
assert len(audit)==34
for row in support["sources"]:
    rel=row["source"]
    if not rel.startswith("design/proposals/support-plates/"):
        errors.append("unexpected support source "+rel)
        continue
    value=digest(ROOT/rel)
    if value!=row["sourceSHA256"]:errors.append(f"support source changed: {rel}")
    if audit.get(rel,{}).get("sha256")!=value:
        errors.append(f"semantic/source digest disagrees: {rel}")
    for flag in ("ownerApproved","nativeAndroidVerified","independentlyOpticallyReviewed"):
        if row[flag]:errors.append(f"unsupported evidence promotion: {rel} {flag}")
    root=ET.parse(ROOT/rel).getroot()
    if root.get("viewBox")!="0 0 1160 920": errors.append("changed support geometry "+rel)
for row in support["contacts"]:
    if digest(DIR/row["name"])!=row["SHA256"]:
        errors.append(f"support contact raster changed: {row['name']}")
if len(support["contacts"])!=6 or sum(x["sourceCount"] for x in support["contacts"])!=34:
    errors.append("wrong support contact distribution")

journey_unique=set()
for group in pairs["groups"]:
    if group["androidVerified"] or group["ownerApproved"]:
        errors.append("incorrect journey proof level "+group["group"])
    if len(group["frames"])!=2:
        errors.append("journey pair missing member "+group["group"])
    for frame in group["frames"]:
        rel=frame["path"]
        journey_unique.add(rel)
        if not (rel.startswith("design/proposals/storyboards/") or
                rel.startswith("design/proposals/advanced-storyboards/")):
            errors.append("unexpected journey source "+rel)
        if digest(ROOT/rel)!=frame["sha256"]:
            errors.append("journey source digest drift "+rel)
        if frame["crop"]!=[15,14,416,904] or frame["pixels"]!=[401,890]:
            errors.append("wrong journey crop "+rel)
if len(journey_unique)!=46:errors.append("journey source reuse in 23 representative pairs")
if sum(x["pairs"] for x in pairs["contacts"])!=23:
    errors.append("wrong journey contact distribution")
for row in pairs["contacts"]:
    if digest(DIR/row["name"])!=row["sha256"]:
        errors.append("journey contact raster changed "+row["name"])

if digest(ROOT/compact["source"])!=compact["sourceSHA256"]:
    errors.append("compact source hash drift")
compact_png=DIR/"compact-report-sections.v0.1.png"
if digest(compact_png)!=compact["previewSHA256"]:
    errors.append("compact PNG drift")
if len(compact["controls"])!=12: errors.append("12 compact controls expected")
for item in compact["controls"]:
    if min(item["sourceHitWidth"],item["sourceHitHeight"])<54:
        errors.append("too-small compact source study hit region: "+item["name"])
if digest(DIR/"compact-fontscale-stress.jpg")!=stress["imageSHA256"]:
    errors.append("font-scale stress raster drift")

# Every single P0/P1 frame, not only the representative 46, has a source-
# matched visual challenge crop. Byte-integrity does not certify appearance.
full=json.loads((DIR/"full-journey-corpus/full-journey-source-ledger.json").read_text(encoding="utf-8"))
if full["kind"]!="B_UNAPPROVED_FULL_STORYBOARD_OPTICAL_CHALLENGE":
    errors.append("full journey ledger wrong evidence level")
if full["countGroups"]!=23 or full["countUniquePhones"]!=137:
    errors.append("full journey coverage incomplete")
full_seen=set()
for group in full["groups"]:
    if (not group["generatedOnly"] or group["independentlyOpticallyInspected"] or
            group["nativeAndroidVerified"] or group["ownerApproved"]):
        errors.append("full journey evidence level overstated: "+group["group"])
    if group["count"]!=len(group["frames"]):
        errors.append("full journey group count mismatch: "+group["group"])
    if digest(DIR/"full-journey-corpus"/group["contact"])!=group["contactSHA256"]:
        errors.append("full journey JPEG SHA mismatch: "+group["group"])
    for f in group["frames"]:
        full_seen.add(f["source"])
        if digest(ROOT/f["source"])!=f["sha256"]:
            errors.append("full journey source drift: "+f["source"])
        if f["crop"]!=[15,14,416,904] or f["pixels"]!=[401,890]:
            errors.append("full journey crop geometry drift: "+f["source"])
if len(full_seen)!=137:
    errors.append("full journey source identities incomplete")
for f in journey_unique:
    if f not in full_seen:errors.append("paired frame missing from full journey corpus: "+f)


result={"scope":"B source/raster byte integrity only","supportSources":len(all_paths),
        "supportContacts":len(support["contacts"]),"journeyPairs":pairs["pairs"],
        "journeySources":len(journey_unique),"journeyContacts":len(pairs["contacts"]),
        "fullJourneyPhones":len(full_seen),"fullJourneyContacts":len(full["groups"]),
        "compactControls":len(compact["controls"]),"errors":errors,
        "evidence":"NOT native, human, owner, TalkBack, fontScale2 nor wallpaper OS result"}
print(json.dumps(result,indent=2))
if errors:raise SystemExit(1)
