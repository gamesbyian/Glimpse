#!/usr/bin/env python3
"""Create source-identical photographic R01 mid-drag and tap-to-focus candidates.

This reuses original X05 physical wheel paths and R01 six real JPEG research
fixtures. Tap press and tap settle are separate states; no wallpaper apply.
"""
from __future__ import annotations
import argparse
import hashlib
import html
import json
import re
import runpy
from pathlib import Path
from xml.etree import ElementTree as ET

ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/"design/proposals/audit-revisions"
GEN=runpy.run_path(str(ROOT/"scripts/build-audit-r01-photo-variants.py"))
BY_P={"p10":"art2","p11":"art3","p12":"art1","p13":"art4","p14":"art5"}
INPUTS={
 "r01-photo-drag-36.svg":("04-drag-36.svg","drag-36","p12",
     ["fixture2","fixture3","fixture1","fixture4","fixture5","fixture1"]),
 "r01-photo-tap-p13-pressed.svg":("02-touch-down.svg","tap-p13-pressed","p12",
     ["fixture2","fixture3","fixture1","fixture4","fixture5","fixture1"]),
}
# Order must match original X05 frame's 6 USE nodes (one hero, five wedges).
PAPER_USE=("fixture1","fixture2","fixture3","fixture1","fixture2","fixture3")

def meta(source):
    m=re.search(r"<metadata>(.*?)</metadata>",source,re.S)
    assert m
    return m,json.loads(html.unescape(m.group(1)))

def update_meta(source,changes):
    m,d=meta(source)
    d.update(changes)
    source=source[:m.start(1)]+html.escape(json.dumps(d,separators=(",",":")),quote=True)+source[m.end(1):]
    return source

def base_with_fixture(original,state,selected,refs):
    original_svg=(GEN["P0"]/original).read_text(encoding="utf-8")
    uses=re.findall(r'<use href="#(fixture\d+)"',original_svg)
    assert len(uses)==6 and tuple(uses)==PAPER_USE,(original,uses)
    assert len(refs)==6
    # All five nearby IDs have unique physical JPEGs; sixth reference is the hero.
    body=GEN["generate"]("photo-rest-22",
         (original,state,selected,refs,BY_P))
    m,d=meta(body)
    assert d["selectedAssetId"]=="p12" and d["photoTotal"]==22
    assert d["lastConfirmedAppliedAssetId"]=="p04"
    assert d["fixtureArtworkByPhotoId"]==BY_P
    assert len(set(d["photoFixtureSHA256"].values()))==5
    return body

def make_drag():
    # 36° original pose is retained; no premature snap or selected ID change.
    s=base_with_fixture(*INPUTS["r01-photo-drag-36.svg"])
    s=update_meta(s,{"stateId":"photo-drag-36","auditInteraction":"wheel-drag",
      "pendingFocusAssetId":"p13","gestureDeltaDegrees":36,
      "positionCommitted":False,"selectedAssetId":"p12",
      "androidProof":"unverified","ownerApproved":False})
    return s

def make_pressed():
    s=base_with_fixture(*INPUTS["r01-photo-tap-p13-pressed.svg"])
    assert s.count(">Frame 2 / 8: Finger down</text>")==1
    s=s.replace(">Frame 2 / 8: Finger down</text>",
                ">Tap p13 / pointer pressed</text>")
    assert s.count(">Pointer captured; no selected-ID commit</text>")==1
    s=s.replace(">Pointer captured; no selected-ID commit</text>",
                ">p13 touch target; selection remains p12 until settled</text>")
    s=update_meta(s,{"stateId":"tap-p13-pressed",
       "auditInteraction":"tap-neighbour-aperture",
       "tapTargetAssetId":"p13","selectedAssetId":"p12",
       "positionCommitted":False,"motionPhase":"tap-pointer-down",
       "androidProof":"unverified","ownerApproved":False})
    return s

def make_settled():
    source=OUT/"r01-photo-snap-13.svg"
    s=source.read_text(encoding="utf-8")
    assert "Photo 13 of 22 · Preview only" in s
    assert "SELECTED p13" in s
    assert s.count(">Frame 5 / 8: Snapped to item 13</text>")==1
    s=s.replace(">Frame 5 / 8: Snapped to item 13</text>",
                ">Tap p13 / selection settled</text>")
    assert s.count(">Current reel angle relative to settled frame: 0°</text>")==1
    s=s.replace(">Current reel angle relative to settled frame: 0°</text>",
                ">Settled one notch; p13 selected by tap</text>")
    s=update_meta(s,{"stateId":"tap-p13-settled",
       "auditInteraction":"tap-neighbour-aperture",
       "tapTargetAssetId":"p13","selectedAssetId":"p13",
       "positionCommitted":True,"motionPhase":"tap-settled",
       "gestureDeltaDegrees":0,
       "sourceSettledSvgSHA256":hashlib.sha256(source.read_bytes()).hexdigest(),
       "lastConfirmedAppliedAssetId":"p04","androidProof":"unverified",
       "ownerApproved":False})
    return s

def main():
    ap=argparse.ArgumentParser()
    ap.add_argument("--write",action="store_true")
    args=ap.parse_args()
    outputs={
     "r01-photo-drag-36.svg":make_drag(),
     "r01-photo-tap-p13-pressed.svg":make_pressed(),
     "r01-photo-tap-p13-settled.svg":make_settled(),
    }
    for name,s in outputs.items():
        xml=ET.fromstring(s)
        assert xml.tag=="{http://www.w3.org/2000/svg}svg"
        assert xml.get("viewBox")=="0 0 1160 920"
        _,d=meta(s)
        assert d["lastConfirmedAppliedAssetId"]=="p04"
        assert d["appliedStatus"]=="unchanged"
        assert d["positionCommitted"]==(name=="r01-photo-tap-p13-settled.svg")
        if args.write:
            (OUT/name).write_text(s,encoding="utf-8")
        else:
            assert (OUT/name).read_text(encoding="utf-8")==s,name
        print(name,hashlib.sha256(s.encode()).hexdigest())
if __name__=="__main__":
    main()
