#!/usr/bin/env python3
"""Render all 137 P0/P1 source-editable storyboard phones, not just sample pairs.

Group-exact and SHA-indexed contact sheets support real optical inspection.
Raster production itself does NOT automatically mark any screen as inspected,
native-tested, human-approved, or a valid depiction of an OS system screen.
"""
from __future__ import annotations

import hashlib
import io
import json
import math
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont
from cairosvg import svg2png

ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/"design/reviews/support-agent-b-2026-10-24/full-journey-corpus"
SOURCES=["design/proposals/storyboard-coverage.v1.json",
         "design/proposals/advanced-storyboard-coverage.v1.json"]
PHONE_CROP=[15,14,416,904]
PHONE_SIZE=[401,890]
def digest(p:Path)->str:
    return hashlib.sha256(p.read_bytes()).hexdigest()
def main():
    groups=[]
    for path in SOURCES:
        groups.extend(json.loads((ROOT/path).read_text(encoding="utf-8"))["groups"])
    assert len(groups)==23 and len({g["id"] for g in groups})==23
    OUT.mkdir(parents=True,exist_ok=True)
    try: font=ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",15)
    except OSError:font=ImageFont.load_default()
    ledger=[]
    seen=set()
    for group in groups:
        frames=group["frames"]
        n=len(frames)
        cols=min(n,3)
        rows=math.ceil(n/cols)
        pad=12; card_w=401; card_h=922
        canvas=Image.new("RGB",(cols*card_w+(cols+1)*pad,rows*card_h+(rows+1)*pad),
                         "#EEE7DD")
        draw=ImageDraw.Draw(canvas)
        members=[]
        for i,f in enumerate(frames):
            path=ROOT/f["path"]
            assert path.is_file() and f["path"] not in seen
            seen.add(f["path"])
            assert f["ordinal"]==i+1
            raw=path.read_bytes()
            with Image.open(io.BytesIO(svg2png(bytestring=raw,output_width=1160,
                                               output_height=920))) as png:
                phone=png.crop(tuple(PHONE_CROP)).convert("RGB")
                assert list(phone.size)==PHONE_SIZE
            x=pad+(i%cols)*(card_w+pad)
            y=pad+(i//cols)*(card_h+pad)
            draw.text((x,y+3),f"{group['id'].upper()} {f['id']}",fill="#153F46",font=font)
            canvas.paste(phone,(x,y+26))
            members.append({"ordinal":f["ordinal"],"id":f["id"],"source":f["path"],
                            "sha256":digest(path),
                            "crop":PHONE_CROP,"pixels":PHONE_SIZE})
        name=f"{group['id']}-all-{n:02d}-phones.jpg"
        canvas.save(OUT/name,quality=92,optimize=True,format="JPEG")
        ledger.append({"group":group["id"],"title":group["title"],"count":n,
                       "contact":name,"contactSHA256":digest(OUT/name),
                       "frames":members,
                       "generatedOnly":True,"independentlyOpticallyInspected":False,
                       "nativeAndroidVerified":False,"ownerApproved":False})
    assert len(seen)==137
    report={"schemaVersion":1,"kind":"B_UNAPPROVED_FULL_STORYBOARD_OPTICAL_CHALLENGE",
            "sources":SOURCES,"groups":ledger,
            "countGroups":len(ledger),"countUniquePhones":len(seen),
            "contactCrop":PHONE_CROP,"phonePixels":PHONE_SIZE,
            "limitations":"CairoSVG output only; direct optical inspection is a separate manual event. Not native Android or human acceptance."}
    (OUT/"full-journey-source-ledger.json").write_text(
        json.dumps(report,indent=2,ensure_ascii=False)+"\n",encoding="utf-8")
    print(f"PASS: {len(ledger)} group contact sheets / {len(seen)} exact source-hashed 401x890 phones, not optical acceptance")
if __name__=="__main__":main()
