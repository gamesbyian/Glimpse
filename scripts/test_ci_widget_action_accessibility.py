"""Negative controls for the real widget host's accessibility action gate."""
import importlib.util
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch

SCRIPT = Path(__file__).resolve().parent / "capture-android-home-widget.py"
spec = importlib.util.spec_from_file_location("glimpse_widget_capture", SCRIPT)
widget = importlib.util.module_from_spec(spec)
spec.loader.exec_module(widget)


def hierarchy(label, clickable=True):
    return ('<hierarchy><node class="android.appwidget.AppWidgetHostView">'
            '<node text="%s" clickable="%s" bounds="[100,100][300,200]" />'
            '</node></hierarchy>') % (
                label, "true" if clickable else "false")


class ActualWidgetActionGateTest(unittest.TestCase):
    def test_needs_two_real_consecutive_actionable_snapshots(self):
        values = iter([hierarchy("CHANGE HOME", False),
                       hierarchy("CHANGE HOME"),
                       hierarchy("CHANGE HOME")])
        with tempfile.TemporaryDirectory() as folder:
            with patch.object(widget, "hierarchy", side_effect=lambda: next(values)), (
                 patch.object(widget, "tap") as tap), (
                 patch.object(widget.time, "sleep")):
                widget.tap_stably_accessible("CHANGE HOME", Path(folder), attempts=3)
                tap.assert_called_once()
                self.assertEqual(tap.call_args.kwargs["xml_text"], hierarchy("CHANGE HOME"))
            self.assertTrue((Path(folder) / "widget-action-not-accessible-00.xml").exists())

    def test_flickering_initial_layout_is_never_tapped(self):
        values = iter([hierarchy("OPEN GLIMPSE", False),
                       hierarchy("CHANGE HOME"),
                       hierarchy("OPEN GLIMPSE", False),
                       hierarchy("CHANGE HOME")])
        with tempfile.TemporaryDirectory() as folder:
            with patch.object(widget, "hierarchy", side_effect=lambda: next(values)), (
                 patch.object(widget, "tap") as tap), (
                 patch.object(widget, "capture") as capture), (
                 patch.object(widget, "adb", return_value="synthetic activity status")), (
                 patch.object(widget.time, "sleep")):
                with self.assertRaisesRegex(RuntimeError, "never stayed accessible"):
                    widget.tap_stably_accessible("CHANGE HOME", Path(folder), attempts=4)
                tap.assert_not_called()
                capture.assert_called_once()
                self.assertTrue((Path(folder) / "widget-change-action-activity-state.txt").exists())

    def test_inert_label_without_clickable_ancestor_cannot_be_tapped(self):
        with patch.object(widget, "hierarchy", return_value=hierarchy("CHANGE HOME", False)):
            with patch.object(widget, "adb") as adb:
                with self.assertRaisesRegex(RuntimeError, "was missing"):
                    widget.tap("CHANGE HOME")
                adb.assert_not_called()


if __name__ == "__main__":
    unittest.main()
