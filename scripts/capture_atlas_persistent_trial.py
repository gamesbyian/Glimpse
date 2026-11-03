#!/usr/bin/env python3
"""Experimental full native atlas using a persistent accessibility session.

Kept deliberately separate from the authoritative shell-based native atlas.
No emulator-less rendering, fixture changes, app source changes or silent
backend fallback. A failed Appium read fails the actual Android journey.
"""
import argparse
import json
import os
import sys
import time
import xml.etree.ElementTree as ET
from pathlib import Path

import benchmark_hierarchy_transport as research

base = research.base


def normalize_hierarchy(raw):
    """Normalize Appium's element tags for existing Android XML consumers.

    Android's shell dumper emits <node class="..."> elements; Appium's
    UiAutomator2 emits tags named <android.widget.*>. Every attribute used
    by Glimpse's established real-device assertions is retained.
    """
    root = ET.fromstring(raw)
    if root.tag != "hierarchy":
        raise RuntimeError("Persistent Android accessibility source has no hierarchy root")
    observed = 0
    for element in root.iter():
        if element is root:
            continue
        original = element.tag
        if not isinstance(original, str) or original.startswith("{"):
            raise RuntimeError("Unexpected Appium element namespace")
        if original != "node":
            element.set("class", element.attrib.get("class") or original)
            element.tag = "node"
        observed += 1
    if observed < 2:
        raise RuntimeError("Persistent Android returned an implausibly empty hierarchy")
    return ('<?xml version="1.0" encoding="UTF-8"?>\n' +
            ET.tostring(root, encoding="unicode"))


class PersistentAtlasHierarchy:
    def __init__(self, session_id):
        self.session_id = session_id
        self.calls = 0
        self.seconds = 0.0
        self.peak = 0.0

    def read(self):
        started = time.monotonic()
        try:
            raw = research.appium_source(self.session_id)
            return normalize_hierarchy(raw)
        finally:
            elapsed = time.monotonic() - started
            self.calls += 1
            self.seconds += elapsed
            self.peak = max(self.peak, elapsed)
            # Keep the existing manifest + validator and first-error
            # diagnostics aligned with the authoritative atlas structure.
            base.HIERARCHY_TIMING["calls"] += 1
            base.HIERARCHY_TIMING["totalSeconds"] += elapsed
            base.HIERARCHY_TIMING["maxSeconds"] = max(
                base.HIERARCHY_TIMING["maxSeconds"], elapsed)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--out", type=Path, required=True)
    parser.add_argument("--mode", choices=("smoke", "full"), default="full")
    args = parser.parse_args()
    out = args.out.resolve()
    out.mkdir(parents=True, exist_ok=True)
    if base.adb("shell", "getprop", "ro.kernel.qemu").strip() != "1":
        raise RuntimeError("REFUSE persistent atlas on any non-emulated handset")
    if not research.request("GET", "/status", timeout=10)["value"].get("ready"):
        raise RuntimeError("Pinned Appium server is not ready")
    session, setup_seconds = research.appium_session()
    transport = PersistentAtlasHierarchy(session)
    previous_argv = sys.argv
    original_hierarchy = base.hierarchy
    status = "incomplete"
    try:
        base.hierarchy = transport.read
        sys.argv = [str(Path(base.__file__)), "--out", str(out),
                    "--mode", args.mode]
        # The complete original Android journey is run unmodified, including
        # the real sharesheets, picker, persistence and 46 PNG + XML states.
        base.main()
        status = "complete"
    finally:
        base.hierarchy = original_hierarchy
        sys.argv = previous_argv
        try:
            research.request("DELETE", f"/session/{session}", timeout=35)
        except Exception as error:
            print("::warning::Appium cleanup issue:", type(error).__name__,
                  str(error)[:160], flush=True)
        report = {
            "schema": "glimpse/optin-persistent-native-atlas/v1",
            "researchOnly": True,
            "authoritativeAtlasBackendChanged": False,
            "sourceSha": os.environ.get("GITHUB_SHA"),
            "mode": args.mode,
            "completed": status == "complete",
            "appiumSessionStartupSeconds": setup_seconds,
            "realAndroidHierarchyCalls": transport.calls,
            "realAndroidHierarchySeconds": round(transport.seconds, 3),
            "maxCallSeconds": round(transport.peak, 3),
            "backend": "pinned Appium 3 UiAutomator2, normalized XML",
            "actualAndroidSource": True,
            "simulationUsed": False,
            "originalShellAtlasUntouched": True
        }
        (out / "persistent-transport-report.json").write_text(
            json.dumps(report, indent=2, sort_keys=True) + "\n",
            encoding="utf-8")
        print("Opt-in complete atlas transport experiment:",
              json.dumps(report, sort_keys=True), flush=True)


if __name__ == "__main__":
    main()
