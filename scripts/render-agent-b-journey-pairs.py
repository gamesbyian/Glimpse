#!/usr/bin/env python3
"""23 connected-journey optical challenge sheets with exact source hashes.

One chosen before/after or contrasting branch pair per P0/P1 storyboard group,
rendered as two full 401x890 source-equivalent phones. Drawing pairs are
illustrative; cannot prove behavior, Android focus, native timing or approval.
"""
from __future__ import annotations

import hashlib
import io
import json
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont
from cairosvg import svg2png

ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/"design/reviews/support-agent-b-2026-10-24"
P0="design/proposals/storyboard-coverage.v1.json"
P1="design/proposals/advanced-storyboard-coverage.v1.json"
# Deliberately include non-linear contrasts; do not treat every frame as next.
PAIR=[
 ("x01","04-copy-progress","06-import-result","sequential-checkpoint"),
 ("x02","05-delete-confirm","06-empty-after-removal","sequential-checkpoint"),
 ("x03","04-acknowledged","05-uncertain","alternative-outcomes"),
 ("x04","04-delayed","05-paused","alternative-states"),
 ("x05","07-interrupted","08-last","distinct-gesture-states"),
 ("x06","03-three","05-twenty-two","different-dataset-sizes"),
 ("x07","01-reel-focus","03-photo-detail","same-photo-identity"),
 ("x08","01-home-active","06-status-collapsed","same-family-variant"),
 ("x09","01-home-200-text","07-error-compact","large-type-variants"),
 ("x10","01-cloud-pending","02-uri-revoked","interrupted-import-branch"),
 ("x11","04-save-only","06-use-now","edit-does-not-auto-apply"),
 ("x12","01-reel-touch-regions","04-talkback-focus","pointer-vs-focus-studies"),
 ("x13","03-chooser","04-cancel","chooser-return-uncertain"),
 ("x14","02-lock","04-unsupported","target-capability-branches"),
 ("x15","04-confirm-exclude","05-restore","explicit-undo"),
 ("x16","02-conflict","05-simulate","rule-conflict-testing"),
 ("x17","03-image-licence","04-offline","rights-vs-network-branches"),
 ("x18","02-unknown","04-retry","unknown-outcome-guard"),
 ("x19","04-restore-conflict","05-restore-result","MISSING-EXPLICIT-CONFIRM-BEFORE-RESULT"),
 ("x20","01-launcher-widget","05-unavailable","capability-branches"),
 ("x21","04-report","05-return","help-report-return"),
 ("x22","03-consent","04-chooser","MISSING-PAYLOAD-REVIEW-AFTER-CONSENT"),
 ("x23","04-duplicates","05-batch-error","asynchronous-error-branches"),
]
def main():
    a=json.loads((ROOT/P0).read_text())
    b=json.loads((ROOT/P1).read_text())
    groups={g["id"]:g for g in a["groups"]+b["groups"]}
    assert len(groups)==len(PAIR)==23
    assert set(groups)=={x[0] for x in PAIR}
    OUT.mkdir(parents=True,exist_ok=True)
    try:font=ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",13)
    except OSError:font=ImageFont.load_default()
    summary=[]
    images=[]
    for page,start in enumerate(range(0,len(PAIR),4),1):
        subset=PAIR[start:start+4]
        pad=14
        h=930
        contact=Image.new("RGB",(2*401+3*pad,len(subset)*h+pad),"#ede7dc")
        draw=ImageDraw.Draw(contact)
        for j,(groupId,leftId,rightId,kind) in enumerate(subset):
            group=groups[groupId]
            frameById={f["id"]:f for f in group["frames"]}
            assert leftId in frameById and rightId in frameById,(groupId,leftId,rightId)
            hashes=[]
            for col,id_ in enumerate((leftId,rightId)):
                frame=frameById[id_]
                path=ROOT/frame["path"]
                raw=path.read_bytes()
                sha=hashlib.sha256(raw).hexdigest()
                with Image.open(io.BytesIO(svg2png(bytestring=raw,output_width=1160,output_height=920))) as png:
                    phone=png.crop((15,14,416,904)).convert("RGB")
                assert phone.size==(401,890)
                x=pad+col*(401+pad)
                y=39+j*h
                contact.paste(phone,(x,y))
                draw.text((x,y-26),f"{groupId.upper()} {id_}",fill="#153F46",font=font)
                hashes.append({"frameId":id_,"path":frame["path"],"sha256":sha,
                               "crop":[15,14,416,904],"pixels":[401,890]})
            summary.append({"group":groupId,"title":group["title"],"pairKind":kind,
                            "frames":hashes,"opticalReviewPerformedByGenerator":False,
                            "androidVerified":False,"ownerApproved":False})
        filename=f"journey-pair-contact-{page:02}.jpg"
        file=OUT/filename
        contact.save(file,format="JPEG",quality=91,optimize=True)
        images.append({"name":filename,"sha256":hashlib.sha256(file.read_bytes()).hexdigest(),
                       "pairs":len(subset)})
    (OUT/"journey-pair-source-ledger.json").write_text(json.dumps({
        "schemaVersion":1,"type":"SOURCE_EQUIVALENT_TWO_PHONE_TRANSITION_STUDIES",
        "pairs":len(summary),"frames":2*len(summary),"groups":summary,"contacts":images,
        "warning":"Branch comparisons are labelled as such, not fictitiously sequential. Optical/native/owner evidence is independent."
    },indent=2)+"\n")
    print(f"PASS: {len(summary)} group pairs / 46 source-matched 401x890 phones / 6 optical contact sheets; no automatic optical approval")
if __name__=="__main__": main()
