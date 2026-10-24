#!/usr/bin/env python3
"""Source-exact current-selection primary-screen contact sheets (draft raster, not approval)."""
from __future__ import annotations

import argparse
import hashlib
import io
import json
from pathlib import Path
import xml.etree.ElementTree as ET

ROOT=Path(__file__).resolve().parents[1]
REGISTRY=ROOT/"design/proposals/whole-product-review-selection.v1.json"
OUT=ROOT/"design/reviews/primary-2026-10-24"


def sources():
    d=json.loads(REGISTRY.read_text(encoding="utf-8"))
    rows=[]
    for item in d["primarySurfaces"]:
        rel=item["reviewCandidatePath"]
        file=ROOT/rel
        data=file.read_bytes()
        root=ET.fromstring(data)
        assert root.attrib.get("viewBox")=="0 0 1160 920",rel
        assert rel.startswith("design/proposals/") and rel.endswith(".svg")
        assert item["ownerChoice"] is None and item["exactOwnerApprovalSHA256"] is None
        rows.append((item["surfaceId"],rel,hashlib.sha256(data).hexdigest()))
    assert len(rows)==24 and len(set(x[1] for x in rows))==24
    return rows


def render():
    from PIL import Image,ImageDraw,ImageFont
    from cairosvg import svg2png
    rows=sources()
    OUT.mkdir(exist_ok=True,parents=True)
    font=ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",14)
    sheets=[]
    annotated=[]
    for start in range(0,24,6):
        images=rows[start:start+6]
        pad=14; top=46; sx=415; sy=949
        panel=Image.new("RGB",(3*401+4*pad,2*sy+pad),"#ede7dc")
        draw=ImageDraw.Draw(panel)
        for i,(name,rel,sha) in enumerate(images):
            png=svg2png(bytestring=(ROOT/rel).read_bytes(),output_width=1160,output_height=920)
            with Image.open(io.BytesIO(png)) as raw:
                phone=raw.convert("RGB").crop((15,14,416,904))
            assert phone.size==(401,890)
            x=pad+(i%3)*sx; y=top+(i//3)*sy
            panel.paste(phone,(x,y))
            draw.text((x,y-23),f"{start+i+1:02d}. {name}",font=font,fill="#153f46")
            annotated.append({"surfaceId":name,"path":rel,"sourceSHA256":sha,
                "phonePixels":[401,890],"rendered":True,
                "agentOpticallyInspected":False,"nativeVerified":False,"ownerApproved":False})
        path=OUT/f"selected-primary-contact-{start//6+1:02d}.jpg"
        panel.save(path,quality=92,optimize=True)
        sheets.append({"name":path.name,"sha256":hashlib.sha256(path.read_bytes()).hexdigest(),"count":len(images)})
    (OUT/"source-ledger.json").write_text(json.dumps({
        "schemaVersion":1,
        "evidenceType":"SVG_SELECTION_EXACT_SOURCE_RASTER_NO_OPTICAL_INFERENCE",
        "sourceCount":24,"sourceRows":annotated,"sheets":sheets,
        "ownerApproved":False,"nativeVerified":False
    },indent=2)+"\n",encoding="utf-8")
    print("PASS: 24 current primary candidates, four source-hashed 401x890 phone contact sheets; zero approvals")


def main():
    ap=argparse.ArgumentParser()
    ap.add_argument("--check",action="store_true")
    opts=ap.parse_args()
    if opts.check:
        print(f"PASS: {len(sources())}/24 current selection primary paths, roots and source SHA inputs")
    else:
        render()


if __name__=="__main__":
    main()
