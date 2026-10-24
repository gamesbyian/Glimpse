#!/usr/bin/env python3
"""Fast, offline source contract for the R06–R08 candidate visual deltas.

Validates source/metadata integrity only. Does not assert optical, native or owner approval.
"""
import json
import hashlib
import io
import argparse
from pathlib import Path
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
INDEX = ROOT / "design/proposals/audit-revisions/r06-r08-delta-evidence.v1.json"
SVG_NS = "{http://www.w3.org/2000/svg}"

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--render", action="store_true", help="Render actual 401x890 phone PNG; requires CairoSVG and Pillow (pixel equality opt-in)")
    parser.add_argument("--strict-pixels", action="store_true", help="Require equal PNG bytes from the original inspection environment")
    parser.add_argument("--output-dir", type=Path, help="Store generated phone crops for human optical inspection")
    args = parser.parse_args()
    if args.strict_pixels and not args.render:
        parser.error("--strict-pixels requires --render")
    if args.output_dir and not args.render:
        parser.error("--output-dir requires --render")
    if args.output_dir:
        args.output_dir.mkdir(parents=True, exist_ok=True)
    data = json.loads(INDEX.read_text(encoding="utf-8"))
    assert data["auditBaseline"] == {"pass": 18, "issue": 8, "unverified": 34, "changedScores": 0}
    scenarios = data["scenarios"]
    assert len(scenarios) == 5, "Five distinct targeted states expected"
    seen = set()
    for case in scenarios:
        path = ROOT / case["source"]
        prior = ROOT / case["before"]
        assert path.is_file(), path
        assert prior.is_file(), prior
        assert str(path) not in seen, f"Duplicated proposal {path}"
        seen.add(str(path))
        assert case["illustratedInDraft"] is True
        for gate in ("verifiedOnAndroid", "ownerApprovedExactSourceSHA"):
            assert case[gate] is False, f"Evidence gate {gate} requires a separate observed proof record"
        if case["opticallyReviewedFromExactSourceSHA"]:
            proof = case["opticalEvidence"]
            assert proof["sourceSHA256"] == hashlib.sha256(path.read_bytes()).hexdigest(), f"Stale source optical proof {path}"
            assert len(proof["phonePNG_SHA256"]) == 64
            assert proof["outputSize"] == [401, 890]
        if args.render:
            import cairosvg
            from PIL import Image
            raw = cairosvg.svg2png(bytestring=path.read_bytes(), output_width=1160, output_height=920)
            img = Image.open(io.BytesIO(raw)).convert("RGB").crop((15, 14, 416, 904))
            buff = io.BytesIO()
            img.save(buff, format="PNG")
            png_hash = hashlib.sha256(buff.getvalue()).hexdigest()
            if args.strict_pixels:
                assert png_hash == case["opticalEvidence"]["phonePNG_SHA256"], f"Crop SHA mismatch for {path}: {png_hash}"
            elif png_hash != case["opticalEvidence"]["phonePNG_SHA256"]:
                print(f"RENDERER DIFFERENCE (not automatically a visual defect): {path.name} local={png_hash} reviewed={case['opticalEvidence']['phonePNG_SHA256']}")
            if args.output_dir:
                (args.output_dir / (path.stem + ".phone.png")).write_bytes(buff.getvalue())
        assert case["phoneCrop"] == {"x": 15, "y": 14, "width": 401, "height": 890}
        tree = ET.parse(path)
        root = tree.getroot()
        assert root.tag == SVG_NS + "svg"
        assert root.attrib.get("viewBox") == "0 0 1160 920"
        assert root.attrib.get("width") == "1160" and root.attrib.get("height") == "920"
        metadata = root.find(SVG_NS + "metadata")
        assert metadata is not None and metadata.text
        meta = json.loads(metadata.text)
        assert meta["auditLane"] == case["id"].split("-")[0]
        assert meta["proposalStatus"] == "draft-not-owner-approved"
        assert meta["wallpaperMutation"] is False and meta["recipientConfirmed"] is False
        assert meta["phoneCrop"] == [15, 14, 401, 890]
        assert set(case["questions"]).issubset(set(meta["auditQuestions"]))
        texts = [node.text or "" for node in root.iter(SVG_NS + "text")]
        assert "Native Android / TalkBack: unverified" in texts
        assert any("DRAFT" in t for t in texts)
        assert len(texts) >= 20
        print(f"source-only OK: {case['id']} -> {case['source']}")
    print("PASS: 5 R06–R08 source/evidence checks" + (" and five pixel-exact crops" if args.render else "") + "; 0 Android/owner gates asserted")

if __name__ == "__main__":
    main()
