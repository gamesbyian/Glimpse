#!/usr/bin/env python3
"""Verify a real Android ACTION_SEND entry using only emulator-owned synthetic media."""
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
FIXTURE = APP + "/.debug.PickerMediaFixtureLauncher"

def until(fn, seconds=35):
    end = time.monotonic() + seconds
    while time.monotonic() < end:
        value = fn()
        if value:
            return value
        time.sleep(0.8)
    return None

def visible(fragment):
    xml = base.hierarchy()
    return xml if fragment in xml else None

def one_imported_photo():
    # The Room-backed collection cover/count can be visible when the
    # lower "Photos: 1" label is pushed below the fold by new Home actions.
    # The SHA-named private original is checked independently afterward.
    xml = base.hierarchy()
    return xml if ("Photos: 1" in xml or "1 photograph" in xml) else None

def send(uri):
    report = base.adb("shell","am","start","-W","-a","android.intent.action.SEND",
                      "-p",APP,"-t","image/jpeg","--grant-read-uri-permission",
                      "--eu","android.intent.extra.STREAM",uri)
    if "Error" in report:
        raise RuntimeError("Android refused share intent: " + report[:300])
    print("Dispatched Android ACTION_SEND", flush=True)

def main(out):
    out.mkdir(parents=True, exist_ok=True)
    base.OUT = out
    base.RESULTS.clear()
    if base.adb("shell","getprop","ro.kernel.qemu").strip() != "1":
        raise RuntimeError("REFUSE synthetic-image testing on physical hardware")
    base.start_fixture(0)
    assert visible("No images yet."), "Room fixture not empty"
    base.adb("shell","am","start","-n",FIXTURE)
    raw = until(lambda: base.adb("shell","run-as",APP,"cat",
                                "files/picker-fixture-ready.json",check=False))
    assert raw and raw.startswith("{"), "No real MediaStore fixture"
    fixture = json.loads(raw)
    assert fixture.get("status") == "READY", "Fixture failed"
    uri = fixture["sourceUri"]
    assert uri.startswith("content://media/")
    original = base.adb("exec-out","run-as",APP,"cat","files/picker-fixture-original.jpg",
                        binary=True)
    sha = hashlib.sha256(original).hexdigest()
    assert sha == fixture["sourceSHA256"]
    send(uri)
    pending = until(lambda: visible("Import shared images"))
    if not pending:
        base.capture_failure("no-share-banner","Missing explicit incoming confirmation")
        raise RuntimeError("ACTION_SEND was not received")
    # The incoming banner pushes the empty-list label below the viewport on
    # many screen sizes. UIAutomator cannot prove a missing Room membership.
    # Check that no exact SHA-named original was privately committed instead.
    stored_before = base.adb("shell","run-as",APP,"ls","files/originals",
                             check=False)
    assert sha + ".jpg" not in stored_before, "Imported without consent"
    base.capture("01-unconfirmed-share","Shared image has not imported until confirmation")
    base.click_label("Dismiss")
    stored_after_dismiss = base.adb("shell","run-as",APP,"ls","files/originals",
                                    check=False)
    assert sha + ".jpg" not in stored_after_dismiss, "Dismiss imported data"
    assert not visible("Import shared images"), "Dismiss did not clear share"
    base.capture("02-dismissed","Dismissed share without side effects")
    send(uri)
    assert until(lambda: visible("Import shared images")), "Second share not delivered"
    base.click_label("Import shared images")
    confirmed = until(one_imported_photo,seconds=40)
    if not confirmed:
        base.capture_failure("no-import","No production Room update after explicit import")
        raise RuntimeError("Shared original was not committed")
    assert "Import shared images" not in confirmed, "Accepted share replayed"
    base.capture("03-explicit-import","User-confirmed shared original is in collection")
    copied = base.adb("exec-out","run-as",APP,"cat",
                      "files/originals/"+sha+".jpg",binary=True)
    assert copied == original, "Private SHA original differs from external share"
    base.adb("shell","am","force-stop",APP)
    base.adb("shell","am","start","-n",APP+"/.MainActivity")
    restored = until(one_imported_photo)
    assert restored and "Import shared images" not in restored
    assert base.adb("exec-out","run-as",APP,"cat",
                    "files/originals/"+sha+".jpg",binary=True) == original
    base.capture("04-process-restart","Stored private original survived restart; no share replay")
    (out/"receipt.json").write_text(json.dumps({
        "source":"synthetic Android MediaStore via ACTION_SEND",
        "sha256":sha,"bytes":len(original),"dismissedWithoutImport":True,
        "explicitConfirmationRequired":True,"privateBytesIdentical":True,
        "survivedRestart":True,"humanAcceptance":False
    },indent=2)+"\n")
    (out/"manifest.json").write_text(json.dumps(base.RESULTS,indent=2)+"\n")
    base.write_gallery()

if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--out",type=Path,default=Path("artifacts/incoming-image-share"))
    main(parser.parse_args().out.resolve())
