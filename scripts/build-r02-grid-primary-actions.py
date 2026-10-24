#!/usr/bin/env python3
"""Source-faithful p12 Grid: Search, multi-selection and share on the phone.

This X07-based primary candidate keeps exact p12 photo pixels and stable ID;
all operations are proposals, never inferred Android runtime actions.
"""
import argparse,base64,hashlib,html,json,re
from pathlib import Path
import xml.etree.ElementTree as ET
ROOT=Path(__file__).resolve().parents[1]
SRC=ROOT/"design/proposals/storyboards/x07/02-grid-focus.svg"
DEST=ROOT/"design/proposals/audit-revisions/r02-grid-primary-actions-p12.svg"

PAIRS=[
('<text x="39" y="122" font-family="Arial,Helvetica,sans-serif" font-size="14" font-weight="500" fill="#667975">Grid focus p12</text>',
 '<text x="39" y="122" font-family="Arial,Helvetica,sans-serif" font-size="14" font-weight="500" fill="#536F70">Selected p12 of 22 · same as Reel</text>'),
('<text x="40" y="182" font-family="Arial,Helvetica,sans-serif" font-size="16" font-weight="650" fill="#153F46">Selected p12 of 22</text>',
 '<rect x="39" y="139" width="344" height="49" rx="12" fill="#FFFCF6" stroke="#1D5960" stroke-width="2"/>'
 '<text x="55" y="170" font-family="Arial,Helvetica,sans-serif" font-size="15" font-weight="500" fill="#536F70">Search photos / jump to number</text>'
 '<circle cx="353" cy="163" r="8" fill="none" stroke="#1D5960" stroke-width="2"/>'
 '<path d="M359 169 L369 179" fill="none" stroke="#1D5960" stroke-width="2"/>'),
('<rect x="41" y="591" width="158" height="43" rx="18" fill="#FFFCF6" stroke="#C4B7A4"/>',
 '<rect x="41" y="588" width="158" height="49" rx="13" fill="#FFFCF6" stroke="#1D5960" stroke-width="2"/>'),
('<rect x="208" y="591" width="173" height="43" rx="18" fill="#1D5960" stroke="#1D5960"/>',
 '<rect x="208" y="588" width="173" height="49" rx="13" fill="#1D5960" stroke="#1D5960"/>'),
('<text x="55" y="618" font-family="Arial,Helvetica,sans-serif" font-size="14" font-weight="650" fill="#153F46">Back to reel</text>',
 '<text x="55" y="619" font-family="Arial,Helvetica,sans-serif" font-size="14" font-weight="700" fill="#153F46">Back to reel</text>'),
('<text x="222" y="618" font-family="Arial,Helvetica,sans-serif" font-size="14" font-weight="650" fill="white">View p12</text>',
 '<text x="222" y="619" font-family="Arial,Helvetica,sans-serif" font-size="14" font-weight="700" fill="white">View p12</text>'
 '<rect x="41" y="649" width="158" height="49" rx="13" fill="#FFFCF6" stroke="#1D5960" stroke-width="2"/>'
 '<text x="58" y="680" font-family="Arial,Helvetica,sans-serif" font-size="15" font-weight="700" fill="#1D5960">Share p12</text>'
 '<rect x="208" y="649" width="173" height="49" rx="13" fill="#FFFCF6" stroke="#1D5960" stroke-width="2"/>'
 '<text x="222" y="680" font-family="Arial,Helvetica,sans-serif" font-size="15" font-weight="700" fill="#1D5960">Select multiple</text>')
]

def generate():
    s=SRC.read_text(encoding="utf-8")
    for old,new in PAIRS:
        assert s.count(old)==1,old[:65]
        s=s.replace(old,new,1)
    # The five visibly adjacent IDs already have distinct public JPEG fixtures
    # in R01. Reuse the exact bytes so Grid and Reel cannot depict different
    # photographs under the same stable p10/p11/p13/p14/p15 IDs.
    reel=ROOT/"design/proposals/audit-revisions/r01-photo-rest-22.svg"
    reel_match=re.search(r"<metadata>(.*?)</metadata>",reel.read_text(encoding="utf-8"),re.S)
    assert reel_match
    reel_meta=json.loads(html.unescape(reel_match.group(1)))
    source_sha=reel_meta["photoFixtureSHA256"]
    mapping=[
      ("p10","art2",38,325),("p11","art3",126,325),
      ("p13","art4",302,325),("p14","art5",38,434),
      ("p15","art6",126,434)
    ]
    fixture_dir=ROOT/"design/research/reel-layout/fixtures/source"
    extra=[]
    photo_sha={"p12":reel_meta["photoFixtureSHA256"]["p12"]}
    photo_art={"p12":"art1"}
    for pid,art,x,y in mapping:
        raw=(fixture_dir/(art+".jpg")).read_bytes()
        assert raw[:3]==bytes.fromhex("ffd8ff")
        digest=hashlib.sha256(raw).hexdigest()
        if pid in source_sha: assert source_sha[pid]==digest,(pid,"R01 fixture byte drift")
        photo_sha[pid]=digest
        photo_art[pid]=art
        payload=base64.b64encode(raw).decode("ascii")
        extra.append(
          f'<clipPath id="grid-verified-{pid}"><rect x="{x}" y="{y}" width="80" height="99" rx="12"/></clipPath>'
          f'<g clip-path="url(#grid-verified-{pid})"><image x="{x}" y="{y}" width="80" height="99" '
          f'preserveAspectRatio="xMidYMid slice" href="data:image/jpeg;base64,{payload}"/></g>'
          f'<rect x="{x}" y="{y}" width="80" height="99" rx="12" fill="none" stroke="#B6A48D" stroke-width="1.5"/>'
          f'<rect x="{x+7}" y="{y+73}" width="66" height="20" rx="7" fill="#FFFCF6"/>'
          f'<text x="{x+14}" y="{y+88}" font-family="Arial,Helvetica,sans-serif" font-size="11" '
          f'font-weight="700" fill="#153F46">{pid}</text>'
        )
    assert len(set(photo_sha.values()))==6, "All 6 shared photo IDs require distinct JPEGs"
    first_action='<rect x="41" y="588" width="158" height="49"'
    assert s.count(first_action)==1
    s=s.replace(first_action,"".join(extra)+first_action,1)
    m=re.search(r"<metadata>(.*?)</metadata>",s,re.S)
    assert m
    d=json.loads(html.unescape(m.group(1)))
    assert d["selectedAssetId"]=="p12" and d["appliedStatus"]=="unchanged"
    d.update({
     "surfaceId":"grid","stateId":"grid-primary-p12-search-share-multiselect",
     "sourceVariant":"storyboards/x07/02-grid-focus.svg",
     "sourceSHA256":hashlib.sha256(SRC.read_bytes()).hexdigest(),
     "designCategory":"AUDIT_CANDIDATE_NOT_APPROVED",
     "selectedAssetId":"p12","photoTotal":22,
     "selectedAssetIDs":["p12"],"multiSelectActive":False,
     "fixtureArtworkByPhotoId":photo_art,"photoFixtureSHA256":photo_sha,
     "gridPhotoFixturePolicy":"six shared p10 through p15 photo IDs use exact checked-in source artwork; other shown tiles remain explicitly schematic",
     "phoneControls":["Search photos / jump to number","Back to reel","View p12","Share p12","Select multiple"],
     "searchBehavior":"Search or position jump preserves selected p12 until user chooses another photo",
     "shareBehavior":"Android Sharesheet chooser; delivery cannot be inferred",
     "multiSelectBehavior":"Opens distinct multi-selection mode with per-photo visibly marked selection",
     "wallpaperMutation":False,"ownerApproved":False,
     "androidVerified":False,"opticalStatus":"needs-current-401x890-pixels-review"
    })
    s=s[:m.start(1)]+html.escape(json.dumps(d,separators=(",",":")),quote=True)+s[m.end(1):]
    s=s.replace("<title>Reel ↔ Grid ↔ detail / 02-grid-focus</title>",
       "<title>R02 · Grid primary p12 with task actions · draft</title>")
    ET.fromstring(s)
    for word in ("Share p12","Select multiple","Search photos / jump to number","View p12","Back to reel"):
        assert len(re.findall(r'<text[^>]*>'+word+r'</text>',s))==1,word
    assert '<use href="#fixture1"' in s
    return s

def main():
    a=argparse.ArgumentParser();a.add_argument("--write",action="store_true")
    args=a.parse_args()
    s=generate()
    if args.write:DEST.write_text(s,encoding="utf-8")
    else:assert DEST.read_text(encoding="utf-8")==s
    print(json.dumps({"source":str(DEST.relative_to(ROOT)),
      "sha256":hashlib.sha256(s.encode()).hexdigest(),
      "selected":"p12","ownerApproved":False,"nativeVerified":False}))
if __name__=="__main__":main()
