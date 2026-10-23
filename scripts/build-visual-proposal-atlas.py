#!/usr/bin/env python3
"""Package Glimpse's exact draft-reference set for offline/mobile browsing.

Only published SVG research proposals are included. Never bundle user imports,
Android captures, private photo paths or the abandoned legacy decks.
"""
from __future__ import annotations

import hashlib
import json
import re
import shutil
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
OUTPUT = ROOT / "artifacts" / "visual-proposal-atlas"
ATLAS = ROOT / "design" / "atlas" / "index.html"


def expected_entries() -> list[dict]:
    main = json.loads((ROOT / "design/proposals/visual-coverage.v1.json").read_text())
    support = json.loads((ROOT / "design/proposals/support-coverage.v1.json").read_text())
    result = [
        {"id": "core/" + s["id"],
         "path": "design/proposals/plates/" + s["id"] + ".svg"}
        for s in main["screens"]
    ]
    result.extend([
        {"id": "reel/" + state,
         "path": "design/proposals/critical-reel/reel-" + state + ".svg"}
        for state in ("empty", "one", "three", "twenty-two", "last",
                      "reduced-motion", "large-text", "interrupted")
    ])
    for screen in support["screens"]:
        for state in screen["states"]:
            result.append({
                "id": "support/" + screen["id"] + "/" + state,
                "path": "design/proposals/support-plates/" +
                        screen["id"] + "--" + state + ".svg"
            })
    return result


def main():
    html = ATLAS.read_text(encoding="utf-8")
    match = re.search(r"const ENTRIES=(\[.*?\]);\s*const byId", html, re.DOTALL)
    if not match:
        raise SystemExit("Atlas entry registry is missing or malformed")
    provided = json.loads(match.group(1))
    wanted = expected_entries()
    assert len(provided) == len(wanted) == 66, (len(provided), len(wanted))
    assert [(x["id"], x["path"]) for x in provided] == [
        (x["id"], x["path"]) for x in wanted
    ], "Atlas is stale: regenerate entries from the two reference manifests"

    if OUTPUT.exists():
        shutil.rmtree(OUTPUT)
    (OUTPUT / "design/atlas").mkdir(parents=True)
    for filename in ("index.html", "storyboards.html", "README.md"):
        shutil.copy2(ROOT / "design/atlas" / filename,
                     OUTPUT / "design/atlas" / filename)
    manifest = []
    for item in provided:
        src = ROOT / item["path"]
        if not src.is_file():
            raise SystemExit("Missing SVG: " + item["path"])
        dest = OUTPUT / item["path"]
        dest.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(src, dest)
        manifest.append({
            "id": item["id"],
            "path": item["path"],
            "sha256": hashlib.sha256(src.read_bytes()).hexdigest(),
            "title": item["title"],
            "variant": item["variant"],
            "classification": "proposal_not_approved",
        })
    # Storyboards supplement the 66-screen baseline. They must not be
    # merged into the original current-generation count or treated as
    # independent, complete screen mockups.
    coverage = json.loads(
        (ROOT / "design/proposals/storyboard-coverage.v1.json").read_text(
            encoding="utf-8"
        )
    )
    groups = coverage["groups"]
    assert [g["id"] for g in groups] == [
        "x%02d" % i for i in range(1, 13)
    ], "Twelve ordered P0 audit packages required"
    storyboard_frames = [f for g in groups for f in g["frames"]]
    assert len(storyboard_frames) == coverage["counts"]["additionalStoryboardSvg"]
    assert len(storyboard_frames) == 82, len(storyboard_frames)
    assert len({f["path"] for f in storyboard_frames}) == len(storyboard_frames)
    embedded = (ROOT / "design/atlas/storyboards.html").read_text(
        encoding="utf-8"
    )
    match = re.search(r"const DATA=(\\[.*?\\]);\\s*let selected", embedded, re.DOTALL)
    assert match, "Storyboard gallery has no embedded registry"
    gallery_groups = json.loads(match.group(1))
    assert [
        (g["id"], [f["path"] for f in g["frames"]]) for g in gallery_groups
    ] == [
        (g["id"], [f["path"] for f in g["frames"]]) for g in groups
    ], "Storyboard gallery no longer matches coverage manifest"
    storyboard_manifest = []
    for frame in storyboard_frames:
        src = ROOT / frame["path"]
        assert src.is_file(), frame["path"]
        dest = OUTPUT / frame["path"]
        dest.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(src, dest)
        storyboard_manifest.append({
            "id": frame["id"], "path": frame["path"],
            "sha256": hashlib.sha256(src.read_bytes()).hexdigest(),
            "classification": "supplementary_storyboard_draft",
        })
    # Several earlier reel motion studies reference the existing public
    # research JPEGs by relative path, so bundle their original assets too.
    fixture_root = ROOT / "design/research/reel-layout/fixtures/source"
    for jpg in sorted(fixture_root.glob("art*.jpg")):
        dst = OUTPUT / jpg.relative_to(ROOT)
        dst.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(jpg, dst)
    (OUTPUT / "manifest.json").write_text(
        json.dumps({
            "type": "Glimpse coherent reference proposals",
            "status": "not user approved and not Android runtime screenshots",
            "count": len(manifest),
            "storyboardCount": len(storyboard_manifest),
            "storyboardItems": storyboard_manifest,
            "items": manifest,
        }, indent=2) + "\n",
        encoding="utf-8"
    )
    print("Packaged", len(manifest), "original SVG references and",
          len(storyboard_manifest), "storyboard frames. Open design/atlas/index.html or design/atlas/storyboards.html")


if __name__ == "__main__":
    main()
