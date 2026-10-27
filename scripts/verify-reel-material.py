#!/usr/bin/env python3
"""Verify static reel material transparency, geometry and immutable image identity.

This is a source-asset gate, NOT evidence of tactile quality or Android optics.
Only stdlib is required so this can run before Gradle compilation.
"""
import hashlib
import math
from pathlib import Path
import struct
import zlib

root = Path(__file__).resolve().parents[1]
path = root / "app/src/main/res/drawable-nodpi/glimpse_paper_reel_face.png"
blob = path.read_bytes()
assert blob.startswith(b"\x89PNG\r\n\x1a\n"), "Expected a PNG resource"
offset = 8
pixels = bytearray()
width = height = 0
encoded = bytearray()
while offset < len(blob):
    length = struct.unpack(">I", blob[offset:offset+4])[0]
    kind = blob[offset+4:offset+8]
    data = blob[offset+8:offset+8+length]
    checksum = struct.unpack(">I", blob[offset+8+length:offset+12+length])[0]
    assert zlib.crc32(kind+data)&0xFFFFFFFF == checksum, "PNG CRC mismatch"
    if kind == b"IHDR":
        width, height, depth, color, compression, filtering, interlace = struct.unpack(">IIBBBBB", data)
        assert (width, height, depth, color, compression, filtering, interlace) == (640,640,8,6,0,0,0)
    if kind == b"IDAT": encoded.extend(data)
    offset += 12+length
raw = zlib.decompress(encoded)
stride = 1+width*4
assert len(raw)==height*stride and all(raw[y*stride]==0 for y in range(height)), "Rows use PNG filter 0"

def alpha(x,y):
    return raw[y*stride + 1 + x*4 + 3]

assert alpha(320,320)>=250, "Center hub must be present"
assert alpha(4,4)==0, "Off-disc corners must be transparent"
assert alpha(320,4)>=200, "Outer paper rim must be present"
for k in range(5):
    angle = math.radians(-90+k*72)
    x = round(320+316*.66*math.cos(angle))
    y = round(320+316*.66*math.sin(angle))
    assert alpha(x,y) <= 5, f"Cutout {k} is opaque: ({x},{y})"
for k in range(5):
    angle = math.radians(-90+(k+.5)*72)
    x = round(320+316*.67*math.cos(angle))
    y = round(320+316*.67*math.sin(angle))
    assert alpha(x,y) > 235, f"Paper separator {k} disappeared"

print("PASS: 640x640 baked alpha-mask/cardstock face has five clear photo windows and intact hub/rim/separators")
print("Source SHA256:",hashlib.sha256(blob).hexdigest())
