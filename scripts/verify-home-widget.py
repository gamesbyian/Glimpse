#!/usr/bin/env python3
"""Offline widget surface/permission contract for Glimpse's Home-only control.

This guards the exported OS lifecycle entry versus the non-exported action
receiver. Native/emulator tests must still prove actual launcher placement,
PendingIntent behavior, accessibility and bitmap presentation.
"""
from pathlib import Path
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
ANDROID = "{http://schemas.android.com/apk/res/android}"
def get(node, attr):
    return node.attrib.get(ANDROID + attr)

manifest = ET.parse(ROOT / "app/src/main/AndroidManifest.xml").getroot()
app = manifest.find("application")
assert app is not None
assert all(get(p, "name") != "android.permission.BIND_APPWIDGET"
           for p in manifest.findall("uses-permission"))
receivers = {get(r, "name"): r for r in app.findall("receiver")}
provider = receivers[".GlimpseHomeWidgetProvider"]
action = receivers[".GlimpseWidgetActionReceiver"]
assert get(provider, "exported") == "true"
assert get(action, "exported") == "false"
assert not action.findall("intent-filter"), "Private widget action cannot be a public filter"
assert any(get(a, "name") == "android.appwidget.action.APPWIDGET_UPDATE"
           for f in provider.findall("intent-filter")
           for a in f.findall("action"))
assert any(get(meta, "resource") == "@xml/glimpse_home_widget_info"
           for meta in provider.findall("meta-data"))

info = ET.parse(
    ROOT / "app/src/main/res/xml/glimpse_home_widget_info.xml"
).getroot()
assert get(info, "initialLayout") == "@layout/glimpse_home_widget"
assert int(get(info, "updatePeriodMillis")) >= 1_800_000
assert int(get(info, "minHeight").removesuffix("dp")) >= 220
assert int(get(info, "minResizeHeight").removesuffix("dp")) >= 200
assert get(info, "widgetCategory") == "home_screen"

layout = ET.parse(
    ROOT / "app/src/main/res/layout/glimpse_home_widget.xml"
).getroot()
children = {get(node, "id"): node for node in layout.iter()}
button = children["@+id/glimpse_widget_action"]
assert int(get(button, "layout_height").removesuffix("dp")) >= 48
assert get(button, "contentDescription"), "Action must identify purpose"
assert "@+id/glimpse_widget_preview" in children
assert "@+id/glimpse_widget_status" in children
skip = children["@+id/glimpse_widget_skip"]
assert int(get(skip, "layout_width").removesuffix("dp")) >= 48
assert int(get(skip, "layout_height").removesuffix("dp")) >= 48
assert get(skip, "visibility") == "gone", "Skip is unavailable until a verified preview"
assert "Skip" in get(skip, "contentDescription")

receiver_source = (
    ROOT / "app/src/main/java/io/github/gamesbyian/glimpse/GlimpseHomeWidget.kt"
).read_text(encoding="utf-8")
assert "QuickHomeTileService.UNIQUE_WORK" in receiver_source
assert "PendingIntent.FLAG_IMMUTABLE" in receiver_source
assert "verifyPrivateOriginal(" in receiver_source
assert "WidgetPreviewKind.UNCERTAIN" in receiver_source
assert "maxEdge = 256" in receiver_source
assert "ACTION_SKIP_PREVIEW" in receiver_source
assert "OneTimeWorkRequestBuilder<GlimpseWidgetSkipWorker>()" in receiver_source
assert "WallpaperCommands(applicationContext, dao).skipNextPreview(collection, expected)" in receiver_source
assert "EXTRA_SKIP_HEAD" in receiver_source
assert "EXTRA_SKIP_COLLECTION" in receiver_source
assert "putExtra(GlimpseWidgetActionReceiver.EXTRA_SKIP_HEAD, expectedPhotoId)" in receiver_source
assert 'setIdentifier("widget-skip:$collectionId:$expectedPhotoId")' in receiver_source
assert "private fun skipPreviewIntent" in receiver_source
assert "if (skipVisible) View.VISIBLE else View.GONE" in receiver_source
assert "verifiedThumb != null" in receiver_source
print("PASS: Home widget lifecycle/action boundary, no added permission, "
      "30min+ refresh, 48dp action, immutable PendingIntent and verified local thumb")
