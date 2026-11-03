#!/usr/bin/env python3
"""Fail-closed inspection of actual Android visual atlas PNG/XML receipts."""
import argparse
import json
import os
import re
import struct
import sys
import xml.etree.ElementTree as ET
import zlib
from pathlib import Path

PNG_SIG = b'\x89PNG\r\n\x1a\n'


def require(condition, explanation):
    if not condition:
        raise ValueError(explanation)


def png_dimensions(path):
    """Verify PNG framing and per-chunk CRCs without installing image tools."""
    with path.open('rb') as f:
        require(f.read(8) == PNG_SIG, f'{path.name}: invalid PNG signature')
        width = height = None
        have_idat = have_iend = False
        first = True
        while not have_iend:
            header = f.read(4)
            require(len(header) == 4, f'{path.name}: missing IEND')
            size = struct.unpack('>I', header)[0]
            require(size <= 64 * 1024 * 1024, f'{path.name}: excessive PNG chunk')
            kind, data, stored = f.read(4), f.read(size), f.read(4)
            require(len(kind) == 4 and len(data) == size and len(stored) == 4,
                    f'{path.name}: truncated PNG chunk')
            actual = zlib.crc32(data, zlib.crc32(kind)) & 0xffffffff
            require(struct.unpack('>I', stored)[0] == actual,
                    f'{path.name}: PNG CRC mismatch')
            if first:
                require(kind == b'IHDR' and size == 13,
                        f'{path.name}: malformed PNG IHDR')
                width, height = struct.unpack('>II', data[:8])
                require(0 < width <= 10000 and 0 < height <= 10000,
                        f'{path.name}: invalid dimensions')
                first = False
            elif kind == b'IDAT':
                have_idat = True
            elif kind == b'IEND':
                require(size == 0, f'{path.name}: malformed IEND')
                have_iend = True
        require(have_idat and not f.read(1),
                f'{path.name}: no IDAT or extra bytes')
    return width, height


def validate(directory, expected_sha=None, minimum_full=46):
    directory = Path(directory)
    manifest_path = directory / 'manifest.json'
    require(manifest_path.is_file(), 'native Android atlas manifest is missing')
    manifest = json.loads(manifest_path.read_text(encoding='utf-8'))
    mode = manifest.get('mode')
    require(mode in ('smoke', 'full'), 'invalid atlas mode')
    sha = manifest.get('sourceSha')
    require(isinstance(sha, str) and len(sha) >= 8,
            'missing/invalid atlas source SHA')
    if expected_sha:
        require(sha == expected_sha,
                f'atlas source {sha} differs from checked-out {expected_sha}')
    require(manifest.get('errors') == [], 'atlas reports capture failures')
    captures = manifest.get('captures')
    require(isinstance(captures, list), 'atlas capture inventory absent')
    minimum = minimum_full if mode == 'full' else 1
    require(len(captures) >= minimum,
            f'{mode} atlas has {len(captures)} captures, requires >= {minimum}')
    seen = set()
    for cap in captures:
        require(isinstance(cap, dict), 'invalid capture record')
        slug = cap.get('id')
        require(isinstance(slug, str) and
                re.fullmatch(r'[a-z0-9][a-z0-9-]*', slug),
                'unsafe/invalid capture ID')
        require(slug not in seen, f'duplicate capture ID {slug}')
        seen.add(slug)
        png, xml = slug + '.png', slug + '.xml'
        require(cap.get('image') == png, f'{slug}: screenshot reference mismatch')
        require(cap.get('hierarchy') == xml,
                f'{slug}: missing real XML reference')
        image_path, xml_path = directory / png, directory / xml
        require(image_path.is_file() and xml_path.is_file(),
                f'{slug}: missing PNG or XML evidence')
        require(png_dimensions(image_path) ==
                (cap.get('widthPx'), cap.get('heightPx')),
                f'{slug}: image dimensions differ from Android receipt')
        content = xml_path.read_text(encoding='utf-8')
        require(content.lstrip().startswith('<?xml'),
                f'{slug}: XML is not a device hierarchy dump')
        require(ET.fromstring(content).tag == 'hierarchy',
                f'{slug}: malformed UIAutomator root')
        require("is not responding" not in content and
                "isn't responding" not in content,
                f'{slug}: hierarchy shows obscuring Android ANR')
    timing = manifest.get('hierarchyTiming')
    require(isinstance(timing, dict) and
            timing.get('calls', 0) >= len(captures) and
            isinstance(timing.get('totalSeconds'), (int, float)) and
            timing['totalSeconds'] >= 0,
            'missing/invalid Android hierarchy timing')
    settled = manifest.get('inputSettleTiming')
    if settled is not None:
        require(isinstance(settled, dict) and all(
            isinstance(settled.get(key), (int, float)) and settled[key] >= 0
            for key in ('taps', 'swipes', 'requestedSeconds')),
            'invalid Android input-settle timing')
    return {'sourceSha': sha, 'mode': mode, 'captures': len(captures),
            'hierarchyCalls': timing['calls'],
            'hierarchySeconds': round(timing['totalSeconds'], 3),
            'inputSettling': settled}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--artifacts', type=Path, required=True)
    parser.add_argument('--expected-sha', default=os.getenv('GITHUB_SHA'))
    args = parser.parse_args()
    try:
        result = validate(args.artifacts, expected_sha=args.expected_sha)
    except (ValueError, ET.ParseError, OSError, json.JSONDecodeError) as error:
        print(f'::error::Native atlas evidence invalid: {error}', file=sys.stderr)
        return 1
    print('PASS: verified actual Android atlas ' + json.dumps(result, sort_keys=True))
    summary = os.getenv('GITHUB_STEP_SUMMARY')
    if summary:
        with open(summary, 'a', encoding='utf-8') as report:
            report.write('### Real Android atlas evidence\n'
                         f'- Source: {result["sourceSha"]} ({result["mode"]})\n'
                         f'- Screenshots and accessibility XML: '
                         f'{result["captures"]}/{result["captures"]}\n'
                         f'- UIAutomator: {result["hierarchyCalls"]} dumps, '
                         f'{result["hierarchySeconds"]:.1f} seconds\n')
            if result['inputSettling']:
                settle = result['inputSettling']
                report.write(f'- Input settling: {settle["taps"]} taps, '
                             f'{settle["swipes"]} swipes, '
                             f'{settle["requestedSeconds"]:.1f} seconds\n')
    return 0


if __name__ == '__main__':
    sys.exit(main())
