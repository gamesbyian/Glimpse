#!/usr/bin/env python3
"""Glimpse current-generation Collections reference with real photographic covers.

Source-editable SVG is 1160×920 with exact 401×890 phone window. Cards use
*static* photo-paper covers, not draggable micro reels. Photos are existing,
distinct, public reference JPGs; non-Rainbows asset IDs are not asserted.
"""
from __future__ import annotations
import argparse
import base64
import hashlib
import html
import json
from pathlib import Path
import xml.etree.ElementTree as ET

ROOT=Path(__file__).resolve().parents[1]
FIX=ROOT/"design/research/reel-layout/fixtures/source"
OUT=ROOT/"design/proposals/whole-product-candidates/collections-photo-covers.svg"
ART={"Rainbows":("art1.jpg",22,"Selected for browsing","rainbows"),
     "Golden Hour":("art4.jpg",50,"Saved collection","golden"),
     "Patterns":("art6.jpg",3,"Saved collection","patterns")}

def txt(x,y,s,size=14,weight=500,fill="#153F46"):
    return f'<text x="{x}" y="{y}" font-family="Arial,Helvetica,sans-serif" font-size="{size}" font-weight="{weight}" fill="{fill}">{html.escape(s)}</text>'
def rect(x,y,w,h,fill="#FFFCF6",rx=13,stroke="#C4B7A4"):
    return f'<rect x="{x}" y="{y}" width="{w}" height="{h}" rx="{rx}" fill="{fill}" stroke="{stroke}"/>'

def build():
    photos={}
    for name,(filename,count,state,key) in ART.items():
        data=(FIX/filename).read_bytes()
        assert data[:3]==bytes.fromhex("ffd8ff")
        photos[name]={"file":filename,"sha256":hashlib.sha256(data).hexdigest(),
                      "base64":base64.b64encode(data).decode("ascii"),"count":count,
                      "state":state,"key":key}
    assert len(set(p["sha256"] for p in photos.values()))==3
    meta={"surfaceId":"collections","stateId":"photographic-list",
        "designCategory":"WHOLE_PRODUCT_CANDIDATE_NOT_AUDIT_BASELINE",
        "exampleCollectionCounts":{"Rainbows":22,"Golden Hour":50,"Patterns":3},
        "previewAssetId":"p12","previewCollectionId":"Rainbows",
        "sourcePhotoMap":{name:{"file":obj["file"],"sha256":obj["sha256"]}
                          for name,obj in photos.items()},
        "coverBehavior":"tap static photo cover opens corresponding collection",
        "noGestureWheelOnCollectionsList":True,
        "wallpaperMutation":False,"providerAlbumSyncImplied":False,
        "visualApproval":"not-owner-approved","nativeProof":"unverified"}
    svg=['<svg xmlns="http://www.w3.org/2000/svg" width="1160" height="920" viewBox="0 0 1160 920" role="img" aria-label="Glimpse Collections photo-paper reference draft">',
        "<title>Collections · photographic paper covers</title>",
        "<desc>Three sample finite collections with distinct source-backed public photographic covers, conventional navigation and no small draggable reel.</desc>",
        "<metadata>"+html.escape(json.dumps(meta,separators=(",",":")),quote=True)+"</metadata>"]
    svg.append('<defs>')
    for name,p in photos.items():
        svg.append(f'<clipPath id="{p["key"]}-photo"><rect x="49" y="0" width="112" height="109" rx="13"/></clipPath>')
    svg.append('</defs>')
    svg += [rect(0,0,1160,920,"#E9E2D6",0,"none"),rect(15,14,401,890,"#F6F0E6",22),
            txt(38,44,"9:41",14,700),txt(358,44,"● ▰",12,500,"#667975"),
            txt(38,100,"Collections",30,700),txt(39,131,"Keep your favorite photographs together",13,500,"#536F70"),
            rect(39,153,353,49),txt(57,184,"Search collections",15,500,"#667975"),
            '<circle cx="358" cy="176" r="7" stroke="#537174" stroke-width="2" fill="none"/>',
            '<path d="M364 182l8 8" stroke="#537174" stroke-width="2"/>',
            txt(40,229,"3 collections · locally saved",15,700),
            txt(323,229,"Sort ▾",14,600,"#1D5960")]
    y0=245
    for idx,(name,p) in enumerate(photos.items()):
        y=y0+idx*150
        svg.append(rect(39,y,353,139))
        svg.append(f'<rect x="49" y="{y+12}" width="112" height="113" rx="15" fill="#EAD9BD" stroke="#AE9B84" stroke-width="3"/>')
        svg.append(f'<clipPath id="photo-{idx}"><rect x="53" y="{y+16}" width="104" height="105" rx="11"/></clipPath>')
        svg.append(f'<g clip-path="url(#photo-{idx})"><image x="53" y="{y+16}" width="104" height="105" preserveAspectRatio="xMidYMid slice" href="data:image/jpeg;base64,{p["base64"]}"/></g>')
        # Static paper corners give the cover photo a tangible, printed-media edge.
        svg.append(f'<path d="M53 {y+103}q10 13 32 18H53z" fill="#F4E4CF" fill-opacity=".65"/>')
        svg.append(txt(176,y+41,name,19,700))
        svg.append(txt(176,y+67,f'{p["count"]} photographs',14,500,"#536F70"))
        if idx==0:
            svg.append(rect(176,y+79,101,24,"#E4F1EC",7,"none"))
            svg.append(txt(183,y+96,"Active · local",12,700,"#1D5960"))
        else:
            svg.append(txt(176,y+96,p["state"],12,500,"#667975"))
        svg.append(txt(176,y+122,"Open collection",14,700,"#1D5960"))
        svg.append(f'<path d="M336 {y+116} l12 0 -5 -5 m5 5 -5 5" fill="none" stroke="#1D5960" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>')
    svg += [rect(39,711,173,55,"#1D5960"),txt(55,745,"Add photographs",16,700,"#FFFFFF"),
            rect(221,711,171,55),txt(236,745,"New collection",16,700),
            rect(24,820,384,60,"#FFFCF6",0,"none"),
            '<path d="M24 820H408" stroke="#C4B7A4"/>',
            txt(39,856,"Home",13,500,"#667975"),txt(127,856,"Discover",13,500,"#667975"),
            txt(223,856,"Collections",13,700,"#1D5960"),txt(331,856,"Automation",13,500,"#667975"),
            txt(39,895,"DRAFT · TAP COVER OPENS, DOES NOT SPIN",10,700,"#944B3E")]
    svg += [rect(438,14,707,890,"#FFFCF6",18),
            txt(467,61,"COLLECTIONS · current visual candidate",23,700),
            txt(467,97,"UNAPPROVED · exact 401×890 phone pixels",14,700,"#944B3E"),
            '<path d="M460 123H1119" stroke="#C4B7A4"/>',
            txt(467,162,"Why the photographed covers matter",18,700,"#1D5960")]
    for i,s in enumerate([
        "Image thumbnails use three distinct public sample JPEG source bytes.",
        "A printed photo cover is a tap target, not a miniature draggable wheel.",
        "The active Rainbows collection retains its real finite count of 22.",
        "Names and counts remain plain, selectable, accessible text.",
        "Search / Sort / Add photos / New collection are conventional controls.",
        "Opening a collection preserves its focused image selection.",
        "Neither browsing nor importing implies a wallpaper API action.",
        "No simulated Google Photos album synchronization or rights claim."
    ]): svg.append(txt(467,201+35*i,s,15))
    svg+=[txt(467,529,"Open questions for the owner",18,700,"#1D5960")]
    for i,s in enumerate([
        "Should static photographic covers echo more of the paper rim?",
        "Does the active/local distinction read without the color highlight?",
        "At 200% system text, which cards need compact or vertical layout?",
        "Does Add photographs mean to selected collection or chooser?",
        "Native TalkBack/48dp and real provider import still unverified."
    ]):svg.append(txt(467,570+39*i,s,15))
    svg.append(txt(467,860,"No native runtime, accessibility or owner approval implied.",14,700,"#944B3E"))
    svg.append("</svg>")
    content="\n".join(svg)+"\n"
    tree=ET.fromstring(content)
    assert tree.get("viewBox")=="0 0 1160 920"
    assert len(set(p["sha256"] for p in photos.values()))==3
    return content

def main():
    parser=argparse.ArgumentParser()
    parser.add_argument("--write",action="store_true")
    args=parser.parse_args()
    s=build()
    if args.write:
        OUT.parent.mkdir(parents=True,exist_ok=True)
        OUT.write_text(s,encoding="utf-8")
    else: assert OUT.read_text(encoding="utf-8")==s
    print(json.dumps({"source":str(OUT.relative_to(ROOT)),
                      "sourceSHA256":hashlib.sha256(s.encode()).hexdigest(),
                      "status":"draft-only"}))
if __name__=="__main__":main()
