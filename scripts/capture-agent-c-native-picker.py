#!/usr/bin/env python3
"""Safely capture the actual Android Photo Picker / chooser cancellation flow.

Disposable API34 emulator ONLY. Opens native system UI through the running
production MainActivity's Add photos action. Does not select a private photo,
grant media access, call WallpaperManager or claim Google Photos acceptance.
"""
import argparse
import importlib.util
import json
from pathlib import Path
import sys
import time
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
spec = importlib.util.spec_from_file_location("glimpse_c_atlas", ROOT / "scripts/capture-android-atlas.py")
base = importlib.util.module_from_spec(spec)
spec.loader.exec_module(base)
APP = "io.github.gamesbyian.glimpse"


def packages_in_visible_hierarchy():
    root = ET.fromstring(base.hierarchy())
    return sorted({n.attrib.get("package", "") for n in root.iter("node")
                   if n.attrib.get("package")})


def capture_picker(out):
    base.OUT = out
    base.OUT.mkdir(parents=True, exist_ok=True)
    if base.adb("shell", "getprop", "ro.kernel.qemu").strip() != "1":
        raise RuntimeError("REFUSING destructive native Photo Picker fixture on physical hardware")
    base.start_fixture(0)
    empty = base.hierarchy()
    if "No images yet." not in empty:
        raise AssertionError("Could not establish empty collection before picker")
    base.click_label("Add photos")
    time.sleep(1.5)
    packages = packages_in_visible_hierarchy()
    focus_dump = base.adb("shell", "dumpsys", "window", check=False)
    focus_lines = [line.strip() for line in focus_dump.splitlines()
                   if "mCurrentFocus" in line or "mFocusedApp" in line]
    external = sorted(p for p in packages if p not in (APP, "com.android.systemui"))
    base.capture("00-android-picker-or-chooser",
                 "Actual Android native result of tapping Add photos, before any selection")
    base.RESULTS[-1]["capturedFrom"] = ("Actual external Android picker/chooser" if external
                                               else "Unverified UI after Android picker intent")
    base.RESULTS[-1]["visiblePackages"] = packages
    base.RESULTS[-1]["systemPickerOrChooserObserved"] = bool(external)
    (out / "00-android-picker-focus.json").write_text(json.dumps({
        "visiblePackages": packages, "externalPackages": external,
        "focusHints": focus_lines,
        "proofClass": "emulator OS surface; not imported content://, Google Photos or real handset",
    }, indent=2) + "\n")
    # Back out without selecting or accepting a source. Never pretend a chooser
    # result is proof of delivery or of URI persistence.
    restored = False
    for _ in range(3):
        base.adb("shell", "input", "keyevent", "KEYCODE_BACK", check=False)
        time.sleep(0.8)
        if "No images yet." in base.hierarchy():
            restored = True
            break
    if not restored:
        raise AssertionError("Picker cancellation did not return to unchanged empty Glimpse")
    base.capture("00-after-picker-cancel",
                 "Production Glimpse after backing out of system picker, still zero photos")
    base.RESULTS[-1]["capturedFrom"] = "Production MainActivity, canceled external picker"
    record = {
        "sourceSha": "local/check GitHub Actions run HEAD",
        "evidenceKind": "actual Android API34 emulator system picker invocation and cancel",
        "systemPickerOrChooserObserved": bool(external),
        "nativeOSPackages": external,
        "returnedToEmptyCollection": restored,
        "notVerified": ["Google Photos integration", "real personal content URI lifetime",
                         "TalkBack announcement", "app sharing intent delivery",
                         "current Home wallpaper", "human design approval"],
        "captures": base.RESULTS
    }
    (out / "agent-c-picker-manifest.json").write_text(json.dumps(record,indent=2)+"\n")
    base.write_gallery()
    if not external:
        raise AssertionError(
            "No external Android Photo Picker/chooser package could be verified; "
            "artifacts are recorded as unverified, not evidence of integration"
        )
    print("PASS: Actual Android picker/chooser surface captured and canceled; ",
          "external package(s):", external)
    return 0


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--out", default="artifacts/agent-c-native-picker")
    args = parser.parse_args()
    try:
        sys.exit(capture_picker(Path(args.out).resolve()))
    except Exception as exc:
        print("FAIL: OS photo picker emulator evidence:", exc, file=sys.stderr)
        sys.exit(1)
