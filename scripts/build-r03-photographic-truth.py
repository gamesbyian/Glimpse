#!/usr/bin/env python3
"""Repair R03 p12 photographic source identity across preview and UNKNOWN states.

Deterministically derives from checked-in originals and the exact p12 R01
fixture; no inferred device screenshot, OS acknowledgement, or owner approval.
"""
from __future__ import annotations
import argparse,base64,hashlib,html,json,re
from pathlib import Path
from xml.etree import ElementTree as ET
ROOT=Path(__file__).resolve().parents[1]
DIR=ROOT/"design/proposals/audit-revisions"
REEL=DIR/"r01-photo-rest-22.svg"
SOURCE_NAMES=("r03-preview-not-applied.svg","r03-unknown-after-request.svg")
# Capture only p12 vector art immediately under the hero; keep the real selected
# badge and status/result area unchanged.
PHOTO_PATTERN=re.compile(
    r'<rect x="45" y="164" width="342" height="316" rx="14" fill="#17232F"/>'
    r'(?:<path d="[^"]+" stroke="[^"]+" stroke-width="18" fill="none" stroke-linecap="round"/>){5}')

def fixture():
    src=REEL.read_text(encoding="utf-8")
    m=re.search(r'<image id="fixture1"[^>]+href="(data:image/jpeg;base64,[^"]+)"',src)
    assert m
    uri=m.group(1);raw=base64.b64decode(uri.partition(",")[2])
    assert raw[:3]==bytes.fromhex("ffd8ff")
    return uri,hashlib.sha256(raw).hexdigest()

def construct(name):
    original=(DIR/name).read_text(encoding="utf-8")
    photo,photo_sha=fixture()
    art='''<rect x="45" y="164" width="342" height="316" rx="14" fill="#17232F"/>
<defs><clipPath id="r03-p12-photo-frame"><rect x="48" y="167" width="336" height="310" rx="12"/></clipPath></defs>
<g clip-path="url(#r03-p12-photo-frame)"><image x="48" y="167" width="336" height="310"
preserveAspectRatio="xMidYMid slice" href="'''+photo+'''"/></g>'''
    draft,count=PHOTO_PATTERN.subn(lambda _:art,original)
    assert count==1,(name,count)
    m=re.search(r'<metadata>(.*?)</metadata>',draft,re.S);assert m
    j=json.loads(html.unescape(m.group(1)))
    assert j["selectedAssetId"]=="p12"
    assert j["lastConfirmedAppliedAssetId"]=="p04"
    assert j["appliedStatus"] in ("unknown","unchanged")
    j.update({"sourceAssetId":"p12","selectedPhotoJPEGFixtureSHA256":photo_sha,
              "selectedPhotoFixtureSource":"r01-photo-rest-22.svg#fixture1",
              "r03PhotoIdentityRevision":1,"opticalStatus":"re-render-required",
              "androidProof":"unverified","ownerApproved":False})
    draft=draft[:m.start(1)]+html.escape(json.dumps(j,separators=(",",":")),quote=True)+draft[m.end(1):]
    root=ElementTree.fromstring(draft)
    assert root.get("viewBox")=="0 0 1160 920"
    assert "SELECTED · p12" in draft and draft.count('id="r03-p12-photo-frame"')==1
    return draft

def main():
    ap=argparse.ArgumentParser()
    ap.add_argument("--write",action="store_true")
    args=ap.parse_args()
    for name in SOURCE_NAMES:
        content=construct(name)
        path=DIR/name
        if args.write:path.write_text(content,encoding="utf-8")
        else:assert path.read_text(encoding="utf-8")==content,name
        print(name,hashlib.sha256(content.encode()).hexdigest())
if __name__=="__main__":
    main()
