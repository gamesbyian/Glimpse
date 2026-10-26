#!/usr/bin/env python3
"""Visual challenge fixture for static 320dp and enlarged SVG text.

The stressed mockup deliberately does NOT perform Android Compose reflow,
fontScale, safe-inset handling or TalkBack testing. Collisions are risk signals.
"""
from __future__ import annotations
import hashlib
import io
import json
from pathlib import Path
import re
from PIL import Image,ImageDraw,ImageFont
from cairosvg import svg2png

ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/"design/reviews/support-agent-b-2026-10-24"
CASES=[
 "help--topics",
 "report-problem--with-diagnostics",
 "diagnostic-preview--expanded",
 "storage-management--delete-confirm",
 "beta-feedback--share-preview",
 "external-handler-fallback--chooser-cancelled",
]
FONT_RE=re.compile(r'(<text\s+[^>]*?x="(?P<x>[0-9.]+)"[^>]*?font-size=")(?P<size>[0-9.]+)(")')
def dynamic_stress(source:str)->str:
    left,div,right=source.partition('<rect x="440"')
    if not div:raise ValueError("missing annotations divider")
    def bump(m):
        if float(m.group('x'))>=416:return m.group(0)
        return m.group(1)+str(round(float(m.group('size'))*2,2))+m.group(4)
    return FONT_RE.sub(bump,left)+div+right
def crop(raw:bytes)->Image.Image:
    png=svg2png(bytestring=raw,output_width=1160,output_height=920)
    with Image.open(io.BytesIO(png)) as image:
        original=image.crop((15,14,416,904)).convert('RGB')
    return original.resize((320,710),Image.Resampling.LANCZOS)
def main():
    OUT.mkdir(parents=True,exist_ok=True)
    try:font=ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",13)
    except OSError:font=ImageFont.load_default()
    panel=Image.new("RGB",(3*340+10,4*754+40),"#ece6da")
    draw=ImageDraw.Draw(panel)
    rows=[]
    for i,name in enumerate(CASES):
        path=ROOT/"design/proposals/support-plates"/(name+".svg")
        raw=path.read_bytes()
        standard=crop(raw)
        stressed=crop(dynamic_stress(raw.decode("utf8")).encode("utf8"))
        for column,image in ((0,standard),(1,stressed)):
            xx=10+(i%3)*340
            yy=36+(i//3)*1508+column*754
            panel.paste(image,(xx,yy))
            label=f"{i+1:02d} {name} / {'STATIC 320' if column==0 else '2x SOURCE FONT · NOT NATIVE'}"
            draw.text((xx,yy-23),label,fill="#153F46",font=font)
        rows.append({"source":str(path.relative_to(ROOT)),
                     "sourceSHA256":hashlib.sha256(raw).hexdigest(),
                     "scaledPhonePixels":[320,710],
                     "modes":["simple 401-to-320 uniform rescale","font-size attr doubled without reflow"],
                     "nativeAndroidEvidence":False,"ownerApproved":False})
    file=OUT/"compact-fontscale-stress.jpg"
    panel.save(file,format="JPEG",quality=92,optimize=True)
    ledger={"schemaVersion":1,"kind":"STATIC_RISK_SIMULATION_NOT_NATIVE_ANDROID",
            "cases":rows,"imageSHA256":hashlib.sha256(file.read_bytes()).hexdigest(),
            "warning":"No physical dp/insets, dynamic font reflow, OS focus, TalkBack or device result; visual redlines only"}
    (OUT/"compact-fontscale-stress.json").write_text(json.dumps(ledger,indent=2)+"\n")
    print("PASS: six static compact/2x-type visual risk probes rendered, no device acceptance")
if __name__=="__main__":main()
