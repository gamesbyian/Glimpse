#!/usr/bin/env python3
"""One-time production C CC0 Wikimedia lead acquisition for source audit.

Acquires a photographer self-described own-work CC0 photo. Prints a bounded
transport copy so the actual downloaded, EXIF-stripped bytes can be reviewed
and *explicitly* committed as a distinct p04 image, not silently substituted.
Never grants a licence for Glimpse's older research art.
"""
import base64
import hashlib
import io
import json
from PIL import Image, ImageOps
from urllib.request import Request, urlopen

URL = "https://upload.wikimedia.org/wikipedia/commons/f/f0/Random_landscape_photo_I_took.jpg"
PAGE = "https://commons.wikimedia.org/wiki/File:Random_landscape_photo_I_took.jpg"

def sha(data):
    return hashlib.sha256(data).hexdigest()

def main():
    req = Request(URL, headers={"User-Agent": "GlimpseAssetProvenance/1.0 (research reference; Wikimedia Commons)"})
    with urlopen(req, timeout=50) as response:
        raw = response.read(4_000_001)
    if not raw.startswith(bytes.fromhex("ffd8ff")) or len(raw) > 4_000_000:
        raise ValueError("Expected a bounded authentic source JPEG")
    with Image.open(io.BytesIO(raw)) as photo:
        if photo.format != "JPEG":
            raise ValueError("Not a JPEG")
        image = ImageOps.exif_transpose(photo).convert("RGB")
    image.thumbnail((900, 900), Image.Resampling.LANCZOS)
    output = io.BytesIO()
    image.save(output, "JPEG", quality=82, optimize=True)
    smaller = output.getvalue()
    if len(smaller) > 180_000:
        raise ValueError("Generated small fixture unexpectedly oversized")
    print("PHOTO_PROVENANCE=" + json.dumps({
        "photoId": "p04",
        "sourcePage": PAGE,
        "originalUrl": URL,
        "creator": "Philsacor",
        "license": "CC0 1.0 Universal",
        "licenseUrl": "https://creativecommons.org/publicdomain/zero/1.0/",
        "licensePageVerifiedOn": "2026-10-24",
        "originalSHA256": sha(raw),
        "originalByteLength": len(raw),
        "publishedDerivativeSHA256": sha(smaller),
        "derivativeBytes": len(smaller),
        "derivativeDimensions": [image.width, image.height],
        "transform": "EXIF-transpose, stripped metadata, <=900px, JPEG quality 82",
        "evidenceClass": "source+web license page; not applied to Android",
    }, separators=(",", ":")), flush=True)
    print("PHOTO_BASE64_BEGIN", flush=True)
    encoded = base64.b64encode(smaller).decode("ascii")
    for at in range(0, len(encoded), 160):
        print("C64:" + encoded[at:at+160], flush=True)
    print("PHOTO_BASE64_END", flush=True)

if __name__ == "__main__":
    main()
