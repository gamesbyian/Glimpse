#!/usr/bin/env python3
"""Offline regression checks for comparative UIAutomator transport research."""
import importlib.util
import unittest
from pathlib import Path

source = Path(__file__).with_name("benchmark_hierarchy_transport.py")
spec = importlib.util.spec_from_file_location("glimpse_transport_probe", source)
m = importlib.util.module_from_spec(spec)
spec.loader.exec_module(m)

SHELL = ('<?xml version="1.0"?><hierarchy>'
         '<node class="android.widget.Button" package="io.github.gamesbyian.glimpse"'
         ' text="Add photos" clickable="true" bounds="[1,1][80,50]" />'
         '<node class="android.widget.TextView" package="io.github.gamesbyian.glimpse"'
         ' content-desc="Photos: 3" clickable="false" bounds="[1,50][80,80]" />'
         '</hierarchy>')
APPIUM = ('<?xml version="1.0"?><hierarchy>'
          '<android.widget.Button class="android.widget.Button"'
          ' package="io.github.gamesbyian.glimpse"'
          ' text="Add photos" clickable="true" bounds="[1,1][80,50]" />'
          '<android.widget.TextView class="android.widget.TextView"'
          ' package="io.github.gamesbyian.glimpse"'
          ' content-desc="Photos: 3" clickable="false" bounds="[1,50][80,80]" />'
          '</hierarchy>')
MISSING = ('<?xml version="1.0"?><hierarchy>'
           '<node class="android.widget.Button" text="Add photos"'
           ' clickable="true" bounds="[1,1][80,50]"/></hierarchy>')


class Contract(unittest.TestCase):
    def test_matching_elements_even_with_different_xml_tag_names(self):
        result = m.compare_xml(SHELL, APPIUM)
        self.assertTrue(result["equivalentForExperiment"])
        self.assertEqual(result["labelledIntersection"], 2)
        self.assertEqual(result["shellLabelRecall"], 1.0)

    def test_missing_semantic_content_is_not_qualified(self):
        result = m.compare_xml(SHELL, MISSING)
        self.assertFalse(result["equivalentForExperiment"])
        self.assertEqual(result["labelledIntersection"], 1)
        self.assertEqual(result["shellLabelRecall"], .5)

    def test_missing_bounds_fails_even_if_labels_match(self):
        no_bounds = APPIUM.replace(' bounds="[1,1][80,50]"', '').replace(
            ' bounds="[1,50][80,80]"', '')
        result = m.compare_xml(SHELL, no_bounds)
        self.assertFalse(result["equivalentForExperiment"])
        self.assertEqual(result["shellLabelRecall"], 1.0)

    def test_failed_shell_dump_cannot_return_stale_accessibility_file(self):
        calls = []

        def adb(*args, **kwargs):
            calls.append(args)
            if args[:3] == ("shell", "uiautomator", "dump"):
                return "ERROR: UiAutomation connection is owned by Appium"
            if args[:3] == ("shell", "rm", "-f"):
                return ""
            raise AssertionError("Attempted to read potentially stale XML")

        from unittest import mock
        with mock.patch.object(m.base, "adb", side_effect=adb):
            with self.assertRaisesRegex(RuntimeError, "no fresh hierarchy"):
                m.strict_shell_hierarchy()
        self.assertEqual(len(calls), 2)
        self.assertEqual(calls[0][:3], ("shell", "uiautomator", "dump"))
        self.assertEqual(calls[1][:3], ("shell", "rm", "-f"))

    def test_shell_dump_reads_exact_new_path_and_parses_real_xml(self):
        calls = []
        target = []

        def adb(*args, **kwargs):
            calls.append(args)
            if args[:3] == ("shell", "uiautomator", "dump"):
                target.append(args[3])
                return "UI hierchary dumped to: " + args[3]
            if args[:2] == ("exec-out", "cat"):
                self.assertEqual(args[2], target[-1])
                return SHELL
            if args[:3] == ("shell", "rm", "-f"):
                self.assertEqual(args[3], target[-1])
                return ""
            raise AssertionError("unexpected ADB call")

        from unittest import mock
        with mock.patch.object(m.base, "adb", side_effect=adb):
            self.assertEqual(m.strict_shell_hierarchy(), SHELL)
            self.assertEqual(m.strict_shell_hierarchy(), SHELL)
        self.assertEqual(len(target), 2)
        self.assertNotEqual(target[0], target[1])
        self.assertTrue(all(name.startswith("/sdcard/glimpse-hierarchy-probe-")
                            for name in target))

    def test_incomplete_xml_is_never_counted_as_a_read(self):
        with self.assertRaises(Exception):
            m.nodes('<?xml version="1.0"?><hierarchy><node')


if __name__ == "__main__":
    unittest.main()
