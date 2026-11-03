#!/usr/bin/env python3
"""Fast, emulator-free behavioral contracts for the native visual-atlas harness."""
import importlib.util
import tempfile
import unittest
from pathlib import Path
from unittest import mock

SOURCE = Path(__file__).with_name("capture-android-atlas.py")
spec = importlib.util.spec_from_file_location("glimpse_atlas_capture", SOURCE)
atlas = importlib.util.module_from_spec(spec)
spec.loader.exec_module(atlas)

NORMAL = ('<?xml version="1.0"?><hierarchy>'
          '<node text="Open photo details" clickable="true" '
          'bounds="[0,0][100,100]"/></hierarchy>')
ANR = ('<?xml version="1.0"?><hierarchy>'
       '<node text="Glimpse is not responding"/>'
       '<node text="Wait" clickable="true" bounds="[10,20][90,100]"/>'
       '</hierarchy>')
NO_WAIT = ('<?xml version="1.0"?><hierarchy>'
           '<node text="Glimpse is not responding"/></hierarchy>')
PNG = b"\x89PNG\r\n\x1a\n" + b"0" * 16000
AFTER_ANR_PNG = b"\x89PNG\r\n\x1a\n" + b"1" * 16000


class AtlasHarnessTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        atlas.OUT = Path(self.temp.name)
        atlas.RESULTS.clear()
        atlas.HIERARCHY_TIMING.update(calls=0, totalSeconds=0.0, maxSeconds=0.0)
        atlas.ACTION_SETTLE.update(taps=0, swipes=0, requestedSeconds=0.0)
        atlas._GEOMETRY_CACHE = None
        atlas.GEOMETRY_CACHE_STATS.update(
            freshReads=0, cacheHits=0, invalidations=0, readSeconds=0.0)

    def test_geometry_cache_uses_two_reads_until_wm_changes(self):
        state = {"size": "1440x3120", "density": "560"}
        commands = []

        def run(argv, **_):
            args = tuple(argv[1:])
            commands.append(args)
            if args[:3] == ("shell", "wm", "size"):
                if len(args) == 4:
                    state["size"] = args[3]
                    return mock.Mock(returncode=0, stdout=b"", stderr=b"")
                return mock.Mock(returncode=0, stdout=(
                    "Physical size: " + state["size"]).encode(), stderr=b"")
            if args[:3] == ("shell", "wm", "density"):
                if len(args) == 4:
                    state["density"] = args[3]
                    return mock.Mock(returncode=0, stdout=b"", stderr=b"")
                return mock.Mock(returncode=0, stdout=(
                    "Physical density: " + state["density"]).encode(), stderr=b"")
            raise AssertionError("unexpected ADB operation " + str(args))

        with mock.patch.object(atlas.subprocess, "run", side_effect=run):
            self.assertEqual(atlas.screen_geometry(), (1440, 3120, 560))
            self.assertEqual(atlas.screen_geometry(), (1440, 3120, 560))
            self.assertEqual(len(commands), 2)  # second read is cache-only
            atlas.adb("shell", "wm", "size", "320x700")
            self.assertEqual(atlas.screen_geometry(), (320, 700, 560))
            atlas.adb("shell", "wm", "density", "720")
            self.assertEqual(atlas.screen_geometry(), (320, 700, 720))
            atlas.adb("shell", "wm", "density", "reset")
            self.assertEqual(atlas.GEOMETRY_CACHE_STATS["invalidations"], 3)
        self.assertEqual(atlas.GEOMETRY_CACHE_STATS["freshReads"], 3)
        self.assertEqual(atlas.GEOMETRY_CACHE_STATS["cacheHits"], 1)
        self.assertGreaterEqual(atlas.GEOMETRY_CACHE_STATS["readSeconds"], 0)

    def test_failed_wm_mutation_never_preserves_stale_geometry(self):
        atlas._GEOMETRY_CACHE = (1440, 3120, 560)
        with mock.patch.object(atlas.subprocess, "run",
                 return_value=mock.Mock(returncode=1, stdout=b"",
                                        stderr=b"wm denied")):
            with self.assertRaisesRegex(RuntimeError, "wm denied"):
                atlas.adb("shell", "wm", "size", "320x700")
        self.assertIsNone(atlas._GEOMETRY_CACHE)
        self.assertEqual(atlas.GEOMETRY_CACHE_STATS["invalidations"], 1)

    def test_invalid_native_geometry_does_not_poison_cache(self):
        with mock.patch.object(atlas, "adb", side_effect=(
                "Physical size: 0x700", "Physical density: 560")):
            with self.assertRaisesRegex(RuntimeError, "Invalid Android geometry"):
                atlas.screen_geometry()
        self.assertIsNone(atlas._GEOMETRY_CACHE)

    def test_normal_click_uses_exactly_one_hierarchy(self):
        with mock.patch.object(atlas, "hierarchy", return_value=NORMAL) as dump, \
             mock.patch.object(atlas, "adb") as adb, \
             mock.patch.object(atlas.time, "sleep") as settle:
            atlas.click_label("Open photo details")
        dump.assert_called_once_with()
        adb.assert_called_once_with("shell", "input", "tap", "50", "50")
        settle.assert_called_once_with(atlas.TAP_SETTLE_SECONDS)
        self.assertEqual(atlas.ACTION_SETTLE["taps"], 1)
        self.assertEqual(atlas.ACTION_SETTLE["swipes"], 0)
        self.assertEqual(atlas.ACTION_SETTLE["requestedSeconds"], atlas.TAP_SETTLE_SECONDS)

    def test_swipe_preserves_real_input_and_bounded_settle(self):
        with mock.patch.object(atlas, "screen_geometry", return_value=(320, 700, 160)), \
             mock.patch.object(atlas, "adb") as adb, \
             mock.patch.object(atlas.time, "sleep") as settle:
            atlas.swipe("up")
        adb.assert_called_once_with("shell", "input", "swipe", "294", "343",
                                    "294", "119", "520")
        settle.assert_called_once_with(atlas.SWIPE_SETTLE_SECONDS)
        self.assertEqual(atlas.ACTION_SETTLE["taps"], 0)
        self.assertEqual(atlas.ACTION_SETTLE["swipes"], 1)
        self.assertEqual(atlas.ACTION_SETTLE["requestedSeconds"], atlas.SWIPE_SETTLE_SECONDS)

    def test_settle_rejects_unrecognized_action_without_sleep(self):
        with mock.patch.object(atlas.time, "sleep") as settle:
            with self.assertRaisesRegex(ValueError, "Unknown Android input action"):
                atlas.settle_after_action("unknown")
        settle.assert_not_called()
        self.assertEqual(atlas.ACTION_SETTLE["requestedSeconds"], 0.0)

    def test_anr_wait_uses_actual_bounds_and_fresh_observation(self):
        with mock.patch.object(atlas, "hierarchy", side_effect=[ANR, NORMAL]) as dump, \
             mock.patch.object(atlas, "adb") as adb, \
             mock.patch.object(atlas.time, "sleep"):
            observed = atlas.clear_system_obstructions()
        self.assertEqual(observed, NORMAL)
        self.assertEqual(dump.call_count, 2)
        adb.assert_called_once_with("shell", "input", "tap", "50", "60")

    def test_normal_capture_uses_one_dump_and_preserves_evidence(self):
        def adb(*args, **kwargs):
            return PNG if args[:3] == ("exec-out", "screencap", "-p") else "1.0"

        with mock.patch.object(atlas, "hierarchy", return_value=NORMAL) as dump, \
             mock.patch.object(atlas, "screen_geometry", return_value=(320, 700, 160)), \
             mock.patch.object(atlas, "adb", side_effect=adb):
            atlas.capture("verified-frame", "Actual running Android UI")
        dump.assert_called_once_with()
        self.assertEqual((atlas.OUT / "verified-frame.png").read_bytes(), PNG)
        self.assertEqual((atlas.OUT / "verified-frame.xml").read_text(), NORMAL)
        self.assertEqual(atlas.RESULTS[0]["hierarchy"], "verified-frame.xml")
        self.assertEqual(atlas.RESULTS[0]["clickableCount"], 1)
        self.assertIn("captureElapsedSeconds", atlas.RESULTS[0])

    def test_anr_capture_replaces_obscured_png_after_wait(self):
        pictures = iter((PNG, AFTER_ANR_PNG))
        def adb(*args, **kwargs):
            if args[:3] == ("exec-out", "screencap", "-p"):
                return next(pictures)
            return "1.0"

        with mock.patch.object(atlas, "hierarchy", side_effect=[ANR, NORMAL]) as dump, \
             mock.patch.object(atlas, "screen_geometry", return_value=(320, 700, 160)), \
             mock.patch.object(atlas, "adb", side_effect=adb) as shell, \
             mock.patch.object(atlas.time, "sleep"):
            atlas.capture("cleared-anr", "No obscuring system UI")
        self.assertEqual(dump.call_count, 2)
        self.assertEqual((atlas.OUT / "cleared-anr.png").read_bytes(), AFTER_ANR_PNG)
        self.assertEqual((atlas.OUT / "cleared-anr.xml").read_text(), NORMAL)
        self.assertIn(mock.call("shell", "input", "tap", "50", "60"), shell.call_args_list)

    def test_anr_without_wait_fails_without_publishing_capture(self):
        def adb(*args, **kwargs):
            return PNG if args[:3] == ("exec-out", "screencap", "-p") else "1.0"

        with mock.patch.object(atlas, "hierarchy", return_value=NO_WAIT), \
             mock.patch.object(atlas, "screen_geometry", return_value=(320, 700, 160)), \
             mock.patch.object(atlas, "adb", side_effect=adb):
            with self.assertRaisesRegex(RuntimeError, "no safe Wait"):
                atlas.capture("obscured", "Must never be published")
        self.assertFalse((atlas.OUT / "obscured.png").exists())
        self.assertFalse(atlas.RESULTS)

    def test_unavailable_hierarchy_does_not_fabricate_evidence(self):
        def adb(*args, **kwargs):
            return PNG if args[:3] == ("exec-out", "screencap", "-p") else "1.0"

        with mock.patch.object(atlas, "hierarchy", side_effect=RuntimeError("UIAutomator failed")), \
             mock.patch.object(atlas, "screen_geometry", return_value=(320, 700, 160)), \
             mock.patch.object(atlas, "adb", side_effect=adb):
            atlas.capture("incomplete", "Accessibility failure is explicit")
        self.assertIsNone(atlas.RESULTS[0]["hierarchy"])
        self.assertTrue((atlas.OUT / "incomplete.xml-error.txt").exists())
        self.assertFalse((atlas.OUT / "incomplete.xml").exists())


if __name__ == "__main__":
    unittest.main()
