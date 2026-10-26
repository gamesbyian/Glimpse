"""Adversarial regressions for six-surface photographic source-byte integrity."""
import base64
import importlib.util
import json
import shutil
import tempfile
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
MODULE = ROOT / "scripts/validate-agent-c-cc0-proposals.py"
spec = importlib.util.spec_from_file_location("agent_c_photo_exact", MODULE)
audit = importlib.util.module_from_spec(spec)
spec.loader.exec_module(audit)

class ExactPhotographAuditTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory(prefix="glimpse-photo-audit-")
        self.root = Path(self.temp.name)
        self.ca = json.loads((ROOT / audit.C).read_text())
        self.aa = json.loads((ROOT / audit.A).read_text())
        files = [audit.C, audit.A]
        files += [x["assetPath"] for x in
                  self.ca["requestedIndependentPhotos"] +
                  self.ca["verifiedReplacementPhotos"]]
        files += list(self.aa["sourcePaths"].values())
        for source in files:
            path = self.root / source
            path.parent.mkdir(parents=True, exist_ok=True)
            shutil.copy2(ROOT / source, path)

    def tearDown(self):
        self.temp.cleanup()

    def write_manifests(self):
        (self.root / audit.C).write_text(json.dumps(self.ca, indent=2) + "\n")
        (self.root / audit.A).write_text(json.dumps(self.aa, indent=2) + "\n")

    def test_unmodified_six_screens_prove_25_exact_instances(self):
        self.assertEqual(25, audit.validate(self.root))

    def test_owner_approval_fabrication_is_rejected(self):
        self.aa["ownerApproved"] = True
        self.write_manifests()
        with self.assertRaisesRegex(ValueError, "claims acceptance"):
            audit.validate(self.root)

    def test_one_byte_mutation_of_actual_cc0_file_is_rejected(self):
        photo = self.root / self.ca["verifiedReplacementPhotos"][2]["assetPath"]
        raw = bytearray(photo.read_bytes())
        raw[len(raw)//2] ^= 1
        photo.write_bytes(raw)
        with self.assertRaisesRegex(ValueError, "missing or mutated exact image bytes"):
            audit.validate(self.root)

    def test_missing_p12_embedded_in_share_is_rejected(self):
        # Keep the SVG parseable with a real but *wrong* verified JPEG.
        import xml.etree.ElementTree as ET
        path = self.root / self.aa["sourcePaths"]["share"]
        root = ET.parse(path).getroot()
        p04 = next(p for p in self.ca["requestedIndependentPhotos"]
                   if p["photoId"] == "p04")
        alt = "data:image/jpeg;base64," + base64.b64encode(
            (self.root / p04["assetPath"]).read_bytes()).decode("ascii")
        count = 0
        for node in root.iter():
            if node.tag.rsplit("}", 1)[-1] == "image":
                key = "href" if "href" in node.attrib else audit.XLINK
                node.set(key, alt)
                count += 1
        self.assertGreater(count, 0)
        ET.ElementTree(root).write(path, encoding="unicode")
        with self.assertRaisesRegex(ValueError, "share includes missing or unauthorized"):
            audit.validate(self.root)

    def test_extra_authorized_image_in_wrong_screen_role_is_rejected(self):
        # Even a legitimately sourced p04 image cannot be quietly injected
        # into a p12-only Share screen: role truth is part of source integrity.
        import xml.etree.ElementTree as ET
        path = self.root / self.aa["sourcePaths"]["share"]
        tree = ET.parse(path)
        root = tree.getroot()
        p04 = next(p for p in self.ca["requestedIndependentPhotos"]
                   if p["photoId"] == "p04")
        source = base64.b64encode(
            (self.root / p04["assetPath"]).read_bytes()
        ).decode("ascii")
        ET.SubElement(root, "{http://www.w3.org/2000/svg}image", {
            "href": "data:image/jpeg;base64," + source,
            "x": "0", "y": "0", "width": "10", "height": "10"
        })
        tree.write(path, encoding="unicode")
        with self.assertRaisesRegex(ValueError, "share includes missing or unauthorized"):
            audit.validate(self.root)

    def test_two_stable_photo_ids_aliasing_same_jpeg_is_rejected(self):
        p10 = next(p for p in self.ca["verifiedReplacementPhotos"]
                   if p["photoId"] == "p10")
        p11 = next(p for p in self.ca["verifiedReplacementPhotos"]
                   if p["photoId"] == "p11")
        for field in ("sha256", "assetPath", "derivativeBytes"):
            p11[field] = p10[field]
        a11 = next(p for p in self.aa["verifiedIndependentPhotos"] if p["id"] == "p11")
        a11["derivedSHA256"] = p10["sha256"]
        a11["assetPath"] = p10["assetPath"]
        self.write_manifests()
        with self.assertRaisesRegex(ValueError, "Two photo IDs alias same JPEG"):
            audit.validate(self.root)

if __name__ == "__main__":
    unittest.main()
