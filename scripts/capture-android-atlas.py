#!/usr/bin/env python3
"""Capture the actual running debug Glimpse UI on an Android device/emulator.

Uses a debug-only real Room fixture launcher, the production MainActivity and
Android screencap/UIAutomator. No alternative mock screen, no user photos.
"""
import argparse
import html
import json
import os
import re
import subprocess
import time
import xml.etree.ElementTree as ET
from pathlib import Path

APP = "io.github.gamesbyian.glimpse"
FIXTURE = APP + "/.debug.VisualFixtureLauncher"
OUT = None
RESULTS = []


def adb(*args, check=True, binary=False):
    result = subprocess.run(
        ["adb", *args],
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
        check=False,
    )
    if check and result.returncode:
        raise RuntimeError("adb " + " ".join(args) + ": " +
                           result.stderr.decode(errors="replace")[:500])
    return result.stdout if binary else result.stdout.decode(errors="replace").strip()


def hierarchy():
    result = adb("shell", "uiautomator", "dump", "/sdcard/glimpse-atlas.xml", check=False)
    raw = adb("exec-out", "cat", "/sdcard/glimpse-atlas.xml", check=False)
    if not raw.startswith("<?xml"):
        raise RuntimeError("Cannot obtain the Android accessibility hierarchy: " + result + raw[:150])
    return raw


def screen_geometry():
    size = adb("shell", "wm", "size")
    density = adb("shell", "wm", "density")
    pairs = re.findall(r"(\d+)x(\d+)", size)
    if not pairs:
        raise RuntimeError("Missing Android screen size: " + size)
    w, h = map(int, pairs[-1])
    d = re.findall(r"\d+", density)
    dpi = int(d[-1]) if d else 420
    return w, h, dpi


def start_fixture(count, active=False):
    # ONLY use on a disposable emulator/test device; this destroys Glimpse data.
    adb("shell", "am", "force-stop", APP, check=False)
    adb("shell", "pm", "clear", APP)
    adb("shell", "am", "start", "-n", FIXTURE,
        "--ei", "count", str(count), "--ez", "active", str(active).lower())
    for _ in range(35):
        time.sleep(0.8)
        try:
            xml = hierarchy()
            if "ATLAS FIXTURE" in xml and "Photos: " + str(count) in xml:
                time.sleep(0.5)
                return
        except Exception:
            pass
    raise RuntimeError("Fixture did not display " + str(count) + " photos in MainActivity")


def click_label(expected):
    xml = hierarchy()
    root = ET.fromstring(xml)
    for node in root.iter("node"):
        value = node.attrib.get("text", "") or node.attrib.get("content-desc", "")
        if value == expected or expected in value:
            bounds = re.findall(r"\d+", node.attrib.get("bounds", ""))
            if len(bounds) != 4:
                continue
            x0, y0, x1, y1 = map(int, bounds)
            adb("shell", "input", "tap", str((x0 + x1)//2), str((y0 + y1)//2))
            time.sleep(1.2)
            return
    raise RuntimeError("UI action not found: " + expected)


def swipe(direction="up"):
    w, h, _ = screen_geometry()
    a = int(h * (0.78 if direction == "up" else 0.24))
    b = int(h * (0.24 if direction == "up" else 0.78))
    adb("shell", "input", "swipe", str(w//2), str(a),
        str(w//2), str(b), "520")
    time.sleep(1)


def inspect_controls(xml, dpi):
    root = ET.fromstring(xml)
    examples = []
    for node in root.iter("node"):
        if node.attrib.get("clickable") != "true":
            continue
        bounds = re.findall(r"\d+", node.attrib.get("bounds", ""))
        if len(bounds) != 4:
            continue
        left, top, right, bottom = map(int, bounds)
        label = node.attrib.get("text") or node.attrib.get("content-desc") or "(unlabeled)"
        width_dp = round((right - left) * 160 / dpi, 1)
        height_dp = round((bottom - top) * 160 / dpi, 1)
        examples.append({
            "label": label[:100],
            "bounds": [left, top, right, bottom],
            "widthDp": width_dp,
            "heightDp": height_dp,
            "under44dp": width_dp < 44 or height_dp < 44,
        })
    return examples


def capture(slug, explanation):
    w, h, dpi = screen_geometry()
    raw = adb("exec-out", "screencap", "-p", binary=True)
    if not raw.startswith(b"\x89PNG\r\n\x1a\n") or len(raw) < 15000:
        raise RuntimeError("Android did not return a plausible screenshot: " + slug)
    (OUT / (slug + ".png")).write_bytes(raw)
    xml = hierarchy()
    (OUT / (slug + ".xml")).write_text(xml, encoding="utf-8")
    controls = inspect_controls(xml, dpi)
    RESULTS.append({
        "id": slug,
        "image": slug + ".png",
        "hierarchy": slug + ".xml",
        "reason": explanation,
        "widthPx": w, "heightPx": h, "densityDpi": dpi,
        "clickableCount": len(controls),
        "smallClickableControls": [c for c in controls if c["under44dp"]][:20],
        "capturedFrom": "Android emulator running actual debug MainActivity",
    })
    print("Captured", slug, w, "x", h, "clickable:", len(controls), flush=True)


def write_gallery():
    rows = []
    for case in RESULTS:
        rows.append(
            '<article><h2>' + html.escape(case["id"]) + '</h2><p>' +
            html.escape(case["reason"]) + '</p><a href="' +
            html.escape(case["image"]) + '"><img loading="lazy" src="' +
            html.escape(case["image"]) + '" alt="' +
            html.escape(case["id"]) + '"></a><p>' +
            str(case["widthPx"]) + '×' + str(case["heightPx"]) +
            ' · <a href="' + html.escape(case["hierarchy"]) +
            '">Accessibility hierarchy</a></p></article>'
        )
    page = """<!doctype html><html lang="en"><meta charset="utf-8">
<title>Glimpse native Android evidence atlas</title>
<meta name="viewport" content="width=device-width, initial-scale=1">
<style>body{font:16px system-ui;margin:22px;background:#f6f0e6;color:#183f43}
header{max-width:78ch}section{display:grid;grid-template-columns:repeat(auto-fit,
minmax(min(100%,310px),1fr));gap:18px}article{background:#fffaf2;border:1px
solid #c9b9a1;border-radius:14px;padding:14px}img{display:block;width:100%;
max-height:620px;object-fit:contain;background:#ddd}h2{font-size:1.1rem}
a{color:#175c66}p{line-height:1.5}</style>
<header><h1>Glimpse · actual implementation atlas</h1>
<p>Screenshots from the running debug APK with synthetic visual-fixture collections.
These are <strong>implementation evidence</strong>, not approved UI references
or physical-device/user acceptance. No private user's photographs appear here.</p>
<p>Open <a href="manifest.json">manifest and control-size diagnostics</a>.
Compare with the Glimpse design reference drawings in the repository.</p></header><section>""" + "".join(rows) + "</section></html>"
    (OUT / "index.html").write_text(page, encoding="utf-8")


def main():
    global OUT
    parser = argparse.ArgumentParser()
    parser.add_argument("--out", default="artifacts/android-visual-atlas")
    parser.add_argument("--mode", choices=("smoke", "full"), default="smoke")
    args = parser.parse_args()
    OUT = Path(args.out).resolve()
    OUT.mkdir(parents=True, exist_ok=True)
    original_scale = adb("shell", "settings", "get", "system", "font_scale", check=False)
    failures = []
    try:
        # The emulator is deliberately set to Pixel 6 Pro-sized logical geometry.
        # This does not simulate a physical Pixel 6 Pro GPU, touch or display.
        adb("shell", "wm", "size", "1440x3120")
        adb("shell", "wm", "density", "560")
        adb("shell", "settings", "put", "system", "font_scale", "1.0")
        start_fixture(22)
        capture("22-reel-initial", "Collection with 22 actual records, before any interaction")
        swipe("up")
        capture("22-reel-and-hero", "Scroll to photo selection and selected hero")
        if args.mode == "full":
            swipe("up")
            capture("22-controls-and-rotation", "Bottom controls and rotation status")
            start_fixture(3)
            capture("03-reel-initial", "Three photos: empty end apertures, no fake repeats")
            start_fixture(22)
            click_label("View all · Grid")
            capture("22-grid", "The actual View all Grid after selecting the grid control")
            adb("shell", "settings", "put", "system", "font_scale", "2.0")
            start_fixture(22)
            capture("22-reel-text-200-percent", "OS font scale 200%, first viewport")
            swipe("up")
            capture("22-text-200-percent-scrolled", "OS font scale 200%, following viewport")
    except Exception as error:
        failures.append(str(error))
        print("Atlas failed:", error, flush=True)
    finally:
        if original_scale not in ("", "null"):
            adb("shell", "settings", "put", "system", "font_scale", original_scale, check=False)
        adb("shell", "wm", "size", "reset", check=False)
        adb("shell", "wm", "density", "reset", check=False)
        (OUT / "manifest.json").write_text(json.dumps({
            "sourceSha": os.environ.get("GITHUB_SHA", "local"),
            "runId": os.environ.get("GITHUB_RUN_ID"),
            "mode": args.mode,
            "evidenceKind": "automated emulator screenshot, not user/handset observation",
            "fixture": "six public design fixtures, numbered as synthetic unique records",
            "captures": RESULTS,
            "errors": failures,
        }, indent=2) + "\n", encoding="utf-8")
        write_gallery()
    if failures:
        raise SystemExit(1)


if __name__ == "__main__":
    main()
