#!/usr/bin/env python3
"""Run *real Android Photo Picker selection* against disposable emulator media.

This is not a direct importer test or mocked picker UI. It crosses Android's
actual system Photos sheet, selects a unique synthetic MediaStore JPEG, then
checks the actual production UI and byte-identical private saved original.
Does not set a wallpaper, touch personal photos or claim Google Photos support.
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

ROOT = Path(__file__).resolve().parents[1]
spec = importlib.util.spec_from_file_location("glimpse_picker_base", ROOT / "scripts/capture-android-atlas.py")
base = importlib.util.module_from_spec(spec)
spec.loader.exec_module(base)
APP = "io.github.gamesbyian.glimpse"
FIXTURE = APP + "/.debug.PickerMediaFixtureLauncher"


def visible_nodes():
    return list(ET.fromstring(base.hierarchy()).iter("node"))


def bounds(node):
    numbers = list(map(int, re.findall(r"\d+", node.attrib.get("bounds", ""))))
    return numbers if len(numbers) == 4 else None


def tap_node(node):
    a = bounds(node)
    if not a:
        raise RuntimeError("System control has no coordinates")
    base.adb("shell", "input", "tap", str((a[0] + a[2]) // 2), str((a[1] + a[3]) // 2))
    time.sleep(1.5)


def wait_for_photo_count(n, timeout=32):
    end = time.monotonic() + timeout
    while time.monotonic() < end:
        xml = base.hierarchy()
        if "Photos: " + str(n) in xml and "No images yet." not in xml:
            return xml
        time.sleep(1)
    return None


def main(out):
    base.OUT = out
    out.mkdir(parents=True, exist_ok=True)
    if base.adb("shell", "getprop", "ro.kernel.qemu").strip() != "1":
        raise RuntimeError("REFUSE picker selection fixture on physical handset")
    base.start_fixture(0)
    assert "No images yet." in base.hierarchy(), "Not initially empty"
    base.adb("shell", "am", "start", "-n", FIXTURE)
    record = None
    for i in range(30):
        raw = base.adb("shell", "run-as", APP, "cat", "files/picker-fixture-ready.json", check=False)
        if raw.startswith("{"):
            record = json.loads(raw)
            break
        time.sleep(.5)
    if not record or record.get("status") != "READY":
        raise RuntimeError("Synthetic MediaStore fixture unavailable: " + str(record))
    required = {"sourceUri", "sourceSHA256", "sourceBytes", "sourceMime"}
    assert required.issubset(record), "Missing source identity fields"
    source_data = base.adb("exec-out", "run-as", APP, "cat",
                           "files/picker-fixture-original.jpg", binary=True)
    assert hashlib.sha256(source_data).hexdigest() == record["sourceSHA256"]
    assert len(source_data) == record["sourceBytes"]
    print("Published one synthetic Android MediaStore JPEG:", record["sourceUri"], flush=True)
    base.click_label("Add photos")
    time.sleep(2.0)
    root = visible_nodes()
    packages = {n.attrib.get("package", "") for n in root}
    assert "com.android.providers.media.module" in packages, "Real Android platform Photo Picker not focused"
    base.capture("01-real-picker-with-published-source",
                 "Native Android Photo Picker after publishing one unique synthetic MediaStore JPEG")
    candidates = []
    for node in root:
        d = node.attrib
        a = bounds(node)
        if d.get("package") != "com.android.providers.media.module" or not a:
            continue
        if d.get("clickable") != "true" or a[3] - a[1] < 45 or a[2] - a[0] < 45:
            continue
        if a[1] < 500:
            continue
        desc = (d.get("content-desc", "") + " " + d.get("text", "")).lower()
        rid = d.get("resource-id", "")
        candidates.append({"node": node, "desc": desc, "id": rid, "bounds": a})
    (out / "picker-click-candidates.json").write_text(json.dumps([
        {k: v for k, v in candidate.items() if k != "node"}
        for candidate in candidates
    ], indent=2) + "\n")
    # The isolated emulator contains ONE published picture. Refuse guessing
    # among unknown controls or selecting media on a non-emulator.
    likely = [x for x in candidates if any(v in (x["id"] + " " + x["desc"]).lower()
                for v in ("photo", "thumbnail", "image", "media_grid", "item"))]
    if len(likely) != 1:
        raise RuntimeError("Cannot identify exactly one source thumbnail in system picker: " +
                           json.dumps([{k:v for k,v in x.items() if k != "node"} for x in candidates])[:1200])
    tap_node(likely[0]["node"])
    base.capture("02-real-picker-selected",
                 "Actual system photo tile selected, before final grant/confirm")
    if not wait_for_photo_count(1, timeout=3):
        controls = visible_nodes()
        # Android's native multi-select picker exposes both "View selected"
        # and the actual commit button "Add (1)". Never treat the former as
        # a grant/confirmation or choose between the two arbitrarily.
        accept = [node for node in controls
                  if node.attrib.get("clickable") == "true"
                  and node.attrib.get("resource-id") ==
                      "com.android.providers.media.module:id/button_add"
                  and re.fullmatch(r"Add \\(1\\)", node.attrib.get("text", ""))]
        if len(accept) != 1:
            raise RuntimeError("Cannot identify one Android picker confirm action: " +
                               str([(n.attrib.get("text"), n.attrib.get("content-desc"))
                                    for n in accept]))
        tap_node(accept[0])
    ui = wait_for_photo_count(1)
    if not ui:
        raise RuntimeError("Actual production MainActivity did not import the selected photo")
    base.capture("03-glimpse-after-real-picker-import",
                 "Production Glimpse displays one source selected via Android Photo Picker")
    copied = base.adb("exec-out", "run-as", APP, "cat",
                      "files/originals/" + record["sourceSHA256"] + ".jpg",
                      binary=True)
    assert copied == source_data, "Private offline JPEG no longer byte-identical to selected source"
    assert hashlib.sha256(copied).hexdigest() == record["sourceSHA256"]
    assert "Previewing 1 of 1" in ui or "Photos: 1" in ui
    assert "No images yet." not in ui
    receipt = {
        "sourceIdentity": record["sourceSHA256"],
        "sourceUriScheme": "content",
        "sourceProvider": "Android MediaStore via OS Photo Picker",
        "sourceBytes": record["sourceBytes"],
        "sourceEqualsPrivateCopy": True,
        "pickerPackageObserved": "com.android.providers.media.module",
        "uiImportedPhotoCount": 1,
        "noWallpaperActionTaken": True,
        "physicalHandset": False,
        "googlePhotosAlbumNotVerified": True,
        "ownerVisualApproval": False,
        "evidenceScreenshots": [x["id"] for x in base.RESULTS],
    }
    (out / "picker-selection-proof.json").write_text(json.dumps(receipt,indent=2)+"\n")
    base.write_gallery()
    print("PASS: real Android picker selection, Glimpse UI Photos: 1, exact offline JPEG SHA "+record["sourceSHA256"], flush=True)


if __name__ == "__main__":
    ap=argparse.ArgumentParser()
    ap.add_argument("--out",default="artifacts/agent-c-real-picker-import")
    args=ap.parse_args()
    out=Path(args.out).resolve()
    try:
        main(out)
    except Exception as exc:
        print("FAIL: actual Android Photo Picker import:",str(exc),file=sys.stderr,flush=True)
        out.mkdir(parents=True,exist_ok=True)
        base.OUT=out
        base.capture_failure("actual-picker-selection-failure",str(exc))
        (out/"selection-failure.txt").write_text(str(exc)[:4000])
        try: base.write_gallery()
        except Exception: pass
        raise SystemExit(1)
