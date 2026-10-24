#!/usr/bin/env python3
"""Negative and positive tests of the C source-integrity contract."""
import copy
import importlib.util
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[2]
spec = importlib.util.spec_from_file_location(
    "agent_c_asset_integrity", ROOT / "scripts/validate-agent-c-asset-integrity.py"
)
module = importlib.util.module_from_spec(spec)
spec.loader.exec_module(module)


class ProvenanceContractTest(unittest.TestCase):
    def test_unverified_record_must_not_publish_bytes_as_cleared(self):
        record = {"rightsStatus": "unverified", "assetPath": None, "sha256": None}
        self.assertEqual("unverified", module.validate_record(record))
        record["assetPath"] = "public/false-photo.jpg"
        with self.assertRaisesRegex(ValueError, "Unverified"):
            module.validate_record(record)

    def test_verified_rights_require_real_metadata_not_just_a_url(self):
        record = {
            "rightsStatus": "verified",
            "assetPath": "design/assets/example.jpg",
            "sha256": "a" * 64,
            "sourceUrl": "https://example.test/photo",
            "creator": "The author",
            "license": "CC BY 4.0",
            "verifiedAt": "2026-10-24",
        }
        self.assertEqual("verified", module.validate_record(record))
        for field in ("sourceUrl", "creator", "license", "verifiedAt", "sha256"):
            damaged = copy.deepcopy(record)
            damaged.pop(field)
            with self.subTest(field=field), self.assertRaises(ValueError):
                module.validate_record(damaged)

    def test_short_digest_is_never_integrity_proof(self):
        self.assertFalse(module.is_sha("abc123"))
        self.assertFalse(module.is_sha("Z" * 64))
        self.assertTrue(module.is_sha("b" * 64))

    def test_cc0_asset_cannot_change_after_license_audit(self):
        import json
        manifest = json.loads(
            (ROOT / "design/assets/production-c-provenance.v1.json").read_text()
        )
        p04 = next(x for x in manifest["requestedIndependentPhotos"]
                   if x["photoId"] == "p04")
        self.assertEqual("verified", p04["rightsStatus"])
        self.assertEqual("available-not-integrated", p04["sourceStatus"])
        actual = ROOT / p04["assetPath"]
        self.assertTrue(actual.exists())
        self.assertEqual(p04["sha256"], module.digest(actual.read_bytes()))

    def test_all_thirteen_licensed_photo_ids_are_distinct(self):
        import json
        manifest = json.loads(
            (ROOT / "design/assets/production-c-provenance.v1.json").read_text()
        )
        originals = manifest["requestedIndependentPhotos"]
        replacements = manifest["verifiedReplacementPhotos"]
        hashes = [item["sha256"] for item in originals + replacements]
        self.assertEqual(13, len(hashes))
        self.assertEqual(13, len(set(hashes)))
        legacy = {x["sha256"] for x in manifest["priorFixtureSet"]["assets"]}
        self.assertFalse(set(hashes) & legacy)

    def test_migration_fails_closed_until_selected_svg_bytes_are_replaced(self):
        import json
        manifest = json.loads(
            (ROOT / "design/assets/production-c-provenance.v1.json").read_text()
        )
        self.assertEqual(
            "legacy-research",
            manifest["crossSurfaceProofTarget"]["photoSourceMode"]
        )
        manifest["crossSurfaceProofTarget"]["photoSourceMode"] = (
            "rights-verified-replacements"
        )
        with self.assertRaisesRegex(ValueError, "actual embedded bytes missing"):
            module.validate(manifest, ROOT)

    def test_embedded_photo_cannot_be_an_http_reference(self):
        import tempfile
        with tempfile.TemporaryDirectory() as temporary:
            file = Path(temporary) / "bad.svg"
            file.write_text(
                '<svg xmlns="http://www.w3.org/2000/svg">'
                '<metadata>{"ownerApproved":false,"androidVerified":false}</metadata>'
                '<image href="https://example.test/photo.jpg"/></svg>'
            )
            with self.assertRaisesRegex(ValueError, "external or unchecked"):
                module.source_svg(file)

    def test_manifest_and_current_four_proposal_sources(self):
        import json
        manifest = json.loads(
            (ROOT / "design/assets/production-c-provenance.v1.json").read_text()
        )
        blockers, embedded = module.validate(manifest, ROOT)
        self.assertEqual(0, blockers)
        self.assertGreaterEqual(embedded, 6)


if __name__ == "__main__":
    unittest.main()
