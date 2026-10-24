#!/usr/bin/env python3
"""Validate 12 mobile audit visual comparison pairs without claiming optical approval."""
import json
import re
from pathlib import Path

root = Path(__file__).resolve().parents[1]
path = root / "design/atlas/audit-visual-comparisons.html"
body = path.read_text(encoding="utf-8")
m = re.search(r"const scenarios=(\[[\s\S]*?\]);", body)
assert m, "No static audit comparison scenario manifest"
scenes = json.loads(m.group(1))
assert len(scenes) == 12, "Expect 6 R01 + 4 R02 + 2 R03 comparison scenes"
assert sum(s[0].startswith("R01") for s in scenes) == 6
assert sum(s[0].startswith("R02") for s in scenes) == 4
assert sum(s[0].startswith("R03") for s in scenes) == 2
for scene in scenes:
    assert len(scene) == 5 and all(scene)
    for file in scene[1:3]:
        assert not file.startswith("/") and ".." not in file, file
        p = root / "design/proposals" / file
        assert p.is_file(), (scene[0], p)
        source = p.read_text(encoding="utf-8")
        assert '<svg' in source and 'viewBox="0 0 1160 920"' in source or (
            '<svg' in source and 'width="1160" height="920"' in source), p
assert 'aspect-ratio:401/890' in body
assert 'Visual source evidence' in body
assert 'owner signoff' in body
print(json.dumps({"pairScenes":12,"r01":6,"r02":4,"r03":2,
                  "assertions":"all links resolved, 401x890 screen window",
                  "opticalOrNativeApproval":False}))
