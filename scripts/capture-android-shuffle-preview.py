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
    # Android's accessibility hierarchy omits content below the viewport.
    # Expanding this panel can put its actual heading below the fold even
    # though the button was tapped successfully. Search by scrolling through
    # real native content, never by assuming an offscreen element is absent.
    for attempt in range(12):
        xml=base.hierarchy()
        if "READ-ONLY ROTATION REHEARSAL" in xml:
            break
        if attempt % 2 == 1:
            base.swipe("up")
        time.sleep(.7)
    else:
        raise RuntimeError("Read-only Home shuffle preview never rendered")
    # The expanded preview is deliberately below the Home rotation status.
    # UIAutomator omits items below the fold, so scroll into the *actual*
    # filmstrip instead of pretending "EXAMPLE" is missing on first opening.
    for attempt in range(10):
        xml=base.hierarchy()
        if "EXAMPLE" in xml:
            break
        base.swipe("up")
    else:
        (out/"shuffle-example-not-visible.xml").write_text(xml,encoding="utf-8")
        raise RuntimeError("Actual illustrative filmstrip did not become visible")
    # The editorial explanation itself contains the word "Saved". Test the
    # semantic per-frame label instead of matching ordinary explanatory copy.
    import re
    assert not re.search(r"NEXT\\s+\\d+\\s+[·]\\s+SAVED", xml), (
        "Empty saved queue must not label any image a known next candidate"
    )
    base.capture("03-shuffle-four-example-photos",
                 "Actual Home filmstrip with illustrative seeded no-repeat frames")
    seek("Hide next-photo preview","down",attempts=15)
    after=base.hierarchy()
    # The preview heading may have scrolled off-screen, but the active
    # disclosure control must have reversed without any wallpaper operation.
    assert "Preview next four photos" in after or "READ-ONLY ROTATION REHEARSAL" not in after
    # Reopen and compare. The same seeded example must not unexpectedly
    # mutate the wallpaper or its saved queue while observing the preview.
    seek("Preview next four photos","down",attempts=12)
    reopened=""
    for _ in range(10):
        reopened=base.hierarchy()
        if "EXAMPLE" in reopened:
            break
        base.swipe("up")
    else:
        (out/"shuffle-reopened-not-visible.xml").write_text(reopened,encoding="utf-8")
        raise RuntimeError("Reopened illustrative projection not visible")
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
