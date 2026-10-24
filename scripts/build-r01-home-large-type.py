#!/usr/bin/env python3
"""Use identical source-photo paper wheel on Home crescent and 200% type proposals.

Both are pure vector/JPEG reference drafts, never Android accessibility proof.
"""
from __future__ import annotations
import argparse,base64,hashlib,html,json,re
from pathlib import Path
from xml.etree import ElementTree as ET

ROOT=Path(__file__).resolve().parents[1]
SOURCE=ROOT/"design/proposals/audit-revisions/r01-photo-rest-22.svg"
OUT=SOURCE.parent
FONT="Arial,Helvetica,sans-serif"

def paper():
    content=SOURCE.read_text(encoding="utf-8")
    defs=re.search(r"<defs>.*?</defs>",content,re.S)
    assert defs
    start=content.index('<g clip-path="url(#wheel-cut)">')
    end=content.index('<path d="M201 452',start)
    wheel=content[start:end].strip()
    assert wheel.count('<g clip-path="url(#c')==5
    assert len(re.findall(r'<use href="#fixture[1-6]"',wheel))==5
    match=re.search(r'<image id="fixture1"[^>]+href="(data:image/jpeg;base64,[^"]+)"',defs.group())
    assert match
    uri=match.group(1)
    sha=hashlib.sha256(base64.b64decode(uri.partition(",")[2])).hexdigest()
    return defs.group(),wheel,uri,sha

def t(x,y,s,size=15,color="#153F46",weight=500):
    return f'<text x="{x}" y="{y}" fill="{color}" font-family="{FONT}" font-size="{size}" font-weight="{weight}">{html.escape(s)}</text>'
def r(x,y,w,h,fill,rx=12,stroke="none"):
    return f'<rect x="{x}" y="{y}" width="{w}" height="{h}" rx="{rx}" fill="{fill}" stroke="{stroke}"/>'
def frame(title,meta,defs,phone,annotations):
    xml=f'''<svg xmlns="http://www.w3.org/2000/svg" width="1160" height="920" viewBox="0 0 1160 920" role="img" aria-label="{html.escape(title,quote=True)}"><title>{html.escape(title)}</title>
<desc>Source-linked current-generation Glimpse photographic paper reel, not native Android screenshot or approved layout.</desc>
<metadata>{html.escape(json.dumps(meta,separators=(",",":")),quote=True)}</metadata>
{defs}
{r(0,0,1160,920,"#E9E2D6",0)}{r(15,14,401,890,"#F6F0E6",22,"#C4B7A4")}
{t(37,43,"9:41",13)}{t(354,43,"● ▰",12,"#667975")}
{phone}
{r(439,14,706,890,"#FFFCF6",18,"#C4B7A4")}
{t(464,57,title,21,weight=700)}{t(465,94,"DRAFT · NOT NATIVE OR OWNER APPROVAL",14,"#944B3E",700)}
<path d="M459 118 H1126" stroke="#C4B7A4"/>
{annotations}
</svg>'''
    root=ET.fromstring(xml)
    assert root.get("viewBox")=="0 0 1160 920"
    return xml

def home(defs,wheel,uri,sha):
    # Smaller crescent on Home is an explicit ROUTE control, never a second
    # full photo selector or an application gesture.
    meta={"auditLane":"R01","scenario":"disc-full-and-crescent",
      "surfaceId":"home","collectionId":"Rainbows","photoTotal":22,
      "homeCrescentAction":"OpenCollection(Rainbows)",
      "homeCrescentDraggable":False,"browseSelectedAssetId":"p12",
      "lastConfirmedAppliedAssetId":"p04","currentWallpaperReadback":False,
      "wallpaperMutation":False,"fullReelReference":"r01-photo-rest-22.svg",
      "paperWheelSourceSHA256":hashlib.sha256(SOURCE.read_bytes()).hexdigest(),
      "fixture1SHA256":sha,"ownerApproved":False,"androidVerified":False,
      "opticalStatus":"unreviewed","proposalStatus":"draft-not-owner-approved"}
    p=t(39,99,"Home",28,weight=700)+t(39,129,"Last Glimpse acknowledgement · Home",14,"#596F6E")
    p+=r(39,151,352,323,"#FFFCF6",14,"#C4B7A4")
    p+=r(51,164,328,236,"#203442",11)
    for y,color in [(215,"#E8876C"),(270,"#F2C05B"),(325,"#55B58A")]:
        p+=f'<path d="M73 {y} Q215 {y-66} 357 {y+2}" fill="none" stroke="{color}" stroke-width="19" stroke-linecap="round"/>'
    p+=t(58,423,"p04 · last acknowledged",16,weight=700)
    p+=t(58,447,"Device wallpaper may now differ",13,"#944B3E")
    p+=r(39,495,352,69,"#EDF0EB",13)
    p+=t(53,524,"Rotation: eligible soon, Android may defer",13,weight=700)
    p+=t(53,546,"Change wallpaper only by explicit command",12,"#596F6E")
    p+=t(40,599,"Open your Rainbows collection",19,weight=700)
    p+=r(39,618,352,157,"#EAD9BD",16,"#B6A48D")
    # Clip outside the crescent to leave collection name and thumb target legible.
    p+='<clipPath id="home-crescent-window"><rect x="228" y="621" width="160" height="151" rx="15"/></clipPath>'
    p+='<g clip-path="url(#home-crescent-window)"><g transform="translate(206 313) scale(.60)">'+wheel+'</g></g>'
    p+=t(55,659,"Rainbows",19,weight=700)+t(55,685,"22 photos",15,"#596F6E")
    p+=r(50,705,172,55,"#1D5960",13)+t(62,740,"Open collection",15,"#FFF",700)
    p+=t(39,801,"Home    Discover    Collections    Automation",12,"#667975")
    p+=t(39,875,"DRAFT · crescent opens collection; no drag",11,"#944B3E",700)
    a=t(465,156,"One consistent physical paper material",18,"#1D5960",700)
    for i,line in enumerate([
      "R01 full five-aperture wheel is the collection browser",
      "Home displays a partial photo-paper crescent as collection cover",
      "Crescent is one 55px-high Open collection button/route",
      "Do not gesture-capture, rotate, or apply wallpaper from Home",
      "Home last acknowledged p04 differs from browse selection p12",
      "Five apertures are a rendering of a finite 22-photo collection"
    ]):a+=t(465,200+i*41,line,15)
    a+=t(465,517,"Decision needed",18,"#1D5960",700)
    for i,line in enumerate([
      "Is this Home crescent visually worthwhile, or unnecessary?",
      "Review same-task alternative WITHOUT a crescent",
      "Native 48dp hit bounds, reachability and 200% font still absent"
    ]):a+=t(465,563+i*43,line,15)
    a+=t(465,840,"Unapproved option: never force a second decorative reel.",14,"#944B3E",700)
    return frame("R01 / Home crescent and full paper reel",meta,defs,p,a)

def large(defs,wheel,uri,sha):
    meta={"auditLane":"R01","scenario":"large-type",
      "surfaceId":"reel","collectionId":"Rainbows","photoTotal":22,
      "selectedAssetId":"p12","lastConfirmedAppliedAssetId":"p04",
      "appliedStatus":"unchanged","fontScaleDesign":2,
      "viewportDp":[420,900],"systemBarsIllustrative":True,
      "wheelSource":"r01-photo-rest-22.svg","sourcePhotoFixtureSHA256":sha,
      "gestureAlternativeControls":["Previous photo","Next photo","Grid"],
      "nativeFontScaleVerified":False,"ownerApproved":False,"opticalStatus":"unreviewed",
      "proposalStatus":"draft-not-owner-approved"}
    p=t(39,93,"Rainbows",31,weight=700)
    p+=t(39,132,"Photo 12 of 22",24,weight=700)
    p+=t(39,161,"Preview only",20,"#944B3E",700)
    p+=r(40,177,350,206,"#FFFCF6",14,"#B6A48D")
    p+='<clipPath id="hero-large"><rect x="43" y="180" width="344" height="200" rx="12"/></clipPath>'
    p+='<g clip-path="url(#hero-large)"><image x="43" y="180" width="344" height="200" preserveAspectRatio="xMidYMid slice" href="'+uri+'"/></g>'
    p+=r(51,342,181,31,"#FFFCF6",13)+t(61,363,"SELECTED p12",17,weight=700)
    p+=t(39,423,"Browse photos",23,weight=700)
    p+=r(291,385,98,62,"#FFFCF6",13,"#1D5960")+t(311,422,"Grid",21,"#1D5960",700)
    p+='<g transform="translate(51 136) scale(.76)">'+wheel+'</g>'
    p+='<path d="M199 477 L231 477 L215 499 Z" fill="#E8876C"/>'
    p+=r(39,696,169,62,"#FFFCF6",13,"#1D5960")+r(220,696,171,62,"#1D5960",13)
    p+=t(50,732,"Previous",19,weight=700)+t(237,732,"Next photo",19,"#FFFFFF",700)
    p+='<path d="M30 774H402" stroke="#C4B7A4"/>'
    p+=t(42,802,"Home",18,"#667975")+t(229,802,"Discover",18,"#667975")
    p+=t(42,833,"Collections",18,"#667975")+t(229,833,"Automation",18,"#667975")
    p+=r(176,874,80,5,"#153F46",3)
    a=t(465,159,"Large-font density and photographed paper",18,"#1D5960",700)
    for i,line in enumerate([
      "Photo p12 hero and center paper wedge use exact same source",
      "Dynamic type proposal uses 20–31pt primary content",
      "Two-row 18pt navigation and 62px conventional controls",
      "48px-or-larger Grid affordance remains visible",
      "No selection/apply coupling; last Glimpse acknowledgement p04",
      "Full wheel geometry source unchanged from R01 photographed reel"
    ]):a+=t(465,203+i*41,line,15)
    a+=t(465,522,"Do NOT mark accessibility PASS yet",18,"#944B3E",700)
    for i,line in enumerate([
      "Android fontScale=2.0 reflow vs proposed art must be measured",
      "Native TalkBack focus order, zoom, keyboard/switch targets",
      "Small displays, rotated landscape and system inset bounds",
      "Inspect generated 401x890 PNG for overlap before approval"
    ]):a+=t(465,565+i*38,line,15)
    return frame("R01 / Photo reel at large text",meta,defs,p,a)

def main():
    ap=argparse.ArgumentParser()
    ap.add_argument("--write",action="store_true")
    args=ap.parse_args()
    defs,wheel,uri,sha=paper()
    candidates={"r01-photo-home-crescent.svg":home(defs,wheel,uri,sha),
                "r01-photo-large-type.svg":large(defs,wheel,uri,sha)}
    for name,s in candidates.items():
        target=OUT/name
        if args.write:target.write_text(s,encoding="utf-8")
        else:assert target.read_text(encoding="utf-8")==s,name
        print(name,hashlib.sha256(s.encode()).hexdigest())
if __name__=="__main__":
    main()
