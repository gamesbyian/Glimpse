#!/usr/bin/env python3
"""Create a representative PNG contact sheet from real source storyboard SVGs.

Not a screenshot of Android and not a usability/approval test. All assets are
public first-party research examples. Failed rasterization fails the build.
"""
import io
import json
import sys
from pathlib import Path

try:
    import cairosvg
    from PIL import Image, ImageDraw, ImageFont
except ImportError:
    raise SystemExit("Install CairoSVG and Pillow to rasterize storyboard samples")

ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / "artifacts/visual-proposal-atlas/storyboard-contact-sheet.png"
groups = json.loads((ROOT / "design/proposals/storyboard-coverage.v1.json").read_text())["groups"]
width, image_h, label_h = 346, 276, 44
sheet = Image.new("RGB",(3*width, len(groups)*(image_h+label_h)), "#f6f0e6")
draw = ImageDraw.Draw(sheet)
for row, group in enumerate(groups):
    frames = group["frames"]
    sample = [frames[0], frames[len(frames)//2], frames[-1]]
    for col, frame in enumerate(sample):
        path = ROOT / frame["path"]
        data = cairosvg.svg2png(url=str(path), output_width=width, output_height=image_h)
        im = Image.open(io.BytesIO(data)).convert("RGB")
        x,y = col*width,row*(image_h+label_h)
        sheet.paste(im,(x,y))
        label = group["id"].upper() + "  " + frame["caption"][:31]
        draw.rectangle((x,y+image_h,x+width,y+image_h+label_h),fill="#fffcf6")
        draw.text((x+8,y+image_h+9),label,fill="#153f46")
OUT.parent.mkdir(parents=True,exist_ok=True)
sheet.save(OUT,optimize=True)
print("Rendered",len(groups)*3,"actual SVG frames to",OUT,"(",OUT.stat().st_size,"bytes )")
