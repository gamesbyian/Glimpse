#!/usr/bin/env python3
"""Produce the primary interactive Reel *candidate* with an explicit wallpaper action.

Keeps R01's photographed finite five-wedge wheel, hero, stable p12, source JPEG
bytes and fixed pointer. The action opens destination choice and does NOT
apply wallpaper until a separate confirmed command. This is unapproved SVG art.
"""
from __future__ import annotations
import argparse
import hashlib
import html
import json
import re
from pathlib import Path
import xml.etree.ElementTree as ET

ROOT=Path(__file__).resolve().parents[1]
SRC=ROOT/"design/proposals/audit-revisions/r01-photo-rest-22.svg"
DEST=ROOT/"design/proposals/audit-revisions/r01-photo-primary-wallpaper-action.svg"
NS="{http://www.w3.org/2000/svg}"

OLD=(
'<text x="42" y="433" font-family="Arial,Helvetica,sans-serif" font-size="15" font-weight="700" fill="#153F46">Browse by rotating the paper reel</text>\n'
'<text x="306" y="433" font-family="Arial,Helvetica,sans-serif" font-size="14" font-weight="600" fill="#1D5960">Grid</text>'
'<path d="M345 433 L359 419 M348 419 H359 V430" fill="none" stroke="#1D5960" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"/>'
)
NEW=(
'<rect x="40" y="407" width="122" height="47" rx="12" fill="#FFFCF6" stroke="#1D5960" stroke-width="2"/>'
'<text x="64" y="436" font-family="Arial,Helvetica,sans-serif" font-size="15" font-weight="700" fill="#1D5960">View all</text>'
'<rect x="172" y="407" width="219" height="47" rx="12" fill="#1D5960" stroke="#1D5960"/>'
'<text x="191" y="436" font-family="Arial,Helvetica,sans-serif" font-size="15" font-weight="700" fill="#FFFFFF">Set wallpaper…</text>'
)
def generate():
    s=SRC.read_text(encoding="utf-8")
    assert s.count(OLD)==1, "R01 action row changed upstream: reconcile instead of guessing"
    s=s.replace(OLD,NEW,1)
    pointer='<path d="M201 452 L229 452 L215 472 Z" fill="#E8876C"/>'
    lower='<path d="M201 461 L229 461 L215 482 Z" fill="#E8876C"/>'
    assert s.count(pointer)==1,"the existing reel pointer changed upstream"
    s=s.replace(pointer,lower,1)
    assert s.count("Previous photo")==1 and s.count("Next photo")==1
    assert s.count('<g clip-path="url(#c')==5
    assert len(re.findall(r'<use href="#fixture[1-6]"',s))==6
    m=re.search(r"<metadata>(.*?)</metadata>",s,re.S)
    assert m
    metadata=json.loads(html.unescape(m.group(1)))
    assert metadata["selectedAssetId"]=="p12"
    assert metadata["photoTotal"]==22
    assert metadata["appliedStatus"]=="unchanged"
    metadata.update({
        "stateId":"primary-reel-with-wallpaper-action",
        "designCategory":"AUDIT_REVISION_NOT_OWNER_APPROVED",
        "sourceVariant":"r01-photo-rest-22.svg",
        "originalSourceSHA256":hashlib.sha256(SRC.read_bytes()).hexdigest(),
        "visibleButtons":["View all","Set wallpaper…","Previous photo","Next photo"],
        "primaryActionSemantic":"Open destination chooser; do not invoke WallpaperManager until later explicit confirmation",
        "wallpaperTarget":None,
        "requestedAssetId":None,
        "lastConfirmedAppliedAssetId":"p04",
        "nativeAccessibilityMeasured":False,
        "sourceOpticalStatus":"REQUIRES_EXACT_PNG_REVIEW",
        "ownerApproved":False,
    })
    s=s[:m.start(1)]+html.escape(json.dumps(metadata,separators=(",",":")),quote=True)+s[m.end(1):]
    # The original explanatory pane is retained. Correct command explanation.
    old='Browsing never applies wallpaper or consumes rotation state.'
    new='Browse keeps p12 selected; Set wallpaper opens a target chooser.'
    assert s.count(old)==1
    s=s.replace(old,new)
    s=s.replace("R01 · PHOTOGRAPHIC REEL COMPARISON",
                "R01 · PRIMARY PHOTO REEL / EXPLICIT APPLY ENTRY")
    ET.fromstring(s)
    # Enforce 47px tall visible command affordances above the wheel's clipped start.
    assert 'x="172" y="407" width="219" height="47"' in s
    assert 'x="40" y="407" width="122" height="47"' in s
    assert 'Set wallpaper…' in s and 'Photo 12 of 22 · Preview only' in s
    return s

def main():
    ap=argparse.ArgumentParser()
    ap.add_argument("--write",action="store_true")
    args=ap.parse_args()
    s=generate()
    if args.write:
        DEST.write_text(s,encoding="utf-8")
    else:
        assert DEST.read_text(encoding="utf-8")==s,"Committed source differs from deterministic recipe"
    print(json.dumps({"source":str(DEST.relative_to(ROOT)),
                      "sha256":hashlib.sha256(s.encode()).hexdigest(),
                      "buttons":["View all","Set wallpaper…","Previous photo","Next photo"],
                      "nativeTested":False,"ownerApproved":False}))
if __name__=="__main__":main()
