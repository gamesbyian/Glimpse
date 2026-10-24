#!/usr/bin/env python3
"""Source-linked photographic Photo Detail with separate real task affordances.

Uses X07 p12 fixture unchanged. This phone art is a UI candidate, not native
WallpaperManager/ShareSheet evidence. Commands are chooser/open intents only.
"""
import argparse, hashlib, html, json, re
from pathlib import Path
import xml.etree.ElementTree as ET

ROOT=Path(__file__).resolve().parents[1]
SRC=ROOT/"design/proposals/storyboards/x07/03-photo-detail.svg"
DEST=ROOT/"design/proposals/audit-revisions/r02-photo-detail-actions-p12.svg"

OLD=(
'<rect x="43" y="683" width="339" height="43" rx="18" fill="#FFFCF6" stroke="#C4B7A4"/>\n'
'<text x="57" y="710" font-family="Arial,Helvetica,sans-serif" font-size="14" font-weight="650" fill="#153F46">Back to Grid</text>'
)
NEW=(
'<rect x="269" y="65" width="113" height="46" rx="13" fill="#FFFCF6" stroke="#1D5960" stroke-width="2"/>'
'<path d="M292 88 H282 M282 88 L288 82 M282 88 L288 94" fill="none" stroke="#1D5960" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"/>'
'<text x="299" y="93" font-family="Arial,Helvetica,sans-serif" font-size="14" font-weight="700" fill="#1D5960">Grid</text>'
'<rect x="43" y="680" width="339" height="48" rx="14" fill="#1D5960" stroke="#1D5960"/>'
'<text x="63" y="710" font-family="Arial,Helvetica,sans-serif" font-size="16" font-weight="700" fill="#FFFFFF">Set wallpaper…</text>'
'<rect x="43" y="739" width="164" height="48" rx="14" fill="#FFFCF6" stroke="#1D5960" stroke-width="2"/>'
'<text x="61" y="769" font-family="Arial,Helvetica,sans-serif" font-size="15" font-weight="700" fill="#1D5960">Share photo</text>'
'<rect x="217" y="739" width="165" height="48" rx="14" fill="#FFFCF6" stroke="#1D5960" stroke-width="2"/>'
'<text x="233" y="769" font-family="Arial,Helvetica,sans-serif" font-size="15" font-weight="700" fill="#1D5960">Edit copy</text>'
)

def generate():
    s=SRC.read_text(encoding="utf-8")
    assert s.count(OLD)==1, "X07 photo detail buttons changed; reconcile manually"
    assert s.count('<use href="#fixture1"')==1
    s=s.replace(OLD,NEW,1)
    m=re.search(r"<metadata>(.*?)</metadata>",s,re.S)
    assert m
    info=json.loads(html.unescape(m.group(1)))
    assert info["selectedAssetId"]=="p12" and info["appliedStatus"]=="unchanged"
    info.update({
      "surfaceId":"photo-detail",
      "stateId":"source-photo-p12-with-explicit-actions",
      "designCategory":"AUDIT_CANDIDATE_NOT_OWNER_APPROVED",
      "sourceVariant":"storyboards/x07/03-photo-detail.svg",
      "originalSourceSHA256":hashlib.sha256(SRC.read_bytes()).hexdigest(),
      "userActions":["Back to Grid","Set wallpaper…","Share photo","Edit copy"],
      "setWallpaperSemantic":"Open Home/Lock target chooser then require a separate explicit apply; opening image does not apply",
      "shareSemantic":"Open original-versus-edited payload choice/Android Sharesheet; delivery unverified",
      "editSemantic":"Open non-destructive editor; preserve original p12",
      "appliedStatus":"unchanged",
      "ownerApproved":False,
      "nativeAccessibilityMeasured":False,
      "opticalReview":"needs-exact-401x890-PNG-inspection"
    })
    s=s[:m.start(1)]+html.escape(json.dumps(info,separators=(",",":")),quote=True)+s[m.end(1):]
    s=s.replace('<title>Reel ↔ Grid ↔ detail / 03-photo-detail</title>',
       '<title>R02 · photographic p12 detail with real image actions · draft</title>')
    ET.fromstring(s)
    assert s.count("Set wallpaper…")==1 and s.count("Edit copy")==1
    assert s.count("Share photo")==1 and s.count("Grid</text>")==1
    assert '<text x="57" y="531"' in s and '<use href="#fixture1"' in s
    return s

def main():
    ap=argparse.ArgumentParser()
    ap.add_argument("--write",action="store_true")
    args=ap.parse_args()
    s=generate()
    if args.write:DEST.write_text(s,encoding="utf-8")
    else:assert DEST.read_text(encoding="utf-8")==s,"Non-deterministic candidate"
    print(json.dumps({"svg":str(DEST.relative_to(ROOT)),
      "sourceSHA256":hashlib.sha256(s.encode()).hexdigest(),
      "selectedAssetId":"p12","ownerApproved":False,"nativeValidated":False}))
if __name__=="__main__":main()
