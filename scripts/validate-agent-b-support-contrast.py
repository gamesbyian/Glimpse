#!/usr/bin/env python3
"""WCAG text contrast desk-audit for unapproved Glimpse support SVGs.

Uses declared foreground and immediately underlying painted solid SVG rectangle.
The flat rectangle inference is conservative, and all uncertain background/alpha
cases are explicitly excluded rather than called passes. No claim about actual
Android text colors, accessibility service output, spatial hitboxes or approval.
"""
from __future__ import annotations

import argparse
import json
import math
import re
import xml.etree.ElementTree as ET
from collections import Counter
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
NS = "{http://www.w3.org/2000/svg}"
COLORS = {
    "white": "#ffffff", "black": "#000000",
}
RGB = re.compile(r"^#[0-9a-fA-F]{6}$")
TR = re.compile(r"translate\(\s*(-?[0-9.]+)(?:[ ,]+(-?[0-9.]+))?\s*\)")
def hexcolor(value: str | None) -> str | None:
    if value is None:
        return None
    value = COLORS.get(value.lower(), value)
    if re.fullmatch(r"#[0-9a-fA-F]{3}", value):
        return "#" + "".join(c * 2 for c in value[1:]).lower()
    return value.lower() if RGB.fullmatch(value) else None

def luminance(h: str) -> float:
    rgb = [int(h[n:n+2], 16) / 255 for n in (1, 3, 5)]
    lin = [v / 12.92 if v <= 0.04045 else ((v + 0.055) / 1.055)**2.4 for v in rgb]
    return 0.2126*lin[0] + 0.7152*lin[1] + 0.0722*lin[2]

def contrast(fg: str, bg: str) -> float:
    a,b = luminance(fg), luminance(bg)
    return (max(a,b) + .05)/(min(a,b) + .05)

def paint(svg_text: str, name: str):
    root = ET.fromstring(svg_text)
    regions = []
    audit = []
    def visit(node, dx=0.0, dy=0.0):
        raw_trans = node.get("transform", "")
        if raw_trans:
            match=TR.fullmatch(raw_trans.strip())
            if not match:
                # Support art only uses translation in the actual in-app body.
                # Unsupported transforms are excluded from inference, never
                # silently treated as if they were untranslated.
                if node.tag in (NS + "g", NS + "text"):
                    audit.append({"state":name,"kind":"unknown-transform",
                                  "element":node.tag.split("}")[-1],
                                  "transform":raw_trans})
                return
            dx += float(match.group(1))
            dy += float(match.group(2) or 0)
        if node.tag == NS+"rect":
            try:
                r = {"x":float(node.get("x",0))+dx,
                     "y":float(node.get("y",0))+dy,
                     "w":float(node.get("width",0)),
                     "h":float(node.get("height",0)),
                     "color":hexcolor(node.get("fill")),
                     "opacity":float(node.get("opacity",1))*float(node.get("fill-opacity",1))}
                regions.append(r)
            except ValueError:
                audit.append({"state":name,"kind":"invalid-rect"})
        elif node.tag in (NS+"image", NS+"use"):
            # Bitmap pixels and image-backed fixture references are not
            # solid rect backgrounds. Mark occlusion unknown, not AA pass.
            try:
                regions.append({"x":float(node.get("x",0))+dx,
                                "y":float(node.get("y",0))+dy,
                                "w":float(node.get("width",0)),
                                "h":float(node.get("height",0)),
                                "color":None,"opacity":1.0})
            except ValueError:
                audit.append({"state":name,"kind":"image-region-unresolved"})
        elif node.tag == NS+"text":
            try:
                x,y = float(node.attrib["x"])+dx,float(node.attrib["y"])+dy
                size = float(node.get("font-size",14))
            except (ValueError,KeyError):
                return
            # Excludes launcher status, the bottom system/navigation region,
            # and the right-hand proposal annotation desk.
            if not (15 <= x < 416 and 190 <= y <= 810):
                return
            text = "".join(node.itertext()).strip()
            if not text: return
            fg = hexcolor(node.get("fill"))
            if fg is None:
                audit.append({"state":name,"kind":"unknown-text-fill","text":text})
                return
            # The baseline is in the middle of the glyph ascent, so choose
            # a point guaranteed inside the painted line (not its baseline).
            sample_y = y - size*0.48
            covering = [r for r in regions if r["x"] <= x <= r["x"]+r["w"]
                        and r["y"] <= sample_y <= r["y"]+r["h"]]
            if not covering:
                audit.append({"state":name,"kind":"unknown-background","text":text})
                return
            bg = covering[-1]
            if not bg["color"] or bg["opacity"] < 0.999:
                audit.append({"state":name,"kind":"non-solid-background",
                              "text":text,"color":bg["color"],"opacity":bg["opacity"]})
                return
            weight = node.get("font-weight","400")
            heavy = weight.lower() in ("bold","bolder") or (weight.isdigit() and int(weight)>=700)
            threshold = 3.0 if size >= 24 or (heavy and size >= 18.66) else 4.5
            cr = contrast(fg,bg["color"])
            audit.append({"state":name,"kind":"text-contrast",
                          "text":text,"fontSize":size,"weight":weight,
                          "foreground":fg,"background":bg["color"],
                          "ratio":round(cr,3),"minimum":threshold,
                          "passed":cr+1e-8>=threshold})
        for child in node:
            # SVG text child tspans are already counted above; avoid replay.
            if node.tag == NS+"text": break
            visit(child,dx,dy)
    visit(root)
    return audit

def main():
    p=argparse.ArgumentParser()
    p.add_argument("--report",type=Path)
    p.add_argument("--fail-low-contrast",action="store_true")
    p.add_argument("--include-storyboards",action="store_true")
    args=p.parse_args()
    inv=json.loads((ROOT/"design/proposals/support-coverage.v1.json").read_text())
    states=[f"{g['id']}--{s}" for g in inv["screens"] for s in g["states"]]
    assert len(states)==34 and len(set(states))==34
    data=[]
    for state in states:
        file=ROOT/"design/proposals/support-plates"/(state+".svg")
        data.extend(paint(file.read_text(encoding="utf8"),state))
    frame_count=0
    if args.include_storyboards:
        for manifest in ("storyboard-coverage.v1.json",
                         "advanced-storyboard-coverage.v1.json"):
            groups=json.loads((ROOT/"design/proposals"/manifest).read_text())
            for group in groups["groups"]:
                for frame in group["frames"]:
                    rel=frame["path"]
                    data.extend(paint((ROOT/rel).read_text(encoding="utf8"),rel))
                    frame_count+=1
        assert frame_count==137
    checks=[x for x in data if x["kind"]=="text-contrast"]
    failures=[x for x in checks if not x["passed"]]
    uncertainty=[x for x in data if x["kind"]!="text-contrast"]
    findings={
        "status":"UNAPPROVED_STATIC_DESK_AUDIT_NOT_NATIVE",
        "sourceStates":len(states),"sourceStoryboardFrames":frame_count,"measuredTextElements":len(checks),
        "belowAA":len(failures),"unknownCases":len(uncertainty),
        "groupedFailureStates":dict(sorted(Counter(x["state"] for x in failures).items())),
        "failures":failures,"uncertainties":uncertainty,
        "limitations":"Solid SVG source color against latest covering rectangle at glyph mid-ascent only; not pixel-composited, text wrapping, alpha, Android rendering, 320dp, TalkBack or human acceptance."
    }
    output=json.dumps(findings,indent=2,ensure_ascii=False)+"\n"
    if args.report:
        args.report.parent.mkdir(parents=True,exist_ok=True)
        args.report.write_text(output,encoding="utf8")
    print(json.dumps({k:v for k,v in findings.items() if k not in ("failures","uncertainties")},indent=2))
    for f in failures[:40]:print("LOW",f["state"],repr(f["text"]),f["ratio"],"<",f["minimum"],f["foreground"],f["background"])
    for f in uncertainty[:20]:print("UNKNOWN",f["state"],f["kind"],f.get("text",""))
    if args.fail_low_contrast and failures:
        raise SystemExit(1)
if __name__=="__main__":main()
