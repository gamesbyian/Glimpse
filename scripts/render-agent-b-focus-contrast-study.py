#!/usr/bin/env python3
"""One unselected source-editable Help focus-ring contrast study.

This is a visual proposal, not a native focus system or design-owner choice.
It preserves every string/target and source SVG except the first Help card's
border color, width and explicit static data-focus-visible annotation.
"""
from __future__ import annotations
import hashlib
import importlib.util
import io
import json
import re
from pathlib import Path
from xml.etree import ElementTree as ET
from PIL import Image,ImageDraw,ImageFont
from cairosvg import svg2png

ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/"design/reviews/support-agent-b-2026-10-25"
SRC=ROOT/"design/proposals/support-plates/help--topics.svg"
NEEDLE=re.compile(r'<rect[^>]*data-control="card-read-import-guide"[^>]*/>')
def sha(b:bytes)->str:return hashlib.sha256(b).hexdigest()
def main():
    data=SRC.read_text(encoding="utf-8")
    found=NEEDLE.findall(data)
    assert len(found)==1
    old=found[0]
    assert 'stroke="#C8B7A1" stroke-width="1"' in old
    revised=old.replace('stroke="#C8B7A1" stroke-width="1"',
                        'stroke="#1D5960" stroke-width="4" data-focus-visible="true"')
    changed=data.replace(old,revised,1)
    assert changed!=data and changed.replace(revised,old,1)==data
    ET.fromstring(changed)
    # Explicit, computable paint signal. Native must implement a real focus ring.
    def L(h):
        v=[int(h[i:i+2],16)/255 for i in (1,3,5)]
        t=[x/12.92 if x<=.04045 else ((x+.055)/1.055)**2.4 for x in v]
        return .2126*t[0]+.7152*t[1]+.0722*t[2]
    contrast=lambda a,b: (max(L(a),L(b))+.05)/(min(L(a),L(b))+.05)
    old_ratio=contrast("#C8B7A1","#F6F0E6")
    new_ratio=contrast("#1D5960","#F6F0E6")
    assert old_ratio<3 and new_ratio>=3
    OUT.mkdir(parents=True,exist_ok=True)
    svg=OUT/"unselected-help-focus-ring.v1.svg"
    svg.write_text(changed,encoding="utf-8")
    def phone(raw):
        b=svg2png(bytestring=raw,output_width=1160,output_height=920)
        with Image.open(io.BytesIO(b)) as image:
            return image.crop((15,14,416,904)).convert("RGB")
    before=phone(data.encode());after=phone(changed.encode())
    sheet=Image.new("RGB",(2*401+3*14,945),"#EEE7DD")
    sheet.paste(before,(14,36));sheet.paste(after,(429,36))
    draw=ImageDraw.Draw(sheet)
    try:font=ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",15)
    except OSError:font=ImageFont.load_default()
    draw.text((14,10),"Existing help cards · no keyboard focus drawn",fill="#153F46",font=font)
    draw.text((429,10),"Focused first card · visible 4-unit teal outline",fill="#153F46",font=font)
    contact=OUT/"unselected-help-focus-ring.v1.jpg"
    sheet.save(contact,"JPEG",quality=93,optimize=True)
    report={"status":"UNSELECTED_STATIC_ACCESSIBILITY_STUDY_NOT_NATIVE_OR_OWNER_APPROVAL",
            "source":str(SRC.relative_to(ROOT)),"sourceSHA256":sha(data.encode()),
            "candidate":str(svg.relative_to(ROOT)),"candidateSHA256":sha(svg.read_bytes()),
            "contact":contact.name,"contactSHA256":sha(contact.read_bytes()),
            "control":"card-read-import-guide",
            "defaultOutlineRatio":round(old_ratio,3),"focusedOutlineRatio":round(new_ratio,3),
            "staticTarget":[348,100],
            "limitations":"Demonstrates one proposed visually focused card; neither Android keyboard or TalkBack focus movement, pressed/disabled state nor 48dp native targets verified."}
    (OUT/"unselected-help-focus-ring.v1.json").write_text(json.dumps(report,indent=2)+"\n")
    print("PASS: one exact source variant, 1.0–3.0 baseline vs >=3.0 focus outline, 401x890 paired render")
if __name__=="__main__":main()
