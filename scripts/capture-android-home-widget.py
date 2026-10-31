#!/usr/bin/env python3
"""Real API34 AppWidgetHost acceptance for the production Glimpse widget.

The emulator grants the one widget bind through Android's REAL
ACTION_APPWIDGET_BIND consent dialog. No privileged shell grant, production
binding permission or mock host is used. Only disposable emulator media.
"""
import argparse
from pathlib import Path
import re
import subprocess
import time
import xml.etree.ElementTree as ET

APP = "io.github.gamesbyian.glimpse"
def adb(*args, check=True):
    result = subprocess.run(["adb", *args], check=check, capture_output=True)
    return result.stdout.decode("utf-8", errors="replace").strip()

def wait_for(predicate, description, attempts=30):
    for _ in range(attempts):
        try:
            outcome = predicate()
            if outcome:
                return outcome
        except (RuntimeError, subprocess.CalledProcessError, ET.ParseError):
            pass
        time.sleep(1)
    raise RuntimeError("Timed out: " + description)

def hierarchy():
    adb("shell", "uiautomator", "dump", "/sdcard/glimpse-widget.xml")
    return adb("shell", "cat", "/sdcard/glimpse-widget.xml")

def labels(xml):
    return " ".join(
        node.attrib.get(key, "")
        for node in ET.fromstring(xml).iter("node")
        for key in ("text", "content-desc")
    )

def capture(out, label):
    (out / (label + ".xml")).write_text(hierarchy(), encoding="utf-8")
    png = subprocess.run(["adb", "exec-out", "screencap", "-p"],
                         check=True, capture_output=True).stdout
    assert png.startswith(b"\x89PNG\r\n\x1a\n"), "Not a genuine Android screenshot"
    (out / (label + ".png")).write_bytes(png)
    print("Captured", label, len(png), "PNG bytes", flush=True)

def start_probe(out):
    # A previous force-stop bypasses Activity.onDestroy(), so the old READY
    # file can survive. Delete it *before* launching a fresh widget host.
    # Otherwise the wait loop sees a stale ID and leaves Android's second
    # real widget-bind consent dialog unanswered.
    adb("shell", "run-as", APP, "rm", "-f", "files/widget-probe-ready.txt")
    adb("shell", "am", "start", "-n", APP + "/.debug.WidgetHostProbeActivity")
    user_approved = False
    for _ in range(45):
        ready = adb("shell", "run-as", APP,
                    "cat", "files/widget-probe-ready.txt", check=False)
        if ready.startswith("ERROR:"):
            raise RuntimeError("Actual Android widget host rejected binding: " + ready)
        if ready.startswith("READY:"):
            print("Real widget host:", ready, flush=True)
            try:
                return wait_for(
                    lambda: (xml if "G L I M P S E" in labels(xml)
                            else None) if (xml := hierarchy()) else None,
                    "production RemoteViews inflation", 35
                )
            except RuntimeError:
                capture(out, "widget-host-inflation-failure")
                (out / "widget-activity-state.txt").write_text(
                    adb("shell", "dumpsys", "activity", "activities", check=False),
                    encoding="utf-8"
                )
                (out / "widget-system-state.txt").write_text(
                    adb("shell", "dumpsys", "appwidget", check=False),
                    encoding="utf-8"
                )
                raise

        if not user_approved:
            xml = hierarchy()
            root = ET.fromstring(xml)
            # Android Settings asks for this *one* widget. Never tick
            # "Always allow". A different dialog with no exact supported
            # positive button must fail rather than grant blindly.
            buttons = [
                n for n in root.iter("node")
                if n.attrib.get("text", "").strip().casefold() in {
                    "create", "add", "allow", "add widget"
                } and n.attrib.get("clickable") == "true"
            ]
            if len(buttons) == 1:
                name = buttons[0].attrib.get("text", "")
                capture(out, "android-system-widget-bind-consent")
                tap(name)
                print("Approved one Android widget bind through real UI:", name,
                      flush=True)
                user_approved = True
        time.sleep(.8)
    raise RuntimeError("Timed out waiting for real Android widget bind result")

def tap(label):
    xml = ET.fromstring(hierarchy())
    def visit(node, parents=()):
        words = (node.attrib.get("text", ""), node.attrib.get("content-desc", ""))
        if any(label in text for text in words):
            target = next((n for n in (node, *reversed(parents))
                           if n.attrib.get("clickable") == "true"), node)
            bounds = re.findall(r"\d+", target.attrib.get("bounds", ""))
            if len(bounds) == 4:
                x0,y0,x1,y1 = map(int, bounds)
                adb("shell", "input", "tap", str((x0+x1)//2),str((y0+y1)//2))
                return True
        return any(visit(c, parents + (node,)) for c in node)
    if not visit(xml):
        raise RuntimeError("Real Android action was missing: " + label)

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--out", required=True)
    args = parser.parse_args()
    out = Path(args.out)
    out.mkdir(parents=True, exist_ok=True)

    # Exercise the user-visible Android ACTION_APPWIDGET_BIND permission
    # dialog instead of the shell appwidget grantbind command, which some
    # API34 emulator images kill with exit 137 before invoking the service.
    adb("wait-for-device")
    wait_for(lambda: adb("shell", "getprop", "sys.boot_completed") == "1",
             "emulator boot complete", 45)
    adb("shell", "pm", "clear", APP)
    time.sleep(2)
    start_probe(out)
    wait_for(lambda: "Choose a collection" in labels(hierarchy()),
             "empty widget must explicitly require saved source")
    assert "OPEN GLIMPSE" in labels(hierarchy())
    capture(out, "widget-empty-source")
    tap("OPEN GLIMPSE")
    wait_for(lambda: "Glimpse" in labels(hierarchy()),
             "widget's private Open intent returns to app")

    adb("shell", "am", "start", "-n", APP + "/.debug.VisualFixtureLauncher",
        "--ei", "count", "3", "--ez", "active", "true")
    wait_for(lambda: "READY:3:" in adb("shell","run-as",APP,
            "cat","files/visual-atlas-ready.txt",check=False),
            "actual Room records from synthetic offline image fixture", 35)
    # The fixture starts MainActivity asynchronously after saving its Room
    # rows. It can otherwise resume over the new widget host after READY.
    # Settle that navigation, then deliberately start the widget host as
    # the last foreground activity; force-stop keeps the actual Room bytes.
    time.sleep(3)
    adb("shell", "am", "force-stop", APP)
    start_probe(out)
    wait_for(lambda: "Next photo unconfirmed" in labels(hierarchy()),
             "an unsampled shuffle must never show a fictitious next photo", 45)
    assert "CHANGE HOME" in labels(hierarchy())
    capture(out, "widget-true-undecided-draw")
    # Android may recreate the host Activity while applying a widget
    # RemoteViews update. Require the *same* real labeled action to be
    # actionable in a fresh UI hierarchy before touching it; never blindly
    # reuse old coordinates, synthesize a broadcast or claim a wallpaper set.
    for attempt in range(5):
        try:
            tap("CHANGE HOME")
            break
        except RuntimeError as failure:
            if "Real Android action was missing: CHANGE HOME" not in str(failure):
                raise
            if attempt == 4:
                capture(out, "widget-change-action-disappeared")
                (out / "widget-change-action-activity-state.txt").write_text(
                    adb("shell", "dumpsys", "activity", "activities", check=False)[:100000],
                    encoding="utf-8")
                raise
            time.sleep(0.7)
    # The click itself is not an acknowledgment. Capture actual post-tap UI
    # and OS state, so a wrong PendingIntent, stalled worker or failed Android
    # wallpaper call is diagnosable rather than an opaque late timeout.
    capture(out, "widget-immediately-after-change-request")
    # A real Android wallpaper request may acknowledge, report unknown, or
    # be delayed. Never falsify a success; the updated widget must show one
    # of the gateway's durable truthful states.
    def check_updated_widget():
        xml = hierarchy()
        seen = labels(xml)
        if any(text in seen for text in (
            "Next in the bag", "Next saved photo", "Result uncertain"
        )):
            return xml
        return None

    try:
        updated = wait_for(
            check_updated_widget,
            "widget refresh after an actual user-requested Home command", 14
        )
    except RuntimeError:
        capture(out, "widget-not-refreshed-after-change-request")
        # The debug Activity records the actual DB command outcome without
        # paths or IDs. Never mistake a host's XML initialLayout for a
        # failed WallpaperManager request, or for a successful one.
        (out / "widget-command-outcome.json").write_text(
            adb("shell", "run-as", APP,
                "cat", "files/widget-probe-rotation-state.json",
                check=False)[:512], encoding="utf-8"
        )
        # This emulator is seeded only with generated images; do not dump
        # full databases, URI grants, or provider contents into CI artifacts.
        for name, args in (
            ("widget-postclick-appwidget-state.txt", ("shell", "dumpsys", "appwidget")),
            ("widget-postclick-activity-state.txt", ("shell", "dumpsys", "activity", "activities")),
            ("widget-postclick-work-state.txt", ("shell", "dumpsys", "jobscheduler")),
        ):
            (out / name).write_text(
                adb(*args, check=False)[:100000], encoding="utf-8"
            )
        raise
    actual = labels(updated)
    assert "Choose a collection" not in actual
    capture(out, "widget-after-actual-home-command")
    print("PASS: Android AppWidgetHost, empty state, verified source, " +
          "honest undecided preview and user-requested result", flush=True)

if __name__ == "__main__":
    main()
