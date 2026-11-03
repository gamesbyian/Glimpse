"""Zero-emulator contracts for truthful Glimpse visual-atlas publication."""
import json
import struct
import tempfile
import unittest
import zlib
from pathlib import Path
from validate_android_atlas_evidence import validate


def chunk(kind, data):
    return (struct.pack('>I', len(data)) + kind + data +
            struct.pack('>I', zlib.crc32(data, zlib.crc32(kind)) & 0xffffffff))


def png():
    return (b'\x89PNG\r\n\x1a\n' +
            chunk(b'IHDR', struct.pack('>IIBBBBB', 1, 1, 8, 6, 0, 0, 0)) +
            chunk(b'IDAT', zlib.compress(b'\x00\xff\x00\x00\xff')) +
            chunk(b'IEND', b''))


class AtlasEvidenceContracts(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.manifest = {
            'mode': 'full', 'sourceSha': 'a' * 40, 'errors': [],
            'hierarchyTiming': {'calls': 46, 'totalSeconds': 67.2},
            'inputSettleTiming': {'taps': 3, 'swipes': 5, 'requestedSeconds': 2.85},
            'captures': []
        }
        self.image = png()
        for index in range(46):
            slug = f'frame-{index}'
            (self.root / (slug + '.png')).write_bytes(self.image)
            (self.root / (slug + '.xml')).write_text(
                '<?xml version="1.0"?><hierarchy />')
            self.manifest['captures'].append({
                'id': slug, 'image': slug + '.png', 'hierarchy': slug + '.xml',
                'widthPx': 1, 'heightPx': 1
            })
        self.save()

    def save(self):
        (self.root / 'manifest.json').write_text(json.dumps(self.manifest))

    def test_full_native_receipts(self):
        self.assertEqual(validate(self.root, expected_sha='a' * 40)['captures'], 46)

    def test_smoke_has_distinct_minimum(self):
        self.manifest['mode'] = 'smoke'
        self.manifest['captures'] = self.manifest['captures'][:1]
        self.save()
        self.assertEqual(validate(self.root)['captures'], 1)

    def test_missing_accessibility_evidence_is_red(self):
        (self.root / 'frame-9.xml').unlink()
        with self.assertRaisesRegex(ValueError, 'missing PNG or XML'):
            validate(self.root)

    def test_missing_full_frames_are_red(self):
        self.manifest['captures'].pop()
        self.save()
        with self.assertRaisesRegex(ValueError, 'requires >= 46'):
            validate(self.root)

    def test_corrupt_screenshot_crc_is_red(self):
        broken = bytearray(self.image)
        broken[-8] ^= 1
        (self.root / 'frame-1.png').write_bytes(broken)
        with self.assertRaisesRegex(ValueError, 'CRC mismatch'):
            validate(self.root)

    def test_stale_source_is_red(self):
        with self.assertRaisesRegex(ValueError, 'differs from checked-out'):
            validate(self.root, expected_sha='b' * 40)

    def test_manifest_errors_are_red(self):
        self.manifest['errors'] = ['atlas failed']
        self.save()
        with self.assertRaisesRegex(ValueError, 'capture failures'):
            validate(self.root)

    def test_duplicate_capture_ids_are_red(self):
        self.manifest['captures'][1] = self.manifest['captures'][0]
        self.save()
        with self.assertRaisesRegex(ValueError, 'duplicate capture ID'):
            validate(self.root)

    def test_anr_hierarchy_is_red(self):
        (self.root / 'frame-7.xml').write_text(
            '<?xml version="1.0"?><hierarchy>'
            '<node text="App is not responding"/></hierarchy>')
        with self.assertRaisesRegex(ValueError, 'obscuring Android ANR'):
            validate(self.root)


if __name__ == '__main__':
    unittest.main()
