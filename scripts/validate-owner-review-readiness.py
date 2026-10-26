#!/usr/bin/env python3
"""Check factual integrity of Glimpse's still-unapproved owner-review package.

A PASS means documented source links/choices are coherent, never visual approval.
"""
import json
from pathlib import Path

root=Path(__file__).resolve().parents[1]
def load(path):
    return json.loads((root/path).read_text(encoding="utf-8"))
selection=load("design/proposals/whole-product-review-selection.v1.json")
migration=load("design/proposals/whole-product-candidates/cc0-consistent-20261024/migration-manifest.v1.json")
snapshot=load("design/atlas/whole-product-review-snapshot.v1.json")
guide=root/"docs/planning/OWNER-REVIEW-2026-10-25-DRAFT-DECISION-GUIDE.md"
assert guide.is_file(), "Missing owner-review decision guide"
assert selection["approval"]["ownerApproved"] is False
assert migration["ownerApproved"] is False and migration["nativeAcceptance"] is False
assert snapshot["status"]=="DRAFT_REVIEW_SNAPSHOT_NOT_APPROVED"
questions=selection["explicitOwnerQuestions"]
assert [q["id"] for q in questions]==[f"D{i:02d}" for i in range(1,9)],"Eight owner questions changed"
txt=guide.read_text(encoding="utf-8")
for q in questions:
    assert q["id"] in txt, f"Missing review decision {q['id']}"
assert migration["status"]=="EDITORIAL_CC0_SIX_REVIEW_CANDIDATE_NOT_OWNER_APPROVED"
assert migration["androidWallpaperCurrentVerified"] is False
assert migration["selectedId"]=="p12" and migration["lastGlimpseAcknowledgedId"]=="p04"
assert len(migration["sourcePaths"])==len(migration["currentSelectedPaths"])==6
assert set(migration["sourcePaths"])==set(migration["currentSelectedPaths"])
for path in list(migration["sourcePaths"].values())+list(migration["currentSelectedPaths"].values()):
    assert (root/path).is_file(),f"Missing coherent photo screen: {path}"
asset_rows=migration["verifiedIndependentPhotos"]
assert len(asset_rows)==13 and len({a["id"] for a in asset_rows})==13
assert len({a["derivedSHA256"] for a in asset_rows})==13
for a in asset_rows:
    assert (root/a["assetPath"]).is_file(),f"Missing independent photo: {a['assetPath']}"
    assert len(a["derivedSHA256"])==64
for path in ("design/atlas/whole-product-review-selection.html",
             "design/system/reviews/ci-renders/cc0-consistent-six-screen-contact.jpg",
             "design/reviews/support-agent-b-2026-10-24/full-journey-corpus/OPTICAL-REVIEW.md"):
    assert (root/path).is_file(),f"Missing owner-viewable evidence: {path}"
print("PASS: eight pending decisions, six paired alternative/selected sources and 13 distinct CC0 asset paths exist; NO approval claimed")
