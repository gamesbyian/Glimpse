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
import math
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SVG = ROOT / "design/proposals"
P0 = json.loads((SVG / "storyboard-coverage.v1.json").read_text())
P1 = json.loads((SVG / "advanced-storyboard-coverage.v1.json").read_text())
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
# A complete archive class is never inferred from the representative subset.
ASSERTED_CLASSES = {"original": 66, "P0": 82, "P1": 55, "new": 10}

def run(out, full=False):
    try:
        import cairosvg
        from PIL import Image, ImageDraw, ImageFont
    except ImportError as exc:
        raise SystemExit("Install cairosvg and Pillow in a disposable environment") from exc
    out.mkdir(parents=True, exist_ok=True)
    summary = {"source": "draft SVG source, not Android runtime",
               "opticalSignoff": False, "ownerApproved": False,
               "inventoryClasses": ASSERTED_CLASSES, "sheets": []}
    for name, files in SETS.items():
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
            # Rasterize the actual SVG, including fonts and clip paths.
            data = cairosvg.svg2png(url=str(source), output_width=1160, output_height=920)
            with Image.open(io.BytesIO(data)) as result:
                # Left phone crop only; SVG canvas and right-hand notes excluded.
                region = result.convert("RGB").crop((15, 14, 416, 904))
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
    args = parser.parse_args()
    run(args.output, args.full)
