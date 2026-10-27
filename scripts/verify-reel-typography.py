#!/usr/bin/env python3
"""Offline identity test for the two native Glimpse OFL font families."""
import hashlib
from pathlib import Path

root = Path(__file__).resolve().parents[1]
expected = {
    "app/src/main/res/font/fraunces_display.ttf": ("0ed4f0c5115b70752dfdd32f47409ce30d0300e7", b"\x00\x01\x00\x00"),
    "app/src/main/res/font/plus_jakarta_sans.ttf": ("0cb13a998ed525ba226d911b10d6c4c4f923a961", b"\x00\x01\x00\x00"),
    "docs/licenses/fonts/Fraunces-OFL.txt": ("22fa7ce5733434d0c634f6e55116f410858e938c", None),
    "docs/licenses/fonts/PlusJakartaSans-OFL.txt": ("ea69b0ca12a02556c436680ea0a159efb7a748fa", None),
}
for path, (sha, magic) in expected.items():
    data = (root / path).read_bytes()
    git_sha = hashlib.sha1(b"blob " + str(len(data)).encode() + b"\0" + data).hexdigest()
    assert git_sha == sha, f"Source font/licence byte identity changed: {path}"
    if magic:
        assert data[:4] == magic, f"Unexpected font container: {path}"
    else:
        assert b"Open Font License" in data, f"Missing full OFL text: {path}"
    print(f"PASS {path}: {len(data)} bytes · upstream Git blob {sha}")
print("PASS: original Fraunces and Plus Jakarta Sans resources and licences travel together")
