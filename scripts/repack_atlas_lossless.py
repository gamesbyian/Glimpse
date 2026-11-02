#!/usr/bin/env python3
"""Opt-in lossless WebP shadow bundle for actual Android screenshot-atlas ZIPs.

This NEVER overwrites or deletes source PNGs; no production workflow uses it.
A manifest retains each original file path/hash, image dimensions and decoded
RGBA pixel digest. A WebP pack cannot reconstruct the original *PNG bytes*.
"""
from __future__ import annotations

import argparse
import os
import tempfile
from hashlib import sha256
from io import BytesIO
import json
from pathlib import PurePosixPath, Path
import zipfile

from PIL import Image

SCHEMA = "glimpse-optin-atlas-lossless/v1"
MANIFEST = "atlas-lossless-manifest.json"


def digest(data):
    return sha256(data).hexdigest()


def safe_path(name):
    p = PurePosixPath(name)
    if (not name or name.startswith("/") or "\\" in name or p.is_absolute() or
            ".." in p.parts or "" in p.parts or name == MANIFEST):
        raise ValueError("unsafe, ambiguous or reserved archive path")
    return str(p)


def pixels(data, *, fmt):
    with Image.open(BytesIO(data)) as im:
        if im.format != fmt or im.width <= 0 or im.height <= 0:
            raise ValueError("wrong image encoding or empty image")
        rgba = im.convert("RGBA")
        return rgba.size, rgba.tobytes(), rgba


def pack(input_zip, output_zip, *, method=2):
    if method not in (0, 1, 2, 3, 4, 5, 6):
        raise ValueError("unsupported lossless encoder effort")
    src, dst = Path(input_zip).resolve(), Path(output_zip).resolve()
    if src == dst:
        raise ValueError("input and output must be different files")
    if dst.exists():
        raise FileExistsError("refusing to overwrite an existing output archive")
    entries = []
    with zipfile.ZipFile(src, "r") as source:
        names = source.namelist()
        if len(names) != len(set(names)) or not names or len(names) > 2000:
            raise ValueError("duplicate, empty or excessive source ZIP entries")
        png_count = 0
        original_bytes = 0
        webp_bytes = 0
        # Stage atomically. Never expose a partially constructed archive.
        with tempfile.NamedTemporaryFile(dir=dst.parent, suffix=".partial", delete=False) as temp:
            temp_path = Path(temp.name)
        try:
            with zipfile.ZipFile(temp_path, "w", compression=zipfile.ZIP_DEFLATED, compresslevel=6) as out:
                for name in names:
                    normalized = safe_path(name)
                    if normalized != name or name.endswith("/"):
                        raise ValueError("noncanonical source archive path")
                    payload = source.read(name)
                    if name.lower().endswith(".png"):
                        dims, original_pixels, rgba = pixels(payload, fmt="PNG")
                        blob = BytesIO()
                        rgba.save(blob, format="WEBP", lossless=True, method=method)
                        webp = blob.getvalue()
                        webp_dims, webp_pixels, _ = pixels(webp, fmt="WEBP")
                        if dims != webp_dims or original_pixels != webp_pixels:
                            raise ValueError("lossless pixel equality failed")
                        replacement = name[:-4] + ".webp"
                        if replacement in names:
                            raise ValueError("replacement filename collides with original")
                        out.writestr(replacement, webp)
                        entries.append({"original_path": name, "packed_path": replacement,
                                        "png_sha256": digest(payload), "webp_sha256": digest(webp),
                                        "rgba_sha256": digest(original_pixels),
                                        "width": dims[0], "height": dims[1],
                                        "png_bytes": len(payload), "webp_bytes": len(webp)})
                        png_count += 1
                        original_bytes += len(payload)
                        webp_bytes += len(webp)
                    else:
                        out.writestr(name, payload)
                        entries.append({"original_path": name, "packed_path": name,
                                        "sha256": digest(payload), "bytes": len(payload)})
                if not png_count:
                    raise ValueError("no PNG screenshots present")
                manifest = {"schema": SCHEMA, "encoder_method": method,
                            "conversion": "lossless RGBA pixels; original PNG bytes NOT reconstructible",
                            "png_count": png_count, "source_png_bytes": original_bytes,
                            "webp_bytes": webp_bytes, "entries": entries}
                out.writestr(MANIFEST, json.dumps(manifest, sort_keys=True, indent=2) + "\n")
            verify(temp_path)
            # Atomic create-only publish; never clobber any existing output.
            os.link(temp_path, dst)
        finally:
            temp_path.unlink(missing_ok=True)
    return manifest


def verify(pack_zip):
    with zipfile.ZipFile(pack_zip) as archive:
        names = archive.namelist()
        if len(names) != len(set(names)) or MANIFEST not in names:
            raise ValueError("duplicate ZIP entries or missing manifest")
        manifest = json.loads(archive.read(MANIFEST))
        if manifest.get("schema") != SCHEMA or not isinstance(manifest.get("entries"), list):
            raise ValueError("unsupported manifest")
        seen, png_count, input_bytes, output_bytes = set(), 0, 0, 0
        for entry in manifest["entries"]:
            name = safe_path(entry["packed_path"])
            if name in seen or name not in names:
                raise ValueError("missing/duplicate manifest entry")
            seen.add(name)
            data = archive.read(name)
            if name.endswith(".webp"):
                dims, rgba, _ = pixels(data, fmt="WEBP")
                if (entry.get("webp_sha256") != digest(data) or
                        entry.get("rgba_sha256") != digest(rgba) or
                        [entry.get("width"), entry.get("height")] != list(dims) or
                        not str(entry.get("original_path", "")).endswith(".png")):
                    raise ValueError("encoded screenshot provenance/pixels mismatch")
                png_count += 1
                input_bytes += entry["png_bytes"]
                output_bytes += len(data)
            elif entry.get("sha256") != digest(data) or entry.get("bytes") != len(data):
                raise ValueError("unchanged file hash mismatch")
        if ((seen | {MANIFEST}) != set(names) or png_count != manifest.get("png_count") or
                input_bytes != manifest.get("source_png_bytes") or
                output_bytes != manifest.get("webp_bytes")):
            raise ValueError("pack inventory/total mismatch")
        return manifest


def main():
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument("--input", type=Path, help="existing original GitHub Actions artifact ZIP")
    ap.add_argument("--output", type=Path, help="new, non-overwriting shadow ZIP")
    ap.add_argument("--method", type=int, default=2)
    ap.add_argument("--verify", type=Path, help="verify an existing shadow ZIP")
    args = ap.parse_args()
    if args.verify:
        result = verify(args.verify)
    elif args.input and args.output:
        result = pack(args.input, args.output, method=args.method)
        verify(args.output)
    else:
        ap.error("choose --verify, or both --input and --output")
    print(json.dumps({"schema": result["schema"], "png_count": result["png_count"],
                      "png_bytes": result["source_png_bytes"],
                      "webp_bytes": result["webp_bytes"]}, sort_keys=True))


if __name__ == "__main__":
    main()
