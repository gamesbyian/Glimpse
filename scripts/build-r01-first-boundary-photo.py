#!/usr/bin/env python3
"""Make a source-faithful N=22 first-item paper-wheel draft (no ghost slots).

The source design comes from the existing R01/X05 photographed five-aperture
wheel. Geometry stays unchanged. Only selected index and valid slot occupancy
change. No proof of Android reachability is inferred from this artwork.
"""
import argparse
import hashlib
import html
import json
import re
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
SOURCE=ROOT/"design/proposals/audit-revisions/r01-photo-rest-22.svg"
DEST=ROOT/"design/proposals/audit-revisions/r01-photo-first-22.svg"
SVG_NS="{http://www.w3.org/2000/svg}"


def build():
    s=SOURCE.read_text(encoding="utf-8")
    # Original paper wheel indexes read 10,11,12,13,14. At the first photo,
    # slots [c0,c1,c2,c3,c4] become [blank,blank,p01,p02,p03].
    for idx in [0,1]:
        pattern=r'<g clip-path="url\(#c'+str(idx)+r'\)"><use href="#fixture\d+" transform="[^"]+"/></g>'
        s,n=re.subn(pattern,"",s)
        assert n==1,(idx,n)
    def label(m):
        number=int(m.group(1))
        new={10:None,11:None,12:1,13:2,14:3}[number]
        return "" if new is None else m.group(0).replace(
            ">"+str(number)+"</text>",">"+f"{new:02d}"+"</text>")
    pattern=(r'<rect x="[\d.]+" y="[\d.]+" width="39" height="23" rx="7" '
             r'fill="#FFFCF6" stroke="none"/>\s*'
             r'<text x="[\d.]+" y="[\d.]+" font-family="Arial,Helvetica,sans-serif" '
             r'font-size="13" font-weight="700" fill="#153F46">(10|11|12|13|14)</text>')
    s,count=re.subn(pattern,label,s)
    assert count==5,(count,"photo badge pattern drift")

    changes={
        "Photo 12 of 22 · Preview only":"Photo 1 of 22 · Preview only",
        "SELECTED p12":"SELECTED p01",
        "Fixed pointer aligned with item 12; selected preview is p12":
            "First valid item p01 aligned under the fixed top pointer",
        "Current selectedAssetId: p12":"Current selectedAssetId: p01",
        "Frame 1 / 8: At rest":"Finite first boundary · 1 of 22",
        "sourceAssetId: p12":"sourceAssetId: p01",
        "selectedAssetId: p12":"selectedAssetId: p01",
        "Browse by rotating the paper reel":"Beginning of the finite paper reel",
        "Reel has five apertures; the collection has 22 real images.":
            "Five apertures; 22 photos; two leading holes are genuinely blank.",
    }
    for old,new in changes.items():
        assert s.count(old)==1,(old,s.count(old))
        s=s.replace(old,new,1)
    # Do not present a tappable Previous button that does nothing.
    before='<rect x="41" y="755" width="163" height="48" rx="18" fill="#FFFCF6" stroke="#C4B7A4"/>'
    after='<rect x="41" y="755" width="163" height="48" rx="18" fill="#D9DEDA" stroke="#89928B"/>'
    assert s.count(before)==1
    s=s.replace(before,after)
    assert s.count(">Previous photo</text>")==1
    s=s.replace(">Previous photo</text>",">At first photo</text>")

    match=re.search(r"<metadata>(.*?)</metadata>",s,re.S)
    assert match
    meta=json.loads(html.unescape(match.group(1)))
    hashes=meta["photoFixtureSHA256"]
    mapping={"p01":"art1","p02":"art4","p03":"art5"}
    # Use original valid neighboring photographic assets, not ID-only colors.
    remap={"p01":"p12","p02":"p13","p03":"p14"}
    meta.update({
      "stateId":"first-22","selectedAssetId":"p01",
      "sourceAssetId":"p01","photoTotal":22,"gestureDeltaDegrees":0,
      "motionPhase":"settled-first","positionCommitted":False,
      "atFiniteFirstBoundary":True,"previousEnabled":False,"nextEnabled":True,
      "auditLane":"R01","auditStatus":"photographic-draft-not-owner-approved",
      "auditSource":"r01-photo-rest-22.svg","fixtureArtworkByPhotoId":mapping,
      "photoFixtureSHA256":{id:hashes[old] for id,old in remap.items()},
      "sourceWheelSHA256":hashlib.sha256(SOURCE.read_bytes()).hexdigest(),
      "lastConfirmedAppliedAssetId":"p04","androidProof":"not-collected",
    })
    s=s[:match.start(1)]+html.escape(json.dumps(meta,separators=(",",":")),quote=True)+s[match.end(1):]
    s=s.replace("R01 · PHOTOGRAPHIC REEL COMPARISON","R01 · FIRST FINITE PHOTO BOUNDARY")
    s=s.replace("R01 DRAFT. NOT ANDROID OR OWNER EVIDENCE.","R01 FIRST-ITEM DRAFT. NOT ANDROID OR OWNER EVIDENCE.")
    assert len(re.findall(r'<g clip-path="url\(#c[0-4]\)"><use href="#fixture[1-6]"',s))==3
    assert len(re.findall(r'<text x="[\d.]+" y="[\d.]+" font-family="Arial,Helvetica,sans-serif" font-size="13" font-weight="700" fill="#153F46">(\d\d)</text>',s))==3
    doc=ET.fromstring(s)
    assert doc.tag==SVG_NS+"svg" and doc.get("viewBox")=="0 0 1160 920"
    return s


def main():
    p=argparse.ArgumentParser()
    p.add_argument("--write",action="store_true")
    args=p.parse_args()
    s=build()
    if args.write:
        DEST.write_text(s,encoding="utf-8")
    else:
        assert DEST.read_text(encoding="utf-8")==s,"First-item photo source drift"
    print(json.dumps({"path":str(DEST.relative_to(ROOT)),
                      "sha256":hashlib.sha256(s.encode()).hexdigest(),
                      "state":"N=22 at p01, two empty leading apertures",
                      "approved":False}))


if __name__=="__main__":
    main()
