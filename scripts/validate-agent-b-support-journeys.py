#!/usr/bin/env python3
"""Agent B source-level support, geometry and storyboard transition audit.

Drawings cannot prove 320dp/200% Android layout, TalkBack focus, navigation,
recipient delivery, image rights, provider persistence or human comprehension.
"""
from __future__ import annotations
import hashlib
import json
from pathlib import Path
import re
from xml.etree import ElementTree as ET

ROOT=Path(__file__).resolve().parents[1]
NS="{http://www.w3.org/2000/svg}"
def load(p):
    return json.loads((ROOT/p).read_text(encoding="utf8"))
support=load("design/proposals/support-coverage.v1.json")
p0=load("design/proposals/storyboard-coverage.v1.json")
p1=load("design/proposals/advanced-storyboard-coverage.v1.json")
scenarios=load("design/proposals/audit-scenario-evidence.v1.json")
def rel_phone(source):
    return source.split('<rect x="440"',1)[0]
errors=[]
sources={}
for family in support["screens"]:
    for state in family["states"]:
        name=f"{family['id']}--{state}"
        p=Path(f"design/proposals/support-plates/{name}.svg")
        raw=(ROOT/p).read_text(encoding="utf8")
        root=ET.fromstring(raw)
        if root.get("viewBox")!="0 0 1160 920": errors.append(f"{p}: wrong viewBox")
        if '<g id="support-body" transform="translate(0,-24)">' not in raw:
            errors.append(f"{p}: stale non-app decorative banner or missing layout")
        phone=rel_phone(raw)
        assert 'SCREEN PROPOSAL • NOT USER-APPROVED' in phone
        controls={r.get("data-control"):r for r in root.iter(NS+"rect") if r.get("data-control")}
        is_modal=name=="storage-management--delete-confirm"
        required={"cancel-delete","confirm-delete"} if is_modal else {"back","nav-home","nav-discover","nav-collections","nav-automation"}
        if not required<=controls.keys():errors.append(f"{p}: absent required focus/hit regions {required-controls.keys()}")
        if is_modal and any(x.startswith("nav-") for x in controls):
            errors.append(f"{p}: modal background navigation still actionable")
        for key,shape in controls.items():
            width=float(shape.get("data-hit-width","0"))
            height=float(shape.get("data-hit-height","0"))
            if width<44 or height<44: errors.append(f"{p}:{key} sub-44 design target {width}x{height}")
            if not shape.get("data-focus-order"):errors.append(f"{p}:{key} absent focus proposal")
        if name=="beta-feedback--share-preview":
            for text in ("Review outgoing feedback","Screenshot: none selected","Diagnostics: off","Message: draft, not yet sent","Open Android Sharesheet"):
                if text not in phone:errors.append(f"{p}: lost outbound consent distinction {text}")
        if name=="storage-management--delete-confirm":
            for text in ("delete-modal-scrim","Example count; Android must calculate it.","Original provider photos stay intact.","Review active rotation first"):
                if text not in phone:errors.append(f"{p}: missing destructive preflight {text}")
        if name=="report-problem--with-diagnostics" and "Diagnostics selected, not sent" not in phone:
            errors.append(f"{p}: diagnostic selection indistinguishable from sent result")
        if name=="report-problem--draft" and "Nothing sent; diagnostics off" not in phone:
            errors.append(f"{p}: draft data-transmission truth")
        sources[str(p)]={"sha256":hashlib.sha256(raw.encode("utf8")).hexdigest(),
                         "actions":sorted(controls),"modal":is_modal,
                         "minVisibleFont":min((int(x) for x in re.findall(r'font-size="(\d+)"',phone)),default=None)}
assert len(sources)==34
groups=p0["groups"]+p1["groups"]
if len(p0["groups"])!=12 or len(p1["groups"])!=11 or len(groups)!=23:
    errors.append("Unexpected 23-journey inventory")
frames=[]
for group in groups:
    items=group["frames"]
    if not items:errors.append(f"{group['id']}: empty journey")
    for index,f in enumerate(items,start=1):
        if f.get("ordinal")!=index:errors.append(f"{group['id']}: broken storyboard ordinal {f.get('id')}")
        path=f["path"]
        if not (ROOT/path).is_file():errors.append(f"{group['id']}: missing frame {path}")
        else:
            root=ET.parse(ROOT/path).getroot()
            if root.get("viewBox")!="0 0 1160 920":errors.append(f"{group['id']}: invalid phone frame {path}")
        frames.append(path)
if len(frames)!=137 or len(set(frames))!=137: errors.append(f"Expected 82+55 unique frames; got {len(frames)}")
ss=scenarios["scenarios"]
if len(ss)!=56:errors.append("Unexpected audit scenario count")
for sc in ss:
    if sc.get("evidenceState")!="PICTURED":errors.append(f"Scenario not pictured: {sc['scenario']}")
    for path in sc.get("sourcePaths",[]):
        if not (ROOT/path).is_file(): errors.append(f"Scenario missing source {sc['scenario']} -> {path}")
# Risk probe: don't conflate static SVG preview with Android's actual dynamic font scaling.
small=[(k,v["minVisibleFont"]) for k,v in sources.items() if v["minVisibleFont"] is not None and v["minVisibleFont"]<14]
print(json.dumps({"scope":"Agent B unapproved source-only audit",
 "supportStates":len(sources),"journeys":len(groups),"storyboardFrames":len(frames),"illustratedScenarios":len(ss),
 "supportMinimumFontBelow14":len(small),
 "fontScaleVerdict":"320dp/200% real Android reflow unverified: source fonts and targets are only static reference geometry",
 "navigationVerdict":"data-control rectangles are visual geometry proposals, not native semantics/focus tests",
 "sourceSHA256":sources, "errors":errors},indent=2))
if errors:raise SystemExit(1)
