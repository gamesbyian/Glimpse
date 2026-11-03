#!/usr/bin/env python3
"""Read-only research: compare shell dumps with one persistent Appium UIA2 session.

This is NOT a new acceptance runner or a replacement for the native atlas.
It can run only on an isolated emulator with synthetic Glimpse test fixtures.
The real Android UIAutomator dump remains the authoritative reference.
"""
import argparse
import importlib.util
import json
import os
import statistics
import sys
import time
import urllib.error
import urllib.request
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
spec = importlib.util.spec_from_file_location(
    "glimpse_atlas_benchmark_base", ROOT / "scripts/capture-android-atlas.py")
base = importlib.util.module_from_spec(spec)
spec.loader.exec_module(base)

URL = "http://127.0.0.1:4723"
APP = "io.github.gamesbyian.glimpse"
PICKER = "com.android.providers.media.module"


def request(method, path, payload=None, timeout=120):
    body = json.dumps(payload).encode() if payload is not None else None
    req = urllib.request.Request(
        URL + path, method=method, data=body,
        headers={"Content-Type": "application/json"})
    try:
        with urllib.request.urlopen(req, timeout=timeout) as reply:
            result = json.loads(reply.read(8_000_000))
    except urllib.error.HTTPError as err:
        excerpt = err.read(500).decode(errors="replace")
        raise RuntimeError(f"Appium {method} {path} HTTP {err.code}: {excerpt}") from err
    if not isinstance(result, dict) or "value" not in result:
        raise RuntimeError(f"Appium {method} {path} returned no W3C value")
    value = result["value"]
    if isinstance(value, dict) and value.get("error"):
        raise RuntimeError(f"Appium {method} {path}: {value['error']}: "
                           f"{str(value.get('message', ''))[:250]}")
    return result


def appium_session():
    started = time.monotonic()
    result = request("POST", "/session", {
        "capabilities": {"alwaysMatch": {
            "platformName": "Android",
            "appium:automationName": "UiAutomator2",
            "appium:udid": "emulator-5554",
            "appium:noReset": True,
            "appium:autoLaunch": False,
            "appium:appPackage": APP,
            "appium:appActivity": ".MainActivity",
            "appium:newCommandTimeout": 180,
            "appium:uiautomator2ServerLaunchTimeout": 120000,
            "appium:uiautomator2ServerInstallTimeout": 120000
        }}
    }, timeout=180)
    value = result["value"]
    session_id = value.get("sessionId") if isinstance(value, dict) else None
    if not session_id:
        session_id = result.get("sessionId")
    if not isinstance(session_id, str) or not session_id:
        raise RuntimeError("Appium did not create a W3C session")
    # The independent legacy dump also has no ten-second idle wait. A short,
    # bounded idle timeout is needed for Glimpse's animated reel, but the
    # evidence comparison below determines whether this loses real nodes.
    request("POST", f"/session/{session_id}/appium/settings",
            {"settings": {"waitForIdleTimeout": 100}}, timeout=30)
    return session_id, round(time.monotonic() - started, 3)


def appium_source(session_id):
    response = request("GET", f"/session/{session_id}/source", timeout=90)
    value = response["value"]
    if not isinstance(value, str) or not value.lstrip().startswith("<?xml"):
        raise RuntimeError("Appium did not return complete XML page source")
    return value


def strict_shell_hierarchy():
    """Require fresh shell output, never read an old XML after dump failure.

    UiAutomator's one-active-connection constraint makes simultaneous shell
    and Appium probes invalid. Each shell read uses a unique destination,
    requires successful exit and an actual XML document, and runs only
    BEFORE a persistent session is started.
    """
    unique = "/sdcard/glimpse-hierarchy-probe-" + str(time.monotonic_ns()) + ".xml"
    try:
        result = base.adb("shell", "uiautomator", "dump", unique,
                          timeout=40)
        if "dumped to" not in result.lower():
            raise RuntimeError("uiautomator reported no fresh hierarchy dump")
        raw = base.adb("exec-out", "cat", unique, timeout=15)
        nodes(raw)
        return raw
    finally:
        base.adb("shell", "rm", "-f", unique, check=False)


def nodes(xml):
    root = ET.fromstring(xml)
    if root.tag != "hierarchy":
        raise ValueError(f"Unexpected accessibility root {root.tag!r}")
    result = set()
    bounds_count = 0
    for element in root.iter():
        if element is root:
            continue
        a = element.attrib
        label = (a.get("text") or a.get("content-desc") or "").strip()
        klass = a.get("class") or element.tag
        result.add((klass, a.get("resource-id", ""), a.get("text", ""),
                    a.get("content-desc", ""), a.get("clickable", "")))
        if a.get("bounds"):
            bounds_count += 1
    return result, bounds_count


def compare_xml(shell, appium):
    first, first_bounds = nodes(shell)
    second, second_bounds = nodes(appium)
    # Extra Appium nodes do not prove a regression, and the visual tree may
    # legitimately advance between two observations. Compare the labelled
    # subset and bounds availability, preserving mismatch visibility.
    labelled = lambda n: {node for node in n if node[2] or node[3]}
    left, right = labelled(first), labelled(second)
    matched = len(left & right)
    score = matched / len(left) if left else 0.0
    return {
        "shellNodes": len(first), "appiumNodes": len(second),
        "shellLabelledNodes": len(left), "appiumLabelledNodes": len(right),
        "labelledIntersection": matched,
        "shellLabelRecall": round(score, 4),
        "shellNodesWithBounds": first_bounds,
        "appiumNodesWithBounds": second_bounds,
        "equivalentForExperiment": bool(left and score >= .90 and
                                        second_bounds >= .85 * first_bounds)
    }


def measure(reader, attempts=5):
    samples = []
    last = ""
    for _ in range(attempts):
        start = time.monotonic()
        last = reader()
        elapsed = time.monotonic() - start
        # Force parsing on every call. Successful return of a cached or
        # partial XML string cannot be counted as a hierarchy observation.
        nodes(last)
        samples.append(round(elapsed * 1000, 3))
    return {
        "samplesMs": samples,
        "medianMs": round(statistics.median(samples), 3),
        "meanMs": round(statistics.mean(samples), 3)
    }, last


def scene(label, session, shell_baseline, expected_package=None):
    shell_times, shell = shell_baseline
    appium_times, appium = measure(lambda: appium_source(session))
    parity = compare_xml(shell, appium)
    # A package check confirms the actual subject: Glimpse Compose or an
    # Android platform window, never merely matching unrelated UI labels.
    def contains_package(xml, target):
        root = ET.fromstring(xml)
        return any(target == node.get("package", "")
                   for node in root.iter())
    packages = {
        "shellTargetVisible": contains_package(shell, expected_package)
        if expected_package else None,
        "appiumTargetVisible": contains_package(appium, expected_package)
        if expected_package else None
    }
    is_usable = parity["equivalentForExperiment"] and all(
        v is not False for v in packages.values())
    return {
        "scene": label, "shell": shell_times, "persistent": appium_times,
        "comparison": parity, "target": packages,
        "candidateCompatible": is_usable,
        "measuredReadSpeedup": round(
            shell_times["medianMs"] / appium_times["medianMs"], 3)
        if appium_times["medianMs"] > 0 else None,
    }


def appium_reach_and_click(session, label, direction="down", attempts=14):
    """Find a real visible clickable label using Appium, then native ADB tap."""
    import re
    for attempt in range(attempts):
        xml = appium_source(session)
        root = ET.fromstring(xml)
        candidates = []

        def visit(node, ancestors=()):
            a = node.attrib
            if label in (a.get("text", ""), a.get("content-desc", "")):
                target = next((n for n in (node, *reversed(ancestors))
                               if n.attrib.get("clickable") == "true"), None)
                if target is not None:
                    bounds = re.findall(r"\d+", target.attrib.get("bounds", ""))
                    if len(bounds) == 4:
                        candidates.append(tuple(map(int, bounds)))
            for child in node:
                visit(child, ancestors + (node,))
        visit(root)
        if candidates:
            x0, y0, x1, y1 = candidates[0]
            base.adb("shell", "input", "tap",
                     str((x0+x1)//2), str((y0+y1)//2))
            time.sleep(0.65)
            return
        if attempt != attempts - 1:
            base.swipe(direction)
    raise RuntimeError("Appium could not locate actual visible action " + label)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--out", type=Path, required=True)
    args = parser.parse_args()
    out = args.out.resolve()
    out.mkdir(parents=True, exist_ok=True)
    base.OUT = out

    if base.adb("shell", "getprop", "ro.kernel.qemu").strip() != "1":
        raise RuntimeError("REFUSE instrumentation on any physical phone")
    if not request("GET", "/status", timeout=10)["value"].get("ready"):
        raise RuntimeError("Appium is not ready to create a UIAutomator2 session")

    print("Preparing disposable real Android Glimpse 3-photo UI", flush=True)
    base.start_fixture(3)

    # Capture actual shell evidence while shell UiAutomator owns the
    # automation connection. Running it simultaneously with Appium can fail
    # quickly and silently reuse the previous XML file, fabricating speed.
    shell_compose = measure(strict_shell_hierarchy)
    shell_picker = None
    shell_picker_limitation = None
    try:
        base.reach_and_click("Add photos", "down", attempts=14)
        shell_picker = measure(strict_shell_hierarchy)
        picker_xml = shell_picker[1]
        if not any(n.get("package") == PICKER
                   for n in ET.fromstring(picker_xml).iter()):
            raise RuntimeError("Real Android system Photo Picker was not visible")
    except Exception as error:
        shell_picker = None
        shell_picker_limitation = str(error)[:400]
    finally:
        base.adb("shell", "input", "keyevent", "4")
        base.adb("shell", "am", "start", "-n", APP + "/.MainActivity")
        time.sleep(0.6)

    session = None
    report = {
        "schema": "glimpse/hierarchy-backend-research/v2",
        "researchOnly": True,
        "shellReadFreshnessEnforced": True,
        "simultaneousAutomationDisallowed": True,
        "changesNativeAtlas": False,
        "sourceSha": os.environ.get("GITHUB_SHA", ""),
        "device": base.adb("shell", "getprop", "ro.build.version.release"),
        "scenes": [], "appiumSessionSeconds": None,
        "appiumSetupOutsideSessionSeconds": "See GitHub workflow step timing",
        "requiredSystemUiParityVerified": False
    }
    if shell_picker_limitation:
        report["shellSystemPickerLimitation"] = shell_picker_limitation
    try:
        session, report["appiumSessionSeconds"] = appium_session()
        base.adb("shell", "am", "start", "-n", APP + "/.MainActivity")
        time.sleep(0.5)
        report["scenes"].append(scene(
            "glimpse-compose-3-photos", session, shell_compose, APP))
        if shell_picker is not None:
            try:
                appium_reach_and_click(session, "Add photos", "down", attempts=14)
                picker = scene(
                    "real-android-photo-picker", session, shell_picker, PICKER)
                report["scenes"].append(picker)
                report["requiredSystemUiParityVerified"] = picker["candidateCompatible"]
            except Exception as error:
                # Missing/changed OS semantics is explicitly NOT compatible.
                report["systemPickerLimitation"] = str(error)[:400]
        report["appSceneEquivalent"] = report["scenes"][0]["candidateCompatible"]
        report["readyForProductionTrial"] = (
            report["appSceneEquivalent"] and
            report["requiredSystemUiParityVerified"] and
            all(s["persistent"]["medianMs"] < s["shell"]["medianMs"]
                for s in report["scenes"]))
    finally:
        if session:
            try:
                request("DELETE", f"/session/{session}", timeout=30)
            except Exception as error:
                report["cleanupWarning"] = str(error)[:200]
        (out / "hierarchy-benchmark.json").write_text(
            json.dumps(report, indent=2, sort_keys=True) + "\n",
            encoding="utf-8")
    print("Hierarchy transport research:", json.dumps(report, sort_keys=True),
          flush=True)
    if not report["appSceneEquivalent"]:
        print("NOT QUALIFIED: Appium does not provide equivalent Glimpse semantics",
              flush=True)
    elif not report["readyForProductionTrial"]:
        print("NOT QUALIFIED: native Android system UI parity or speed gate missing",
              flush=True)
    else:
        print("PROMISING ONLY: benchmark compatible; no atlas backend changed",
              flush=True)


if __name__ == "__main__":
    try:
        main()
    except Exception as error:
        print(f"FAIL: Android accessibility transport benchmark: {error}",
              file=sys.stderr, flush=True)
        raise
