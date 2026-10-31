"""Deterministic positive/negative guards for architectural safety."""
import tempfile
import unittest
from pathlib import Path
from validate_android_boundaries import inspect, GATEWAY, MANIFEST

MANIFEST_TEXT = '''<manifest xmlns:android="http://schemas.android.com/apk/res/android">
<application android:allowBackup="false" android:fullBackupContent="false" android:dataExtractionRules="@xml/glimpse_data_extraction_rules">
<provider android:name=".GlimpseOriginalFileProvider" android:exported="false" android:grantUriPermissions="true"/>
<provider android:name=".GlimpseEditedFileProvider" android:exported="false" android:grantUriPermissions="true"/>
</application></manifest>'''

DOMAINS = ("root", "file", "database", "sharedpref", "external",
           "device_root", "device_file", "device_database",
           "device_sharedpref")
RULES_TEXT = "<data-extraction-rules>" + "".join(
    "<" + mode + ">" +
    "".join('<exclude domain="' + domain + '" path="."/>'
            for domain in DOMAINS) +
    "</" + mode + ">"
    for mode in ("cloud-backup", "device-transfer")
) + "</data-extraction-rules>"


class BoundaryTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.gateway = self.root / GATEWAY
        self.gateway.parent.mkdir(parents=True)
        self.gateway.write_text('WallpaperManager.getInstance(ctx).setBitmap(bitmap)', encoding='utf8')
        self.manifest = self.root / MANIFEST
        self.manifest.parent.mkdir(parents=True, exist_ok=True)
        self.manifest.write_text(MANIFEST_TEXT, encoding='utf8')
        self.rules = (self.root /
                      'app/src/main/res/xml/glimpse_data_extraction_rules.xml')
        self.rules.parent.mkdir(parents=True, exist_ok=True)
        self.rules.write_text(RULES_TEXT, encoding='utf8')

    def test_compliant_app(self):
        self.assertFalse(inspect(self.root))

    def test_second_setter_fails(self):
        other = self.gateway.parent / 'Rogue.kt'
        other.write_text('fun run() = wallpaper.setBitmap(bitmap)', encoding='utf8')
        self.assertTrue(any('second wallpaper gateway' in x for x in inspect(self.root)))

    def test_destructive_migration_fails(self):
        self.gateway.write_text('fallbackToDestructiveMigration()', encoding='utf8')
        self.assertTrue(any('destructive Room fallback' in x for x in inspect(self.root)))

    def test_unprotected_importer_attachment_fails(self):
        rogue = self.gateway.parent / "UnprotectedImporter.kt"
        rogue.write_text("fun import(dao: GlimpseDao) { dao.attach(photo, collection) }",
                         encoding="utf8")
        self.assertTrue(any("unguarded Room" in x for x in inspect(self.root)))

    def test_external_direct_membership_insert_fails(self):
        rogue = self.gateway.parent / "UnsafeCollectionEdit.kt"
        rogue.write_text(
            "fun change(dao: GlimpseDao) { dao.insertMembership(membership) }",
            encoding="utf8")
        self.assertTrue(any("unguarded Room" in x for x in inspect(self.root)))

    def test_guarded_importer_api_is_allowed(self):
        safe = self.gateway.parent / "SafeImporter.kt"
        safe.write_text(
            "fun import(dao: GlimpseDao) { dao.attachIfCollectionExists(photo, id) }",
            encoding="utf8")
        self.assertFalse(inspect(self.root))

    def test_exported_photo_provider_fails(self):
        self.manifest.write_text(MANIFEST_TEXT.replace('android:exported="false"',
            'android:exported="true"', 1), encoding='utf8')
        self.assertTrue(any('Unsafe provider' in x for x in inspect(self.root)))

    def test_broad_backup_fails(self):
        self.manifest.write_text(MANIFEST_TEXT.replace('allowBackup="false"',
            'allowBackup="true"'), encoding='utf8')
        self.assertTrue(any('allowBackup=false' in x for x in inspect(self.root)))

    def test_missing_device_transfer_rules_fail(self):
        self.rules.write_text(RULES_TEXT.replace(
            "<device-transfer>", "<unapproved-device-transfer>"
        ).replace("</device-transfer>", "</unapproved-device-transfer>"),
            encoding='utf8')
        self.assertTrue(any("device-transfer" in x for x in inspect(self.root)))

    def test_incomplete_device_domain_exclusion_fails(self):
        self.rules.write_text(RULES_TEXT.replace(
            '<exclude domain="device_database" path="."/>', ""),
            encoding='utf8')
        self.assertTrue(any("device_database" in x for x in inspect(self.root)))

    def test_xml_rules_absence_fails(self):
        self.rules.unlink()
        self.assertTrue(any("data extraction rules" in x for x in inspect(self.root)))

    def test_cloud_backup_cannot_include_private_files(self):
        self.rules.write_text(RULES_TEXT.replace(
            "<cloud-backup>", '<cloud-backup><include domain="file" path="."/>'
        ), encoding='utf8')
        self.assertTrue(any("must have no includes" in x for x in inspect(self.root)))

    def test_backward_compat_backup_attribute_is_required(self):
        self.manifest.write_text(MANIFEST_TEXT.replace(
            'android:fullBackupContent="false"', 'android:fullBackupContent="true"'
        ), encoding='utf8')
        self.assertTrue(any("fullBackupContent=false" in x for x in inspect(self.root)))

    def test_invalid_manifest_fails(self):
        self.manifest.write_text('<manifest>', encoding='utf8')
        self.assertTrue(any('Missing or malformed' in x for x in inspect(self.root)))

if __name__ == '__main__':
    unittest.main()
