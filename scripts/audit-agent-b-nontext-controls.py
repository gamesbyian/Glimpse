#!/usr/bin/env python3
"""Conservative, separate source-only Glimpse non-text control contrast probe.

WCAG 1.4.11 can require 3:1 for visual information needed to identify a
control; an invisible hit region alone is NOT automatically a WCAG violation.
This collects risk candidates to be visually examined, without equating
decorative cards, transparent tap regions or OS widgets to a native failure.
The native focus stroke and TalkBack semantics cannot be inferred from SVGs.
"""
from __future__ import annotations
import argparse
import importlib.util
import json
import math
import re
import xml.etree.ElementTree as ET
from collections import Counter
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
HERE=Path(__file__).resolve().parent
spec=importlib.util.spec_from_file_location("b_text_contrast",HERE/"validate-agent-b-support-contrast.py")
assert spec and spec.loader
module=importlib.util.module_from_spec(spec)
spec.loader.exec_module(module)
NS="{http://www.w3.org/2000/svg}"
TRANSLATE=re.compile(r"translate\(\s*(-?[0-9.]+)(?:[ ,]+(-?[0-9.]+))?\s*\)")
def scan(raw:str,source:str):
    root=ET.fromstring(raw)
    painted=[]
    audit=[]
    def visit(node,dx:float=0,dy:float=0):
        transform=node.get("transform","")
        if transform:
            m=TRANSLATE.fullmatch(transform.strip())
            if not m:
                if any(x.get("data-control") for x in node.iter()):
                    audit.append({"source":source,"kind":"unresolved-transform","transform":transform})
                return
            dx+=float(m.group(1));dy+=float(m.group(2) or 0)
        if node.tag==NS+"rect":
            fill=module.hexcolor(node.get("fill"))
            stroke=module.hexcolor(node.get("stroke"))
            try:
                x=float(node.get("x",0))+dx;y=float(node.get("y",0))+dy
                w=float(node.get("width",0));h=float(node.get("height",0))
                alpha=float(node.get("opacity",1))*float(node.get("fill-opacity",1))
            except ValueError:
                audit.append({"source":source,"kind":"malformed-region"})
                return
            ctrl=node.get("data-control")
            if ctrl and x<416:
                cx=x+w/2;cy=y+h/2
                bg=next((r for r in reversed(painted) if r["x"]<=cx<=r["x"]+r["w"] and
                         r["y"]<=cy<=r["y"]+r["h"] and r["color"]),None)
                background=bg["color"] if bg else None
                fill_ratio=round(module.contrast(fill,background),3) if fill and background and alpha>=.999 else None
                stroke_ratio=round(module.contrast(stroke,background),3) if stroke and background else None
                # "weak distinguishability" is an inspection candidate, not a normative judgment.
                risks=(background is not None and max(fill_ratio or 0,stroke_ratio or 0)<3)
                audit.append({"source":source,"kind":"control",
                    "control":ctrl,"action":node.get("data-action-label"),
                    "focusOrder":node.get("data-focus-order"),"x":round(x,2),"y":round(y,2),
                    "width":w,"height":h,"fill":fill,"stroke":stroke,"background":background,
                    "fillRatio":fill_ratio,"strokeRatio":stroke_ratio,"riskBelow3":bool(risks),
                    "status":"STATIC_BORDER_FILL_RISK_NOT_NATIVE_PROOF"})
            if fill and alpha>=.999:
                painted.append({"x":x,"y":y,"w":w,"h":h,"color":fill})
        for child in node:
            visit(child,dx,dy)
    visit(root)
    return audit

def inventory():
    doc=json.loads((ROOT/"design/proposals/support-coverage.v1.json").read_text())
    return [f"design/proposals/support-plates/{g['id']}--{state}.svg"
            for g in doc["screens"] for state in g["states"]]

def self_test():
    raw='<svg xmlns="http://www.w3.org/2000/svg"><rect x="0" y="0" width="420" height="900" fill="#f6f0e6"/><g transform="translate(0,-24)"><rect x="20" y="100" width="100" height="54" fill="#1d5960" data-control="good" data-focus-order="1"/><rect x="20" y="200" width="100" height="54" fill="#fffcf6" stroke="#c8b7a1" data-control="weak" data-focus-order="2"/></g><rect x="0" y="400" width="420" height="200" fill="none" stroke="#fff"/></svg>'
    checks=[x for x in scan(raw,"synthetic") if x["kind"]=="control"]
    assert len(checks)==2
    assert checks[0]["y"]==76.0 and not checks[0]["riskBelow3"]
    assert checks[1]["y"]==176.0 and checks[1]["riskBelow3"]
    assert checks[0]["background"]=="#f6f0e6"==checks[1]["background"]
    print("PASS: source control fill contrast and outline-only transparency test")

def main():
    p=argparse.ArgumentParser()
    p.add_argument("--report",type=Path)
    p.add_argument("--self-test",action="store_true")
    args=p.parse_args()
    if args.self_test:
        self_test()
        return
    paths=inventory()
    assert len(paths)==34 and len(set(paths))==34
    items=[]
    for rel in paths:
        items.extend(scan((ROOT/rel).read_text(encoding="utf-8"),rel))
    controls=[x for x in items if x["kind"]=="control"]
    risk=[x for x in controls if x["riskBelow3"]]
    by_type=Counter(x["control"].split("-")[0] for x in risk)
    report={"status":"UNAPPROVED_SVG_CONTROL_CONTRAST_RISK_SCAN_NOT_WCAG_OR_NATIVE_VERDICT",
            "sourceSupportStates":len(paths),"controls":len(controls),
            "riskBelow3":len(risk),"riskTypes":dict(sorted(by_type.items())),
            "uncertainties":[x for x in items if x["kind"]!="control"],
            "risks":risk,
            "limitations":"Only the immediate solid source fill/stroke versus last flat underlying painted rect at center. Includes invisible nav hitboxes. Native semantics, focus ring, alpha compositing and whether visual boundary is needed for identification remain UNVERIFIED."}
    print(json.dumps({k:v for k,v in report.items() if k not in ("risks","uncertainties")},indent=2))
    if args.report:
        args.report.parent.mkdir(parents=True,exist_ok=True)
        args.report.write_text(json.dumps(report,indent=2)+"\n",encoding="utf-8")
if __name__=="__main__":main()
