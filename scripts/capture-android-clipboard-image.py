#!/usr/bin/env python3
"""Verify foreground user-tapped Android clipboard URI import using synthetic media."""
import argparse
import hashlib
import importlib.util
import json
import time
from pathlib import Path

root = Path(__file__).resolve().parents[1]
spec = importlib.util.spec_from_file_location("glimpse_atlas", root / "scripts/capture-android-atlas.py")
base = importlib.util.module_from_spec(spec)
spec.loader.exec_module(base)
APP = "io.github.gamesbyian.glimpse"
PICKER_FIXTURE = APP + "/.debug.PickerMediaFixtureLauncher"
CLIP_FIXTURE = APP + "/.debug.ClipboardImageFixtureLauncher"

def until(fn, seconds=40):
    end = time.monotonic() + seconds
    while time.monotonic() < end:
        result = fn()
        if result:
            return result
        time.sleep(0.9)
    return None

def app_file(name):
    return base.adb("shell","run-as",APP,"cat","files/"+name,check=False)

def set_clipboard(text_only=False):
    base.adb("shell","am","start","-n",CLIP_FIXTURE,
             "--ez","textOnly",str(text_only).lower())
    raw = until(lambda: app_file("clipboard-fixture-ready.json"),seconds=25)
    assert raw and raw.startswith("{"), "Clipboard fixture result missing"
    receipt = json.loads(raw)
    assert receipt["status"] == "READY", receipt
    assert receipt["mode"] == ("TEXT" if text_only else "IMAGE")
    time.sleep(2.2)

def main(out):
    base.OUT = out
    base.RESULTS.clear()
    out.mkdir(parents=True, exist_ok=True)
    if base.adb("shell","getprop","ro.kernel.qemu").strip() != "1":
        raise RuntimeError("REFUSE clipboard fixture on physical hardware")
    base.start_fixture(0)
    assert "No images yet." in base.hierarchy()
    base.adb("shell","am","start","-n",PICKER_FIXTURE)
    raw = until(lambda: app_file("picker-fixture-ready.json"),seconds=25)
    assert raw and raw.startswith("{"), "No published synthetic source"
    source = json.loads(raw)
    assert source["status"] == "READY"
    original = base.adb("exec-out","run-as",APP,"cat",
                        "files/picker-fixture-original.jpg",binary=True)
    sha = hashlib.sha256(original).hexdigest()
    assert sha == source["sourceSHA256"]
    set_clipboard(text_only=True)
    base.click_label("Paste image")
    assert sha+".jpg" not in base.adb("shell","run-as",APP,"ls","files/originals",
                                     check=False), "Text clipboard caused photo import"
    base.capture("01-text-clipboard-rejected",
                 "User-tapped Paste correctly declines a non-image clipboard")
    set_clipboard(text_only=False)
    base.click_label("Paste image")
    imported = until(lambda: base.hierarchy() if "Photos: 1" in base.hierarchy() else None)
    if not imported:
        base.capture_failure("clipboard-import-failed","No image membership after explicit paste")
        raise RuntimeError("Pasted content image did not import")
    assert "No wallpaper change has been acknowledged yet" in imported
    private = base.adb("exec-out","run-as",APP,"cat",
                       "files/originals/"+sha+".jpg",binary=True)
    assert private == original, "Clipboard import did not preserve original bytes"
    base.capture("02-image-clipboard-imported",
                 "Actual synthetic clipboard URI imported via production Paste image button")
    base.adb("shell","am","force-stop",APP)
    base.adb("shell","am","start","-n",APP+"/.MainActivity")
    assert until(lambda: base.hierarchy() if "Photos: 1" in base.hierarchy() else None)
    assert base.adb("exec-out","run-as",APP,"cat",
                    "files/originals/"+sha+".jpg",binary=True) == original
    base.capture("03-clipboard-import-after-restart",
                 "Exact private original survives process restart; clipboard was not persisted")
    (out/"receipt.json").write_text(json.dumps({
        "source":"synthetic Android MediaStore through explicit foreground clipboard read",
        "sourceSha256":sha,"sourceBytes":len(original),"textRejected":True,
        "exactOfflineBytes":True,"restartSurvived":True,"wallpaperApplied":False,
        "physicalHandsetAccepted":False
    },indent=2)+"\n")
    (out/"manifest.json").write_text(json.dumps(base.RESULTS,indent=2)+"\n")
    base.write_gallery()

if __name__ == "__main__":
    ap = argparse.ArgumentParser()
    ap.add_argument("--out",type=Path,default=Path("artifacts/android-clipboard-image"))
    main(ap.parse_args().out.resolve())
