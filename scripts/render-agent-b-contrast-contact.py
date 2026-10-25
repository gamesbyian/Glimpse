#!/usr/bin/env python3
"""Source-hashed optical contrast review after focused B text repairs.

Six representative 401x890 phone renderings spanning pale lavender, warm
error, beige, and reel marker cases. Static raster source is not a device,
TalkBack, text reflow, content ownership, or human acceptance claim.
"""
from __future__ import annotations
import hashlib
import io
import json
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont
from cairosvg import svg2png

ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/"design/reviews/support-agent-b-2026-10-25"
CASES=[
 "design/proposals/storyboards/x01/01-collection-entry.svg",
 "design/proposals/storyboards/x01/05-partial-recovery.svg",
 "design/proposals/storyboards/x02/06-empty-after-removal.svg",
 "design/proposals/storyboards/x06/03-three.svg",
 "design/proposals/storyboards/x08/03-reel-hero.svg",
 "design/proposals/storyboards/x09/07-error-compact.svg",
]
def sha(b:bytes)->str:
    return hashlib.sha256(b).hexdigest()
def main():
    OUT.mkdir(parents=True,exist_ok=True)
    width=3*401+4*13
    height=2*928+3*13
    img=Image.new("RGB",(width,height),"#eee7dd")
    draw=ImageDraw.Draw(img)
    try:font=ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",13)
    except OSError:font=ImageFont.load_default()
    ledger=[]
    for i,name in enumerate(CASES):
        raw=(ROOT/name).read_bytes()
        png=svg2png(bytestring=raw,output_width=1160,output_height=920)
        with Image.open(io.BytesIO(png)) as pic:
            phone=pic.crop((15,14,416,904)).convert("RGB")
        assert phone.size==(401,890)
        x=13+(i%3)*414
        y=13+(i//3)*941
        img.paste(phone,(x,y+25))
        draw.text((x,y),f"{i+1:02d} {name.split('/')[-1]}",font=font,fill="#153f46")
        ledger.append({"path":name,"sourceSHA256":sha(raw),"crop":[15,14,416,904],
                       "phone":[401,890],"independentlyOpticallyReviewed":False,
                       "nativeVerified":False,"ownerApproved":False})
    jpg=OUT/"contrast-repaired-6-phones.jpg"
    img.save(jpg,quality=93,optimize=True)
    record={"status":"UNAPPROVED_RASTER_CONTRAST_STUDY",
            "totalPhones":len(ledger),"sources":ledger,
            "contact":"contrast-repaired-6-phones.jpg",
            "contactSHA256":sha(jpg.read_bytes()),
            "limitations":"6 direct review candidates, not all 137 inspected after this color pass; nominal SVG crop, no actual Android rendering, source assets unchanged."}
    (OUT/"contrast-repaired-6-phones.json").write_text(
        json.dumps(record,indent=2,ensure_ascii=False)+"\n",encoding="utf8")
    print("PASS: 6 source-SHA-bound phone crops; no native or human acceptance")
if __name__=="__main__":main()
