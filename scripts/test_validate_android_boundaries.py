"""Deterministic positive/negative guards for architectural safety."""
import tempfile
import unittest
from pathlib import Path
from validate_android_boundaries import inspect, GATEWAY, MANIFEST

MANIFEST_TEXT = '''<manifest xmlns:android="http://schemas.android.com/apk/res/android">
<application android:allowBackup="false">
<provider android:name=".GlimpseOriginalFileProvider" android:exported="false" android:grantUriPermissions="true"/>
<provider android:name=".GlimpseEditedFileProvider" android:exported="false" android:grantUriPermissions="true"/>
</application></manifest>'''

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

    def test_compliant_app(self):
        self.assertFalse(inspect(self.root))

    def test_second_setter_fails(self):
        other = self.gateway.parent / 'Rogue.kt'
        other.write_text('fun run() = wallpaper.setBitmap(bitmap)', encoding='utf8')
        self.assertTrue(any('second wallpaper gateway' in x for x in inspect(self.root)))

    def test_destructive_migration_fails(self):
        self.gateway.write_text('fallbackToDestructiveMigration()', encoding='utf8')
        self.assertTrue(any('destructive Room fallback' in x for x in inspect(self.root)))

    def test_exported_photo_provider_fails(self):
        self.manifest.write_text(MANIFEST_TEXT.replace('android:exported="false"',
            'android:exported="true"', 1), encoding='utf8')
        self.assertTrue(any('Unsafe provider' in x for x in inspect(self.root)))

    def test_broad_backup_fails(self):
        self.manifest.write_text(MANIFEST_TEXT.replace('allowBackup="false"',
            'allowBackup="true"'), encoding='utf8')
        self.assertTrue(any('allowBackup=false' in x for x in inspect(self.root)))

    def test_invalid_manifest_fails(self):
        self.manifest.write_text('<manifest>', encoding='utf8')
        self.assertTrue(any('Missing or malformed' in x for x in inspect(self.root)))

if __name__ == '__main__':
    unittest.main()
