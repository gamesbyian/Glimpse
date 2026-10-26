#!/usr/bin/env python3
"""Agent C's destructive *emulator-only* native fidelity sweep.

Runs the shipped production MainActivity with debug Room photo records;
never draws a replacement fake UI, never sets wallpaper, never uses user photos.
Outputs actual screencaps/hierarchy bounds, NOT acceptance of the reference art.
"""
import argparse
import importlib.util
import json
import os
from pathlib import Path
import sys
import time
import xml.etree.ElementTree as ET
import re

ROOT = Path(__file__).resolve().parents[1]
ATLAS_SCRIPT = ROOT / "scripts/capture-android-atlas.py"
spec = importlib.util.spec_from_file_location("glimpse_native_base_atlas", ATLAS_SCRIPT)
base = importlib.util.module_from_spec(spec)
spec.loader.exec_module(base)


def require_emulator():
    qemu = base.adb("shell", "getprop", "ro.kernel.qemu")
    if qemu.strip() != "1":
        raise RuntimeError(
            "REFUSING to wipe real-device Glimpse data. Agent C fixture "
            "sweep is destructive and emulator-only."
        )


def check_preview_count(count):
    xml = base.hierarchy()
    if count and ("Previewing 1 of " + str(count)) not in xml:
        raise AssertionError("No settled source-selection preview for " + str(count))
    if count == 0 and "No images yet." not in xml:
        raise AssertionError("Empty-collection native state not observed")


def test_step_maintains_preview_separate_from_wallpaper(count):
    if count < 3:
        return
    base.swipe("up")
    base.capture(str(count).zfill(2) + "-reel-scrolled",
                 "Real native photo reel after one vertical page scroll")
    base.click_label("Next photo")
    base.capture(str(count).zfill(2) + "-next-preview",
                 "Native Next clicked; visually inspect hero and focused real photo")
    xml = base.hierarchy()
    # A vertically scrolled UIAutomator viewport hides the Previewing label
    # higher up the page. The Reel status is in view and independently visible.
    if ("Photo 2 of " + str(count) + " · Swipe to browse") not in xml:
        raise AssertionError("Visible Reel still does not report photo 2 of " + str(count))
    if "Set Home wallpaper now" not in xml:
        raise AssertionError("Explicit apply command disappeared after browsing")
    base.click_label("Previous photo")
    base.capture(str(count).zfill(2) + "-previous-preview",
                 "Native Previous clicked; photo 1 and exact hero/thumbnail review")
    xml = base.hierarchy()
    if ("Photo 1 of " + str(count) + " · Swipe to browse") not in xml:
        raise AssertionError("Visible Reel did not return to first selected ID")


def verify_large_type_navigation_targets():
    """Regression gate for previously measured 38.9dp visible native chips."""
    xml = ET.fromstring(base.hierarchy())
    _, _, dpi = base.screen_geometry()
    for label in ("Reel", "View all · Grid"):
        matched = []
        for node in xml.iter("node"):
            if node.attrib.get("clickable") != "true":
                continue
            descendants = [n.attrib.get("text") or n.attrib.get("content-desc")
                           for n in node.iter("node")]
            if label not in descendants:
                continue
            values = list(map(int, re.findall(r"\d+", node.attrib.get("bounds", ""))))
            if len(values) != 4:
                continue
            height_dp = (values[3] - values[1]) * 160 / dpi
            matched.append(height_dp)
        if not matched:
            raise AssertionError("Missing 200% scale accessible nav target: " + label)
        if max(matched) < 44.0:
            raise AssertionError(
                f"Native {label} nav target only {max(matched):.1f}dp at 200% font"
            )
        print("Native 200%-font navigation hit target:", label,
              f"{max(matched):.1f}dp", flush=True)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--out", default="artifacts/agent-c-native")
    args = parser.parse_args()
    base.OUT = Path(args.out).resolve()
    base.OUT.mkdir(parents=True, exist_ok=True)
    failures = []
    scale = base.adb("shell", "settings", "get", "system", "font_scale", check=False)
    try:
        require_emulator()
        # 1440/560*160 = 411dp. Captures are NOT proof of a real Pixel 6 Pro.
        base.adb("shell", "wm", "size", "1440x3120")
        base.adb("shell", "wm", "density", "560")
        base.adb("shell", "settings", "put", "system", "font_scale", "1.0")
        for n in (0, 1, 3, 5, 22, 60):
            base.start_fixture(n)
            check_preview_count(n)
            base.capture(str(n).zfill(2) + "-first-viewport",
                         "Native initial layout from Room fixtures, no UI interactions")
            if n in (3, 22, 60):
                test_step_maintains_preview_separate_from_wallpaper(n)
        # Target ~320dp; density 560, logical width 1120px.
        base.adb("shell", "wm", "size", "1120x2240")
        base.start_fixture(5)
        base.capture("05-narrow-320dp",
                     "Native ~320dp viewport; source is not 401x890 mockup")
        base.adb("shell", "settings", "put", "system", "font_scale", "2.0")
        base.start_fixture(22)
        base.capture("22-native-200-percent",
                     "Android system font scale 2.0, optical/a11y review pending")
        # UIAutomator dumps only on-screen nodes: measure visible chip height
        # BEFORE scrolling chips off-screen. Previously this was measured after
        # the scroll and falsely failed with zero clickable nodes.
        verify_large_type_navigation_targets()
        base.swipe("up")
        base.capture("22-native-200-percent-scrolled",
                     "Second viewport at 2.0 font scale, no approval inferred")
    except Exception as error:
        failures.append(str(error))
        print("Agent C native sweep failed:", error, flush=True)
        base.capture_failure("agent-c-sweep-error", str(error))
    finally:
        if scale not in ("", "null"):
            base.adb("shell", "settings", "put", "system", "font_scale",
                     scale, check=False)
        base.adb("shell", "wm", "size", "reset", check=False)
        base.adb("shell", "wm", "density", "reset", check=False)
        (base.OUT / "agent-c-manifest.json").write_text(json.dumps({
            "sourceSha": os.environ.get("GITHUB_SHA", "local"),
            "githubRunId": os.environ.get("GITHUB_RUN_ID"),
            "evidenceKind": "native API34 emulator auto interaction and screenshots",
            "strictlyNot": ["real handset", "human usability", "TalkBack audio",
                            "OS current wallpaper readback", "Doze evidence",
                            "200% acceptance", "owner visual approval"],
            "seededCounts": [0, 1, 3, 5, 22, 60],
            "captures": base.RESULTS,
            "errors": failures,
        }, indent=2) + "\n")
        base.write_gallery()
    if failures:
        return 1
    print("PASS: native emulator capture and explicit Next/Previous actions; "
          "inspect actual PNG + hierarchy artifacts before optical claims")
    return 0


if __name__ == "__main__":
    sys.exit(main())
