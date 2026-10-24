#!/usr/bin/env python3
"""Validate evidence-led Glimpse audit inventory and follow-up mockup traceability.

This is document/asset integrity, never UI or owner acceptance.
"""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
instrument = json.loads((ROOT / "docs/design/glimpse-ux-audit-60.v1.json").read_text())
cycle = json.loads((ROOT / "docs/design/glimpse-ux-audit-cycle-a.v1.json").read_text())
plan = json.loads((ROOT / "design/proposals/audit-driven-revision-plan.v1.json").read_text())
ids = [q["id"] for q in instrument["questions"]]
assert ids == [f"GUX-{i:02d}" for i in range(1, 61)], "60 sequential question IDs required"
assert len(cycle["results"]) == len(ids)
assert [x["id"] for x in cycle["results"]] == ids, "same ordered audit IDs"
assert len(instrument["categories"]) == 6
assert len(plan["lanes"]) == 8
for q, v in zip(instrument["questions"], cycle["results"]):
    assert q["question"] == v["question"] and q["weight"] == v["weight"]
    assert v["status"] in {"PASS", "ISSUE", "UNVERIFIED", "NOT_APPLICABLE"}
    if v["status"] in {"PASS", "ISSUE"}:
        assert v["evidence"] and (ROOT / v["evidence"]).exists(), (v["id"], v["evidence"])
    assert v["finding"] and v["nextAction"], v["id"]
for lane in plan["lanes"]:
    assert lane["questions"] and lane["requiredScenarios"]
    assert all(x in ids for x in lane["questions"])
    for path in lane.get("initialSourceDrafts", []):
        assert (ROOT / path).exists(), path
# Verify the illustrative finite disc does not fake photographs or change the
# selected stable ID while an 18-degree drag is still in progress.
import re
for state, chosen, total in (
    ("idle-22", 12, 22), ("drag-18", 12, 22),
    ("settled-snap", 13, 22), ("last-22", 22, 22),
    ("empty-0", 0, 0), ("single-1", 1, 1),
):
    path = ROOT / "design/proposals/audit-revisions" / f"r01-{state}.svg"
    src = path.read_text()
    visible_photo_ids = [int(x) for x in re.findall(r">p(\d\d)</text>", src)]
    expected = [chosen + delta for delta in (0, 1, 2, -2, -1)
                if 1 <= chosen + delta <= total]
    assert visible_photo_ids == expected, (state, visible_photo_ids, expected)
    assert f'"photoTotal":{total}' in src, state
    assert (f'"selectedAssetId":"p{chosen:02d}"' if chosen else
            '"selectedAssetId":null') in src, state
    assert "lastConfirmedAppliedAssetId" in src
assert "Glimpse" in (ROOT / "design/proposals/audit-revisions/r01-idle-22.svg").read_text()
# R02 trace: crossing storyboard families must preserve the selected ID,
# while the crop editor keeps the source ID until an explicit saved copy exists.
import html
import re
chain = json.loads((ROOT / "design/proposals/audit-revisions/r02-identity-chain.v1.json").read_text())
assert len(chain["steps"]) == 8
for step in chain["steps"]:
    src = (ROOT / step["path"]).read_text()
    metadata = re.search(r"<metadata>(.*?)</metadata>", src, re.S)
    assert metadata, step["path"]
    state = json.loads(html.unescape(metadata.group(1)))
    assert state["selectedAssetId"] == step["expectedSelectedAssetId"], (step["id"], state["selectedAssetId"])
    assert state.get("proposalStatus", state.get("status", "")).startswith("draft"), step["id"]
assert [x["expectedSelectedAssetId"] for x in chain["steps"][:6]] == ["p12"] * 6
assert chain["steps"][6]["expectedSelectedAssetId"] == "p12"
assert chain["steps"][7]["expectedSelectedAssetId"] == "p12c"
# Two first-pass R02 drawings must visibly name the save transaction,
# never claim the p12c derivative exists before a save is acknowledged.
for state, source_id, derivative, acknowledged in (
    ("crop-pending", "p12", "null", "false"),
    ("copy-saved", "p12", '"p12c"', "true"),
):
    svg = (ROOT / "design/proposals/audit-revisions" / f"r02-{state}.svg").read_text()
    assert f'"sourceAssetId":"{source_id}"' in svg, state
    assert f'"derivedAssetId":{derivative}' in svg, state
    assert f'"saveAcknowledged":{acknowledged}' in svg, state
    assert '"lastConfirmedAppliedAssetId":"p04"' in svg, state
for state in ("share-choice", "return-dismiss"):
    svg = (ROOT / "design/proposals/audit-revisions" / f"r02-{state}.svg").read_text()
    assert '"shareRecipientConfirmed":false' in svg
    assert "p12c" in svg and "p12" in svg
# The R02 revision now uses the exact portable photographic p12 fixture from
# the established X07 Grid, not a new rainbow placeholder with the same label.
import base64
x07 = (ROOT / "design/proposals/storyboards/x07/02-grid-focus.svg").read_text()
match = re.search(r'<image id="fixture1"[^>]*href="data:image/jpeg;base64,([^"]+)"', x07)
assert match, "X07 must have original p12 photo data"
photo = base64.b64decode(match.group(1))
for state in ("crop-pending", "copy-saved", "share-choice", "return-dismiss"):
    svg = (ROOT / "design/proposals/audit-revisions" / f"r02-{state}.svg").read_text()
    frames = re.findall(r'href="data:image/jpeg;base64,([^"]+)"', svg)
    assert frames and all(base64.b64decode(p) == photo for p in frames), state
    assert '"sourcePhotoFixture":"x07/p12/art1.jpg"' in svg, state
    assert 'transform="translate(-116 -138) scale(1.5)"' in svg, state
    assert '"lastConfirmedAppliedAssetId":"p04"' in svg, state
assert len(re.findall(r'href="data:image/jpeg;base64,',
       (ROOT / "design/proposals/audit-revisions/r02-share-choice.svg").read_text())) == 3
assert 'transform="translate(-139.5 -276.75) scale(1.5)"' in (
    ROOT / "design/proposals/audit-revisions/r02-share-choice.svg").read_text()

# R01 photographic upgrades are generated deterministically from the six
# checked-in research photos; verify them without changing the source files.
if (ROOT / "design/proposals/audit-revisions/r01-photo-rest-22.svg").exists():
    from subprocess import run
    assert run(["python3", str(ROOT / "scripts/build-audit-r01-photo-variants.py")],
               cwd=ROOT, check=False).returncode == 0
# R05: preserve explicit absence of native accessibility proof and prevent
# missing reduced-motion/TalkBack source artwork from becoming false PASS.
r05 = json.loads((ROOT / "design/proposals/audit-revisions/r05-accessibility-evidence.v1.json").read_text())
assert len(r05["cases"]) == 9
for case in r05["cases"]:
    assert (ROOT / case["referencePath"]).exists(), case["referencePath"]
    assert case["nativeStatus"] == "unverified", case["id"]
    assert all(q in ids for q in case["questions"]), case["id"]
assert not any(case["dedicatedReferenceMissing"] for case in r05["cases"])
for case in r05["cases"]:
    if "dedicatedReferencePath" in case:
        p = ROOT / case["dedicatedReferencePath"]
        assert p.exists(), case["id"]
        src = p.read_text()
        assert '"nativeProof":"unverified"' in src
        assert "not Android screenshot" in src or "not actual Android" in src
# R04: distinguish bulk-import evidence from screenshot quick-import.
r04 = json.loads((ROOT / "design/proposals/audit-revisions/r04-import-evidence.v1.json").read_text())
assert len(r04["cases"]) == 6
assert len({case["id"] for case in r04["cases"]}) == 6
for case in r04["cases"]:
    assert (ROOT / case["currentPath"]).is_file(), case["id"]
    assert case["nativeEvidence"] == "unverified"
    if case.get("dedicatedReferencePath"):
        src = (ROOT / case["dedicatedReferencePath"]).read_text()
        assert '"auditLane":"R04"' in src, case["id"]
        for required in ('"photoTotal":22','"verifiedLocalCopies":17','"pendingPhotos":5',
                         '"hasFullBatchSuccess":false','"originalsPreserved":true',
                         '"nativeProof":"unverified"'):
            assert required in src, (case["id"], required)
assert len([c for c in r04["cases"] if c.get("dedicatedReferencePath")]) == 2
# R05 photographic overlay is a read-only audit layer on exactly the same
# source-image pixels as the genuine R01 paper reel. Do not confuse it with a
# separate or simulated Android TalkBack implementation.
r01_reel = (ROOT / "design/proposals/audit-revisions/r01-photo-rest-22.svg").read_text()
r05_overlay = (ROOT / "design/proposals/audit-revisions/r05-photo-reel-focus-overlay.svg").read_text()
photo_pattern = r'<image id="(fixture\d+)"[^>]*href="data:image/jpeg;base64,([^"]+)"'
r01_fixture_bytes = re.findall(photo_pattern, r01_reel)
r05_fixture_bytes = re.findall(photo_pattern, r05_overlay)
assert len(r01_fixture_bytes) == 6, "R01 actual photo-fixture group missing"
assert r01_fixture_bytes == r05_fixture_bytes, "R05 must preserve exact R01 photo bytes"
assert 'id="r05-focus-order-audit-overlay"' in r05_overlay
assert '<rect x="299" y="400" width="93" height="48" rx="6"/>' in r05_overlay
assert '"targetBoundsProposedNotMeasured":true' in r05_overlay
assert '"nativeProof":"unverified"' in r05_overlay
assert '"selectedAssetId":"p12"' in r05_overlay and '"lastConfirmedAppliedAssetId":"p04"' in r05_overlay
for case in r05["cases"]:
    if case.get("photoOverlayReferencePath"):
        assert (ROOT / case["photoOverlayReferencePath"]).exists(), case["id"]
for slug in ("rest-22", "drag-18", "snap-13", "last-22", "empty-0", "single-1"):
    image = (ROOT / "design/proposals/audit-revisions" / ("r01-photo-" + slug + ".svg")).read_text()
    assert '>Grid ↗</text>' not in image, slug
    assert 'd="M345 433 L359 419 M348 419 H359 V430"' in image, slug
counts = {x: sum(v["status"] == x for v in cycle["results"])
          for x in ("PASS", "ISSUE", "UNVERIFIED", "NOT_APPLICABLE")}
assert counts == cycle["summary"]["counts"]
assert sum(counts.values()) == 60
print(json.dumps({"questions": 60, "categories": 6, "revisionLanes": 8,
                  "scenarioTargets": sum(len(x["requiredScenarios"]) for x in plan["lanes"]),
                  "results": counts, "type": "document_trace_validation_only"}, indent=2))
