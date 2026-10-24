#!/usr/bin/env python3
"""C-only provenance acquisition. Expects exactly six independently titled CC0 files.

Emits bounded derived JPEG bytes in CI logs so the agent can explicitly verify,
hash, and commit them as ordinary source files. Inputs are from CC0-per-file
manual web-page desk review; API rechecks machine-readable license metadata.
Nothing is copied into approved SVGs automatically.
"""
import base64
import hashlib
import io
import json
import re
import sys
from pathlib import Path
from urllib.parse import urlencode
from urllib.request import Request, urlopen
from PIL import Image, ImageOps

ROOT=Path(__file__).resolve().parents[1]
INPUT=ROOT/"design/assets/production-c-grid-cc0-candidates.v1.json"
HEADERS={"User-Agent":"GlimpseFixtureSourceAudit/1.0 (small CC0 photos; no automated redistribution)"}

def sha(data):
    return hashlib.sha256(data).hexdigest()

def get_json(title):
    query=urlencode({
        "action":"query", "prop":"imageinfo", "iiprop":"url|size|extmetadata",
        "titles":"File:"+title, "format":"json", "formatversion":2
    })
    url="https://commons.wikimedia.org/w/api.php?"+query
    with urlopen(Request(url,headers=HEADERS),timeout=40) as response:
        return json.loads(response.read(1_000_000))

def source_info(record):
    doc=get_json(record["commonsFileTitle"])
    page=doc["query"]["pages"][0]
    if "missing" in page or not page.get("imageinfo"):
        raise ValueError("Commons file unavailable: "+record["photoId"])
    source=page["imageinfo"][0]
    meta=source["extmetadata"]
    license_code=re.sub("<[^>]+>","",meta["LicenseShortName"]["value"]).strip()
    license_url=meta.get("LicenseUrl",{}).get("value","")
    if "CC0" not in license_code and "CC0" not in license_url:
        raise ValueError("Commons current machine-readable license not CC0: "+record["photoId"]+" "+license_code)
    source_url=source["url"]
    if not source_url.startswith("https://upload.wikimedia.org/"):
        raise ValueError("Unexpected Commons binary URL")
    if source.get("size",0)>12_000_000:
        raise ValueError("Oversize photo, refuse unbounded transfer")
    return source_url, license_code, source["size"]

def main():
    manifest=json.loads(INPUT.read_text())
    records=manifest["candidates"]
    wanted={"p06","p07","p08","p09","p16","p17"}
    if {x["photoId"] for x in records}!=wanted or len(records)!=6:
        raise ValueError("Expected six unique stable IDs")
    for record in records:
        ident=record["photoId"]
        original_url, license_code, byte_length=source_info(record)
        with urlopen(Request(original_url,headers=HEADERS),timeout=50) as response:
            original=response.read(12_000_001)
        if len(original)!=byte_length or len(original)>12_000_000 or not original.startswith(bytes.fromhex("ffd8ff")):
            raise ValueError("Source byte size/format mismatch for "+ident)
        with Image.open(io.BytesIO(original)) as im:
            if im.format!="JPEG":
                raise ValueError("Photo not JPEG: "+ident)
            converted=ImageOps.exif_transpose(im).convert("RGB")
        converted.thumbnail((900,900),Image.Resampling.LANCZOS)
        buffer=io.BytesIO()
        converted.save(buffer,"JPEG",quality=82,optimize=True)
        derivative=buffer.getvalue()
        if len(derivative)>220_000:
            raise ValueError("Downsampled jpeg oversized: "+ident)
        record_out={
            "photoId":ident, "author":record["creator"],
            "commonsPage":record["commonsPage"], "sourceUrl":original_url,
            "license":license_code, "licensePageVerifiedOn":record["licensePageInspectedAt"],
            "originalSHA256":sha(original),"originalBytes":len(original),
            "derivativeSHA256":sha(derivative),"derivativeBytes":len(derivative),
            "derivativeDimensions":[converted.width,converted.height],
            "transform":"EXIF transpose, strip metadata, <=900px, JPEG quality 82",
            "evidenceBoundary":"source bytes and uploader license only, not current Android acceptance"
        }
        print("PHOTO_PROVENANCE="+json.dumps(record_out,separators=(",",":")),flush=True)
        print("PHOTO_BASE64_BEGIN:"+ident,flush=True)
        transport=base64.b64encode(derivative).decode("ascii")
        for j in range(0,len(transport),160):
            print("C64:"+ident+":"+transport[j:j+160],flush=True)
        print("PHOTO_BASE64_END:"+ident,flush=True)
    print("ACQUISITION_COMPLETE:6 distinct real CC0 source files",flush=True)

if __name__=="__main__":
    main()
