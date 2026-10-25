#!/usr/bin/env python3
"""Negative cases for C's atomic six-photo reviewer selection guard."""
import copy
import importlib.util
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
spec = importlib.util.spec_from_file_location("review_guard", ROOT / "scripts/validate-agent-c-review-selection.py")
mod = importlib.util.module_from_spec(spec)
spec.loader.exec_module(mod)

class PhotoSelectionTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        import json
        cls.selection, cls.migration, cls.provenance = [
            json.loads((ROOT / p).read_text())
            for p in (mod.SELECTION, mod.MIGRATION, mod.PROVENANCE)
        ]

    def example(self):
        return copy.deepcopy((self.selection, self.migration, self.provenance))

    def test_current_six_accepted_with_caveat(self):
        self.assertEqual(mod.evaluate(*self.example()), "HISTORICAL_SIX_WITH_DISCLOSED_PHOTO_GAPS")

    def test_atomic_six_supported_as_unapproved_alternative(self):
        selection, migration, provenance = self.example()
        for item in selection["primarySurfaces"]:
            if item["surfaceId"] in mod.ROLES:
                item["reviewCandidatePath"] = migration["sourcePaths"][item["surfaceId"]]
        self.assertEqual(mod.evaluate(selection,migration,provenance), "ATOMIC_CC0_SIX_UNAPPROVED")

    def test_partial_migration_rejected(self):
        selection,migration,provenance = self.example()
        next(x for x in selection["primarySurfaces"] if x["surfaceId"] == "home")["reviewCandidatePath"] = migration["sourcePaths"]["home"]
        with self.assertRaisesRegex(ValueError, "mixed or unknown"):
            mod.evaluate(selection,migration,provenance)

    def test_identity_alias_rejected(self):
        selection,migration,provenance = self.example()
        provenance["verifiedReplacementPhotos"][0]["sha256"] = provenance["requestedIndependentPhotos"][0]["sha256"]
        with self.assertRaisesRegex(ValueError, "aliased"):
            mod.evaluate(selection,migration,provenance)

    def test_false_approval_rejected(self):
        selection,migration,provenance = self.example()
        selection["approval"]["ownerApproved"] = True
        with self.assertRaisesRegex(ValueError, "owner approval"):
            mod.evaluate(selection,migration,provenance)

if __name__ == "__main__":
    unittest.main()
