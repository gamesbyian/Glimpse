"""Synthetic positive and adversarial lossless visual-atlas bundle controls."""
import io
import json
import pathlib
import sys
import tempfile
import unittest
import zipfile

from PIL import Image
sys.path.insert(0, str(pathlib.Path(__file__).resolve().parent))
import repack_atlas_lossless as m


def png(color):
    im = Image.new("RGBA", (13, 17), color)
    out = io.BytesIO()
    im.save(out, format="PNG")
    return out.getvalue()


class PackTests(unittest.TestCase):
    def test_complete_pixel_equivalence_and_unmodified_input(self):
        with tempfile.TemporaryDirectory() as td:
            source, pack = pathlib.Path(td)/"source.zip", pathlib.Path(td)/"shadow.zip"
            original_png = png((10, 20, 30, 121))
            with zipfile.ZipFile(source, "w") as archive:
                archive.writestr("screens/a.png", original_png)
                archive.writestr("screens/b.png", original_png)  # intentional distinct capture name
                archive.writestr("screens/a.xml", "<node />")
            original_zip = source.read_bytes()
            result = m.pack(source, pack)
            self.assertEqual(result["png_count"], 2)
            self.assertEqual(source.read_bytes(), original_zip)
            self.assertEqual(len(m.verify(pack)["entries"]), 3)
            with zipfile.ZipFile(pack) as archive:
                self.assertIn("screens/b.webp", archive.namelist())
                self.assertIn("screens/a.xml", archive.namelist())

    def test_reject_path_traversal_and_duplicate(self):
        for entries in [[("../x.png", png((1,2,3,4)))],
                        [("x.png", png((1,2,3,4))), ("x.png", png((3,4,5,6)))]]:
            with self.subTest(entries=entries), tempfile.TemporaryDirectory() as td:
                p, q = pathlib.Path(td)/"source.zip", pathlib.Path(td)/"shadow.zip"
                with zipfile.ZipFile(p, "w") as z:
                    for name, payload in entries:
                        z.writestr(name, payload)
                with self.assertRaises(ValueError):
                    m.pack(p, q)

    def test_mismatch_and_tampered_manifest_rejected(self):
        with tempfile.TemporaryDirectory() as td:
            p, q, broken = [pathlib.Path(td)/x for x in ("source.zip", "packed.zip", "tampered.zip")]
            with zipfile.ZipFile(p,"w") as z:z.writestr("a.png", png((1,2,3,4)))
            m.pack(p,q)
            with zipfile.ZipFile(q) as z:
                manifest=json.loads(z.read(m.MANIFEST))
                manifest["entries"][0]["rgba_sha256"]="0"*64
                with zipfile.ZipFile(broken,"w") as dst:
                    for name in z.namelist():
                        dst.writestr(name, json.dumps(manifest) if name==m.MANIFEST else z.read(name))
            with self.assertRaisesRegex(ValueError,"mismatch"):
                m.verify(broken)

    def test_restored_review_zip_keeps_every_filename_and_exact_pixels(self):
        with tempfile.TemporaryDirectory() as td:
            directory = pathlib.Path(td)
            source, shadow, restored = (directory / name for name in
                                        ("source.zip", "shadow.zip", "review.zip"))
            first = png((13, 92, 172, 77))
            second = png((13, 92, 172, 255))
            with zipfile.ZipFile(source, "w") as z:
                z.writestr("atlas/a.png", first)
                z.writestr("atlas/b.png", first)
                z.writestr("atlas/c.png", second)
                z.writestr("atlas/a.xml", "<hierarchy />")
                z.writestr("index.html", "<html />")
            m.pack(source, shadow)
            shadow_bytes = shadow.read_bytes()
            m.restore(shadow, restored)
            self.assertEqual(shadow_bytes, shadow.read_bytes())
            with zipfile.ZipFile(source) as originals, zipfile.ZipFile(restored) as output:
                self.assertEqual(set(originals.namelist()), set(output.namelist()))
                for name in originals.namelist():
                    if name.endswith(".png"):
                        with Image.open(io.BytesIO(originals.read(name))) as a:
                            with Image.open(io.BytesIO(output.read(name))) as b:
                                self.assertEqual(a.size, b.size)
                                self.assertEqual(a.convert("RGBA").tobytes(),
                                                 b.convert("RGBA").tobytes())
                    else:
                        self.assertEqual(originals.read(name), output.read(name))
            # These are regenerated PNGs, not byte-identical source PNGs.
            self.assertEqual(m.verify(shadow)["png_count"], 3)

    def test_restore_rejects_unsafe_manifest_paths_and_existing_output(self):
        with tempfile.TemporaryDirectory() as td:
            directory = pathlib.Path(td)
            source, shadow, tampered, restored = (directory / name for name in
                ("source.zip", "shadow.zip", "bad.zip", "restored.zip"))
            with zipfile.ZipFile(source, "w") as z:
                z.writestr("a.png", png((20, 40, 60, 80)))
            m.pack(source, shadow)
            m.restore(shadow, restored)
            with self.assertRaises(FileExistsError):
                m.restore(shadow, restored)
            with self.assertRaises(ValueError):
                m.restore(shadow, shadow)
            with zipfile.ZipFile(shadow) as z:
                manifest = json.loads(z.read(m.MANIFEST))
                manifest["entries"][0]["original_path"] = "../outside.png"
                with zipfile.ZipFile(tampered, "w") as dst:
                    for name in z.namelist():
                        dst.writestr(
                            name, json.dumps(manifest) if name == m.MANIFEST else z.read(name)
                        )
            with self.assertRaises(ValueError):
                m.restore(tampered, directory / "not-created.zip")
            self.assertFalse((directory / "not-created.zip").exists())

    def test_no_overwrite_or_missing_png(self):
        with tempfile.TemporaryDirectory() as td:
            p = pathlib.Path(td)/"in.zip"; q = pathlib.Path(td)/"out.zip"
            with zipfile.ZipFile(p,"w") as z:z.writestr("notes.xml", "text")
            with self.assertRaisesRegex(ValueError,"no PNG"):
                m.pack(p,q)
            with self.assertRaises(ValueError):m.pack(p,p)
            self.assertFalse(q.exists())

if __name__ == "__main__":unittest.main()
