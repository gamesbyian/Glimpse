#!/usr/bin/env python3
"""Inventory visible support-page task pills missing explicit study hit/focus metadata.

Scans only existing source-editable SVG geometry. Reports potential controls,
not runtime Compose actions; no source edits without human optical review.
"""
from pathlib import Path
import json
import re
from xml.etree import ElementTree as ET
ROOT=Path(__file__).resolve().parents[1]
DIR=ROOT/"design/proposals/support-plates"
SVG="{http://www.w3.org/2000/svg}"
def number(v):
    try:return float(v)
    except (TypeError,ValueError):return -1
def candidates(path):
    root=ET.parse(path).getroot()
    phone=[]
    for parent in root.iter():
        if parent.tag not in (SVG+"rect",SVG+"text"):continue
        x=number(parent.get("x"));y=number(parent.get("y"))
        if x<0 or x>420 or y<160 or y>=820:continue
        phone.append(parent)
    found=[]
    for node in phone:
        if node.tag!=SVG+"rect":continue
        x=number(node.get("x")); y=number(node.get("y"))
        w=number(node.get("width")); h=number(node.get("height"))
        rx=number(node.get("rx"))
        if not (w>=82 and 42<=h<=62 and 17<=rx<=25):continue
        texts=[n for n in phone if n.tag==SVG+"text"
               and x<=number(n.get("x"))<=x+w
               and y+6<number(n.get("y"))<y+h+1]
        found.append({"x":x,"y":y,"w":w,"h":h,"rx":rx,
                      "declared":node.get("data-control"),
                      "labels":["".join(n.itertext()) for n in texts]})
    return found
def main():
    out=[]
    for path in sorted(DIR.glob("*.svg")):
        items=candidates(path)
        out.append({"name":path.stem,"total":len(items),
                    "undeclared":[i for i in items if not i["declared"]],
                    "declared":[i for i in items if i["declared"]]})
    print(json.dumps({"screens":len(out),"candidates":sum(x["total"] for x in out),
      "undeclared":sum(len(x["undeclared"]) for x in out),"inventory":out},
      ensure_ascii=False,indent=2))
if __name__=="__main__":main()
