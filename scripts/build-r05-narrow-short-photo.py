#!/usr/bin/env python3
"""Create an honest 320x640-density stress reference from the existing R01 paper reel.

This is a draft phone layout, not a screenshot captured from an Android device.
Reuses exactly the X05/R01 photo bytes, wheel paths, paper material and fixture
IDs rather than substituting a decorative thumbnail strip.
"""
from __future__ import annotations

import argparse
import hashlib
import html
import json
import re
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
ORIGINAL=ROOT/"design/proposals/audit-revisions/r01-photo-rest-22.svg"
DEST=ROOT/"design/proposals/audit-revisions/r05-photo-reel-narrow-short-320.svg"


def construct():
    source=ORIGINAL.read_text(encoding="utf-8")
    defs=re.search(r"<defs>.*?</defs>",source,re.S)
    assert defs is not None
    wheel_start=source.index('<g clip-path="url(#wheel-cut)">')
    wheel_end=source.index('<path d="M201 452',wheel_start)
    wheel=source[wheel_start:wheel_end]
    assert wheel.startswith("<g") and wheel.rstrip().endswith("</g>")
    # Five source R01 apertures, same bytes, preserve source-linked identity.
    assert wheel.count('<g clip-path="url(#c')==5
    assert len(re.findall(r'<use href="#fixture[1-6]"',wheel))==5
    raw_image=re.search(r'<image id="fixture1"[^>]+href="(data:image/jpeg;base64,[^"]+)"',defs.group(0))
    assert raw_image
    image_href=raw_image.group(1)
    import base64
    image_bytes=base64.b64decode(image_href.partition(",")[2])
    assert image_bytes[:3]==b"\\xff\\xd8\\xff"
    audit={"auditLane":"R05","stateId":"narrow-short-320",
       "proposalStatus":"draft-not-owner-approved","selectedAssetId":"p12",
       "lastConfirmedAppliedAssetId":"p04","pendingRequestedAssetId":None,
       "photoTotal":22,"visiblePhotoIDs":["p10","p11","p12","p13","p14"],
       "sourceWheel":"design/proposals/audit-revisions/r01-photo-rest-22.svg",
       "wheelSourceSHA256":hashlib.sha256(ORIGINAL.read_bytes()).hexdigest(),
       "fixture1SHA256":hashlib.sha256(image_bytes).hexdigest(),
       "designViewportDp":[320,640],"cropPixels":[47,114,367,754],
       "androidProof":"none","opticalProof":"requires independent real-pixel review",
       "ownerApproved":False}
    out=f'''<svg xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink" width="1160" height="920" viewBox="0 0 1160 920" role="img" aria-label="Glimpse R05 small-screen photographic paper reel 320 by 640 draft">
<title>R05 short phone density · photo reel and alternatives</title>
<desc>Source-linked reduced-area proposal. Distinct photo p12 preview, former confirmed p04, five-window physical paper reel with partial offscreen crop and 48px-class conventional navigation. Not on-device proof.</desc>
<metadata>{html.escape(json.dumps(audit,separators=(",",":")),quote=True)}</metadata>
{defs.group(0)}
<rect width="1160" height="920" fill="#E9E1D5"/>
<rect x="47" y="114" width="320" height="640" rx="21" fill="#F6F0E6" stroke="#9F937D" stroke-width="2"/>
<clipPath id="viewport-short"><rect x="49" y="116" width="316" height="636" rx="19"/></clipPath>
<g clip-path="url(#viewport-short)">
<text x="67" y="140" font-family="Arial,Helvetica,sans-serif" font-size="13" font-weight="700" fill="#153F46">9:41</text>
<text x="311" y="140" font-family="Arial,Helvetica,sans-serif" font-size="12" fill="#153F46">● ▰</text>
<text x="67" y="174" font-family="Arial,Helvetica,sans-serif" font-size="23" font-weight="700" fill="#153F46">Rainbows</text>
<text x="67" y="199" font-family="Arial,Helvetica,sans-serif" font-size="14" fill="#4B6464">Photo 12 of 22 · Preview only</text>
<rect x="64" y="216" width="286" height="152" rx="14" fill="#FFFCF6" stroke="#B6A48D"/>
<clipPath id="short-hero"><rect x="67" y="219" width="280" height="146" rx="12"/></clipPath>
<g clip-path="url(#short-hero)"><image x="67" y="219" width="280" height="146" preserveAspectRatio="xMidYMid slice" href="{image_href}"/></g>
<rect x="74" y="336" width="144" height="25" rx="12" fill="#FFFCF6"/>
<text x="84" y="353" font-family="Arial,Helvetica,sans-serif" font-size="12" font-weight="700" fill="#153F46">SELECTED · p12</text>
<text x="67" y="398" font-family="Arial,Helvetica,sans-serif" font-size="14" font-weight="700" fill="#153F46">Browse photos</text>
<rect x="259" y="373" width="91" height="48" rx="13" fill="#FFFCF6" stroke="#1D5960" stroke-width="2"/>
<text x="276" y="402" font-family="Arial,Helvetica,sans-serif" font-size="14" font-weight="700" fill="#1D5960">Grid</text>
<path d="M318 399 L331 386 M321 386 H331 V396" stroke="#1D5960" stroke-width="2.5" stroke-linecap="round" fill="none"/>
<g transform="translate(40 70) scale(.75)">
{wheel}
</g>
<path d="M188 414 H226 L207 435 Z" fill="#E8876C" stroke="#944B3E" stroke-width="1.5"/>
<rect x="62" y="642" width="130" height="52" rx="12" fill="#FFFCF6" stroke="#1D5960"/>
<rect x="202" y="642" width="149" height="52" rx="12" fill="#FFFCF6" stroke="#1D5960"/>
<text x="76" y="672" font-family="Arial,Helvetica,sans-serif" font-size="14" font-weight="700" fill="#153F46">Previous photo</text>
<text x="226" y="672" font-family="Arial,Helvetica,sans-serif" font-size="14" font-weight="700" fill="#153F46">Next photo</text>
<path d="M56 711 H360" stroke="#D2C6B9"/>
<text x="67" y="735" font-family="Arial,Helvetica,sans-serif" font-size="12" fill="#4B6464">Home   Discover   Collections   Automation</text>
<rect x="172" y="746" width="70" height="4" rx="2" fill="#153F46"/>
</g>
<rect x="438" y="114" width="707" height="640" rx="18" fill="#FFFCF6" stroke="#C4B7A4"/>
<text x="462" y="161" font-family="Arial,Helvetica,sans-serif" font-size="24" font-weight="700" fill="#153F46">R05 / 320 × 640 compact study</text>
<text x="462" y="199" font-family="Arial,Helvetica,sans-serif" font-size="15" fill="#944B3E" font-weight="700">DRAFT · visual density hypothesis · not native observation</text>
<text x="462" y="255" font-family="Arial,Helvetica,sans-serif" font-size="17" font-weight="700" fill="#1D5960">What this must preserve</text>
<text x="462" y="294" font-family="Arial,Helvetica,sans-serif" font-size="15" fill="#153F46">Five physical photographic apertures, finite photo count 22</text>
<text x="462" y="323" font-family="Arial,Helvetica,sans-serif" font-size="15" fill="#153F46">The original physical paper disc and selected image p12</text>
<text x="462" y="352" font-family="Arial,Helvetica,sans-serif" font-size="15" fill="#153F46">A conventional Grid route and full-height Previous / Next targets</text>
<text x="462" y="381" font-family="Arial,Helvetica,sans-serif" font-size="15" fill="#153F46">Selection remains preview-only; last acknowledgement p04</text>
<text x="462" y="432" font-family="Arial,Helvetica,sans-serif" font-size="17" font-weight="700" fill="#1D5960">Device-dependent open questions</text>
<text x="462" y="470" font-family="Arial,Helvetica,sans-serif" font-size="15" fill="#153F46">Actual 320dp insets, hit bounds, density, font scale and OEM bars</text>
<text x="462" y="499" font-family="Arial,Helvetica,sans-serif" font-size="15" fill="#153F46">Five-photo wedge labels become tiny; real focus route is required</text>
<text x="462" y="528" font-family="Arial,Helvetica,sans-serif" font-size="15" fill="#153F46">The paper wheel must not become a scroll or gesture trap</text>
<text x="462" y="592" font-family="Arial,Helvetica,sans-serif" font-size="15" fill="#944B3E">No Android screenshots / no owner approval / no 200% font PASS.</text>
<text x="462" y="688" font-family="Arial,Helvetica,sans-serif" font-size="13" fill="#667975">Crop for this phone: (47,114)–(367,754) = 320 × 640 source pixels</text>
</svg>'''
    root=ET.fromstring(out)
    assert root.get("viewBox")=="0 0 1160 920"
    assert 'Photo 12 of 22 · Preview only' in out and 'Previous photo' in out
    assert len(re.findall(r'<use href="#fixture[1-6]"',wheel))==5
    return out


def main():
    ap=argparse.ArgumentParser()
    ap.add_argument("--write",action="store_true")
    args=ap.parse_args()
    out=construct()
    if args.write:
        DEST.parent.mkdir(parents=True,exist_ok=True)
        DEST.write_text(out,encoding="utf-8")
    else:
        assert DEST.read_text(encoding="utf-8")==out,"Source drift, regenerate intentionally"
    print(json.dumps({"draft":str(DEST.relative_to(ROOT)),
                      "sourceSHA256":hashlib.sha256(out.encode()).hexdigest(),
                      "viewport":[320,640],"approved":False}))


if __name__=="__main__":
    main()
