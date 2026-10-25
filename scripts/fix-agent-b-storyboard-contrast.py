#!/usr/bin/env python3
"""Small, reproducible repairs to failing storyboard text contrast.

Only edits SVG <text> elements whose declared colors actually fail the
current source contrast audit. Leaves selected assets, scene geometry,
photo bytes, non-text accents, metadata, and right-side annotations intact.
No implication that a passing SVG guarantees native Android accessibility.
"""
from __future__ import annotations

import argparse
import html
import importlib.util
import json
import re
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
HERE=Path(__file__).resolve().parent
spec=importlib.util.spec_from_file_location(
    "agent_b_contrast", HERE/"validate-agent-b-support-contrast.py")
assert spec and spec.loader
audit=importlib.util.module_from_spec(spec)
spec.loader.exec_module(audit)

TEXT=re.compile(r"<text\b[^>]*>[^<]*</text>")
FILL=re.compile(r'\bfill="(#[0-9a-fA-F]{6})"')
def groups():
    for filename in ("storyboard-coverage.v1.json",
                     "advanced-storyboard-coverage.v1.json"):
        doc=json.loads((ROOT/"design/proposals"/filename).read_text())
        for g in doc["groups"]:
            for f in g["frames"]:
                yield f["path"]

def changed_source(path:Path):
    source=path.read_text(encoding="utf8")
    problems=[x for x in audit.paint(source,str(path.relative_to(ROOT)))
              if x["kind"]=="text-contrast" and not x["passed"]]
    if not problems:
        return source,0,[]
    bad=set((f["text"],f["foreground"]) for f in problems)
    phone,divider,notes=source.partition('<rect x="442"')
    if not divider:
        phone,divider,notes=source.partition('<rect x="440"')
    if not divider:
        raise AssertionError((path,"no phone/annotation partition"))
    changed=[0]
    def convert(match):
        text=match.group(0)
        raw=html.unescape(re.search(r">([^<]*)</text>",text).group(1))
        fill=FILL.search(text)
        if not fill or (raw,fill.group(1).lower()) not in bad:
            return text
        old=fill.group(1).lower()
        if old=="#667975":
            new="#445851"  # AA even against beige, lavender, soft error pink
        elif old=="#e8876c" and raw=="▼":
            new="#703B27"  # visible pointer glyph over cream
        else:
            raise AssertionError((path,raw,old,"unexpected low-contrast palette"))
        changed[0]+=1
        return text.replace(fill.group(0),f'fill="{new}"',1)
    updated=TEXT.sub(convert,phone)+divider+notes
    if changed[0]==0:
        raise AssertionError((path,"low contrast detected but no source edit"))
    failures=[x for x in audit.paint(updated,str(path.relative_to(ROOT)))
              if x["kind"]=="text-contrast" and not x["passed"]]
    return updated,changed[0],failures

def main():
    p=argparse.ArgumentParser()
    p.add_argument("--apply",action="store_true")
    args=p.parse_args()
    items=list(groups())
    assert len(items)==137 and len(set(items))==137
    modified=[]
    residual=[]
    for rel in items:
        path=ROOT/rel
        source=path.read_text(encoding="utf8")
        rewritten,count,left=changed_source(path)
        if left:residual.extend(left)
        if source!=rewritten:
            modified.append((rel,count))
            if args.apply:
                path.write_text(rewritten,encoding="utf8")
    print(json.dumps({"frames":len(items),"modifiedFrames":len(modified),
                      "rewrittenTextElements":sum(n for _,n in modified),
                      "stillBelowAA":len(residual),
                      "paths":modified},indent=2))
    if residual: raise SystemExit("unresolved storyboard contrast failures")
    if modified and not args.apply:
        raise SystemExit("Storyboard contrast sources still need --apply")
if __name__=="__main__":main()
