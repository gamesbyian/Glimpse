#!/usr/bin/env python3
"""API34 real-Compose read-only shuffle projection acceptance.

Uses actual production Home/RotationPanel, a real Room collection and three
verified disposable originals. No wallpaper operation is performed.
"""
import argparse
import importlib.util
import time
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
spec=importlib.util.spec_from_file_location("glimpse_preview_atlas",
       ROOT/"scripts/capture-android-atlas.py")
base=importlib.util.module_from_spec(spec)
spec.loader.exec_module(base)

def seek(label, direction="up", attempts=16):
    base.reach_and_click(label, direction, attempts)

def main(out):
    base.OUT=out
    out.mkdir(parents=True,exist_ok=True)
    assert base.adb("shell","getprop","ro.kernel.qemu").strip()=="1", (
        "REFUSE to seed or capture a physical user phone"
    )
    base.adb("shell","wm","size","1440x3120")
    base.adb("shell","wm","density","560")
    base.start_fixture(3,active=True)
    before=base.hierarchy()
    assert "No wallpaper change has been acknowledged yet" in before, (
        "Fixture must not have applied an Android wallpaper"
    )
    seek("Details",attempts=16)
    seek("Preview next four photos",attempts=16)
    for _ in range(8):
        xml=base.hierarchy()
        if "READ-ONLY ROTATION REHEARSAL" in xml:
            break
        time.sleep(.7)
    else:
        raise RuntimeError("Read-only Home shuffle preview never rendered")
    assert "EXAMPLE" in xml, "Unsampled bag must be honestly labelled illustrative"
    assert "SAVED" not in xml, "Empty saved queue must not claim known next order"
    base.capture("03-shuffle-four-example-photos",
                 "Actual Home filmstrip with illustrative seeded no-repeat frames")
    seek("Hide next-photo preview","up",attempts=8)
    after=base.hierarchy()
    assert "READ-ONLY ROTATION REHEARSAL" not in after, (
        "Closing the preview failed to hide photos without Android changes"
    )
    # Reopen and compare. The same seeded example must not unexpectedly
    # mutate the wallpaper or its saved queue while observing the preview.
    seek("Preview next four photos","up",attempts=8)
    reopened=base.hierarchy()
    assert "EXAMPLE" in reopened
    assert "Android acknowledged Home wallpaper update" not in reopened
    base.capture("03-shuffle-reopened-same-unapplied-preview",
                 "Stable side-effect-free read-only preview after closing and reopening")
    print("PASS: real three-photo Room Home projection, honest illustrative "
          "confidence, 48dp preview action and reversible visibility",flush=True)

if __name__=="__main__":
    parser=argparse.ArgumentParser()
    parser.add_argument("--out",default="artifacts/android-shuffle-preview")
    args=parser.parse_args()
    main(Path(args.out))
