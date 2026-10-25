#!/usr/bin/env python3
"""Fail closed when the owner-review photo sequence mixes competing source sets.

This checks proposal *selection*, not native Android, image licensing beyond
the recorded C evidence, visual quality, or owner approval. No files are edited.
"""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SELECTION = "design/proposals/whole-product-review-selection.v1.json"
MIGRATION = "design/proposals/whole-product-candidates/cc0-consistent-20261024/migration-manifest.v1.json"
PROVENANCE = "design/assets/production-c-provenance.v1.json"
ROLES = {"home": "home", "reel": "reel", "grid": "grid",
         "photo-detail": "photo-detail", "collections": "collections", "share": "share"}

def evaluate(selection, migration, provenance):
    if selection.get("approval", {}).get("ownerApproved") is not False:
        raise ValueError("review selection cannot claim owner approval")
    if migration.get("ownerApproved") is not False or migration.get("androidWallpaperCurrentVerified") is not False:
        raise ValueError("photo migration claims owner/Android approval")
    if migration.get("selectedId") != "p12" or migration.get("lastGlimpseAcknowledgedId") != "p04":
        raise ValueError("preview and last acknowledged photo IDs drifted")
    all_photos = provenance["requestedIndependentPhotos"] + provenance["verifiedReplacementPhotos"]
    ids = [p["photoId"] for p in all_photos]
    hashes = [p["sha256"] for p in all_photos]
    if len(ids) != 13 or len(set(ids)) != 13 or len(set(hashes)) != 13:
        raise ValueError("13 independent source identities absent or aliased")
    if set(ids) != {"p04"} | {f"p{n:02d}" for n in range(6, 18)}:
        raise ValueError("unexpected photo IDs")
    cards = {p["surfaceId"]: p for p in selection["primarySurfaces"]
             if p["surfaceId"] in ROLES}
    if set(cards) != set(ROLES):
        raise ValueError("missing required photo review surface")
    selected = {role: cards[role]["reviewCandidatePath"] for role in ROLES}
    current = migration["currentSelectedPaths"]
    proposed = migration["sourcePaths"]
    if set(current) != set(ROLES) or set(proposed) != set(ROLES):
        raise ValueError("incomplete coordinated source manifest")
    if selected == current:
        mode = "HISTORICAL_SIX_WITH_DISCLOSED_PHOTO_GAPS"
    elif selected == proposed:
        mode = "ATOMIC_CC0_SIX_UNAPPROVED"
    else:
        differences = [role for role in ROLES if selected[role] != current[role]]
        raise ValueError("mixed or unknown six-screen photo selection: " + ", ".join(differences))
    if any(cards[r].get("ownerChoice") is not None or
           cards[r].get("exactOwnerApprovalSHA256") is not None for r in ROLES):
        raise ValueError("photo proposal erroneously claims per-screen owner selection")
    if migration.get("gridColumns", {}).get("ownerDecision") != "pending":
        raise ValueError("3-versus-4 Grid decision silently closed")
    return mode

def validate(root=ROOT):
    selection, migration, provenance = [
        json.loads((root / path).read_text())
        for path in (SELECTION, MIGRATION, PROVENANCE)
    ]
    mode = evaluate(selection, migration, provenance)
    print("PASS:", mode, "| 13 independent C photo IDs; no split migration/approval")
    return mode

if __name__ == "__main__":
    try:
        validate()
    except (OSError, KeyError, TypeError, ValueError) as exc:
        raise SystemExit(f"FAIL: photograph reviewer selection: {exc}")
