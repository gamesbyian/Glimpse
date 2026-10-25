#!/usr/bin/env python3
"""Real Android API34 Photo Picker: select TWO distinct synthetic MediaStore photos.

Disposable emulator only. No mock picker, Google Photos cloud login, personal
photo, WallpaperManager action, or human visual approval.
"""
import argparse
import hashlib
import importlib.util
import json
import re
import sys
import time
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
spec=importlib.util.spec_from_file_location("c_batch_atlas",ROOT/"scripts/capture-android-atlas.py")
base=importlib.util.module_from_spec(spec)
spec.loader.exec_module(base)
APP="io.github.gamesbyian.glimpse"
FIXTURE=APP+"/.debug.PickerBatchFixtureLauncher"
PICKER="com.android.providers.media.module"

def nodes():
    return list(ET.fromstring(base.hierarchy()).iter("node"))

def rect(node):
    values=list(map(int,re.findall(r"\d+",node.attrib.get("bounds",""))))
    return values if len(values)==4 else None

def tap(node):
    a=rect(node)
    assert a, "Android clickable tile missing bounds"
    base.adb("shell","input","tap",str((a[0]+a[2])//2),str((a[1]+a[3])//2))
    time.sleep(1.3)

def read_private(name,binary=False):
    if binary:
        return base.adb("exec-out","run-as",APP,"cat","files/"+name,binary=True)
    return base.adb("shell","run-as",APP,"cat","files/"+name,check=False)

def read_json(name):
    for _ in range(40):
        text=read_private(name)
        if text.startswith("{"):return json.loads(text)
        time.sleep(.5)
    raise RuntimeError("Missing debug-only test receipt "+name)

def wait_count(n):
    for _ in range(40):
        ui=base.hierarchy()
        if "Photos: "+str(n) in ui and "No images yet." not in ui:return ui
        time.sleep(.8)
    raise RuntimeError("Production Glimpse UI did not show exact Photos: "+str(n))

def main(out):
    out.mkdir(parents=True,exist_ok=True)
    base.OUT=out
    assert base.adb("shell","getprop","ro.kernel.qemu").strip()=="1", "REFUSE physical handset"
    base.start_fixture(0)
    assert "No images yet." in base.hierarchy(), "Collection not initially empty"
    base.adb("shell","am","start","-n",FIXTURE)
    receipt=read_json("batch-fixture-ready.json")
    assert receipt.get("status")=="READY" and len(receipt.get("photos",[]))==2, str(receipt)
    originals={}
    for photo in receipt["photos"]:
        idx=photo["id"]
        raw=read_private("batch-original-"+str(idx)+".jpg",True)
        assert len(raw)==photo["bytes"] and hashlib.sha256(raw).hexdigest()==photo["sha"]
        originals[idx]=raw
        (out/("batch-published-"+str(idx)+".jpg")).write_bytes(raw)
    assert receipt["photos"][0]["sha"] != receipt["photos"][1]["sha"]
    base.click_label("Add photos")
    time.sleep(2.0)
    visible=nodes()
    assert PICKER in {n.attrib.get("package") for n in visible}, "Native Android OS Photo Picker not present"
    candidates=[]
    for n in visible:
        d=n.attrib;a=rect(n)
        if d.get("package")!=PICKER or d.get("clickable")!="true" or not a:
            continue
        if a[1]<500 or a[2]-a[0]<45 or a[3]-a[1]<45:continue
        desc=(d.get("resource-id","")+" "+d.get("content-desc","")+" "+d.get("text","")).lower()
        if any(v in desc for v in ("photo","thumbnail","image","media_grid","item")):
            candidates.append((a,n))
    candidates.sort(key=lambda x:(x[0][1],x[0][0]))
    assert len(candidates)==2, "Picker must present exactly two synthetic thumbnails; got "+str(len(candidates))
    base.capture("01-two-native-picker-photos","Android system picker with exactly two distinct JPEG sources")
    for _,node in candidates:tap(node)
    control=[n for n in nodes() if n.attrib.get("clickable")=="true"
             and n.attrib.get("resource-id")==PICKER+":id/button_add"
             and n.attrib.get("text")=="Add (2)"]
    assert len(control)==1,"Expected unique real Android Add (2) control"
    base.capture("02-two-photo-picker-selected","Two actual media tiles selected in Android picker")
    tap(control[0])
    wait_count(2)
    base.capture("03-glimpse-has-two-private-photos",
                 "Production Glimpse real two-photo import shows Photos: 2 and finite Reel")
    for photo in receipt["photos"]:
        assert read_private("originals/"+photo["sha"]+".jpg",True)==originals[photo["id"]]
    base.adb("shell","am","start","-n",FIXTURE,"--ez","cleanup","true")
    cleaned=read_json("batch-fixture-cleanup.json")
    assert cleaned.get("deleted")==2 and cleaned.get("onlyOwnSyntheticSources") is True, str(cleaned)
    base.adb("shell","am","force-stop",APP)
    base.adb("shell","am","start","-n",APP+"/.MainActivity")
    wait_count(2)
    for photo in receipt["photos"]:
        idx=photo["id"]
        raw=read_private("originals/"+photo["sha"]+".jpg",True)
        assert raw==originals[idx], "Private bytes differ after MediaStore source removal/restart"
        (out/("batch-private-after-restart-"+str(idx)+".jpg")).write_bytes(raw)
    base.capture("04-two-photos-after-source-delete-and-restart",
                 "Both real Picker imports retained after deleting two public images and restarting Glimpse")
    proof={
        "osPickerPackage":PICKER,"actualSourceCount":2,"actualImportedCount":2,
        "deletedOnlyOwnSources":2,"privateByteParityAfterRestart":True,
        "wallpaperChanged":False,"physicalHandset":False,
        "googlePhotosCloudAlbumVerified":False,"ownerApproved":False,
        "photos":[{"id":p["id"],"sha256":p["sha"],"bytes":p["bytes"]} for p in receipt["photos"]],
        "capturedStates":[x["id"] for x in base.RESULTS]
    }
    (out/"batch-picker-proof.json").write_text(json.dumps(proof,indent=2)+"\n")
    base.write_gallery()
    print("PASS: actual Android Add (2) Photo Picker, two byte-identical independent offline JPEGs, "
          "delete two public MediaStore originals and restart; no wallpaper action",flush=True)

if __name__=="__main__":
    parser=argparse.ArgumentParser()
    parser.add_argument("--out",default="artifacts/agent-c-real-picker-batch")
    args=parser.parse_args()
    out=Path(args.out).resolve()
    try:main(out)
    except Exception as exc:
        print("FAIL: real Android two-image Photo Picker acceptance:",exc,file=sys.stderr,flush=True)
        base.OUT=out
        out.mkdir(parents=True,exist_ok=True)
        base.capture_failure("batch-picker-failure",str(exc))
        (out/"batch-picker-failure.txt").write_text(str(exc)[:3500])
        try:base.write_gallery()
        except Exception:pass
        sys.exit(1)
