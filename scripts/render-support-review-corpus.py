#!/usr/bin/env python3
"""Build source-hashed support-state phone contacts, without inferring UX approval.

Exactly the 34 original support variants, not 34 current-generation optical
passes. Each PNG is rendered at 1160x920 then cropped to real phone coordinates
(15,14)-(416,904). Contact JPEGs are a compact *inspection surface*.
"""
from __future__ import annotations

import argparse
import hashlib
import io
import json
from pathlib import Path
import xml.etree.ElementTree as ET

ROOT=Path(__file__).resolve().parents[1]
MANIFEST=ROOT/"design/proposals/support-coverage.v1.json"
OUT=ROOT/"design/reviews/support-2026-10-24"
NS="{http://www.w3.org/2000/svg}svg"

def source_list():
    d=json.loads(MANIFEST.read_text(encoding="utf-8"))
    sources=[]
    for family in d["screens"]:
        for state in family["states"]:
            rel=f"design/proposals/support-plates/{family['id']}--{state}.svg"
            f=ROOT/rel
            assert f.is_file(),rel
            root=ET.parse(f).getroot()
            assert root.tag==NS and root.get("viewBox")=="0 0 1160 920",rel
            sources.append((family["id"],state,rel,hashlib.sha256(f.read_bytes()).hexdigest()))
    assert len(sources)==34
    assert len({x[2] for x in sources})==34
    return sources

def render():
    from PIL import Image,ImageDraw,ImageFont,ImageStat
    from cairosvg import svg2png
    sources=source_list()
    OUT.mkdir(parents=True,exist_ok=True)
    try:font=ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",14)
    except OSError:font=ImageFont.load_default()
    rows=[]
    page_files=[]
    for start in range(0,len(sources),6):
        chunk=sources[start:start+6]
        pad=14; top=47; step_x=401+pad; step_y=890+59
        panel=Image.new("RGB",(3*401+4*pad,2*step_y+pad),"#ede7dc")
        d=ImageDraw.Draw(panel)
        for i,(family,state,rel,digest) in enumerate(chunk):
            x=pad+(i%3)*step_x
            y=top+(i//3)*step_y
            raw=svg2png(bytestring=(ROOT/rel).read_bytes(),output_width=1160,output_height=920)
            with Image.open(io.BytesIO(raw)) as img:
                phone=img.crop((15,14,416,904)).convert("RGB")
            assert phone.size==(401,890)
            assert sum(ImageStat.Stat(phone).stddev)>35,("blank",rel)
            panel.paste(phone,(x,y))
            title=f"{start+i+1:02d}. {family} / {state}"
            d.text((x,y-24),title,fill="#153f46",font=font)
            rows.append({"family":family,"state":state,"source":rel,"sourceSHA256":digest,
                 "phoneCrop":[15,14,416,904],
                 "phonePixels":[401,890],
                 "automatedRenderPassed":True,
                 "independentlyOpticallyReviewed":False,
                 "nativeAndroidVerified":False,"ownerApproved":False})
        name=f"support-phone-contact-{start//6+1:02d}.jpg"
        path=OUT/name
        panel.save(path,format="JPEG",quality=91,optimize=True)
        page_files.append({"name":name,"SHA256":hashlib.sha256(path.read_bytes()).hexdigest(),
                           "sourceCount":len(chunk)})
    ledger={"schemaVersion":1,"evidenceType":"SVG_SOURCE_EQUIVALENT_DRAFT_RASTER",
            "total":34,"reviewStatus":"CONTACT_GENERATED_OPTICAL_REVIEW_NOT_AUTOMATIC",
            "sources":rows,"contacts":page_files}
    (OUT/"source-ledger.json").write_text(json.dumps(ledger,indent=2)+"\n",encoding="utf-8")
    print("PASS: 34 original support-state sources traced and rasterized into 6 phone-sized contact JPEGs; no optical/native/owner approval")

def main():
    a=argparse.ArgumentParser()
    a.add_argument("--check",action="store_true")
    args=a.parse_args()
    if args.check:
        sources=source_list()
        print(f"PASS: {len(sources)} exact required support SVG paths, viewBox and hash inputs")
    else:render()
if __name__=="__main__":main()
