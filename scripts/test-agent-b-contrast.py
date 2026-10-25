#!/usr/bin/env python3
"""Synthetic regression examples for Agent B's conservative SVG WCAG scanner."""
from __future__ import annotations
import importlib.util
from pathlib import Path

HERE=Path(__file__).resolve().parent
spec=importlib.util.spec_from_file_location(
    "contrast",HERE/"validate-agent-b-support-contrast.py")
assert spec and spec.loader
c=importlib.util.module_from_spec(spec)
spec.loader.exec_module(c)

def svg(inner):
    return '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 420 900">' +inner+'</svg>'
def base(fill="#ffffff",txt="#153f46",size=16,weight=400):
    return svg(f'<rect x="0" y="0" width="420" height="900" fill="{fill}"/>'
               f'<text x="40" y="300" font-size="{size}" font-weight="{weight}" fill="{txt}">Test text</text>')
def kinds(raw):
    return c.paint(raw,"synthetic")
def one(raw):
    measured=[z for z in kinds(raw) if z["kind"]=="text-contrast"]
    assert len(measured)==1,measured
    return measured[0]

def main():
    assert one(base())["passed"]
    assert not one(base(txt="#aaaaaa"))["passed"]
    assert not one(base(txt="#777777",size=16))["passed"]
    assert one(base(txt="#777777",size=30))["passed"]
    assert one(base(fill="#e9e0f5",txt="#445851"))["passed"]
    assert not one(base(fill="#e9e0f5",txt="#667975"))["passed"]
    assert one(base(fill="#f6f0e6",txt="#703b27"))["passed"]
    check=svg('<rect x="0" y="0" width="420" height="900" fill="#f6f0e6"/>'
              '<rect x="270" y="210" width="25" height="50" fill="none"/>'
              '<circle cx="279" cy="235" r="11" fill="#1d5960"/>'
              '<text x="273" y="243" font-size="14" font-weight="700" fill="#ffffff">✓</text>')
    assert one(check)["passed"]
    over_image=svg('<rect x="0" y="0" width="420" height="900" fill="#fffcf6"/>'
                   '<image x="0" y="180" width="400" height="200" href="data:image/png;base64,a"/>'
                   '<text x="40" y="300" fill="#153f46" font-size="16">Unknowable backdrop</text>')
    assert any(x["kind"]=="non-solid-background" for x in kinds(over_image))
    translucent=svg('<rect x="0" y="0" width="420" height="900" fill="#ffffff"/>'
                    '<rect x="0" y="200" width="400" height="200" fill="#000000" opacity="0.5"/>'
                    '<text x="40" y="300" fill="#ffffff" font-size="16">Composited color</text>')
    assert any(x["kind"]=="non-solid-background" for x in kinds(translucent))
    no_text_transform=svg('<g transform="rotate(90)"><rect x="5" y="5" width="10" height="10" fill="#fff"/></g>'
                          '<rect x="0" y="0" width="420" height="900" fill="#ffffff"/>'
                          '<text x="40" y="300" fill="#153f46" font-size="16">Simple</text>')
    assert one(no_text_transform)["passed"]
    wrong_transform=svg('<g transform="rotate(90)"><text x="40" y="300" fill="#000000">Text</text></g>')
    assert any(x["kind"]=="unknown-transform" for x in kinds(wrong_transform))
    outside=svg('<rect x="0" y="0" width="1160" height="920" fill="#ffffff"/>'
                '<text x="450" y="300" fill="#aaaaaa" font-size="16">Review notes only</text>')
    assert not any(z["kind"]=="text-contrast" for z in kinds(outside))
    outline=svg('<rect x="0" y="0" width="420" height="900" fill="#f6f0e6"/>'
                '<rect x="22" y="801" width="383" height="88" fill="none" stroke="#e8876c"/>'
                '<text x="40" y="851" font-size="13" fill="#445851">Home</text>')
    observed=[z for z in c.paint(outline,"synthetic",whole_phone=True)
              if z["kind"]=="text-contrast"]
    assert len(observed)==1 and observed[0]["passed"], observed
    header=svg('<rect x="0" y="0" width="420" height="900" fill="#f6f0e6"/>'
               '<text x="40" y="110" font-size="16" fill="#aaaaaa">Header</text>'
               '<text x="40" y="851" font-size="13" fill="#aaaaaa">Bottom nav</text>')
    assert not any(z["kind"]=="text-contrast" for z in c.paint(header,"synthetic"))
    whole=c.paint(header,"synthetic",whole_phone=True)
    assert [z["text"] for z in whole if z["kind"]=="text-contrast"]==["Header","Bottom nav"]
    assert all(not z["passed"] for z in whole)
    # Cross-branch contract: PR20 no-handler privacy truth must survive PR22
    # AA color hardening. Test source content and actual phone text colors,
    # independent of the integrations' eventual source digest updates.
    handoff=(HERE.parent/"design/proposals/advanced-storyboards/x22/05-absent.svg").read_text()
    for phrase in (
        "Example build v0.1 · API 34",
        "Sample only; review actual bytes before sharing",
        "No compatible handler; keep draft for copy or save",
        "Diagnostic report handoff",
        "Photo bytes: excluded",
        "No automatic upload",
    ):
        assert phrase in handoff, "PR20/22 semantic merge regression: "+phrase
    for invalid in ("App v0.1 · Android API 34", "Exact outgoing payload",
                    "Report automatically uploaded"):
        assert invalid not in handoff, "Unverified claim reintroduced: "+invalid
    focus=c.paint(handoff,"x22/05-absent",whole_phone=True)
    navigation=[x for x in focus if x["kind"]=="text-contrast"
                and x["text"] in ("Home","Discover","Collections","Automation")]
    assert len(navigation)==4, navigation
    assert all(x["passed"] for x in navigation), navigation
    print("PASS: low contrast, size, circles, images, alpha, transforms, body and full-phone boundaries")
if __name__=="__main__":main()
