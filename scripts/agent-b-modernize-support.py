#!/usr/bin/env python3
"""Repeatable, narrowly scoped source edits for 34 unapproved support SVGs.

Only design/proposals/support-plates/*.svg is written. This does not validate
Android navigation, 200%-font reflow, TalkBack or any real outgoing payload.
"""
from __future__ import annotations
import argparse
import json
from pathlib import Path
import re
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
DIR = ROOT / "design/proposals/support-plates"
MANIFEST = ROOT / "design/proposals/support-coverage.v1.json"
BANNER = re.compile(r'<rect x="40" y="171" width="348" height="32" rx="8" fill="#E9E0F5" stroke="none" stroke-width="1"/>\s*<text x="52" y="193"[^>]*>PROPOSAL • [^<]*</text>\s*')
FOOTER = '<path d="M29 817h373" stroke="#C8B7A1"/>'
NAV = """<g id="support-navigation" aria-label="Four tab destinations; static touch region proposal only">
<rect x="31" y="822" width="86" height="46" rx="12" fill="#EBE5DB" data-control="nav-home" data-focus-order="90" data-hit-width="86" data-hit-height="46"/>
<rect x="126" y="822" width="86" height="46" rx="12" fill="#EBE5DB" data-control="nav-discover" data-focus-order="91" data-hit-width="86" data-hit-height="46"/>
<rect x="222" y="822" width="91" height="46" rx="12" fill="#EBE5DB" data-control="nav-collections" data-focus-order="92" data-hit-width="91" data-hit-height="46"/>
<rect x="320" y="822" width="91" height="46" rx="12" fill="#EBE5DB" data-control="nav-automation" data-focus-order="93" data-hit-width="91" data-hit-height="46"/>
</g>
"""
MODAL = """<g id="delete-modal-scrim" aria-label="Background actions unavailable while confirmation is open">
<rect x="15" y="55" width="401" height="825" fill="#153F46" opacity="0.60"/>
</g>
<g id="delete-modal" role="group" aria-label="Confirm deletion of local copies">
<rect x="30" y="296" width="371" height="420" rx="24" fill="#FFFCF6" stroke="#C4B7A4" stroke-width="2"/>
<rect x="48" y="318" width="116" height="32" rx="15" fill="#FBE4D6"/>
<text x="60" y="340" font-family="Arial,Helvetica,sans-serif" fill="#703B27" font-size="14" font-weight="700">DESTRUCTIVE</text>
<text x="49" y="387" font-family="Arial,Helvetica,sans-serif" fill="#153F46" font-size="22" font-weight="700">Delete 22 local copies?</text>
<text x="49" y="421" font-family="Arial,Helvetica,sans-serif" fill="#153F46" font-size="15">22 Glimpse copies will be removed.</text>
<text x="49" y="444" font-family="Arial,Helvetica,sans-serif" fill="#153F46" font-size="15">Original provider photos stay intact.</text>
<rect x="48" y="475" width="335" height="91" rx="12" fill="#FBE4D6"/>
<text x="60" y="500" font-family="Arial,Helvetica,sans-serif" fill="#703B27" font-size="16" font-weight="700">Review active rotation first</text>
<text x="60" y="526" font-family="Arial,Helvetica,sans-serif" fill="#703B27" font-size="14">Deleting active copies may leave a</text>
<text x="60" y="547" font-family="Arial,Helvetica,sans-serif" fill="#703B27" font-size="14">shuffle without eligible photos.</text>
<text x="49" y="595" font-family="Arial,Helvetica,sans-serif" fill="#667975" font-size="13">Example count; Android must calculate it.</text>
<rect x="48" y="625" width="135" height="54" rx="19" fill="#E9E3D9" stroke="#C4B7A4" data-control="cancel-delete" data-focus-order="1" data-hit-width="135" data-hit-height="54"/>
<text x="74" y="659" font-family="Arial,Helvetica,sans-serif" fill="#153F46" font-size="16" font-weight="700">Cancel</text>
<rect x="193" y="625" width="191" height="54" rx="19" fill="#703B27" data-control="confirm-delete" data-focus-order="2" data-hit-width="191" data-hit-height="54"/>
<text x="208" y="658" font-family="Arial,Helvetica,sans-serif" fill="#FFFFFF" font-size="15" font-weight="700">Delete 22 copies</text>
</g>
"""
BETA = """<rect x="40" y="506" width="348" height="158" rx="16" fill="#E4ECE6" stroke="#91ADA4"/>
<text x="54" y="533" font-family="Arial,Helvetica,sans-serif" fill="#153F46" font-size="18" font-weight="700">Review outgoing feedback</text>
<text x="54" y="562" font-family="Arial,Helvetica,sans-serif" fill="#153F46" font-size="14">Message: draft, not yet sent</text>
<text x="54" y="586" font-family="Arial,Helvetica,sans-serif" fill="#153F46" font-size="14">Screenshot: none selected</text>
<text x="54" y="610" font-family="Arial,Helvetica,sans-serif" fill="#153F46" font-size="14">Diagnostics: off</text>
<text x="54" y="638" font-family="Arial,Helvetica,sans-serif" fill="#703B27" font-size="13">Choose a recipient in Android to share.</text>
<rect x="43" y="681" width="339" height="52" rx="18" fill="#1D5960" data-control="share-reviewed-feedback" data-focus-order="5" data-hit-width="339" data-hit-height="52"/>
<text x="60" y="713" font-family="Arial,Helvetica,sans-serif" fill="#FFFFFF" font-size="16" font-weight="700">Open Android Sharesheet…</text>
"""

def enforce_provisional_a_tokens(source: str) -> str:
    """B challenge of A revision 0.1: 48px study hit regions and AA body tone.

    This upgrades already modernized sources too. The palette is a draft,
    not proof of native accessibility or an integration-approved freeze.
    """
    phone, divider, notes = source.partition('<rect x="440"')
    assert divider, "missing proposal notes divider"
    phone = phone.replace('fill="#667975"', 'fill="#566861"')
    phone = phone.replace('x="29" y="59" width="91" height="44"',
                          'x="29" y="57" width="91" height="48"')
    phone = phone.replace('data-hit-height="44"', 'data-hit-height="48"')
    for x in (31, 126, 222, 320):
        phone = phone.replace(f'x="{x}" y="822"', f'x="{x}" y="820"')
    phone = phone.replace('height="46" rx="12" fill="#EBE5DB"',
                          'height="50" rx="12" fill="#EBE5DB"')
    phone = phone.replace('data-hit-height="46"', 'data-hit-height="50"')
    result = phone + divider + notes
    ET.fromstring(result)
    return result

def modernize(name: str, original: str) -> str:
    if '<g id="support-body"' in original:
        return enforce_provisional_a_tokens(original)
    source, count = BANNER.subn('<g id="support-body" transform="translate(0,-24)">\n', original, count=1)
    assert count == 1, (name, "missing decorative proposal banner")
    assert source.count(FOOTER) == 1
    source = source.replace(FOOTER, '</g>\n' + FOOTER, 1)
    assert source.count('<text x="35" y="88"') == 1
    source = source.replace('<text x="35" y="88"', '<rect x="29" y="59" width="91" height="44" rx="12" fill="#E4ECE6" data-control="back" data-focus-order="1" data-hit-width="91" data-hit-height="44"/>\n<text x="35" y="88"', 1)
    source = source.replace(FOOTER, FOOTER + '\n' + NAV, 1)
    if name == 'storage-management--delete-confirm':
        start = source.index(FOOTER)
        finish = source.index('<text x="39" y="895"', start)
        source = source[:start] + MODAL + source[finish:]
    if name == 'beta-feedback--share-preview':
        source = source.replace('<rect x="328" y="359" width="45" height="25" rx="15" fill="#1D5960"', '<rect x="328" y="359" width="45" height="25" rx="15" fill="#C8B7A1"', 1)
        source = source.replace('<circle cx="358" cy="371.5"', '<circle cx="344" cy="371.5"', 1)
        start = source.index('<rect x="40" y="516"')
        finish = source.index('</g>\n' + FOOTER, start)
        source = source[:start] + BETA + source[finish:]
    if name == 'report-problem--with-diagnostics':
        source = source.replace('Nothing is sent automatically', 'Diagnostics selected, not sent')
        source = source.replace('Preview and redact before Android sharing', 'Verify exact fields before Android sharing')
    if name == 'report-problem--draft':
        source = source.replace('Nothing is sent automatically', 'Nothing sent; diagnostics off')
    if name in ('diagnostic-preview--minimal', 'diagnostic-preview--expanded'):
        for y, control, focus in ((338, 'include-error-summary', 2), (421, 'include-technical-details', 3)):
            needle = f'<rect x="40" y="{y}" width="348" height="69"'
            new = f'<rect data-control="{control}" data-focus-order="{focus}" data-hit-width="348" data-hit-height="69" x="40" y="{y}" width="348" height="69"'
            assert needle in source
            source = source.replace(needle, new, 1)
    ET.fromstring(source)
    return enforce_provisional_a_tokens(source)

def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument('--apply', action='store_true')
    args = parser.parse_args()
    inventory = json.loads(MANIFEST.read_text())
    names = [f"{family['id']}--{state}" for family in inventory["screens"] for state in family["states"]]
    assert len(names) == 34 and len(set(names)) == 34
    differences = []
    for name in names:
        path = DIR / f"{name}.svg"
        original = path.read_text(encoding="utf8")
        new = modernize(name, original)
        if new != original:
            differences.append(name)
            if args.apply:
                path.write_text(new, encoding="utf8")
        assert "SCREEN PROPOSAL • NOT USER-APPROVED" in new
    print(f"{'Applied' if args.apply else 'Pending'} B-owned visual transformations on {len(differences)} of 34 SVGs")
    if not args.apply and differences:
        raise SystemExit("Run --apply to regenerate expected modernized sources")

if __name__ == "__main__":
    main()
