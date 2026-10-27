#!/usr/bin/env python3
"""Independent raw evidence check of two actual Android OS Picker imports.

Runs after the device harness. Checks 2 distinct original JPEG artifact bytes
against 2 *separate* persisted private images after MediaStore delete + restart,
and parses every actual screenshot/UIAutomator hierarchy. No user photo, cloud
album, physical device, wallpaper or human acceptance is inferred.
"""
import argparse
import hashlib
import json
import re
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

PICKER="com.android.providers.media.module"
APP="io.github.gamesbyian.glimpse"
STAGES=[
    "01-two-native-picker-photos",
    "02-two-photo-picker-selected",
    "03-glimpse-has-two-private-photos",
    "04-two-photos-after-source-delete-and-restart",
]

def require(condition,reason):
    if not condition: raise ValueError(reason)

def validate(root):
    record=json.loads((root/"batch-picker-proof.json").read_text())
    require(record.get("actualSourceCount")==2 and record.get("actualImportedCount")==2
            and record.get("deletedOnlyOwnSources")==2 and
            record.get("privateByteParityAfterRestart") is True,
            "Batch source/import/delete/durable copy not confirmed")
    require(record.get("osPickerPackage")==PICKER,
            "External Android system Photo Picker not confirmed")
    require(record.get("wallpaperChanged") is False and
            record.get("physicalHandset") is False and
            record.get("googlePhotosCloudAlbumVerified") is False and
            record.get("ownerApproved") is False,
            "Evidence exaggerates scope or lacks fail-closed caveats")
    photos=record["photos"]
    require(len(photos)==2 and {p["id"] for p in photos}=={0,1},
            "Exactly two stable independent media sources required")
    checksums=[]
    for photo in photos:
        index=photo["id"]
        a=(root/f"batch-published-{index}.jpg").read_bytes()
        b=(root/f"batch-private-after-restart-{index}.jpg").read_bytes()
        digest=hashlib.sha256(a).hexdigest()
        require(a[:3]==bytes([255,216,255]) and len(a)>15000
                and a==b and len(a)==photo["bytes"]
                and re.fullmatch(r"[0-9a-f]{64}",photo["sha256"]) and
                digest==photo["sha256"],
                f"Original/private byte parity missing for independent {index}")
        checksums.append(digest)
    require(checksums[0]!=checksums[1],"Two photo IDs alias one JPEG")
    require(record.get("capturedStates")==STAGES,"Four native state identifiers changed")
    for idx,stage in enumerate(STAGES):
        png=(root/(stage+".png")).read_bytes()
        require(png[:8]==bytes([137,80,78,71,13,10,26,10]) and len(png)>15000,
                "Missing actual Android PNG "+stage)
        hierarchy=ET.parse(root/(stage+".xml")).getroot()
        nodes=list(hierarchy.iter("node"))
        packages={n.attrib.get("package") for n in nodes}
        if idx<2:
            require(PICKER in packages,"Android external OS Picker hierarchy missing: "+stage)
            if idx==1:
                require(any(n.attrib.get("text")=="Add (2)" for n in nodes),
                        "Native multi-select Add (2) not visible before confirmation")
        else:
            require(APP in packages,"Native Glimpse hierarchy missing: "+stage)
            require(any("Photos: 2" in (n.attrib.get("text") or "")
                        for n in nodes),"Actual app photo count is not 2: "+stage)
    print("PASS: two independently hashed real OS Picker selection photos, "
          "actual Add (2), real Photos: 2 on device, both private JPEG byte-identical "
          "after deleting both public MediaStore originals and restarting app; "
          "four original Android PNG/XML screens inspected.")
    return 0

if __name__=="__main__":
    p=argparse.ArgumentParser()
    p.add_argument("--artifacts",default="artifacts/agent-c-real-picker-batch")
    args=p.parse_args()
    try: sys.exit(validate(Path(args.artifacts)))
    except (ValueError,KeyError,TypeError,OSError,ET.ParseError) as e:
        print("FAIL: independent Android two-photo batch evidence:",e,file=sys.stderr)
        sys.exit(1)
