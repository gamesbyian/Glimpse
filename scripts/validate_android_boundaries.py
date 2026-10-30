#!/usr/bin/env python3
"""Narrow Glimpse safety boundaries; not a substitute for lint or Android tests."""
from pathlib import Path
import argparse
import re
import sys
import xml.etree.ElementTree as ET

GATEWAY = Path("app/src/main/java/io/github/gamesbyian/glimpse/domain/WallpaperCommands.kt")
MANIFEST = Path("app/src/main/AndroidManifest.xml")
ANDROID = "{http://schemas.android.com/apk/res/android}"
PROVIDERS = {".GlimpseOriginalFileProvider", ".GlimpseEditedFileProvider"}
MUTATIONS = re.compile(r"\b(?:WallpaperManager\.getInstance|setBitmap|setStream|setResource)\s*\(")
DESTRUCTIVE = re.compile(r"\bfallbackToDestructiveMigration\w*\s*\(")

def inspect(root: Path) -> list[str]:
    errors = []
    sources = root / "app/src/main/java"
    if not (root / GATEWAY).is_file() or not sources.is_dir():
        return ["Missing production source or canonical wallpaper gateway"]
    for source in sources.rglob("*.kt"):
        body = source.read_text(encoding="utf-8")
        if DESTRUCTIVE.search(body):
            errors.append(str(source.relative_to(root)) + ": destructive Room fallback")
        if source != root / GATEWAY and MUTATIONS.search(body):
            errors.append(str(source.relative_to(root)) + ": second wallpaper gateway")
    try:
        app = ET.parse(root / MANIFEST).getroot().find("application")
        if app is None:
            return errors + ["Missing Android application element"]
        if app.get(ANDROID + "allowBackup") != "false":
            errors.append("Private originals require android:allowBackup=false")
        if app.get(ANDROID + "fullBackupContent") != "false":
            errors.append("Android 11 and lower require fullBackupContent=false")
        if app.get(ANDROID + "dataExtractionRules") != "@xml/glimpse_data_extraction_rules":
            errors.append("Android 12+ requires explicit local-only data extraction rules")
        else:
            rules_path = root / "app/src/main/res/xml/glimpse_data_extraction_rules.xml"
            try:
                extraction = ET.parse(rules_path).getroot()
                if extraction.tag != "data-extraction-rules":
                    errors.append("Data extraction rules have the wrong XML root")
                for channel in ("cloud-backup", "device-transfer"):
                    section = extraction.find(channel)
                    required = {
                        "root", "file", "database", "sharedpref", "external",
                        "device_root", "device_file", "device_database",
                        "device_sharedpref"
                    }
                    excluded = set() if section is None else {
                        x.get("domain") for x in section.findall("exclude")
                        if x.get("path") in {".", "./"}
                    }
                    if not required.issubset(excluded):
                        errors.append(
                            "Missing full private-storage exclusion in " + channel +
                            ": " + ", ".join(sorted(required - excluded))
                        )
                    if section is not None and section.findall("include"):
                        errors.append("Backup mode " + channel + " must have no includes")
            except (OSError, ET.ParseError) as problem:
                errors.append("Unavailable/invalid data extraction rules: " + str(problem))
        providers = {p.get(ANDROID + "name"): p for p in app.findall("provider")}
        for name in sorted(PROVIDERS):
            p = providers.get(name)
            if p is None:
                errors.append("Missing scoped provider " + name)
            elif p.get(ANDROID + "exported") != "false" or p.get(ANDROID + "grantUriPermissions") != "true":
                errors.append("Unsafe provider export or URI grants: " + name)
    except (OSError, ET.ParseError) as failure:
        errors.append("Missing or malformed production manifest: " + str(failure))
    return errors

def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--root", type=Path, default=Path(__file__).resolve().parents[1])
    errors = inspect(parser.parse_args().root)
    for error in errors:
        print("FAIL:", error, file=sys.stderr)
    if errors:
        return 1
    print("PASS: single wallpaper gateway, safe Room fallback and scoped photo providers")
    return 0

if __name__ == "__main__":
    raise SystemExit(main())
