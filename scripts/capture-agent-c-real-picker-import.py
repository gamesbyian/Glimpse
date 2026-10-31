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


def wait_for_photo_count(n, timeout=32, scroll=False):
    end = time.monotonic() + timeout
    attempts = 0
    last_xml = ""
    while time.monotonic() < end:
        xml = base.hierarchy()
        last_xml = xml
        # The fixed paper Reel can physically overlay the count label.
        # UIAutomator omits occluded Compose descendants even when PNG shows
        # "Photos: n" and the collection-card n-photograph count. Its
        # independently Room-backed "Share n originals" action *is* exposed
        # in the actual failed emulator XML. Require either live Room count,
        # then independently require exact SHA/source-byte parity below.
        count_visible = "Photos: " + str(n) in xml
        room_count_action = f"Share {n} originals" in xml
        if (count_visible or room_count_action) and "No images yet." not in xml:
            return xml
        # Photo count is below the paper-reel hero. Android's accessibility
        # tree omits below-fold Compose descendants. Scroll ONLY after an
        # actual Android picker grant/return, never inside the picker itself.
        if scroll and attempts % 3 == 2 and                 "com.android.providers.media.module" not in xml:
            base.swipe("up")
        attempts += 1
        time.sleep(1)
    if scroll:
        (base.OUT / f"picker-photo-count-{n}-not-visible.xml").write_text(
            last_xml, encoding="utf-8"
        )
        base.capture_failure("post-picker-count-missing",
            f"Count {n} not visible after actual Android picker return and scrolling")
    return None


def main(out):
    base.OUT = out
    out.mkdir(parents=True, exist_ok=True)
    if base.adb("shell", "getprop", "ro.kernel.qemu").strip() != "1":
        raise RuntimeError("REFUSE picker selection fixture on physical handset")
    base.start_fixture(0)
    # Fixture readiness means Room writes completed, not that Activity/Compose
    # has completed its first StateFlow and layout emission. Wait for the
    # actual empty production UI rather than reading one transient hierarchy.
    initial_xml = ""
    for attempt in range(20):
        initial_xml = base.hierarchy()
        if "No images yet." in initial_xml and "Photos: 0" in initial_xml:
            break
        if attempt % 2 == 1:
            # Home's actual collection count and empty state live *below*
            # the photographic cover and controls. UIAutomator reports only
            # visible descendants. Look at the real viewport while scrolling.
            base.swipe("up")
        time.sleep(.6)
    else:
        (out / "initial-empty-state-not-rendered.xml").write_text(
            initial_xml, encoding="utf-8"
        )
        raise RuntimeError("Initial empty Room collection state not visible after scrolling")
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
    (out / "published-fixture-original.jpg").write_bytes(source_data)
    print("Published one synthetic Android MediaStore JPEG:", record["sourceUri"], flush=True)
    base.reach_and_click("Add photos", "down", attempts=14)
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
                  and node.attrib.get("text", "") == "Add (1)"]
        if len(accept) != 1:
            raise RuntimeError("Cannot identify one Android picker confirm action: " +
                               str([(n.attrib.get("text"), n.attrib.get("content-desc"))
                                    for n in accept]))
        tap_node(accept[0])
    ui = wait_for_photo_count(1, scroll=True)
    if not ui:
        raise RuntimeError("Actual production MainActivity did not import the selected photo")
    base.capture("03-glimpse-after-real-picker-import",
                 "Production Glimpse displays one source selected via Android Photo Picker")
    copied = base.adb("exec-out", "run-as", APP, "cat",
                      "files/originals/" + record["sourceSHA256"] + ".jpg",
                      binary=True)
    assert copied == source_data, "Private offline JPEG no longer byte-identical to selected source"
    assert hashlib.sha256(copied).hexdigest() == record["sourceSHA256"]
    assert ("Previewing 1 of 1" in ui or "Photos: 1" in ui or
            "Share 1 originals" in ui)
    assert "No images yet." not in ui
    # Select the *same exact original* a second time through the real OS
    # Photo Picker. A correct importer reports a duplicate and does not create
    # a second membership/photo or rewrite the immutable SHA identity.
    # The first successful import leaves the viewport at the collection
    # count/hero; the real Add photos control may be below the finite reel.
    # Use the same bounded, visible-action seeker as the first picker launch,
    # rather than demanding that an offscreen Compose button be in the
    # current UIAutomator hierarchy.
    # Scroll *upward by finger* to move toward lower page content.
    # The first launch starts after a different setup scroll state and uses
    # the opposite direction; do not copy it for this second launch.
    base.reach_and_click("Add photos", "up", attempts=14)
    time.sleep(1.5)
    second = visible_nodes()
    assert "com.android.providers.media.module" in {
        x.attrib.get("package", "") for x in second
    }, "The second Add photos tap did not focus Android's real picker"
    matching = []
    for node in second:
        d, a = node.attrib, bounds(node)
        if d.get("package") != "com.android.providers.media.module" or not a:
            continue
        if d.get("clickable") != "true" or a[3]-a[1] < 45 or a[2]-a[0] < 45 or a[1] < 500:
            continue
        labels = (d.get("resource-id", "")+" "+d.get("content-desc", "")+" "+
                  d.get("text", "")).lower()
        if any(k in labels for k in ("photo", "thumbnail", "image", "media_grid", "item")):
            matching.append(node)
    assert len(matching) == 1, "Second picker has ambiguous picture tile count: "+str(len(matching))
    tap_node(matching[0])
    if not wait_for_photo_count(1, timeout=3):
        buttons = [node for node in visible_nodes()
                   if node.attrib.get("clickable") == "true"
                   and node.attrib.get("resource-id") ==
                       "com.android.providers.media.module:id/button_add"
                   and node.attrib.get("text") == "Add (1)"]
        assert len(buttons) == 1, "Second picker confirmation was not uniquely Add (1)"
        tap_node(buttons[0])
    top_after_repeat = wait_for_photo_count(1, timeout=32, scroll=True)
    if not top_after_repeat:
        raise RuntimeError("Repeat picker action failed to return to a one-photo Glimpse collection")
    base.capture("04-after-repeat-real-picker-photo-count",
                 "Repeat actual OS picker selection still shows exactly Photos: 1")
    # The duplicate result must be visible *beside its initiating action*,
    # not merely somewhere in the long scrolling column behind the fixed Reel.
    # Verify actual Android bounds for both nodes on one screen. The earlier
    # implementation displayed the correct text only beneath RotationPanel.
    notice = None
    screen_height = base.screen_geometry()[1]
    for _ in range(13):
        visible = base.hierarchy()
        nodes = list(ET.fromstring(visible).iter("node"))
        duplicate = next((n for n in nodes
                          if "duplicates 1" in n.attrib.get("text", "") and
                             "imported 0" in n.attrib.get("text", "")), None)
        add = next((n for n in nodes
                    if n.attrib.get("text") == "Add photos"), None)
        duplicate_box = bounds(duplicate) if duplicate is not None else None
        add_box = bounds(add) if add is not None else None
        if duplicate_box and add_box:
            gap = duplicate_box[1] - add_box[3]
            if (0 <= gap < int(screen_height * .30) and
                (duplicate_box[1] + duplicate_box[3]) // 2 <
                    int(screen_height * .55)):
                notice = visible
                break
        base.swipe("up")
    if notice is None:
        base.capture_failure("real-picker-duplicate-report-occluded",
                             "Duplicate result and Add photos are not jointly visible above Reel")
        raise RuntimeError(
            "Real picker duplicate report is not visibly adjacent to Add photos above the fixed Reel"
        )
    base.capture("05-after-repeat-real-picker-duplicate-report",
                 "Actual Android UI shows imported 0 / duplicates 1 immediately beside Add photos")
    # Restore the Room-backed photo count without assuming a fixed number
    # of screens was needed to reveal the paired action/result.
    for _ in range(14):
        visible = base.hierarchy()
        if ("Photos: 1" in visible or "Share 1 originals" in visible) and \
                "No images yet." not in visible:
            break
        base.swipe("down")
    else:
        base.capture_failure("post-duplicate-count-not-restored",
                             "Photo count not visible after bounded upward page traversal")
        raise RuntimeError("Failed to restore Photos: 1 view after checking duplicate report")
    assert base.adb("exec-out","run-as",APP,"cat",
                    "files/originals/"+record["sourceSHA256"]+".jpg",
                    binary=True) == source_data
    # Follow the same photograph through public source removal and a complete
    # Android app-process restart. Only our exact MediaStore URI is deleted by
    # the debug-only emulator activity, never a user's photograph.
    base.adb("shell", "am", "start", "-n", FIXTURE, "--ez", "cleanup", "true")
    cleanup = None
    for _ in range(30):
        payload = base.adb("shell", "run-as", APP, "cat",
                           "files/picker-fixture-cleanup.json", check=False)
        if payload.startswith("{"):
            cleanup = json.loads(payload)
            break
        time.sleep(.5)
    assert cleanup and cleanup.get("deleted") == 1, (
        "Synthetic source removal not confirmed: " + str(cleanup)
    )
    assert cleanup.get("sourceSHA256") == record["sourceSHA256"]
    base.adb("shell", "am", "force-stop", APP)
    base.adb("shell", "am", "start", "-n", APP + "/.MainActivity")
    restarted = wait_for_photo_count(1, timeout=30)
    if not restarted:
        raise RuntimeError("Private imported photograph missing after public deletion and app restart")
    persisted = base.adb("exec-out", "run-as", APP, "cat",
                         "files/originals/" + record["sourceSHA256"] + ".jpg",
                         binary=True)
    assert persisted == source_data, "Offline original bytes changed after deleting public MediaStore image"
    (out / "private-after-restart.jpg").write_bytes(persisted)
    base.capture("06-after-media-source-deleted-and-process-restart",
                 "Glimpse still previews the exact private JPEG after public MediaStore deletion and app restart")
    receipt = {
        "sourceIdentity": record["sourceSHA256"],
        "sourceUriScheme": "content",
        "sourceProvider": "Android MediaStore via OS Photo Picker",
        "sourceBytes": record["sourceBytes"],
        "sourceFixtureArtifact": "published-fixture-original.jpg",
        "privateAfterRestartArtifact": "private-after-restart.jpg",
        "sourceEqualsPrivateCopy": True,
        "sameSourceReimportDeduplicated": True,
        "secondPickerImportCount": 0,
        "secondPickerDuplicateCount": 1,
        "uiPhotoCountAfterRepeatedImport": 1,
        "sourceDeletedFromMediaStore": True,
        "exactPrivateCopySurvivedProcessRestart": True,
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
