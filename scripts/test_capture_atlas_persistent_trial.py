"""No-emulator tests for the opt-in real Android hierarchy normalization."""
import unittest
import xml.etree.ElementTree as ET
from unittest import mock
import capture_atlas_persistent_trial as p

APPIUM = ('<?xml version="1.0"?><hierarchy rotation="0">'
          '<android.widget.FrameLayout class="android.widget.FrameLayout"'
          ' package="io.github.gamesbyian.glimpse" bounds="[0,0][320,700]">'
          '<android.widget.Button class="android.widget.Button"'
          ' text="Add photos" clickable="true" bounds="[1,2][101,62]"'
          ' content-desc="Add photos"/>'
          '<android.widget.TextView text="Photos: 3"'
          ' clickable="false" bounds="[3,70][160,100]"/>'
          '</android.widget.FrameLayout></hierarchy>')


class OptinNormalization(unittest.TestCase):
    def test_xml_rewrites_all_real_element_tags_into_native_node_shape(self):
        xml = p.normalize_hierarchy(APPIUM)
        self.assertTrue(xml.startswith('<?xml version="1.0" encoding="UTF-8"?>'))
        root = ET.fromstring(xml)
        self.assertEqual(root.tag, "hierarchy")
        nodes = list(root.iter("node"))
        self.assertEqual(len(nodes), 3)
        self.assertEqual(nodes[1].attrib["class"], "android.widget.Button")
        self.assertEqual(nodes[1].attrib["content-desc"], "Add photos")
        self.assertEqual(nodes[1].attrib["bounds"], "[1,2][101,62]")
        self.assertEqual(nodes[2].attrib["text"], "Photos: 3")

    def test_unsupported_or_empty_android_tree_fails_closed(self):
        for xml in ('<?xml version="1.0"?><wrong/>',
                    '<?xml version="1.0"?><hierarchy/>',
                    '<?xml version="1.0"?><hierarchy><node/></hierarchy>'):
            with self.subTest(xml=xml), self.assertRaises(RuntimeError):
                p.normalize_hierarchy(xml)

    def test_live_reader_counts_calls_and_instruments_real_source(self):
        stats = p.base.HIERARCHY_TIMING
        original = dict(stats)
        try:
            stats.update(calls=0, totalSeconds=0, maxSeconds=0)
            reader = p.PersistentAtlasHierarchy("session-id")
            with mock.patch.object(p.research, "appium_source",
                                   return_value=APPIUM) as source:
                first = reader.read()
                second = reader.read()
            self.assertEqual(len(list(ET.fromstring(first).iter("node"))), 3)
            self.assertEqual(first, second)
            self.assertEqual(source.call_count, 2)
            self.assertEqual(stats["calls"], 2)
            self.assertEqual(reader.calls, 2)
        finally:
            stats.update(original)

    def test_server_errors_cannot_fake_native_accessibility_success(self):
        stats = p.base.HIERARCHY_TIMING
        original = dict(stats)
        try:
            stats.update(calls=0, totalSeconds=0, maxSeconds=0)
            reader = p.PersistentAtlasHierarchy("session-id")
            with mock.patch.object(p.research, "appium_source",
                                   side_effect=RuntimeError("session expired")):
                with self.assertRaisesRegex(RuntimeError, "session expired"):
                    reader.read()
            self.assertEqual(stats["calls"], 1)
        finally:
            stats.update(original)


if __name__ == "__main__":
    unittest.main()
