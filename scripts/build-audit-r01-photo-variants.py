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

def generate_edge(slug, single):
    """Keep the actual X05 paper wheel, but leave unoccupied wedges empty."""
    source = (P0 / "01-rest.svg").read_text(encoding="utf-8")
    # Blank wedge cutouts are intentionally paper, not copies of art1.
    def clear_wedge(m):
        sector = int(m.group(1))
        if single and sector == 2:
            return m.group(0)
        return ""
    source = re.sub(
        r'<g clip-path="url\(#c([0-4])\)"><use href="#fixture[1-6]" transform="[^"]+"/></g>',
        clear_wedge, source)
    # Do not ship unused JPEG bytes inside an empty disc; one-photo retains
    # only its single source fixture. SVG provenance must be honest and lean.
    source = re.sub(
        r'<image id="fixture([1-6])"[^>]*href="data:image/jpeg;base64,[^"]+"/>',
        lambda m: m.group(0) if single and m.group(1) == "1" else "",
        source)
    # Five small paper labels must disappear unless there is a real item there.
    label = re.compile(
        r'<rect x="[\d.]+" y="[\d.]+" width="39" height="23" rx="7" '
        r'fill="#FFFCF6" stroke="none"/>\s*<text x="[\d.]+" y="[\d.]+" '
        r'font-family="Arial,Helvetica,sans-serif" font-size="13" '
        r'font-weight="700" fill="#153F46">(\d+)</text>')
    def clear_label(m):
        return m.group(0).replace(">12</text>", ">01</text>") if single and m.group(1) == "12" else ""
    source = label.sub(clear_label, source)
    if not single:
        source = re.sub(r'<g clip-path="url\(#hero\)"><use href="#fixture1" '
                        r'transform="[^"]+"/></g>',
                        '<text x="85" y="296" font-family="Arial,sans-serif" '
                        'font-size="21" fill="#153F46">Add photos to begin</text>', source)
    source = source.replace("Photo 12 of 22 · Preview only",
                            "Photo 1 of 1 · Preview only" if single else "No photos yet")
    source = source.replace("SELECTED p12", "SELECTED p01" if single else "COLLECTION EMPTY")
    source = source.replace("Browse by rotating the paper reel",
                            "One photo in your reel" if single else "Add photos to start your reel")
    source = source.replace("Previous photo", "Previous disabled" if single else "Add photos")
    source = source.replace("Next photo", "Next disabled")
    source = source.replace('x="214" y="755" width="177" height="48" rx="18" fill="#FFFCF6"',
                            'x="214" y="755" width="177" height="48" rx="18" fill="#D9DEDA"')
    if single:
        source = source.replace('x="41" y="755" width="163" height="48" rx="18" fill="#FFFCF6"',
                                'x="41" y="755" width="163" height="48" rx="18" fill="#D9DEDA"')
    source = source.replace("Fixed pointer aligned with item 12; selected preview is p12",
                            "The only selected photo is p01" if single else "No selected photo exists")
    source = source.replace("Current selectedAssetId: p12",
                            "Current selectedAssetId: p01" if single else "No selectedAssetId")
    source = source.replace("Reel has five apertures; the collection has 22 real images.",
                            "Five apertures; 1 real image only." if single else
                            "Five unfilled paper apertures; collection is empty.")
    source = source.replace("Frame 1 / 8: At rest",
                            "One-photo finite reel" if single else "Empty finite reel")
    source = source.replace("Selection is settled on a real item ID.",
                            "Single genuine p01 selection." if single else
                            "No photo selected; Add photos is the only primary action.")
    source = source.replace(">touch-down</text>",
                            ">No next item</text>" if single else ">Add photos</text>")
    source = source.replace("sourceAssetId: p12",
                            "sourceAssetId: p01" if single else "sourceAssetId: none")
    source = source.replace("selectedAssetId: p12",
                            "selectedAssetId: p01" if single else "selectedAssetId: none")
    match = re.search(r"<metadata>(.*?)</metadata>", source)
    assert match, slug
    meta = json.loads(html.unescape(match.group(1)))
    meta.update({
        "auditLane": "R01", "auditStatus": "photographic-draft-not-owner-approved",
        "auditFixtureVersion": 2, "auditSource": "01-rest.svg",
        "selectedAssetId": "p01" if single else None,
        "sourceAssetId": "public-research-fixture/art1.jpg" if single else None,
        "photoTotal": 1 if single else 0,
        "fixtureArtworkByPhotoId": {"p01": "art1"} if single else {},
        "lastConfirmedAppliedAssetId": "p04", "androidProof": "not-collected",
    })
    source = source[:match.start(1)] + html.escape(json.dumps(meta, separators=(",", ":")),
                                                  quote=True) + source[match.end(1):]
    source = source.replace("X05 · PHYSICAL REEL INTERACTION STORYBOARD",
                            "R01 · PHOTOGRAPHIC REEL COMPARISON")
    source = source.replace("<title>X05 /", "<title>R01 photographic /")
    assert source.count("<use href=") == (2 if single else 0), (slug, "ghost images")
    labels = re.findall(r'<text[^>]*>(\d\d)</text>', source)
    assert labels == (["01"] if single else []), (slug, "ghost photo indices", labels)
    return source

def main():
    p = argparse.ArgumentParser()
    p.add_argument("--write", action="store_true")
    args = p.parse_args()
    for slug in list(FRAMES) + ["photo-empty-0", "photo-single-1"]:
        target = OUT / f"r01-{slug}.svg"
        src = (generate_edge(slug, slug == "photo-single-1") if slug.startswith("photo-empty") or slug.startswith("photo-single") else generate(slug, FRAMES[slug]))
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
