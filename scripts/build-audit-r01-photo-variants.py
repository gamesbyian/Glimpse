#!/usr/bin/env python3
"""Generate source-linked photographic R01 reel comparisons from the actual X05 paper reel.

Keeps the large five-sector paper wheel, clip geometry, and stationary hero.
Adds independent public source art4/art5/art6 photos so adjacent numbered slots
never impersonate one another with identical placeholder artwork.

Run: python3 scripts/build-audit-r01-photo-variants.py --write
No network, AI image synthesis, native screenshots or hidden fixtures.
"""
import argparse
import base64
import hashlib
import html
import json
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
P0 = ROOT / "design/proposals/storyboards/x05"
OUT = ROOT / "design/proposals/audit-revisions"
SOURCES = ROOT / "design/research/reel-layout/fixtures/source"

# First reference is the stationary hero. Following references are physical
# paper-wedge apertures, ordered in source SVG path order.
# p12=art1, p13=art4 in every adjacent connected frame; p22=art6.
FRAMES = {
    "photo-rest-22": ("01-rest.svg", "rest-22", "p12",
                      ["fixture1", "fixture2", "fixture3", "fixture1",
                       "fixture4", "fixture5"],
                      {"p10": "art2", "p11": "art3", "p12": "art1",
                       "p13": "art4", "p14": "art5"}),
    "photo-drag-18": ("03-drag-18.svg", "drag-18", "p12",
                      ["fixture1", "fixture2", "fixture3", "fixture1",
                       "fixture4", "fixture5"],
                      {"p10": "art2", "p11": "art3", "p12": "art1",
                       "p13": "art4", "p14": "art5"}),
    "photo-snap-13": ("05-snap-72.svg", "snap-13", "p13",
                      ["fixture4", "fixture3", "fixture1", "fixture4",
                       "fixture5", "fixture6"],
                      {"p11": "art3", "p12": "art1", "p13": "art4",
                       "p14": "art5", "p15": "art6"}),
    "photo-last-22": ("08-last.svg", "last-22", "p22",
                      ["fixture6", "fixture2", "fixture3", "fixture6"],
                      {"p20": "art2", "p21": "art3", "p22": "art6"}),
}

def generate(slug, details):
    origin, state, selected, refs, image_map = details
    source = (P0 / origin).read_text(encoding="utf-8")
    assert source.count("<use href=") == len(refs), (slug, "unexpected X05 slot layout")
    assets = {}
    for n in range(1, 7):
        data = (SOURCES / f"art{n}.jpg").read_bytes()
        assert data[:3] == b"\xff\xd8\xff", n
        assets[f"art{n}"] = hashlib.sha256(data).hexdigest()
        if f'id="fixture{n}"' not in source:
            img = (f'<image id="fixture{n}" x="0" y="0" width="480" height="720" '
                   'preserveAspectRatio="xMidYMid slice" '
                   f'href="data:image/jpeg;base64,{base64.b64encode(data).decode("ascii")}"/>')
            source = source.replace("</defs>", img + "\n</defs>", 1)
    assert len(set(assets.values())) == 6, "six real source JPEGs must be distinct"
    it = iter(refs)
    source = re.sub(r'<use href="#fixture[1-6]"', lambda m: '<use href="#' + next(it) + '"', source)
    try:
        next(it)
    except StopIteration:
        pass
    else:
        raise AssertionError("too few use nodes")
    match = re.search(r"<metadata>(.*?)</metadata>", source)
    assert match, slug
    meta = json.loads(html.unescape(match.group(1)))
    assert meta["selectedAssetId"] == selected
    meta.update({
        "auditLane": "R01",
        "auditSource": origin,
        "auditStatus": "photographic-draft-not-owner-approved",
        "auditFixtureVersion": 2,
        "fixtureArtworkByPhotoId": image_map,
        "photoFixtureSHA256": {key: assets[value] for key, value in image_map.items()},
        "lastConfirmedAppliedAssetId": "p04",
        "androidProof": "not-collected",
    })
    source = source[:match.start(1)] + html.escape(json.dumps(meta, separators=(",", ":")), quote=True) + source[match.end(1):]
    source = source.replace("X05 · PHYSICAL REEL INTERACTION STORYBOARD",
                            "R01 · PHOTOGRAPHIC REEL COMPARISON")
    source = source.replace("UNAPPROVED STORYBOARD. NOT RUNTIME EVIDENCE.",
                            "R01 DRAFT. NOT ANDROID OR OWNER EVIDENCE.")
    source = source.replace("<title>X05 /", "<title>R01 photographic /")
    # Reparse every embedded image; ensure each photo maps to a genuine original.
    embedded = {
        int(n): base64.b64decode(data)
        for n, data in re.findall(
            r'<image id="fixture(\d+)"[^>]*href="data:image/jpeg;base64,([^"]+)"', source)
    }
    for pid, art in image_map.items():
        index = int(art[3:])
        assert hashlib.sha256(embedded[index]).hexdigest() == assets[art], (slug, pid)
    assert [m for m in re.findall(r'<use href="#(fixture\d+)"', source)] == refs, slug
    return source

def main():
    p = argparse.ArgumentParser()
    p.add_argument("--write", action="store_true")
    args = p.parse_args()
    for slug, details in FRAMES.items():
        target = OUT / f"r01-{slug}.svg"
        src = generate(slug, details)
        if args.write:
            target.parent.mkdir(parents=True, exist_ok=True)
            target.write_text(src, encoding="utf-8")
        else:
            assert target.read_text(encoding="utf-8") == src, "outdated or modified: " + str(target)
        print(json.dumps({"state": slug, "file": str(target.relative_to(ROOT)),
                          "sourceBytes": len(src), "sha256": hashlib.sha256(src.encode()).hexdigest(),
                          "mode": "write" if args.write else "check"}))
if __name__ == "__main__":
    main()
