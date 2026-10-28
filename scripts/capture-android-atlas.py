#!/usr/bin/env python3
"""Capture the actual running debug Glimpse UI on an Android device/emulator.

Uses a debug-only real Room fixture launcher, the production MainActivity and
Android screencap/UIAutomator. No alternative mock screen, no user photos.
"""
import argparse
import struct
import hashlib
import io
import zipfile
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


def adb(*args, check=True, binary=False, timeout=35):
    result = subprocess.run(
        ["adb", *args],
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
        check=False,
        timeout=timeout,
    )
    if check and result.returncode:
        raise RuntimeError("adb " + " ".join(args) + ": " +
                           result.stderr.decode(errors="replace")[:500])
    return result.stdout if binary else result.stdout.decode(errors="replace").strip()


def hierarchy():
    result = adb("shell", "uiautomator", "dump", "/sdcard/glimpse-atlas.xml", check=False, timeout=25)
    raw = adb("exec-out", "cat", "/sdcard/glimpse-atlas.xml", check=False, timeout=15)
    if not raw.lstrip().startswith("<?xml"):
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


def clear_system_obstructions():
    """A valid atlas image must never be obscured by an Android ANR dialog."""
    for _ in range(3):
        try:
            xml = hierarchy()
        except Exception:
            return
        if "isn't responding" not in xml and "is not responding" not in xml:
            return
        root = ET.fromstring(xml)
        wait = next((n for n in root.iter("node")
                     if n.attrib.get("text") == "Wait"), None)
        if wait is None:
            raise RuntimeError("System ANR obscures Glimpse; no safe Wait control")
        bounds = re.findall(r"\\d+", wait.attrib.get("bounds", ""))
        if len(bounds) != 4:
            raise RuntimeError("System ANR has no tappable Wait bounds")
        a,b,c,d = map(int, bounds)
        adb("shell", "input", "tap", str((a+c)//2), str((b+d)//2))
        time.sleep(2.0)
    xml = hierarchy()
    if "isn't responding" in xml or "is not responding" in xml:
        raise RuntimeError("System ANR still obscures Glimpse after Wait")

def start_fixture(count, active=False):
    """Seed debug Room and confirm completion via a local marker, not UI text."""
    # ONLY use on a disposable emulator/test device; destroys Glimpse data.
    adb("shell", "am", "force-stop", APP, check=False)
    adb("shell", "pm", "clear", APP)
    # Clearing package data tears down the previous Android activity task
    # asynchronously. Starting a new launcher immediately can put it into a
    # process subsequently killed by the *old* task's destroy-timeout handler.
    # See API34 atlas failure 40449390809. Wait for actual activity removal.
    leftovers = []
    for _ in range(28):
        state = adb("shell", "dumpsys", "activity", "activities",
                    check=False, timeout=15)
        leftovers = re.findall(
            r"ActivityRecord\{[^}]*" + re.escape(APP) + r"[^}]*\}", state
        )
        if not leftovers:
            break
        time.sleep(1.0)
    else:
        capture_failure("old-fixture-task-still-active",
                        "Stale Android ActivityRecord after pm clear: " +
                        str(leftovers[:3]))
        raise RuntimeError("Previous fixture task not removed after package clear")
    time.sleep(1.5)
    launch = adb("shell", "am", "start", "-n", FIXTURE,
                 "--ei", "count", str(count),
                 "--ez", "active", str(active).lower(), check=False)
    print("Fixture launcher:", launch[:240], flush=True)
    marker = "READY:" + str(count) + ":"
    result = ""
    for _ in range(45):
        time.sleep(1.0)
        result = adb("shell", "run-as", APP, "cat",
                     "files/visual-atlas-ready.txt", check=False,
                     timeout=15)
        if result.startswith(marker):
            time.sleep(2.0)  # Compose first layout and image reads
            print("Fixture ready:", result, flush=True)
            clear_system_obstructions()
            # Fixture rows are already committed, but Compose's initial
            # collection watch may not yet have emitted them. Refuse false
            # "zero photo" screenshots of a seeded 3/22-photo collection.
            for _ in range(16):
                try:
                    snapshot = hierarchy()
                    # UIAutomator lists only on-screen nodes. At 200% text
                    # the selected photo count legitimately moves below fold;
                    # the visible collection tile still proves the real Room
                    # membership aggregate has emitted after the READY marker.
                    count_label = "Photos: " + str(count)
                    overview_label = ("ATLAS FIXTURE · " + str(count) +
                                      " photos, " + str(count) + " photos")
                    if count_label in snapshot or overview_label in snapshot:
                        return
                    font_scale = adb("shell", "settings", "get", "system",
                                     "font_scale", check=False)
                    if (font_scale.startswith("2") and
                            "HOME WALLPAPER" in snapshot and
                            "Glimpse" in snapshot):
                        # The debug READY marker guarantees committed rows.
                        # At extreme scale the Home header is visible while
                        # both collection and photo counts may be offscreen.
                        return
                except Exception:
                    pass
                time.sleep(0.6)
            capture_failure("fixture-ui-not-ready-" + str(count),
                            "database seeded, Compose count not yet visible")
            raise RuntimeError("Seeded fixture but UI count did not match " + str(count))
    capture_failure("fixture-not-ready-" + str(count), result)
    raise RuntimeError("Fixture never wrote " + marker +
                       "; inspect failure screen and redacted logcat")


def capture_failure(slug, detail):
    """Retain failure evidence even if UIAutomator is entirely unavailable."""
    try:
        raw = adb("exec-out", "screencap", "-p", binary=True, timeout=40)
        if raw.startswith(b"\x89PNG\r\n\x1a\n"):
            (OUT / (slug + ".png")).write_bytes(raw)
        (OUT / (slug + ".txt")).write_text(
            detail[:1200], encoding="utf-8")
        log = adb("logcat", "-d", "-t", "400", check=False, timeout=40)
        # CI contains only published fixtures, but avoid leaking unrelated
        # emulator accounts/URIs and keep artifacts small.
        safe = "\n".join(
            line for line in log.splitlines()
            if any(tag in line for tag in ("glimpse", "Glimpse",
                                          "AndroidRuntime", "FATAL EXCEPTION",
                                          "ActivityTaskManager"))
        )
        (OUT / (slug + ".log")).write_text(safe[-40000:],
                                             encoding="utf-8")
    except Exception as error:
        print("Could not collect all failure evidence:", error, flush=True)


def click_label(expected):
    clear_system_obstructions()
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


def reach_and_click(label, direction="up", attempts=7):
    """A human-visible native action may sit below the current viewport."""
    for attempt in range(attempts):
        try:
            click_label(label)
            return
        except RuntimeError as error:
            if "UI action not found:" not in str(error):
                raise
            if attempt == attempts - 1:
                raise
            swipe(direction)


def swipe(direction="up"):
    w, h, _ = screen_geometry()
    # A bottom-corner reel is a live rotation control, not a scrolling grip.
    # Start in the unobstructed upper content area; crossing the wheel during
    # a gesture is harmless once the scroll owner has accepted the down event.
    a = int(h * (0.49 if direction == "up" else 0.18))
    b = int(h * (0.17 if direction == "up" else 0.53))
    x = int(w * .92)
    adb("shell", "input", "swipe", str(x), str(a),
        str(x), str(b), "520")
    time.sleep(1)


def accessible_bounds(xml, phrase):
    """Read actual rendered Android bounds with bounded recompose tolerance."""
    for attempt in range(6):
        for node in ET.fromstring(xml).iter("node"):
            # Both fields may be populated; accessibility labels own the
            # Canvas, while surrounding layout state may have text too.
            values = (node.attrib.get("text", ""), node.attrib.get("content-desc", ""))
            if any(phrase in value for value in values):
                bounds = re.findall(r"\\d+", node.attrib.get("bounds", ""))
                if len(bounds) == 4:
                    return tuple(map(int, bounds))
        # UIAutomator can omit transient Compose nodes during the asynchronous
        # selector/thumbnail rebind after Next. A captured screenshot from this
        # exact branch proves the Canvas exists; retry a fresh hierarchy rather
        # than reporting a non-existent UI defect from one transient dump.
        if attempt != 5:
            time.sleep(.45)
            xml = hierarchy()
    raise RuntimeError("Native element not visible after retries: " + phrase)

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
    clear_system_obstructions()
    w, h, dpi = screen_geometry()
    raw = adb("exec-out", "screencap", "-p", binary=True)
    if not raw.startswith(b"\x89PNG\r\n\x1a\n") or len(raw) < 15000:
        raise RuntimeError("Android did not return a plausible screenshot: " + slug)
    (OUT / (slug + ".png")).write_bytes(raw)
    xml = ""
    controls = []
    try:
        xml = hierarchy()
        (OUT / (slug + ".xml")).write_text(xml, encoding="utf-8")
        controls = inspect_controls(xml, dpi)
    except Exception as exc:
        (OUT / (slug + ".xml-error.txt")).write_text(
            str(exc)[:800], encoding="utf-8"
        )
        print("Accessibility unavailable for", slug, str(exc)[:180], flush=True)
    RESULTS.append({
        "id": slug,
        "image": slug + ".png",
        "hierarchy": slug + ".xml" if xml else None,
        "reason": explanation,
        "widthPx": w, "heightPx": h, "densityDpi": dpi,
        "logicalWidthDp": round(w * 160 / dpi, 1),
        "fontScale": adb("shell", "settings", "get", "system", "font_scale", check=False),
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
        if args.mode == "full":
            # Android UIAutomator sees the *viewport*, not scrolled-away nodes.
            # Verify truth about Home on the initial viewport before scrolling.
            first_viewport = hierarchy()
            if "No wallpaper change has been acknowledged yet" not in first_viewport:
                capture_failure("incorrect-home-acknowledgment",
                                "Fresh fixture must not claim a wallpaper acknowledgment")
                raise RuntimeError("Initial Home card lacks truthful no-acknowledgment state")
            # A fixed-edge reel is now a primary Home control. The former
            # collection-management tile may scroll under it; the live Room
            # count is deliberately kept in Home's visible introduction.
            if ("ATLAS FIXTURE · 22 photos, 22 photos" not in first_viewport and
                    "Photos: 22" not in first_viewport):
                capture_failure("missing-collection-overview",
                                "No Room-backed 22-photo count in initial Home viewport")
                raise RuntimeError("Actual initial 22-photo collection count missing")
        capture("22-reel-initial", "Collection with 22 actual records, before any interaction")
        if args.mode == "full":
            # Real OS multi-stream Sharesheet with read-only FileProvider grants,
            # distinct from the existing single-photo share acceptance.
            reach_and_click("Share 22 originals", "up", attempts=12)
            time.sleep(1.5)
            batch_xml = hierarchy()
            batch_packages = {node.attrib.get("package")
                              for node in ET.fromstring(batch_xml).iter("node")}
            if not batch_packages.intersection({
                "android", "com.android.intentresolver",
                "com.google.android.intentresolver"
            }):
                capture_failure("batch-original-sharesheet-missing",
                                "ACTION_SEND_MULTIPLE did not launch Android Sharesheet")
                raise RuntimeError("Actual collection Sharesheet failed to launch")
            capture("22-collection-android-batch-sharesheet",
                    "Actual Android Sharesheet with 22 verified private original URI grants")
            adb("shell", "input", "keyevent", "4")
            time.sleep(1.1)
            # A collection archive is chosen through Android DocumentsUI,
            # never a pretend in-app path text field or unapproved broad grant.
            reach_and_click("Export collection ZIP", "up", attempts=12)
            time.sleep(1.7)
            archive_chooser = hierarchy()
            archive_packages = {node.attrib.get("package")
                                for node in ET.fromstring(archive_chooser).iter("node")}
            if not archive_packages.intersection({
                    "com.android.documentsui", "com.google.android.documentsui"}):
                capture_failure("collection-export-no-android-save-as",
                                "Collection ZIP did not launch Android DocumentsUI")
                raise RuntimeError("Collection export chooser was not real DocumentsUI")
            capture("22-collection-export-android-save-as",
                    "Real Android document picker for explicit originals-only ZIP export")
            adb("shell", "input", "keyevent", "4")
            returned = False
            for _ in range(12):
                time.sleep(0.6)
                after_cancel = hierarchy()
                packages = {node.attrib.get("package")
                            for node in ET.fromstring(after_cancel).iter("node")}
                if (APP in packages and
                        "Export collection ZIP" in after_cancel and
                        not packages.intersection({
                            "com.android.documentsui", "com.google.android.documentsui"})):
                    returned = True
                    break
            if not returned:
                capture_failure("collection-export-cancel-return",
                                "Android ZIP Save As cancellation did not return Home")
                raise RuntimeError("Cancelled collection ZIP picker did not restore Home")
            # Complete a second, explicit Android Save As. Validate the bytes
            # independently from the application's success message: this is
            # an actual DocumentsUI provider and user-visible ZIP, not a JVM
            # helper pretending to exercise Android storage permissions.
            reach_and_click("Export collection ZIP", "up", attempts=12)
            time.sleep(1.3)
            zip_picker = hierarchy()
            if "SAVE" not in zip_picker:
                capture_failure("collection-zip-save-button-unavailable",
                                "Android DocumentsUI Save action not found")
                raise RuntimeError("Android ZIP Save As has no SAVE action")
            click_label("SAVE")
            expected_zip = "/sdcard/Download/glimpse-ATLAS-FIXTURE-22-photos-originals.zip"
            stored = None
            for _ in range(18):
                time.sleep(0.8)
                raw = adb("exec-out", "cat", expected_zip,
                          binary=True, check=False, timeout=45)
                if raw.startswith(b"PK\x03\x04"):
                    stored = raw
                    break
            if stored is None:
                capture_failure("collection-zip-missing-from-downloads",
                                "Android did not persist a ZIP in Downloads after Save")
                raise RuntimeError("Android ZIP Save As did not yield a readable archive")
            with zipfile.ZipFile(io.BytesIO(stored)) as archive:
                manifest = json.loads(archive.read("manifest.json"))
                if (manifest.get("format") != "glimpse.collection.originals" or
                        manifest.get("version") != 1 or
                        manifest.get("photoCount") != 22 or
                        len(manifest.get("photos", [])) != 22):
                    raise RuntimeError("Saved ZIP manifest did not identify 22 originals")
                if len(archive.namelist()) != 23:
                    raise RuntimeError("Saved ZIP did not contain exactly 22 originals")
                for item in manifest["photos"]:
                    name = item["path"]
                    sha = item["sha256"]
                    if not (name.startswith("originals/") and
                            name == "originals/" + sha + ".jpg" and
                            len(sha) == 64):
                        raise RuntimeError("Export contains an unsafe original path")
                    exact = archive.read(name)
                    if (len(exact) != item["bytes"] or
                            hashlib.sha256(exact).hexdigest() != sha):
                        raise RuntimeError("Android-exported ZIP changed original bytes")
            capture("22-collection-zip-export-complete",
                    "Real Android DocumentsUI ZIP: 22 original entries, exact SHA and bytes")
            # Support is an offline, actually navigable native screen, not a
            # static proposal or a guessed Play/OS permission dialog.
            reach_and_click("Help & privacy", "down", attempts=12)
            help_xml = hierarchy()
            # UIAutomator XML encodes an ampersand as &amp;. Compare parsed
            # accessibility text, not the escaped XML source string.
            visible_help_text = [
                node.attrib.get("text", "")
                for node in ET.fromstring(help_xml).iter("node")
            ]
            if (not any("Help & privacy." in text for text in visible_help_text) or
                    not any("GETTING STARTED" in text for text in visible_help_text)):
                capture_failure("help-privacy-entry-missing",
                                "Native help title or Getting Started content not accessible")
                raise RuntimeError("Native Help & privacy entry did not render")
            capture("22-help-privacy-initial",
                    "Real offline Help/Privacy/About entry from Home masthead")
            found_privacy = False
            for _ in range(9):
                privacy_xml = hierarchy()
                # Match parsed accessibility labels; raw XML escapes &.
                privacy_labels = [node.attrib.get("text", "")
                                  for node in ET.fromstring(privacy_xml).iter("node")]
                if any("PRIVACY & STORAGE" in label for label in privacy_labels):
                    found_privacy = True
                    break
                swipe("up")
            if not found_privacy:
                raise RuntimeError("Privacy/storage disclosure is unreachable")
            capture("22-help-privacy-disclosure",
                    "Native privacy and backup/storage disclosure reachable by scroll")
            # Opt-in audit must verify actual synthetic immutable originals.
            # This checks the Room photo catalog against the SHA-named bytes,
            # not the different physical-file-count inventory.
            reach_and_click("Verify saved originals", "up", attempts=12)
            audit_ok = False
            for attempt in range(18):
                audit_xml = hierarchy()
                if "Verified 22 of 22 registered originals." in audit_xml:
                    audit_ok = True
                    break
                # The Verify button is often at the end of the viewport. Its
                # resulting status text is *below* it in the scrollable column.
                # UIAutomator does not include offscreen text. Scroll rather
                # than falsely labeling 22 intact originals as corrupt.
                if attempt % 2 == 0:
                    swipe("up")
                time.sleep(0.5)
            if not audit_ok:
                capture_failure("registered-original-audit-failed",
                                "Read-only check did not verify 22 actual fixture originals")
                raise RuntimeError("Registered-original exact-byte audit failed")
            capture("22-help-original-integrity-audit",
                    "User-triggered verified 22/22 private SHA originals, no mutation")
            # Restore the exact ZIP we just exported via the *real* Android
            # OpenDocument chooser. The app must create a new Room collection
            # without changing the source library or applying wallpaper.
            reach_and_click("Restore collection ZIP", "up", attempts=14)
            time.sleep(1.5)
            restore_picker = hierarchy()
            restore_packages = {node.attrib.get("package")
                                for node in ET.fromstring(restore_picker).iter("node")}
            if not restore_packages.intersection({
                    "com.android.documentsui", "com.google.android.documentsui"}):
                capture_failure("restore-zip-no-real-document-picker",
                                "Android OpenDocument did not open")
                raise RuntimeError("Restore did not launch actual DocumentsUI")
            zip_display = "glimpse-ATLAS-FIXTURE-22-photos-originals.zip"
            def documentsui_visible(xml):
                packages = {node.attrib.get("package")
                            for node in ET.fromstring(xml).iter("node")}
                return bool(packages.intersection({
                    "com.android.documentsui", "com.google.android.documentsui"
                }))
            # Android DocumentsUI can truncate a filename in grid mode, and
            # a text node may not itself be clickable. Tap the enclosing
            # clickable file tile, never an arbitrary guessed screen coordinate.
            def choose_document_tile(fragment):
                xml = hierarchy()
                root = ET.fromstring(xml)
                parent = {child: node for node in root.iter()
                          for child in node}
                # DocumentsUI exposes two similarly named targets:
                # a thumbnail-preview icon, and the actual file title inside
                # clickable item_root. The preview icon does NOT return a URI.
                candidates = [node for node in root.iter("node")
                              if node.attrib.get("resource-id") == "android:id/title" and
                              fragment in node.attrib.get("text", "")]
                for node in candidates:
                    target = node
                    while target in parent and target.attrib.get("resource-id") != (
                            "com.android.documentsui:id/item_root"):
                        target = parent[target]
                    if target.attrib.get("resource-id") != (
                            "com.android.documentsui:id/item_root"):
                        continue
                    bounds = re.findall(r"\d+", target.attrib.get("bounds", ""))
                    if len(bounds) != 4:
                        continue
                    x0, y0, x1, y1 = map(int, bounds)
                    adb("shell", "input", "tap",
                        str((x0 + x1)//2), str((y0 + y1)//2))
                    time.sleep(1.3)
                    return True
                return False
            if not choose_document_tile("glimpse-ATLAS"):
                try:
                    click_label("Downloads")
                except RuntimeError:
                    click_label("Show roots")
                    click_label("Downloads")
                if not choose_document_tile("glimpse-ATLAS"):
                    capture_failure("restore-zip-not-listed-in-provider",
                                    "Downloaded Glimpse ZIP not shown in real OpenDocument UI")
                    raise RuntimeError("Exported ZIP unavailable in Android document provider")
            picker_closed = False
            for attempt in range(10):
                response_xml = hierarchy()
                if not documentsui_visible(response_xml):
                    picker_closed = True
                    break
                # Grid mode may highlight a tile on first tap rather than
                # launch the OpenDocument result immediately.
                if not choose_document_tile("glimpse-ATLAS"):
                    for label in ("Open", "OPEN", "Select", "SELECT"):
                        try:
                            click_label(label)
                            break
                        except RuntimeError:
                            pass
                time.sleep(0.6)
            if not picker_closed:
                (OUT / "restore-zip-still-in-documentsui.xml").write_text(
                    hierarchy(), encoding="utf-8"
                )
                capture_failure("restore-zip-picker-still-open",
                                "Picker did not return a document URI after tapping visible ZIP tile")
                raise RuntimeError("Android OpenDocument never returned the chosen ZIP")
            restored = False
            for attempt in range(30):
                result_xml = hierarchy()
                if "Restored 22 original photos into a new collection" in result_xml:
                    restored = True
                    break
                if attempt % 3 == 0:
                    swipe("up")
                time.sleep(0.8)
            if not restored:
                (OUT / "restore-zip-result-unconfirmed.xml").write_text(
                    hierarchy(), encoding="utf-8"
                )
                capture_failure("restore-zip-22-originals-not-confirmed",
                                "OpenDocument returned but no new 22-photo collection reported")
                raise RuntimeError("Glimpse did not confirm restore of all 22 original photos")
            capture("22-collection-zip-restore-complete",
                    "Real Android OpenDocument restores 22 original bytes to independent Room collection")
            # The diagnostic preview is user-triggered and must be readable
            # before any Android chooser or clipboard action exists.
            reach_and_click("Generate local diagnostic report", "up", attempts=10)
            found_diagnostic = False
            for _ in range(9):
                diag_xml = hierarchy()
                if ("Review or edit diagnostic text" in diag_xml and
                        "Copy report" in diag_xml):
                    found_diagnostic = True
                    break
                swipe("up")
            if not found_diagnostic:
                capture_failure("diagnostic-preview-unreachable",
                                "Local diagnostic preview/copy controls not visible")
                raise RuntimeError("Opt-in support diagnostic preview is unreachable")
            capture("22-help-diagnostic-preview",
                    "Real Android local report preview before copy or share")
            reach_and_click("Close report", "up", attempts=8)
            reach_and_click("Back to Home", "down", attempts=12)
            if "Glimpse" not in hierarchy():
                raise RuntimeError("Help & privacy did not return Home")
            # The new primary doorway must open a real photographic Reel,
            # preserve finite identity and return without applying wallpaper.
            click_label("Browse photo reel")
            first_reel = hierarchy()
            if ("Turn a new page." not in first_reel or
                    "Previewing 1 of 22" not in first_reel or
                    "Paper photo reel" not in first_reel):
                capture_failure("focused-reel-entry-missing",
                                "Dedicated Reel lacks selected hero, count or real Canvas")
                raise RuntimeError("Focused Reel entry not visibly functional")
            capture("22-focused-reel-entry",
                    "First-frame dedicated photo room with stationary hero and actual reel")
            click_label("Next photo")
            if "Previewing 2 of 22" not in hierarchy():
                raise RuntimeError("Focused Reel Next did not select stable photo 2")
            capture("22-focused-reel-next",
                    "Photo 2 preview from same collection after semantic Next")
            # The owner found that drags beginning inside photographic
            # apertures failed. Rotate a real fixture from the selected top
            # photo-window centre toward its clockwise neighbour. This is an
            # Android input event, not a synthetic ReelMotion unit assertion.
            x0, y0, x1, y1 = accessible_bounds(hierarchy(), "Paper photo reel")
            rw, rh = x1 - x0, y1 - y0
            cx = x0 + rw * .27
            cy = y0 + rh * .87
            photo_radius = rw * .69 * .62
            start_x, start_y = round(cx), round(cy - photo_radius)
            end_x = round(cx + photo_radius * .707)
            end_y = round(cy - photo_radius * .707)
            if not (x0 <= start_x < x1 and y0 <= start_y < y1):
                raise RuntimeError("Top photo aperture outside native reel bounds")
            adb("shell", "input", "swipe", str(start_x), str(start_y),
                str(end_x), str(end_y), "620")
            time.sleep(1.5)
            if "Previewing 1 of 22" not in hierarchy():
                capture_failure("photo-window-drag-unresponsive",
                                "Native drag started on actual photographic aperture")
                raise RuntimeError("Aperture-origin reel rotation did not select previous photo")
            capture("22-photo-aperture-drag",
                    "Native Android clockwise drag begins within a photographic aperture")
            # Scrolling the underlying page above the reel must leave the reel
            # itself at the identical screen position, rather than carry it
            # upward as an in-flow dashboard widget.
            reel_before = accessible_bounds(hierarchy(), "Paper photo reel")
            home_before = accessible_bounds(hierarchy(), "Back to Home")
            screen_w, screen_h, _ = screen_geometry()
            adb("shell", "input", "swipe",
                str(int(screen_w * .92)), str(int(screen_h * .44)),
                str(int(screen_w * .92)), str(int(screen_h * .16)), "520")
            time.sleep(1)
            scrolled = hierarchy()
            if accessible_bounds(scrolled, "Paper photo reel") != reel_before:
                raise RuntimeError("Fixed reel moved when page content scrolled")
            try:
                home_after = accessible_bounds(scrolled, "Back to Home")
            except RuntimeError:
                home_after = None  # expected: header scrolls away
            if home_after == home_before:
                raise RuntimeError("Page failed to scroll independently of fixed reel")
            capture("22-fixed-reel-after-scroll",
                    "Photo reel stays at its viewport coordinates as content scrolls behind")
            # Restore a visible navigation action without touching the reel.
            adb("shell", "input", "swipe",
                str(int(screen_w * .92)), str(int(screen_h * .20)),
                str(int(screen_w * .92)), str(int(screen_h * .57)), "550")
            time.sleep(1)
            reach_and_click("Back to Home", "down", attempts=8)
            if "Glimpse" not in hierarchy():
                raise RuntimeError("Dedicated reel did not return to Home dashboard")
        swipe("up")
        capture("22-reel-and-hero", "Scroll to photo selection and selected hero")
        if args.mode == "full":
            swipe("up")
            capture("22-controls-and-rotation", "Bottom controls and rotation status")
            start_fixture(3)
            capture("03-reel-initial", "Three photos: empty end apertures, no fake repeats")
            start_fixture(22)
            # The collection Share/Export actions can move Grid below the
            # first viewport; UIAutomator only reports visible nodes.
            reach_and_click("View all · Grid", "up", attempts=12)
            capture("22-grid", "The actual View all Grid after selecting the grid control")
            # R3 uses production PhotoRecord, selection and wallpaper gateway;
            # inspect the actual full-aspect detail, next ID and back path.
            start_fixture(22)
            reach_and_click("Open photo details")
            first_detail = hierarchy()
            if "In focus." not in first_detail or "Photograph 1 of 22" not in first_detail:
                raise RuntimeError("Actual first photo detail state not observed")
            capture("22-photo-detail-original",
                    "Full undistorted original on native photo detail, preview-only")
            reach_and_click("Next photo", "up")
            swipe("down")
            next_detail = hierarchy()
            if "Photograph 2 of 22" not in next_detail:
                raise RuntimeError("Detail Next did not select photograph 2 of 22")
            capture("22-photo-detail-next",
                    "Next selects next stable ID without changing system wallpaper")
            reach_and_click("Share original photo")
            time.sleep(2.0)
            chooser = hierarchy()
            packages = {node.attrib.get("package")
                        for node in ET.fromstring(chooser).iter("node")}
            if not (packages & {"android", "com.android.intentresolver",
                                "com.google.android.intentresolver"}):
                raise RuntimeError("Actual Android Sharesheet never opened: " +
                                   ",".join(sorted(str(p) for p in packages)))
            capture("22-original-android-sharesheet",
                    "Android chooser launched from verified immutable original; no wallpaper operation")
            adb("shell", "input", "keyevent", "4")
            time.sleep(1.0)
            if "In focus." not in hierarchy():
                raise RuntimeError("Photo detail did not survive cancelled Android Sharesheet")
            # New Save As is an OS-owned SAF document picker, not an
            # app-private filename entry pretending to write user storage.
            reach_and_click("Save original copy…", "up", attempts=11)
            time.sleep(1.7)
            save_chooser = hierarchy()
            save_packages = {node.attrib.get("package")
                             for node in ET.fromstring(save_chooser).iter("node")}
            if not (save_packages & {"com.android.documentsui",
                                     "com.google.android.documentsui"}):
                capture_failure("save-original-no-document-picker",
                                "Android DocumentsUI did not open on Save original copy")
                raise RuntimeError("Actual Android document picker did not open: " +
                                   ",".join(sorted(str(p) for p in save_packages)))
            capture("22-original-android-save-as",
                    "Real Android DocumentsUI chosen for user-initiated original export")
            adb("shell", "input", "keyevent", "4")
            # We scrolled down to reach Save As. "In focus." is above the
            # fold and is not a valid post-cancel assertion at this position.
            # Require an actual Glimpse detail action after DocumentsUI exits.
            returned = False
            for _ in range(12):
                time.sleep(0.6)
                after_cancel = hierarchy()
                packages = {node.attrib.get("package")
                            for node in ET.fromstring(after_cancel).iter("node")}
                detail_action = ("Save original copy" in after_cancel or
                                 "Set Home wallpaper now" in after_cancel or
                                 "Share original photo" in after_cancel)
                if (APP in packages and
                        not packages.intersection({"com.android.documentsui",
                                                   "com.google.android.documentsui"}) and
                        detail_action):
                    returned = True
                    break
            if not returned:
                capture_failure("save-original-cancel-return",
                                "Android Save As cancellation did not expose " +
                                "a Glimpse photo detail action")
                raise RuntimeError("Cancelled Save As did not return to photo detail")
            # Exercise the real reversible editor and Android PNG Save As.
            # Test a clockwise quarter-turn and a nontrivial left crop; PNG
            # dimensions prove pixels were transformed, not merely renamed.
            reach_and_click("Edit photo (crop and rotate)", "up", attempts=14)
            reach_and_click("Rotate 90° right", "up", attempts=12)
            reach_and_click("Left crop edge", "up", attempts=16)
            reach_and_click("Save edited PNG…", "up", attempts=16)
            time.sleep(1.5)
            edit_picker = hierarchy()
            edit_pkgs = {node.attrib.get("package")
                         for node in ET.fromstring(edit_picker).iter("node")}
            if not edit_pkgs.intersection({
                    "com.android.documentsui", "com.google.android.documentsui"}):
                capture_failure("edited-png-save-as-missing",
                                "Edited PNG did not launch Android DocumentsUI")
                raise RuntimeError("Edited PNG Save As never opened Android")
            capture("22-edited-png-android-save-as",
                    "Rotated/cropped preview launched genuine Android PNG Save As")
            reach_and_click("SAVE", attempts=4)
            saved_png = None
            for _ in range(20):
                time.sleep(0.8)
                names = adb("shell", "ls", "/sdcard/Download", check=False).splitlines()
                matches = [name for name in names if
                           name.startswith("glimpse-edited-") and name.endswith(".png")]
                if matches:
                    raw = adb("exec-out", "cat",
                              "/sdcard/Download/" + matches[-1],
                              check=False, binary=True, timeout=40)
                    if raw.startswith(b"\x89PNG\r\n\x1a\n") and len(raw) >= 24:
                        saved_png = raw
                        break
            if saved_png is None:
                capture_failure("edited-png-not-created",
                                "The Android document provider did not save the edited PNG")
                raise RuntimeError("No readable edited PNG from Save As")
            png_width, png_height = struct.unpack(">II", saved_png[16:24])
            if not (1 <= png_width < 720 and png_height == 480):
                raise RuntimeError(
                    "Edited PNG did not encode the 90-degree turn and left crop: " +
                    str(png_width) + "x" + str(png_height))
            capture("22-edited-png-saved-and-cropped",
                    "Android saved real rotated/cropped PNG; original private JPEG retained")
            reach_and_click("Back to Reel / Grid", "down")
            capture("22-photo-detail-return",
                    "Return to original Reel/Grid browsing context")
            # R3 foreground shuffle source must be independent of which library
            # the user has opened. Seed two real Room memberships, start active
            # shuffle on the first, then browse the second and inspect status.
            start_fixture(22, active=True)
            # Attach photo 3 to the alternate collection, which initially
            # contains only photos 1 and 2. The same source file is reused.
            reach_and_click("Open photo details")
            reach_and_click("Next photo", "up")
            reach_and_click("Next photo", "up")
            reach_and_click("Add this photo to another collection", "up", attempts=12)
            reach_and_click("Add to ALTERNATE ATLAS", "up", attempts=10)
            reach_and_click("Back to Reel / Grid", "down", attempts=12)
            reach_and_click("ALTERNATE ATLAS")
            found_three = False
            for _ in range(9):
                membership_xml = hierarchy()
                if "Photos: 3" in membership_xml:
                    found_three = True
                    break
                swipe("up")
            if not found_three:
                capture_failure("cross-collection-membership-not-visible",
                                "Membership copy did not appear in alternate collection")
                raise RuntimeError("Adding existing photo did not update target collection")
            capture("22-cross-collection-membership-copy",
                    "Photo 3 reused in alternate collection without recopy or wallpaper change")
            # Removing an additional membership must not delete the verified
            # private original still used by the original 22-photo collection.
            reach_and_click("Open photo details", "up", attempts=12)
            reach_and_click("Remove from this collection", "up", attempts=12)
            reach_and_click("Keep original and remove membership", "up", attempts=12)
            reach_and_click("Back to Reel / Grid", "down", attempts=12)
            removed_only_membership = False
            for _ in range(12):
                after_unlink = hierarchy()
                if "Photos: 2" in after_unlink:
                    removed_only_membership = True
                    break
                swipe("up")
            if not removed_only_membership:
                capture_failure("unlink-shared-membership-not-visible",
                                "Unlink did not restore alternate collection count")
                raise RuntimeError("Shared photo could not be unlinked from alternate collection")
            capture("22-cross-collection-membership-unlink",
                    "Alternate collection returned to two photos; source original retained")
            # Distinct text nodes can sit in different viewports at large type.
            # Observe each independently across bounded scrolling rather than
            # assuming the source title and explanation share one accessibility
            # snapshot after the first match.
            found_source = False
            found_disclosure = False
            for _ in range(12):
                source_snapshot = hierarchy()
                found_source |= "Shuffle collection: ATLAS FIXTURE · 22 photos" in source_snapshot
                found_disclosure |= (
                    "Browsing a different collection does not switch the shuffle"
                    in source_snapshot
                )
                if found_source and found_disclosure:
                    break
                swipe("up")
            if not found_source:
                capture_failure("rotation-source-not-visible",
                                "Unable to find named saved rotation source")
                raise RuntimeError("Current rotation source not labelled in native UI")
            if not found_disclosure:
                capture_failure("rotation-source-mismatch-unexplained",
                                "Browse/shuffle distinction missing over scroll path")
                raise RuntimeError("Browse collection switch lost source disclosure")
            capture("22-shuffle-source-after-browse-switch",
                    "Browsing alternate two-photo collection retains named 22-photo shuffle source")
            # Remove only the redundant container. Both photo originals must
            # remain in the first collection and rotation must stay attached
            # to its original 22-photo source.
            reach_and_click("Remove collection", "down", attempts=16)
            reach_and_click("Confirm remove collection only", "up", attempts=12)
            removed_alternate = False
            for attempt in range(14):
                removed_xml = hierarchy()
                if ("ALTERNATE ATLAS" not in removed_xml and
                        "ATLAS FIXTURE" in removed_xml):
                    removed_alternate = True
                    break
                if attempt % 2 == 0:
                    swipe("down")
            if not removed_alternate:
                capture_failure("redundant-collection-removal-unconfirmed",
                                "Alternate membership container not removed or wrong fallback")
                raise RuntimeError("Safe redundant collection removal did not return source")
            originals_after = adb("shell", "run-as", APP, "ls", "files/originals",
                                  check=False).splitlines()
            if len([name for name in originals_after if name.endswith(".jpg")]) != 22:
                raise RuntimeError("Collection removal unexpectedly changed immutable originals")
            capture("22-safe-redundant-collection-removal",
                    "Redundant collection removed, all 22 exact original files retained")
            adb("shell", "settings", "put", "system", "font_scale", "2.0")
            start_fixture(22)
            capture("22-reel-text-200-percent", "OS font scale 200%, first viewport")
            swipe("up")
            capture("22-text-200-percent-scrolled", "OS font scale 200%, following viewport")
            # The genuinely difficult layout: 320dp logical width at 200% text.
            # Keep verified 22-photo test data; relaunch real Activity to apply
            # the new display-density configuration without clearing Room.
            adb("shell", "wm", "density", "720")
            adb("shell", "am", "force-stop", APP, check=False)
            adb("shell", "am", "start", "-n", APP + "/.MainActivity")
            time.sleep(3.0)
            capture("22-compact-320dp-text-200pct-top",
                    "320dp logical width, OS 200% font, initial viewport")
            swipe("up")
            capture("22-compact-320dp-text-200pct-scrolled",
                    "320dp logical width, OS 200% font, first scroll")
            # A taller responsive Home or collection card may push the real
            # reel several viewports below a fixed two-swipe assumption.
            # Only label the capture as a reel when the actual native Canvas
            # has at least 240dp visible according to UIAutomator bounds.
            w, h, dpi = screen_geometry()
            minimum_visible = int(240 * dpi / 160)
            for attempt in range(12):
                xml = hierarchy()
                reel_node = next((
                    node for node in ET.fromstring(xml).iter("node")
                    if "Paper photo reel" in node.attrib.get("content-desc", "")
                ), None)
                if reel_node is not None:
                    raw_bounds = re.findall(r"\d+", reel_node.attrib.get("bounds", ""))
                    if len(raw_bounds) == 4:
                        x0, y0, x1, y1 = map(int, raw_bounds)
                        visible = max(0, min(y1, h) - max(y0, 0))
                        if visible >= minimum_visible:
                            break
                swipe("up")
            else:
                capture_failure("compact-reel-not-reachable",
                                "Paper reel Canvas never reached 240dp-visible threshold at 320dp/200% font")
                raise RuntimeError("Cannot show enough real paper reel at 320dp/200% type")
            capture("22-compact-320dp-text-200pct-reel",
                    "320dp logical width, OS 200% font, >=240dp actual paper reel Canvas visible")
            # Isolated destructive-to-fixture-only edit acceptance, AFTER all
            # main 22-photo/focus/shuffle/large-type checks. A real separate
            # SHA-named edited PNG is added to a three-photo Room collection.
            adb("shell", "wm", "density", "reset")
            adb("shell", "settings", "put", "system", "font_scale", "1.0")
            start_fixture(3)
            before_sources = adb("shell", "run-as", APP, "ls",
                                 "files/originals", check=False).splitlines()
            if len([name for name in before_sources if name.endswith(".jpg")]) != 3:
                raise RuntimeError("Variant fixture did not begin with 3 intact originals")
            reach_and_click("Open photo details", "up", attempts=12)
            reach_and_click("Edit photo (crop and rotate)", "up", attempts=14)
            reach_and_click("Rotate 90° right", "up", attempts=14)
            reach_and_click("Left crop edge", "up", attempts=16)
            reach_and_click("Save edit recipe…", "up", attempts=15)
            stored_recipe = False
            for _ in range(10):
                receipt = adb("shell", "run-as", APP, "cat",
                              "shared_prefs/glimpse_edit_recipes_v1.xml",
                              check=False)
                if "1|1|" in receipt:
                    stored_recipe = True
                    break
                time.sleep(0.5)
            if not stored_recipe:
                raise RuntimeError("The edited photo recipe was not retained in private preferences")
            reach_and_click("Save edited photo to this collection", "up", attempts=14)
            variant_selected = False
            time.sleep(2)
            # The position caption sits above the editor; UIAutomator only
            # reports visible nodes, so return to the top before reading it.
            for _ in range(6):
                swipe("down")
            for _ in range(24):
                view = hierarchy()
                if "Photograph 4 of 4" in view:
                    variant_selected = True
                    break
                time.sleep(0.8)
            if not variant_selected:
                capture_failure("edited-variant-not-selected",
                                "Native edited PNG was not attached and selected")
                raise RuntimeError("Edited variant did not appear as photograph 4 of 4")
            after_sources = adb("shell", "run-as", APP, "ls",
                                "files/originals", check=False).splitlines()
            if len([n for n in after_sources if n.endswith(".jpg")]) != 3:
                raise RuntimeError("Saving variant unexpectedly removed source originals")
            variants = [n for n in after_sources if n.endswith(".png") and
                        re.fullmatch(r"[0-9a-f]{64}\.png", n)]
            if len(variants) != 1:
                raise RuntimeError("Expected one new SHA-identified edited PNG source")
            variant_file = adb("exec-out", "run-as", APP, "cat",
                               "files/originals/" + variants[0],
                               check=False, binary=True)
            if not variant_file.startswith(b"\x89PNG\r\n\x1a\n"):
                raise RuntimeError("Saved edited member is not actual PNG pixels")
            width, height = struct.unpack(">II", variant_file[16:24])
            if not (1 <= width < 720 and height == 480):
                raise RuntimeError("Saved collection member lost rotation/crop geometry")
            if hashlib.sha256(variant_file).hexdigest() != variants[0][:-4]:
                raise RuntimeError("Saved collection PNG bytes disagree with filename SHA")
            capture("03-edited-variant-immutable-collection-copy",
                    "Actual cropped/rotated PNG became fourth collection photo; three originals retained")
    except Exception as error:
        failures.append(str(error))
        print("Atlas failed:", error, flush=True)
        capture_failure("atlas-unexpected-error", str(error))
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
