#!/usr/bin/env python3
"""Fail-closed structural check for B-owned diagnostic disclosure source ordering.

This measures SVG proposed coordinates, not Android layout, focus or hit testing.
"""
from pathlib import Path
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
PATH = ROOT / "design/proposals/support-plates/diagnostic-preview--expanded.svg"
NS = "{http://www.w3.org/2000/svg}"
tree = ET.parse(PATH).getroot()
body = next(x for x in tree.iter(f"{NS}g") if x.get("id") == "support-body")
texts = {("".join(e.itertext())).strip(): e for e in body.iter(f"{NS}text")}
def find(label):
    if label not in texts:
        raise AssertionError(f"Missing diagnostic content: {label}")
    return float(texts[label].get("y"))
privacy = "No images, source URIs or account tokens"
preview = "Extra included field examples"
heading = "Choose your action"
assert find(preview) < find(privacy) < find(heading), "Disclosure/privacy copy must precede actions"
children = list(body)
indices = {id(element): i for i, element in enumerate(children)}
assert indices[id(texts[preview])] < indices[id(texts[heading])], "SVG document reading order must put disclosure before action heading"
assert indices[id(texts[privacy])] < indices[id(texts[heading])], "Privacy exclusion must precede action heading in DOM"
assert find("Attempt: copy • Result: interrupted") < find(heading)
assert find("Exception type: sanitised • IDs redacted") < find(heading)
buttons = {e.get("data-control"): e for e in body.iter(f"{NS}rect") if (e.get("data-control") or "").startswith("task-")}
assert set(buttons) == {"task-copy", "task-save", "task-share"}
assert all(float(b.get("height")) >= 48 for b in buttons.values())
action_top = min(float(b.get("y")) for b in buttons.values())
assert find(heading) < action_top, "Actions must follow their heading"
card = next(e for e in body.iter(f"{NS}rect") if e.get("fill") == "#E9E0F5")
assert float(card.get("y")) + float(card.get("height")) + 16 <= find(heading), "Disclosure card overlaps action heading"
# support-body transform is translate(0,-24); 817 is the phone footer separator.
assert all(float(b.get("y")) + float(b.get("height")) - 24 <= 817 for b in buttons.values())
assert find(privacy) < float(card.get("y")) + float(card.get("height"))
assert "NOT USER-APPROVED" in PATH.read_text()
print("PASS: expanded diagnostic fields and redaction visible before 48px action targets; static source only")
