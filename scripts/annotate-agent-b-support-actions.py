#!/usr/bin/env python3
"""Add reproducible source-only hit/focus metadata to visibly painted support CTAs.

Drawn rectangles are not runtime Android clickable semantics. Overlay-covered
legacy buttons on the destructive confirmation are deliberately never marked.
"""
from __future__ import annotations
import argparse
import json
from pathlib import Path
import re
from xml.etree import ElementTree as ET

ROOT=Path(__file__).resolve().parents[1]
DIR=ROOT/"design/proposals/support-plates"
COVERAGE=ROOT/"design/proposals/support-coverage.v1.json"
RECT=re.compile(r'<rect\b[^>]*>')
TEXT=re.compile(r'<text\b[^>]*>[^<]*</text>')
ATTR=re.compile(r'([\w-]+)="([^"]*)"')
def attr(node):
    return dict(ATTR.findall(node))
# Full-height task cards with explicit in-card navigation/action captions.
# These are clickable *design proposals*, not runtime Android hit targets.
CARD_ACTIONS={
    "contact-feedback--available":[(222,"Create a report"),(338,"Write feedback"),(454,"Open issues")],
    "contact-feedback--no-handler":[(222,"Create a report"),(338,"Write feedback"),(454,"Open issues")],
    "help--topics":[(221,"Read import guide"),(336,"Learn the reel"),(451,"What changes?"),(566,"Fix a problem")],
    "storage-management--overview":[(223,"View collections"),(336,"Clear cache"),(450,"Review removal")],
    "updates--play-eligible":[(223,"View build info"),(338,"Learn more")],
}
def annotate_task_cards(source,name):
    actions=CARD_ACTIONS.get(name,[])
    if not actions:return source
    phone,divider,notes=source.partition('<rect x="440"')
    assert divider,name
    existing=set(re.findall(r'data-control="([^"]+)"',phone))
    for idx,(y,label) in enumerate(actions,2):
        key="card-"+re.sub(r"[^a-z0-9]+","-",label.lower()).strip("-")
        # The first use installs metadata; all subsequent runs are stable.
        if key in existing:continue
        assert f">{label}</text>" in phone,(name,label)
        pattern=(f'<rect x="40" y="{y}" width="348" height="100" rx="13"')
        if phone.count(pattern)!=1:raise AssertionError((name,y,"task card shape changed"))
        replacement=(f'<rect data-control="{key}" data-focus-order="{idx}"'
                     f' data-hit-width="348" data-hit-height="100"'
                     f' data-action-label="{label}" x="40" y="{y}"'
                     f' width="348" height="100" rx="13"')
        phone=phone.replace(pattern,replacement,1)
        existing.add(key)
    return phone+divider+notes

# Entire row represents the accessible choice, not the tiny 45x25 knob.
# Not-implemented notifications intentionally get no focusable switches.
TOGGLE_ACTIONS={
    "notification-preferences--available":[(346,"Important rotation problems"),
                                           (438,"Source or import failures")],
    "beta-feedback--invitation":[(340,"Attach a screenshot"),(426,"Attach diagnostics")],
    "beta-feedback--share-preview":[(340,"Attach a screenshot"),(426,"Attach diagnostics")],
    "report-problem--with-diagnostics":[(502,"Include diagnostics")],
}
def annotate_toggle_rows(source,name):
    items=TOGGLE_ACTIONS.get(name,[])
    if not items:return source
    phone,divider,notes=source.partition('<rect x="440"')
    assert divider,name
    for index,(y,label) in enumerate(items,2):
        key="toggle-"+re.sub(r"[^a-z0-9]+","-",label.lower()).strip("-")
        if f'data-control="{key}"' in phone:continue
        if f">{label}</text>" not in phone:raise AssertionError((name,label,"row label missing"))
        needle=f'<rect x="40" y="{y}" width="348" height="69" rx="12"'
        if phone.count(needle)!=1:raise AssertionError((name,y,"expected full-height switch row missing"))
        repl=(f'<rect data-control="{key}" data-focus-order="{index}"'
              f' data-hit-width="348" data-hit-height="69"'
              f' data-action-label="{label}" x="40" y="{y}"'
              f' width="348" height="69" rx="12"')
        phone=phone.replace(needle,repl,1)
    return phone+divider+notes

def annotated(raw,name):
    if name=="storage-management--delete-confirm":
        # A blocking scrim replaces prior page actions. Its own Cancel and
        # Delete already have focus IDs in the separate modal drawing.
        return raw
    phone,divider,notes=raw.partition('<rect x="440"')
    assert divider,name
    texts=[]
    for m in TEXT.finditer(phone):
        a=attr(m.group(0))
        if "x" not in a or "y" not in a:continue
        label=re.search(r'>([^<]*)</text>',m.group(0)).group(1)
        texts.append((float(a["x"]),float(a["y"]),label))
    candidates=[]
    for m in RECT.finditer(phone):
        a=attr(m.group(0))
        if "data-control" in a:continue
        try:
            x,y,w,h,rx=[float(a[k]) for k in ("x","y","width","height","rx")]
        except (ValueError,KeyError):continue
        if not (0<=x<=420 and 190<=y<810 and w>=82 and 42<=h<=62 and 17<=rx<=25):
            continue
        labels=[t for tx,ty,t in texts if x<=tx<=x+w and y+6<ty<y+h+1]
        if len(labels)!=1:raise AssertionError((name,"ambiguous painted task",a,labels))
        candidates.append((m,labels[0],tuple(map(round,(x,y,w,h)))))
    focus=set(re.findall(r'data-focus-order="(\d+)"',phone))
    used=set(re.findall(r'data-control="([^"]+)"',phone))
    changes=[]
    for i,(match,label,(x,y,w,h)) in enumerate(sorted(candidates,key=lambda c:(c[2][1],c[2][0]))):
        key="task-"+re.sub(r"[^a-z0-9]+","-",label.lower()).strip("-")
        if key in used:key+=f"-{y}"
        assert key not in used and str(10+i) not in focus,(name,key)
        used.add(key);focus.add(str(10+i))
        extra=(f' data-control="{key}" data-focus-order="{10+i}"'
               f' data-hit-width="{w}" data-hit-height="{h}"'
               f' data-action-label="{label}"')
        old=match.group(0)
        assert old.endswith("/>")
        changes.append((match.start(),match.end(),old[:-2]+extra+"/>"))
    for start,end,new in sorted(changes,reverse=True):
        phone=phone[:start]+new+phone[end:]
    whole=annotate_toggle_rows(annotate_task_cards(phone+divider+notes,name),name)
    ET.fromstring(whole)
    return whole
def main():
    parser=argparse.ArgumentParser()
    parser.add_argument("--apply",action="store_true")
    args=parser.parse_args()
    coverage=json.loads(COVERAGE.read_text(encoding="utf-8"))
    names=[f"{x['id']}--{state}" for x in coverage["screens"] for state in x["states"]]
    assert len(names)==34
    changed=[]
    for name in names:
        path=DIR/(name+".svg")
        raw=path.read_text(encoding="utf-8")
        updated=annotated(raw,name)
        if raw!=updated:
            changed.append(name)
            if args.apply:path.write_text(updated,encoding="utf-8")
    print(f"{'Updated' if args.apply else 'Pending'} CTA annotations for {len(changed)}/34 source plates")
    if changed and not args.apply:raise SystemExit("Source CTA annotations stale; rerun --apply")
if __name__=="__main__":main()
