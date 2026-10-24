#!/usr/bin/env python3
"""Give the honest R03 Home status candidate the default Home task actions.

Source: unchanged photographic p12 preview, last-app-ack p04 illustration, OS
state unknown without readback. Commands are explicitly not implicit applies.
This SVG is a *design draft*, not running Android or owner-approved artwork.
"""
import argparse,hashlib,html,json,re
from pathlib import Path
import xml.etree.ElementTree as ET
ROOT=Path(__file__).resolve().parents[1]
SRC=ROOT/"design/proposals/audit-revisions/r03-home-last-ack-p04.svg"
DEST=ROOT/"design/proposals/audit-revisions/r03-home-primary-actions-p04.svg"

PAIRS=[
('<text x="54" y="639" font-family="Arial,Helvetica,sans-serif" font-size="13" font-weight="500" fill="#465F60">Inspect the system wallpaper</text>',
 '<text x="54" y="639" font-family="Arial,Helvetica,sans-serif" font-size="13" font-weight="700" fill="#1D5960">Open activity · inspect device wallpaper</text>'),
('<rect x="40" y="678" width="351" height="50" rx="13" fill="#1D5960" stroke="#1D5960"/><text x="57" y="710" font-family="Arial,Helvetica,sans-serif" font-size="16" font-weight="700" fill="#FFFFFF">Open Rainbows · preview p12</text>',
 '<rect x="40" y="678" width="216" height="50" rx="13" fill="#1D5960" stroke="#1D5960"/>'
 '<text x="57" y="710" font-family="Arial,Helvetica,sans-serif" font-size="15" font-weight="700" fill="#FFFFFF">Change Home now</text>'
 '<rect x="266" y="678" width="125" height="50" rx="13" fill="#FFFCF6" stroke="#1D5960" stroke-width="2"/>'
 '<text x="280" y="710" font-family="Arial,Helvetica,sans-serif" font-size="14" font-weight="700" fill="#1D5960">Pause shuffle</text>'),
('<rect x="40" y="745" width="351" height="50" rx="13" fill="#FFFCF6" stroke="#C4B7A4"/><text x="57" y="777" font-family="Arial,Helvetica,sans-serif" font-size="16" font-weight="700" fill="#153F46">View wallpaper activity</text>',
 '<rect x="40" y="745" width="175" height="50" rx="13" fill="#FFFCF6" stroke="#1D5960" stroke-width="2"/>'
 '<text x="55" y="777" font-family="Arial,Helvetica,sans-serif" font-size="14" font-weight="700" fill="#1D5960">Open Rainbows</text>'
 '<rect x="225" y="745" width="166" height="50" rx="13" fill="#FFFCF6" stroke="#1D5960" stroke-width="2"/>'
 '<text x="239" y="777" font-family="Arial,Helvetica,sans-serif" font-size="14" font-weight="700" fill="#1D5960">Share saved p04</text>')
]

def generate():
    s=SRC.read_text(encoding="utf-8")
    for old,new in PAIRS:
        assert s.count(old)==1, "R03 source has changed; reconcile manually"
        s=s.replace(old,new,1)
    earlier='Browsing changes no wallpaper'
    clearer='Change now uses active shuffle'
    assert s.count(earlier)==1
    s=s.replace(earlier,clearer,1)
    oldnav='>Auto</text>'
    assert s.count(oldnav)==1
    s=s.replace(oldnav,'>Automation</text>',1)
    m=re.search(r"<metadata>(.*?)</metadata>",s,re.S)
    assert m
    d=json.loads(html.unescape(m.group(1)))
    assert d["lastConfirmedAppliedAssetId"]=="p04"
    assert d["selectedAssetId"]=="p12"
    assert d["presentOSWallpaperStatus"]=="not-read-back"
    d.update({
      "stateId":"home-primary-with-last-ack-truth",
      "sourceVariant":"r03-home-last-ack-p04.svg",
      "sourceSHA256":hashlib.sha256(SRC.read_bytes()).hexdigest(),
      "designCategory":"AUDIT_CANDIDATE_NOT_APPROVED",
      "homePrimaryButtons":["Change Home now","Pause shuffle","Open Rainbows","Share saved p04"],
      "activityRoute":"Open activity then inspect device wallpaper",
      "changeNowScope":"Start a fresh explicit Home shuffle-bag command, not selected preview p12",
      "shareScope":"Share locally saved acknowledged image p04; do not claim currently displayed OS wallpaper",
      "lastConfirmedAppliedAssetId":"p04",
      "presentOSWallpaperStatus":"not-read-back",
      "selectedAssetId":"p12",
      "wallpaperMutation":False,
      "p04HeroArtwork":"illustrative vector only; no source-verified p04 JPEG available",
      "p12Preview":"exact R01 public JPEG fixture",
      "nativeRuntimeVerified":False,
      "ownerApproved":False,
      "opticalReview":"needs-current-SHA full-phone raster and inspection"
    })
    s=s[:m.start(1)]+html.escape(json.dumps(d,separators=(",",":")),quote=True)+s[m.end(1):]
    s=s.replace('R03 / Honest Home acknowledgement','R03 / Complete Home actions and honest OS acknowledgement')
    ET.fromstring(s)
    for word in ("Change Home now","Pause shuffle","Open Rainbows","Share saved p04"):
        assert len(re.findall(r'<text[^>]*>'+word+r'</text>',s))==1,word
    assert 'p12 selected, not applied' in s
    assert 'Device may have changed since' in s
    return s

def main():
    p=argparse.ArgumentParser()
    p.add_argument("--write",action="store_true")
    args=p.parse_args()
    s=generate()
    if args.write:DEST.write_text(s,encoding="utf-8")
    else:assert DEST.read_text(encoding="utf-8")==s,"Drift from current source"
    print(json.dumps({"source":str(DEST.relative_to(ROOT)),
                      "sha256":hashlib.sha256(s.encode()).hexdigest(),
                      "ownerApproved":False,"deviceVerified":False}))
if __name__=="__main__":main()
