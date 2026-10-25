#!/usr/bin/env python3
"""Source-only no-handler, chooser-close and diagnostic-payload distinctions."""
from pathlib import Path
import xml.etree.ElementTree as ET
ROOT=Path(__file__).resolve().parents[1]
names={
 "design/proposals/advanced-storyboards/x22/05-absent.svg":(
   ("No supported handler","No compatible share target","Report remains on device",
    "No compatible handler; keep draft for copy or save",
    "Example build v0.1 · API 34","Sample only; review actual bytes before sharing"),
   ("Retain draft after cancelled/unavailable share","Exact diagnostic report handoff")),
 "design/proposals/advanced-storyboards/x13/04-cancel.svg":(
   ("Delivery not confirmed",),("No image delivered",)),
 "design/proposals/support-plates/external-handler-fallback--chooser-cancelled.svg":(
   ("Share chooser closed","Delivery not confirmed by Android"),("nothing was sent","No recipient received"))
}
for path,(required,absent) in names.items():
    xml=(ROOT/path).read_text(encoding="utf-8")
    ET.fromstring(xml)
    for phrase in required:assert phrase in xml,(path,"missing",phrase)
    for phrase in absent:assert phrase not in xml,(path,"forbidden",phrase)
print("PASS: three source-only draft handoff states, no native recipient proof or owner approval")
