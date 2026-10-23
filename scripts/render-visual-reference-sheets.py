#!/usr/bin/env python3
"""Render representative Glimpse SVG contact sheets locally, on demand.

No screenshot runner, emulator or network call. Does NOT imply optical approval.
Requires: python3 -m pip install cairosvg pillow (in a disposable environment).
Usage:
  python3 scripts/render-visual-reference-sheets.py --output /tmp/glimpse-optical
Open every generated sheet at 100% and the linked SVGs at native resolution.
"""
import argparse
import io
import json
import hashlib
import math
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SVG = ROOT / "design/proposals"
P0 = json.loads((SVG / "storyboard-coverage.v1.json").read_text())
P1 = json.loads((SVG / "advanced-storyboard-coverage.v1.json").read_text())
CORE = json.loads((SVG / "core-state-evidence.v1.json").read_text())
PO = {g["id"]: {f["id"]: f["path"] for f in g["frames"]}
      for g in P0["groups"] + P1["groups"]}
SETS = {
    "finite-0-1-3-5-22-60": [
        ("0", "design/proposals/critical-reel/reel-empty.svg"),
        ("1", PO["x06"]["02-single"]),
        ("3", PO["x06"]["03-three"]),
        ("5", PO["x06"]["04-exact-five"]),
        ("22", PO["x06"]["05-twenty-two"]),
        ("60", PO["x06"]["06-sixty"]),
        ("first", PO["x06"]["01-first"]),
        ("last", PO["x06"]["07-last"]),
    ],
    "swipe-reverse-snap": [(k, f["path"]) for k, f in
        [(f["id"], f) for f in next(g for g in P0["groups"] if g["id"] == "x05")["frames"]]],
    "google-photos-picker-copy": [(f["id"], f["path"]) for f in
        next(g for g in P0["groups"] if g["id"] == "x01")["frames"]],
    "target-apply-and-rotation": [
        (f["id"], f["path"]) for key in ("x03", "x04")
        for f in next(g for g in P0["groups"] if g["id"] == key)["frames"]],
    "reel-grid-crop-share": [
        (f["id"], f["path"]) for key in ("x07", "x11", "x13")
        for f in next(g for g in P0["groups"] + P1["groups"] if g["id"] == key)["frames"]],
    "accessibility-recovery": [
        (f["id"], f["path"]) for key in ("x09", "x10", "x12")
        for f in next(g for g in P0["groups"] if g["id"] == key)["frames"]],
}
# These source-specific review groups were missing from the first optical helper.
# Drawing/structure inventory is not accessibility or owner signoff.
SETS["core-34-named-states"] = [
    (s["surfaceId"] + "." + s["stateId"], s["assetPath"])
    for s in CORE["states"]
]
SETS["state-refinements-25"] = [
    (p.stem, "design/proposals/state-refinements/" + p.name)
    for p in sorted((SVG / "state-refinements").glob("*.svg"))
]
SETS["source-detail-paired"] = [
    ("Discover baseline", "design/proposals/plates/discover.svg"),
    ("Source detail baseline", "design/proposals/plates/source-detail.svg"),
    ("Source detail offline", "design/proposals/state-refinements/source-detail-offline.svg"),
]
SETS["critical-type-comparison"] = [
    ("Large-text critical reel", "design/proposals/critical-reel/reel-large-text.svg"),
    ("X09 Home 200%", PO["x09"]["01-home-200-text"]),
    ("X09 Reel 200%", PO["x09"]["03-reel-200-text"]),
    ("X09 Import 200%", PO["x09"]["05-import-200-text"]),
]
# New audit-driven candidates are a separate class, never included in 66/82/55.
SETS["audit-r03-truth-candidates"] = [
    ("Preview p12 not applied", "design/proposals/audit-revisions/r03-preview-not-applied.svg"),
    ("p12 request outcome unknown", "design/proposals/audit-revisions/r03-unknown-after-request.svg"),
]
# Exhaustive galleries are deliberately opt-in, never part of the cheap default.
SETS["entire-original-66"] = [
    (p.stem, str(p.relative_to(ROOT)))
    for folder in ("plates", "critical-reel", "support-plates")
    for p in sorted((SVG / folder).glob("*.svg"))
]
SETS["entire-p0-82"] = [
    (g["id"] + "/" + f["id"], f["path"])
    for g in P0["groups"] for f in g["frames"]
]
SETS["entire-p1-55"] = [
    (g["id"] + "/" + f["id"], f["path"])
    for g in P1["groups"] for f in g["frames"]
]
# A complete archive class is never inferred from the representative subset.
ASSERTED_CLASSES = {"original": 66, "P0": 82, "P1": 55, "new": 25}

def run(out, full=False, selected=None, native=False):
    try:
        import cairosvg
        from PIL import Image, ImageDraw, ImageFont
    except ImportError as exc:
        raise SystemExit("Install cairosvg and Pillow in a disposable environment") from exc
    out.mkdir(parents=True, exist_ok=True)
    if len(SETS["state-refinements-25"]) != 25 or len(SETS["core-34-named-states"]) != 34:
        raise SystemExit("Optical inventory drift: expected 25 supplementary and 34 named states")
    for key, expected in (("entire-original-66", 66), ("entire-p0-82", 82), ("entire-p1-55", 55)):
        if len(SETS[key]) != expected:
            raise SystemExit(f"Exhaustive review inventory drift: {key} expected {expected}, found {len(SETS[key])}")
    selected = [key for key in SETS if not key.startswith("entire-")] if not selected else selected
    native_dir = out / "native-phone-source-hashed"
    if native:
        native_dir.mkdir(parents=True, exist_ok=True)
    native_done = {}
    summary = {"source": "draft SVG source, not Android runtime",
               "opticalSignoff": False, "ownerApproved": False,
               "inventoryClasses": ASSERTED_CLASSES, "sheets": [],
               "nativePhoneFiles": []}
    for name in selected:
        files = SETS[name]
        cols, panel_w, panel_h, pad, caption_h = 4, 210, 455, 18, 54
        rows = math.ceil(len(files) / cols)
        sheet = Image.new("RGB", (cols * (panel_w + pad) + pad,
                         rows * (panel_h + caption_h + pad) + pad),
                         "#F6F0E6")
        draw = ImageDraw.Draw(sheet)
        for i, (label, relpath) in enumerate(files):
            source = ROOT / relpath
            if not source.is_file():
                raise FileNotFoundError(relpath)
            # Fingerprint exact source bytes, not merely a screenshot name.
            source_sha256 = hashlib.sha256(source.read_bytes()).hexdigest()
            # Rasterize the actual SVG, including fonts and clip paths.
            data = cairosvg.svg2png(url=str(source), output_width=1160, output_height=920)
            with Image.open(io.BytesIO(data)) as result:
                # Left phone crop only; SVG canvas and right-hand notes excluded.
                full_resolution_phone = result.convert("RGB").crop((15, 14, 416, 904))
                if native and source_sha256 not in native_done:
                    native_target = native_dir / (source.stem + "-" + source_sha256[:12] + ".png")
                    full_resolution_phone.save(native_target, optimize=True)
                    native_done[source_sha256] = str(native_target)
                    summary["nativePhoneFiles"].append({"path": str(native_target),
                        "sourcePath": relpath, "sourceSha256": source_sha256,
                        "phoneCrop": [15, 14, 416, 904],
                        "rasterPixels": [full_resolution_phone.width, full_resolution_phone.height]})
                region = full_resolution_phone
                region.thumbnail((panel_w, panel_h), Image.Resampling.LANCZOS)
                if full:
                    region = result.convert("RGB")
                    region.thumbnail((panel_w, panel_h), Image.Resampling.LANCZOS)
                x = pad + (i % cols) * (panel_w + pad)
                y = pad + (i // cols) * (panel_h + caption_h + pad)
                sheet.paste(region, (x + (panel_w-region.width)//2, y))
                draw.text((x+3, y+panel_h+7), label[:29], fill="#153F46")
                draw.text((x+3, y+panel_h+27), relpath.split("/")[-1][:28], fill="#667975")
        target = out / (name + ("-full" if full else "-phone") + ".png")
        sheet.save(target, optimize=True)
        summary["sheets"].append({"name": name, "path": str(target),
                                   "sourceCount": len(files),
                                   "sourcePaths": [p for _, p in files]})
        print(f"{target}: {len(files)} rasterized SVG frames")
    (out / "index.json").write_text(json.dumps(summary, indent=2) + "\n")

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--output", type=Path, default=Path("/tmp/glimpse-optical"))
    parser.add_argument("--full", action="store_true",
                        help="show whole storyboard plus annotation, instead of phone crop")
    parser.add_argument("--set", dest="selected_sets", action="append",
                        choices=sorted(SETS), help="render just this review group; repeat for more")
    parser.add_argument("--native", action="store_true",
                        help="also emit 401x890 native phone crops with SHA-256 source provenance")
    parser.add_argument("--list-sets", action="store_true",
                        help="print available optical group IDs and exit")
    args = parser.parse_args()
    if args.list_sets:
        for name, files in SETS.items():
            print(f"{name}: {len(files)} source entries")
    else:
        run(args.output, args.full, args.selected_sets, args.native)
