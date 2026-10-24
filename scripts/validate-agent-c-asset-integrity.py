#!/usr/bin/env python3
"""Production C: fail closed on unverifiable source art and cross-surface identity.

Proof level: checked-in asset bytes + editable SVG metadata/image bytes only.
Does not check copyright ownership, visual taste, native fidelity, or OS state.
"""
import argparse
import base64
import binascii
import hashlib
import html
import json
from pathlib import Path
import sys
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
SVG = "{http://www.w3.org/2000/svg}"
XLINK = "{http://www.w3.org/1999/xlink}href"
HEX = set("0123456789abcdef")


def require(condition, message):
    if not condition:
        raise ValueError(message)


def digest(data):
    return hashlib.sha256(data).hexdigest()


def is_sha(value):
    return isinstance(value, str) and len(value) == 64 and set(value) <= HEX


def validate_record(record):
    """Verified use rights require all fields, including real reproducible bytes."""
    status = record.get("rightsStatus")
    require(status in ("unverified", "verified"), "Unknown rights status")
    if status == "unverified":
        require(not record.get("assetPath") and not record.get("sha256"),
                "Unverified art cannot be called an integrated asset")
    else:
        require(all(record.get(k) for k in ("assetPath", "sha256", "sourceUrl",
                                             "creator", "license", "verifiedAt")),
                "Verified rights need complete provenance and bytes")
        require(is_sha(record["sha256"]), "Malformed verified SHA-256")
        require(record["sourceUrl"].startswith("https://"),
                "Verified source URL must be HTTPS")
    return status


def checked_in_fixture_assets(manifest, root):
    fixture = manifest["priorFixtureSet"]
    require(fixture["copyrightDisposition"] == "rights-unverified-not-for-new-redistribution",
            "Cannot silently promote old photo-research rights")
    hashes = {}
    for entry in fixture["assets"]:
        require(entry["rightsStatus"] == "unverified", "Historical rights are unresolved")
        require(not entry["originalUrl"] and not entry["license"],
                "Do not invent a licence for historical fixtures")
        name = entry["file"]
        require(Path(name).name == name and name.endswith(".jpg"),
                "Unsafe fixture filename")
        expected = entry["sha256"]
        require(is_sha(expected), f"Malformed {name} SHA")
        binary = (root / fixture["basePath"] / name).read_bytes()
        require(binary.startswith(b"\xff\xd8\xff"), f"{name} is not a JPEG")
        require(digest(binary) == expected, f"Fixture SHA drift: {name}")
        hashes[name] = expected
    require(len(hashes) == 6 and len(set(hashes.values())) == 6,
            "Research sources must remain six distinct byte streams")
    return hashes


def source_svg(path):
    tree = ET.parse(path)
    root = tree.getroot()
    require(root.tag == SVG + "svg", f"{path}: not SVG")
    meta = root.find(SVG + "metadata")
    require(meta is not None and meta.text, f"{path}: missing metadata")
    info = json.loads(html.unescape(meta.text))
    photos = []
    for element in root.iter(SVG + "image"):
        href = element.get("href") or element.get(XLINK) or ""
        if href.startswith("data:image/jpeg;base64,"):
            try:
                raw = base64.b64decode(href.split(",", 1)[1], validate=True)
            except (ValueError, binascii.Error) as error:
                raise ValueError(f"{path}: bad embedded JPEG: {error}") from error
            require(raw.startswith(b"\xff\xd8\xff"), f"{path}: invalid JPEG image")
            photos.append((element.get("id"), digest(raw)))
        elif href.startswith(("https:", "http:", "file:", "data:image")):
            raise ValueError(f"{path}: external or unchecked image link")
    require(not info.get("ownerApproved") and not info.get("androidVerified"),
            f"{path}: unjustified owner/native acceptance in draft metadata")
    return info, photos


def audit_cross_surface(manifest, root, fixture_hashes):
    target = manifest["crossSurfaceProofTarget"]
    selected = target["selectedId"]
    require(selected == "p12" and target["lastGlimpseAcknowledgedId"] == "p04",
            "Baseline source and Home acknowledgement must remain distinct")
    require(target["osCurrentWallpaperVerified"] is False,
            "Source-only evidence cannot verify present OS wallpaper")
    require(set(target["requiredSourcePaths"]) == {"home", "reel", "grid", "detail"},
            "Four primary proposals required")
    mode = target.get("photoSourceMode", "legacy-research")
    require(mode in ("legacy-research", "rights-verified-replacements"),
            "Unknown photographic source migration policy")
    if mode == "legacy-research":
        expected = {id_: fixture_hashes[name] for id_, name in
                    manifest["activeResearchMapping"].items()}
    else:
        expected = {x["photoId"]: x["sha256"] for x in
                    manifest["verifiedReplacementPhotos"]}
    require(set(expected) == {"p10", "p11", "p12", "p13", "p14", "p15"},
            "Research identity mapping was changed")
    require(len(set(expected.values())) == 6, "Simultaneously visible photo ID alias")
    sources = {}
    for surface, relative in target["requiredSourcePaths"].items():
        require(relative.startswith("design/proposals/audit-revisions/"),
                f"Unexpected path for {surface}")
        info, images = source_svg(root / relative)
        require(info["selectedAssetId"] == selected,
                f"{surface}: p12 selected ID changed")
        sources[surface] = (info, images)
        hashes = [sha for _, sha in images]
        require(expected[selected] in hashes,
                f"{surface}: p12 actual embedded bytes missing")
    home = sources["home"][0]
    require(home["lastConfirmedAppliedAssetId"] == "p04",
            "Home acknowledgement is not p04")
    require(home["presentOSWallpaperStatus"] == "not-read-back",
            "Proposal must not assert current device wallpaper")
    require(home["wallpaperMutation"] is False,
            "Home browse/preview must not imply apply")
    p04_reference = next(x for x in manifest["requestedIndependentPhotos"]
                         if x["photoId"] == "p04")
    artwork_claim = home["p04HeroArtwork"]
    if "illustrative vector" in artwork_claim:
        require(not home.get("p04SourceSHA256"),
                "Unintegrated p04 art cannot advertise a photo digest")
    else:
        require("source-verified" in artwork_claim,
                "Photo claim needs explicit source-verified metadata")
        require(p04_reference["rightsStatus"] == "verified",
                "p04 photo rights and source remain unverified")
        require(home.get("p04SourceSHA256") == p04_reference["sha256"],
                "p04 claim does not match independent photo bytes")
        require(p04_reference["sha256"] in {sha for _, sha in sources["home"][1]},
                "Home p04 displayed photo does not embed exact verified bytes")
    for surface in ("reel", "grid", "detail"):
        info = sources[surface][0]
        require(info.get("appliedStatus") == "unchanged",
                f"{surface}: selection must not apply wallpaper")
        require(info["photoTotal"] == 22 if surface != "detail" else True,
                f"{surface}: count altered")
    grid = sources["grid"][0]
    reel = sources["reel"][0]
    for id_, sha in expected.items():
        require(grid["photoFixtureSHA256"].get(id_) == sha,
                f"Grid metadata/bytes drift for {id_}")
    for id_ in ("p10", "p11", "p12", "p13", "p14"):
        require(reel["photoFixtureSHA256"].get(id_) == expected[id_],
                f"Reel metadata diverged for {id_}")
    grid_hashes = {sha for _, sha in sources["grid"][1]}
    require(set(expected.values()) <= grid_hashes,
            "At least one claimed Grid photo has no matching embedded JPEG")
    for surface in ("reel", "grid", "detail"):
        hashes = {sha for _, sha in sources[surface][1]}
        require(expected[selected] in hashes, f"{surface}: missing selected JPEG")
    return len(grid_hashes)


def validate(manifest, root):
    require(manifest["schemaVersion"] == 1, "Unknown asset manifest version")
    require(manifest["evidenceLevel"].startswith("source-bytes-only"),
            "Never promote sources into native evidence")
    fixtures = checked_in_fixture_assets(manifest, root)
    records = manifest["requestedIndependentPhotos"]
    ids = [record["photoId"] for record in records]
    require(sorted(ids) == sorted(("p04", "p06", "p07", "p08", "p09", "p16", "p17")),
            "Independent image identity registry incomplete")
    require(len(set(ids)) == len(ids), "Duplicate independent photo ID")
    unresolved = 0
    for record in records:
        status = validate_record(record)
        if status == "unverified":
            unresolved += 1
            require(record["sourceStatus"] == "blocked",
                    f"{record['photoId']}: unresolved rights not marked blocked")
            require(record.get("visualFallbackPath") and
                    record.get("fallbackGitBlobSha"),
                    f"{record['photoId']}: missing honest non-photographic fallback")
            raw = (root / record["visualFallbackPath"]).read_bytes()
            tree = ET.fromstring(raw)
            require(tree.tag == SVG + "svg",
                    f"{record['photoId']}: fallback is not SVG")
            metadata = tree.find(SVG + "metadata")
            require(metadata is not None and bool(metadata.text),
                    "Schematic missing metadata")
            truth = json.loads(metadata.text)
            require(truth["photoId"] == record["photoId"] and
                    truth["assetClass"] == "ORIGINAL_NONPHOTOGRAPHIC_PLACEHOLDER" and
                    truth["sourcePhotograph"] is False and
                    truth["photographicClearance"] == "blocked" and
                    truth["ownerApproved"] is False,
                    f"{record['photoId']}: misleading schematic photo claim")
            git_blob = hashlib.sha1(
                ("blob " + str(len(raw)) + "\0").encode() + raw
            ).hexdigest()
            require(git_blob == record["fallbackGitBlobSha"],
                    f"{record['photoId']}: fallback bytes silently changed")
        else:
            require(record["sourceStatus"] == "available-not-integrated",
                    "Rights-cleared source does not imply SVG integration")
            file = root / record["assetPath"]
            require(file.is_file() and file.resolve().is_relative_to(root),
                    f"{record['photoId']}: fixture missing or unsafe source path")
            binary = file.read_bytes()
            require(binary.startswith(bytes.fromhex("ffd8ff")),
                    f"{record['photoId']}: source is not a JPEG")
            require(digest(binary) == record["sha256"],
                    f"{record['photoId']}: rights-cleared bytes changed")
            require(len(binary) == record["derivativeBytes"],
                    f"{record['photoId']}: byte length changed")
            require(is_sha(record["originalSHA256"]) and
                    record["originalUrl"].startswith("https://"),
                    "Original source proof is incomplete")
    for lead in manifest["photographicLeads"]:
        published = lead["verificationStatus"].startswith("source-CC0-page-observed")
        if published:
            real = next((x for x in records if x["photoId"] == lead["proposedFor"]), None)
            require(real is not None and real["rightsStatus"] == "verified",
                    "Only rights-cleared source may be published")
            require(lead["assetPath"] == real["assetPath"] and
                    lead["sha256"] == real["sha256"],
                    "Published lead bytes do not match ledger")
        else:
            require(lead["verificationStatus"].startswith("license-page-inspected"),
                    "Lead claim is not supported")
            require(not lead["assetPath"] and not lead["sha256"],
                    "Unpublished lead must not claim local bytes")
    replacements = manifest["verifiedReplacementPhotos"]
    require({x["photoId"] for x in replacements} ==
            {"p10", "p11", "p12", "p13", "p14", "p15"} and len(replacements) == 6,
            "Six distinct photo replacements are required")
    hashes = []
    for record in replacements:
        require(validate_record(record) == "verified",
                "Replacement source lacks verified-rights metadata")
        require(record["sourceStatus"] ==
                "verified-source-pending-cross-surface-insertion",
                "Source acquisition cannot imply primary SVG replacement")
        file = root / record["assetPath"]
        require(file.is_file() and file.resolve().is_relative_to(root),
                "Missing exact independently licensed photo")
        raw = file.read_bytes()
        require(raw.startswith(bytes.fromhex("ffd8ff")) and
                len(raw) == record["derivativeBytes"] and
                digest(raw) == record["sha256"] and
                is_sha(record["originalSHA256"]) and
                record["originalUrl"].startswith("https://"),
                f"Replacement {record['photoId']} lacks exact byte/origin proof")
        hashes.append(record["sha256"])
    all_other_hashes = set(fixtures.values()) | {
        record["sha256"] for record in records if record.get("sha256")
    }
    require(len(set(hashes)) == 6 and
            not (set(hashes) & all_other_hashes),
            "Distinct replacement photo IDs cannot alias existing images")
    embedded_count = audit_cross_surface(manifest, root, fixtures)
    return unresolved, embedded_count

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--root", type=Path, default=ROOT)
    parser.add_argument("--manifest", type=Path,
                        default=Path("design/assets/production-c-provenance.v1.json"))
    args = parser.parse_args()
    root = args.root.resolve()
    manifest_path = args.manifest if args.manifest.is_absolute() else root / args.manifest
    try:
        blockers, embedded = validate(json.loads(manifest_path.read_text()), root)
    except (ValueError, KeyError, OSError, ET.ParseError, json.JSONDecodeError) as exc:
        print(f"FAIL: Production C asset proof: {exc}", file=sys.stderr)
        return 1
    print(f"PASS: 6 distinct SHA-matched source JPEGs; Grid embeds {embedded} "
          f"distinct known JPEG digest(s); exact p12 Home/Reel/Grid/Detail "
          f"continuity; p04 ack != OS readback. {blockers} independently "
          "named requested photos remain explicitly rights/bytes blocked.")
    print("SOURCE PROOF ONLY. Research images' reuse rights remain unverified. "
          "No Android, human, or owner acceptance is implied.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
