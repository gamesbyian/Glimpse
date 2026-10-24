#!/usr/bin/env python3
"""Build source-linked Home acknowledgement and recovery-return Glimpse proposals."""
from __future__ import annotations
import argparse,base64,hashlib,html,json,re
from pathlib import Path
import xml.etree.ElementTree as ET

ROOT=Path(__file__).resolve().parents[1]
R01=ROOT/"design/proposals/audit-revisions/r01-photo-rest-22.svg"
R08=ROOT/"design/proposals/audit-revisions/r08-restore-conflict-choice.svg"
OUT=ROOT/"design/proposals/audit-revisions"
FONT="Arial,Helvetica,sans-serif"

def esc(s):return html.escape(str(s),quote=False)
def text(x,y,t,size=14,color="#153F46",weight=500):
    return f'<text x="{x}" y="{y}" font-family="{FONT}" font-size="{size}" font-weight="{weight}" fill="{color}">{esc(t)}</text>'
def rect(x,y,w,h,fill="#FFFCF6",rx=12,stroke="#C4B7A4"):
    return f'<rect x="{x}" y="{y}" width="{w}" height="{h}" rx="{rx}" fill="{fill}" stroke="{stroke}"/>'
def button(y,label,primary=False):
    return rect(40,y,351,50,"#1D5960" if primary else "#FFFCF6",13,
                "#1D5960" if primary else "#C4B7A4")+text(57,y+32,label,16,
                      "#FFFFFF" if primary else "#153F46",700)

def photo_fixture():
    r=R01.read_text(encoding="utf-8")
    match=re.search(r'<image id="fixture1"[^>]+href="(data:image/jpeg;base64,[^"]+)"',r)
    assert match
    uri=match.group(1)
    data=base64.b64decode(uri.partition(",")[2])
    assert data[:3]==bytes.fromhex("ffd8ff")
    return uri,hashlib.sha256(data).hexdigest()

def common(headline,desc,meta,phone,annotations):
    doc='''<svg xmlns="http://www.w3.org/2000/svg" width="1160" height="920" viewBox="0 0 1160 920"
role="img" aria-label="''' +html.escape(headline,quote=True)+'''">
<title>'''+esc(headline)+'''</title><desc>'''+esc(desc)+'''</desc>
<metadata>'''+html.escape(json.dumps(meta,separators=(",",":")),quote=True)+'''</metadata>
'''+rect(0,0,1160,920,"#E9E2D6",0,"none")+rect(15,14,401,890,"#F6F0E6",22)+'''
'''+text(37,45,"9:41",14)+text(357,45,"● ▰",12,"#667975")+'''
'''+phone+'''
<path d="M31 818H400" stroke="#C4B7A4"/>
'''+text(40,849,"Home",12,"#667975")+text(134,849,"Discover",12,"#667975")+text(229,849,"Collections",12,"#667975")+text(328,849,"Auto",12,"#667975")+'''
<rect x="177" y="878" width="76" height="5" rx="2" fill="#153F46"/>
'''+rect(439,14,706,890)+text(464,60,headline,22,weight=700)+text(465,97,"UNAPPROVED SOURCE-EDITABLE CANDIDATE",14,"#944B3E",700)+'''
<path d="M459 119H1125" stroke="#C4B7A4"/>
'''+annotations+'''
</svg>'''
    root=ET.fromstring(doc)
    assert root.get("viewBox")=="0 0 1160 920"
    return doc

def ack():
    uri,hash_=photo_fixture()
    meta={"auditLane":"R03","stateId":"home-last-glimpse-ack",
     "surfaceId":"home","lastConfirmedAppliedAssetId":"p04",
     "presentOSWallpaperStatus":"not-read-back",
     "selectedAssetId":"p12","selectionSurface":"Rainbows",
     "pendingRequestedAssetId":None,"wallpaperMutation":False,
     "p12FixtureSHA256":hash_,"sourceFixture":"r01-photo-rest-22.svg#fixture1",
     "androidVerified":False,"opticallyAccepted":False,"ownerApproved":False,
     "proposalStatus":"draft-not-owner-approved"}
    phone=text(39,95,"Home",26,weight=700)+text(39,128,"Last Glimpse acknowledgement",15,"#4B6464")+'''
'''+rect(39,147,352,310)+rect(52,159,326,177,"#203442",12,"none")+'''
<path d="M71 203 Q207 159 354 207" stroke="#EC755D" stroke-width="15" fill="none" stroke-linecap="round"/>
<path d="M71 245 Q207 198 354 249" stroke="#EDC563" stroke-width="15" fill="none" stroke-linecap="round"/>
<path d="M71 287 Q207 240 354 291" stroke="#5DAD9C" stroke-width="15" fill="none" stroke-linecap="round"/>
'''+text(63,357,"Photo 04 · Home",17,weight=700)+text(63,386,"Last acknowledged by Glimpse",13,"#365B5D")+text(63,413,"Device may have changed since",13,"#944B3E",700)+'''
'''+rect(39,476,352,100,"#F1EBDE")+text(54,502,"SELECTED FOR PREVIEW · RAINBOWS",12,"#1D5960",700)+'''
<clipPath id="selected-preview"><rect x="53" y="512" width="75" height="50" rx="6"/></clipPath>
<g clip-path="url(#selected-preview)">
<image x="53" y="512" width="75" height="50" preserveAspectRatio="xMidYMid slice" href="'''+uri+'''"/></g>
'''+text(144,535,"p12 selected, not applied",15,weight=700)+text(144,555,"Browsing changes no wallpaper",12,"#596F6E")+'''
'''+rect(39,592,352,61,"#EDF0EB")+text(54,619,"Want to verify what Android displays?",13,weight=700)+text(54,639,"Inspect the system wallpaper",13,"#465F60")+'''
'''+button(678,"Open Rainbows · preview p12",True)+button(745,"View wallpaper activity")
    side=text(464,164,"Separate three state variables",18,"#1D5960",700)
    for i,t in enumerate([
        "Last Glimpse OS acknowledgement: Home p04",
        "Current Android wallpaper: unknown without read-back",
        "Browser selection: Rainbows p12, explicitly preview only",
        "No pending apply and no claim p04 persists currently",
        "Open Rainbows returns to the same selected p12",
        "No wallpaper apply when entering collection or viewing history"
    ]):side+=text(464,210+i*39,t,15)
    side+=text(464,531,"Evidence still missing",18,"#1D5960",700)
    for i,t in enumerate([
        "Native Home hierarchy and actual wallpaper OS state",
        "Cross-app wallpaper mutation and process-restart behavior",
        "Scaled 200% text and native focus order",
        "Owner-firsthand understanding of this last-ack label"
    ]):side+=text(464,575+i*37,t,15)
    side+=text(465,839,"Phone pixels are proposed UI, NOT device observation.",14,"#944B3E",700)
    return common("R03 / Honest Home acknowledgement","p04 last acknowledgement and p12 preview are visibly independent.",meta,phone,side)

def recovery():
    uri,hash_=photo_fixture()
    src=R08.read_bytes()
    meta={"auditLane":"R08","stateId":"restore-skip-return-context",
      "surfaceId":"collections","collectionId":"Rainbows",
      "selectedAssetId":"p12","lastConfirmedAppliedAssetId":"p04",
      "originRoute":"backup-and-restore/conflict-choice",
      "returnRoute":"collections/Rainbows",
      "resolution":"skip-item-review-later","conflictUnresolved":True,
      "restoreCommitted":False,"importedCopies":0,
      "wallpaperMutation":False,"originalBytesPreserved":True,
      "sourceConflictSHA256":hashlib.sha256(src).hexdigest(),
      "p12FixtureSHA256":hash_,
      "proposalStatus":"draft-not-owner-approved",
      "nativeVerified":False,"opticallyAccepted":False,"ownerApproved":False}
    phone=text(39,98,"← Rainbows",25,weight=700)+text(39,129,"22 local photos · p12 selected",14,"#596F6E")+'''
'''+rect(39,153,352,111,"#F1EBDE")+text(55,181,"RESTORE REVIEW PAUSED",13,"#944B3E",700)+text(55,213,"1 conflict skipped for later",16,weight=700)+text(55,241,"No import, overwrite or apply performed",12,"#465F60")+'''
'''+rect(39,278,352,294)+'''
<clipPath id="selected-photo"><rect x="52" y="290" width="326" height="212" rx="12"/></clipPath>
<g clip-path="url(#selected-photo)"><image x="52" y="290" width="326" height="212" preserveAspectRatio="xMidYMid slice" href="'''+uri+'''"/></g>
'''+text(54,530,"SELECTED PHOTO · p12",16,weight=700)+text(54,554,"Original local image still available",13,"#465F60")+'''
'''+rect(39,590,352,68,"#EDF0EB")+text(54,617,"Review remains available under Backup",14,weight=700)+text(54,640,"Return here without losing collection focus",12,"#596F6E")+'''
'''+button(680,"Browse selected photo p12",True)+button(747,"Review skipped restore item")
    side=text(465,163,"What Back and Skip mean",18,"#1D5960",700)
    for i,t in enumerate([
      "The selected p12 and collection Rainbows are restored in UI",
      "One conflict remains unresolved and reviewable",
      "No copy import, replacement, deletion or wallpaper mutation",
      "Navigating Back does not silently commit restore",
      "Skip from R08 conflict-choice is not X19 success result",
      "A separate explicit commit yields X19 restore-result"
    ]):side+=text(465,206+i*41,t,15)
    side+=text(465,522,"Proof boundaries",18,"#1D5960",700)
    for i,t in enumerate([
      "Actual persisted restore journal and interrupted writes",
      "Back stack, rotation/process-death focus and stable p12 bytes",
      "TalkBack returning-focus position and 200% font flow"
    ]):side+=text(465,567+i*40,t,15)
    side+=text(465,841,"Draft reference only · no Android/human signoff",14,"#944B3E",700)
    return common("R08 / Restore Skip returns to Rainbows",
       "Restore review remains pending; back returns to original collection selection.",meta,phone,side)

def main():
    ap=argparse.ArgumentParser()
    ap.add_argument("--write",action="store_true")
    args=ap.parse_args()
    cases={"r03-home-last-ack-p04.svg":ack(),"r08-skip-return-rainbows.svg":recovery()}
    for file,source in cases.items():
        path=OUT/file
        if args.write:path.write_text(source,encoding="utf-8")
        else:assert path.read_text(encoding="utf-8")==source,path
        print(json.dumps({"source":str(path.relative_to(ROOT)),
                          "sha256":hashlib.sha256(source.encode()).hexdigest(),
                          "approved":False}))
if __name__=="__main__":
    main()
