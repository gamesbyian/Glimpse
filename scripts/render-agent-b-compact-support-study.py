#!/usr/bin/env python3
"""Unselected 320dp/200%-type support-form + Sections navigation visual study.

Complements Production A's provisional compact Sections sheet. This source
renders two 320-unit phone canvases (top-level study 1080x800). Drawn hit boxes
and conceptual type do NOT prove Android dp, sp, TalkBack, scroll, focus or
human acceptance. Never add this to approved or the canonical selection.
"""
from __future__ import annotations

import hashlib
import json
from pathlib import Path
from xml.etree import ElementTree as ET

ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/"design/reviews/support-agent-b-2026-10-24"
COLORS={"paper":"#F6F0E6","surface":"#FFFCF6","ink":"#153F46",
        "accent":"#1D5960","secondary":"#566861","border":"#C8B7A1",
        "warning":"#A5482D","selected":"#E4ECE6"}
parts=[]
def rect(x,y,w,h,fill,rx=0,stroke="none",**attrs):
    meta="".join(f' data-{k.replace("_","-")}="{v}"' for k,v in attrs.items())
    parts.append(f'<rect x="{x}" y="{y}" width="{w}" height="{h}" rx="{rx}" fill="{fill}" stroke="{stroke}"{meta}/>')
def line(x1,y1,x2,y2,color):
    parts.append(f'<path d="M{x1} {y1} L{x2} {y2}" fill="none" stroke="{color}"/>')
def t(x,y,s,size=20,color=None,weight=500,anchor="start"):
    from html import escape
    parts.append(f'<text x="{x}" y="{y}" font-family="Arial,Helvetica,sans-serif" font-size="{size}" font-weight="{weight}" fill="{color or COLORS["ink"]}" text-anchor="{anchor}">{escape(s)}</text>')
def phone(x):
    rect(x,20,320,760,COLORS["paper"],24,COLORS["border"])
    t(x+21,57,"9:41",21,weight=700)
    t(x+290,57,"● ▰",17,COLORS["secondary"],anchor="end")
    line(x+18,78,x+303,78,COLORS["border"])
    rect(x+16,90,54,54,COLORS["surface"],18,COLORS["border"],control="back",hit_width=54,hit_height=54)
    t(x+43,128,"‹",42,COLORS["accent"],700,"middle")
    t(x+80,127,"Report a",36,weight=700)
    t(x+80,167,"problem",36,weight=700)

# First phone: scrollable form, sticky actions. Native UI must actually reflow.
phone(18)
t(43,202,"Nothing has been sent.",19,COLORS["secondary"])
t(37,239,"What happened?",27,weight=700)
rect(34,252,288,94,COLORS["surface"],14,COLORS["border"],control="issue-field",hit_width=288,hit_height=94)
t(49,288,"Describe issue…",21,COLORS["secondary"])
t(49,324,"No photo attached",17,COLORS["secondary"])
t(37,390,"Expected result",27,weight=700)
rect(34,405,288,91,COLORS["surface"],14,COLORS["border"],control="expected-field",hit_width=288,hit_height=91)
t(49,440,"What should happen?",20,COLORS["secondary"])
t(49,477,"Optional",17,COLORS["secondary"])
rect(34,516,288,85,COLORS["surface"],14,COLORS["border"],control="diagnostics-off",hit_width=288,hit_height=85)
t(49,552,"Diagnostics",24,weight=700)
t(49,583,"OFF · preview before sharing",17,COLORS["secondary"])
rect(254,530,54,30,COLORS["border"],15)
rect(259,533,24,24,COLORS["surface"],12)
t(42,638,"Scroll for more options ↓",18,COLORS["secondary"])
rect(30,649,296,54,COLORS["accent"],18,control="review-report",hit_width=296,hit_height=54)
t(178,685,"Review report",24,"#FFFFFF",700,"middle")
rect(30,710,296,55,COLORS["surface"],18,COLORS["accent"],control="open-sections",hit_width=296,hit_height=55)
t(178,748,"Sections ▾",24,COLORS["accent"],700,"middle")

# Second phone: same form, scoped modal sheet with four roomy routes.
phone(370)
t(395,202,"Nothing has been sent.",19,COLORS["secondary"])
rect(370,183,320,597,COLORS["ink"],0)
parts[-1]=parts[-1].replace('stroke="none"/>','stroke="none" opacity="0.60"/>')
rect(381,213,298,552,COLORS["surface"],19,COLORS["border"])
t(398,257,"Sections",34,weight=700)
t(398,290,"Choose a destination",20,COLORS["secondary"])
names=[("H","Home"),("D","Discover"),("C","Collections"),("A","Automation")]
for i,(letter,name) in enumerate(names):
    y=307+i*96
    active=(name=="Automation")
    rect(395,y,270,86,COLORS["selected"] if active else COLORS["surface"],14,
         COLORS["accent"] if active else COLORS["border"],control="section_"+name.lower(),hit_width=270,hit_height=86)
    rect(408,y+22,43,43,COLORS["accent"] if active else "#DAEDE1",13)
    t(429,y+52,letter,23,"#FFFFFF" if active else COLORS["accent"],700,"middle")
    t(467,y+54,name,27,weight=700 if active else 600)
rect(395,702,270,54,COLORS["accent"],17,control="close-sections",hit_width=270,hit_height=54)
t(530,737,"Close sections",23,"#FFFFFF",700,"middle")
# Annotated source-only proof boundary.
rect(718,20,345,760,COLORS["surface"],18,COLORS["border"])
t(738,65,"COMPACT SUPPORT STUDY",24,COLORS["accent"],700)
t(738,104,"320dp • 200% type",27,weight=700)
line(739,132,1040,132,COLORS["border"])
for y,txt,size,weight,color in [
 (177,"Paired with A's Sections",20,700,COLORS["ink"]),
 (205,"proposal, not a final route.",18,500,COLORS["secondary"]),
 (258,"Left: report form",22,700,COLORS["ink"]),
 (293,"Larger labels, real rows,",19,500,COLORS["ink"]),
 (322,"scroll-intent body, bottom",19,500,COLORS["ink"]),
 (350,"Review and Sections actions.",19,500,COLORS["ink"]),
 (404,"Right: sections sheet",22,700,COLORS["ink"]),
 (437,"Four routes, each 86px tall.",19,500,COLORS["ink"]),
 (469,"Focus should be trapped in",19,500,COLORS["ink"]),
 (500,"the sheet and restored to",19,500,COLORS["ink"]),
 (529,"Sections on close/cancel.",19,500,COLORS["ink"]),
 (601,"NO NATIVE ACCEPTANCE",21,700,COLORS["warning"]),
 (633,"No proof of TalkBack, real",17,500,COLORS["secondary"]),
 (658,"fontScale=2, dp hitboxes,",17,500,COLORS["secondary"]),
 (683,"insets, or runtime scrolling.",17,500,COLORS["secondary"]),
 (749,"UNSELECTED · UNAPPROVED",19,700,COLORS["warning"])
]:t(738,y,txt,size,color,weight)
svg='<svg xmlns="http://www.w3.org/2000/svg" width="1080" height="800" viewBox="0 0 1080 800" role="img" aria-label="Unselected 320dp large-type report form and Sections sheet study"><title>Agent B compact support and a11y study, not native acceptance</title><desc>Two 320 unit visual proposal phones and explicit native proof limits.</desc><metadata>{"schemaVersion":1,"status":"UNSELECTED_NOT_OWNER_APPROVED","nativeVerified":false,"humanApproved":false,"target":"320dp/fontScale2-static-composition","source":"Agent A draft Sections grammar"}</metadata>'+''.join(parts)+'</svg>\n'
root=ET.fromstring(svg)
controls=[n for n in root.iter() if n.attrib.get("data-control")]
assert len(controls)==9
assert all(min(float(n.attrib["data-hit-width"]),float(n.attrib["data-hit-height"]))>=54 for n in controls)
OUT.mkdir(parents=True,exist_ok=True)
src=OUT/"compact-report-sections.v0.1.svg"
src.write_text(svg)
from cairosvg import svg2png
png=OUT/"compact-report-sections.v0.1.png"
png.write_bytes(svg2png(bytestring=svg.encode(),output_width=1080,output_height=800))
report={"schemaVersion":1,"status":"STATIC_UNSELECTED_NOT_NATIVE_OR_OWNER_ACCEPTANCE",
        "source":str(src.relative_to(ROOT)),
        "sourceSHA256":hashlib.sha256(src.read_bytes()).hexdigest(),
        "previewSHA256":hashlib.sha256(png.read_bytes()).hexdigest(),
        "viewportStudyDp":[320,760],"controls":[{"name":n.attrib["data-control"],
        "sourceHitWidth":int(n.attrib["data-hit-width"]),"sourceHitHeight":int(n.attrib["data-hit-height"])} for n in controls],
        "requiresNative":["fontScale2 dynamic reflow","48dp hitboxes","scroll + insets",
        "TalkBack focus order and return","OS sharing focus return","human approval"]}
(OUT/"compact-report-sections.v0.1.json").write_text(json.dumps(report,indent=2)+"\n")
print("PASS: 2-source-editable 320dp conceptual panels, 9 >=54px study targets; native/owner approval false")
