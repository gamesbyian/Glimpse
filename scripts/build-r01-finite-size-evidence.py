#!/usr/bin/env python3
"""Produce N=3/5/60 photographic *draft* finite paper-reel references.

Uses the existing, verified R01/X05 wheel geometry and checked-in, distinct
public JPEG fixtures. Only visible aperture photos need photographic bytes;
N=60 does NOT mean 60 embedded fixture photographs. No native proof implied.
"""
import argparse
import hashlib
import html
import json
import re
import runpy
from pathlib import Path
from xml.etree import ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
OUTPUT = ROOT / "design/proposals/audit-revisions"
r01 = runpy.run_path(str(ROOT / "scripts/build-audit-r01-photo-variants.py"))

SIZES = {
    3: ("three-3", 2, [None, 1, 2, 3, None]),
    5: ("five-5", 3, [1, 2, 3, 4, 5]),
    60: ("sixty-60", 31, [29, 30, 31, 32, 33]),
}
ORIGINAL_INDEX = [10, 11, 12, 13, 14]
ART_PER_APERTURE = ["art2", "art3", "art1", "art4", "art5"]
LABEL = re.compile(
    r'<rect x="[\\d.]+" y="[\\d.]+" width="39" height="23" rx="7" '
    r'fill="#FFFCF6" stroke="none"/>\\s*<text x="[\\d.]+" y="[\\d.]+" '
    r'font-family="Arial,Helvetica,sans-serif" font-size="13" '
    r'font-weight="700" fill="#153F46">(10|11|12|13|14)</text>')


def create(n):
    slug, selected, positions = SIZES[n]
    # This source is already made from X05, with the same five clipping shapes
    # and six independent source JPEGs, and a portable vector Grid affordance.
    source = r01["generate"]("photo-rest-22", r01["FRAMES"]["photo-rest-22"])
    head, sep, body = source.partition("</defs>")
    assert sep and head.count("<metadata>") == 1

    original = {old: new for old, new in zip(ORIGINAL_INDEX, positions)}
    assert len(original) == 5 and original[12] == selected
    for slot in (0, 4):
        if positions[slot] is None:
            pattern = (r'<g clip-path="url\\(#c' + str(slot) +
                       r'\\)"><use href="#fixture[1-6]" transform="[^"]+"/></g>')
            body, count = re.subn(pattern, "", body)
            assert count == 1, (n, slot, "blank outer window must not hold photo")

    seen_labels = []
    def rewrite_label(m):
        old = int(m.group(1))
        seen_labels.append(old)
        new = original[old]
        return "" if new is None else m.group(0).replace(
            ">" + str(old) + "</text>", ">" + f"{new:02d}" + "</text>")
    body, label_count = LABEL.subn(rewrite_label, body)
    assert label_count == 5 and seen_labels == ORIGINAL_INDEX, (n, seen_labels)

    replacements = {
        "Photo 12 of 22 · Preview only": f"Photo {selected} of {n} · Preview only",
        "SELECTED p12": f"SELECTED p{selected:02d}",
        "Fixed pointer aligned with item 12; selected preview is p12":
            f"Fixed pointer aligned with item {selected}; preview p{selected:02d}",
        "Current selectedAssetId: p12": f"Current selectedAssetId: p{selected:02d}",
        "Reel has five apertures; the collection has 22 real images.":
            f"Five physical apertures; {n} collection items; no wrap.",
        "Frame 1 / 8: At rest": f"Finite collection / {n} items",
        "sourceAssetId: p12": f"sourceAssetId: p{selected:02d}",
        "selectedAssetId: p12": f"selectedAssetId: p{selected:02d}",
    }
    for old, new in replacements.items():
        assert body.count(old) == 1, (n, old)
        body = body.replace(old, new, 1)
    # The five position IDs are only an example of the N-length collection.
    # For N=60, do not pretend unseen 55 assets have real JPEG fixtures.
    artwork = {f"p{pid:02d}": ART_PER_APERTURE[i]
               for i, pid in enumerate(positions) if pid is not None}
    assert len(artwork) == min(n, 5)
    assert len(set(artwork.values())) == min(n, 5)
    fixture_hash = {
        pid: hashlib.sha256((r01["SOURCES"] / (art + ".jpg")).read_bytes()).hexdigest()
        for pid, art in artwork.items()
    }
    match = re.search(r"<metadata>(.*?)</metadata>", head)
    assert match
    meta = json.loads(html.unescape(match.group(1)))
    meta.update({
        "auditLane": "R01", "auditStatus": "photographic-draft-not-owner-approved",
        "auditSource": "01-rest.svg", "selectedAssetId": f"p{selected:02d}",
        "sourceAssetId": f"p{selected:02d}", "photoTotal": n,
        "fixtureArtworkByPhotoId": artwork, "photoFixtureSHA256": fixture_hash,
        "photoFixtureScope": "Distinct checked-in JPEGs for visible apertures only; abstract finite total",
        "lastConfirmedAppliedAssetId": "p04",
        "androidProof": "not-collected",
    })
    head = head[:match.start(1)] + html.escape(
        json.dumps(meta, separators=(",", ":")), quote=True) + head[match.end(1):]
    source = head + sep + body
    source = source.replace(
        "R01 · PHOTOGRAPHIC REEL COMPARISON",
        f"R01 · FINITE PHOTO REEL / {n} ITEMS")
    source = source.replace(
        "R01 DRAFT. NOT ANDROID OR OWNER EVIDENCE.",
        "R01 FINITE-COUNT DRAFT. NOT ANDROID OR OWNER EVIDENCE.")
    ET.fromstring(source)
    photos = re.findall(r'<g clip-path="url\\(#c[0-4]\\)"><use href="#fixture\\d+"', source)
    assert len(photos) == min(n, 5), (n, len(photos))
    labels = re.findall(
        r'<text x="[\\d.]+" y="[\\d.]+" font-family="Arial,Helvetica,sans-serif" '
        r'font-size="13" font-weight="700" fill="#153F46">(\\d\\d)</text>', body)
    assert labels == [f"{i:02d}" for i in positions if i is not None], (n, labels)
    assert f"Photo {selected} of {n} · Preview only" in body
    return source


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--write", action="store_true")
    args = ap.parse_args()
    for n, (slug, _, _) in SIZES.items():
        dest = OUTPUT / ("r01-photo-" + slug + ".svg")
        source = create(n)
        if args.write:
            dest.write_text(source, encoding="utf-8")
        else:
            assert dest.read_text(encoding="utf-8") == source, str(dest)
        print(json.dumps({"size": n, "path": str(dest.relative_to(ROOT)),
                          "bytes": len(source), "sha256": hashlib.sha256(source.encode()).hexdigest(),
                          "wrote": args.write}))


if __name__ == "__main__":
    main()
