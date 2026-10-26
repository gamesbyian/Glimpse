#!/usr/bin/env python3
"""Static source-integrity and interaction-wiring checks for the Pages review studio.

This does not substitute for browser, screen reader or human optical acceptance.
"""
from pathlib import Path
from html.parser import HTMLParser
import json, re

ROOT = Path(__file__).resolve().parents[1]
UI = ROOT / "review"
class IdCollector(HTMLParser):
    def __init__(self):
        super().__init__()
        self.ids = set()
    def handle_starttag(self, tag, attrs):
        d = dict(attrs)
        if "id" in d:
            assert d["id"] not in self.ids, "Duplicate control ID: " + d["id"]
            self.ids.add(d["id"])

markup = (UI/"index.html").read_text(encoding="utf-8")
js = (UI/"app.js").read_text(encoding="utf-8")
css = (UI/"style.css").read_text(encoding="utf-8")
p = IdCollector()
p.feed(markup)
assert len(p.ids) >= 35
for x in re.findall(r'\$\("([A-Za-z][A-Za-z0-9]*)"\)', js):
    assert x in p.ids, "JavaScript references missing HTML ID: " + x
for feature in ("data-section", "data-option", "data-choice", "data-status", "data-use",
                "data-remove", "data-jump", "requestFullscreen", "localStorage",
                "wholeSetVisualAcceptanceRequested", "sourceSHA256"):
    assert feature in markup+js, "Missing review UI feature " + feature
for unsafe in ("fetch('https://", 'fetch("https://', "innerHTML=review.general", "ownerApproved=true"):
    assert unsafe not in js, "Potentially unsafe browser behavior: "+unsafe
assert "prefers-reduced-motion" in css and ":focus-visible" in css
assert "100dvh" in css and "401/890" in css, "Phone must use responsive screen-space crop"

selection=json.loads((ROOT/"design/proposals/whole-product-review-selection.v1.json").read_text())
snapshot=json.loads((ROOT/"design/atlas/whole-product-review-snapshot.v1.json").read_text())
atlas=json.loads((ROOT/"design/atlas/whole-set-evidence-index.json").read_text())
assert selection["approval"]["ownerApproved"] is False
assert len(selection["primarySurfaces"])==24
assert sum(len(s["variants"]) for s in selection["supportSurfaces"])==34
assert len(selection["journeys"])==23
assert sum(len(s["frames"]) for s in selection["journeys"])==137
assert [x["id"] for x in selection["explicitOwnerQuestions"]]==["D%02d"%i for i in range(1,9)]
assert len({i["path"] for c in atlas["chapters"] for i in c["items"]})==atlas["counts"]["uniqueEvidenceSources"]
source_hashes={s["path"]:s["sourceSHA256"] for s in snapshot["sources"]}
for source in selection["primarySurfaces"]:
    for path in [source["reviewCandidatePath"]]+source["alternativesToDecide"]:
        assert path.startswith("design/proposals/") and ".." not in path
        assert (ROOT/path).is_file(), path
        assert path in source_hashes, "Review option not SHA-pinned in snapshot: "+path
for group in selection["supportSurfaces"]:
    for variant in group["variants"]:
        assert variant["reviewCandidatePath"] in source_hashes
for group in selection["journeys"]:
    for path in group["frames"]:
        assert path in source_hashes

for pth in ("review/index.html","review/app.js","review/style.css"):
    assert (ROOT/pth).is_file()
print("PASS: 24 primary, 34 support, 23 journeys / 137 frames, 8 owner choices, exact option SHAs, interactive control IDs, mobile review structure; zero claimed approvals")
