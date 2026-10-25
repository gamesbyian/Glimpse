#!/usr/bin/env python3
"""Source-meaning regression for current primary family repairs, not user or native acceptance."""
from pathlib import Path
import xml.etree.ElementTree as ET

ROOT=Path(__file__).resolve().parents[1]
def phone(p):
    data=(ROOT/p).read_text(encoding="utf-8")
    assert ET.fromstring(data).attrib["viewBox"]=="0 0 1160 920"
    return data.split('<rect x="439"',1)[0].split('<rect x="440"',1)[0]
def check(path,yes,no=()):
    s=phone(path)
    for text in yes:assert text in s,(path,"missing",text)
    for text in no:assert text not in s,(path,"forbidden",text)
def main():
    check("design/proposals/plates/quick-import.svg",
          ["SAMPLE PREVIEW","Saving does not change your wallpaper.","Apply…",
          'y="451" width="100" height="50"'],
          ["Use now</text>","SOURCE SCREENSHOT"])
    check("design/proposals/plates/automation.svg",
          ["Foreground rotation enabled","Five-minute eligibility",
           "Change Home now", "Pause rotation", "exact timing unknown"],
          ["Pause / Change now","~00:16"])
    check("design/proposals/plates/activity.svg",
          ["Recorded Glimpse actions","Home acknowledged; device may differ",
           "Review p04","Review last Home choice","no new apply logged"],
          ["Reapply previous","Deferred by device policy"])
    check("design/proposals/plates/accessibility-preferences.svg",
          ["200% design target"], ["200% supported"])
    check("design/proposals/audit-revisions/r02-share-choice.svg",
          ['text-anchor="middle">Collections','y="852"'],
          ["Home     Discover     Collections     Automation"])
    print("PASS: five current primary references retain conservative action/consent truths")

if __name__=="__main__":main()
