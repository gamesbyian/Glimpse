#!/usr/bin/env python3
"""Real APK-over-APK, same-signer Android update with exact private-data checks.

Only disposable emulator app data seeded by the debug VisualFixtureLauncher.
Never run this test against a real phone. No private files, database rows, or
image hashes are uploaded; only public certificate digests and aggregate counts.
"""
import argparse
import hashlib
import json
import re
import sqlite3
import subprocess
import tempfile
import time
from pathlib import Path

APP = "io.github.gamesbyian.glimpse"
TABLES = ("collections", "photos", "memberships", "rotation", "photo_favorites")


def adb(*args, check=True):
    return subprocess.run(["adb", *args], check=check, capture_output=True)


def text(*args, check=True):
    return adb(*args, check=check).stdout.decode("utf-8", errors="replace").strip()


def assert_emulator():
    assert text("shell", "getprop", "ro.kernel.qemu") == "1", (
        "Refuse APK upgrade fixture on a physical device"
    )
    assert any(part in text("shell", "getprop", "ro.hardware").lower()
               for part in ("ranchu", "goldfish")), (
        "Refuse upgrade on unknown hardware"
    )


def run_as_bytes(*args, optional=False):
    result = adb("exec-out", "run-as", APP, *args, check=False)
    if result.returncode != 0:
        if optional:
            return None
        raise RuntimeError("Debug application private-file read unavailable")
    return result.stdout


def package_snapshot():
    # The app must be stopped when reading its SQLite file and WAL trio.
    # Copies are made inside an ephemeral *host* directory, never uploaded.
    with tempfile.TemporaryDirectory(prefix="glimpse-upgrade-db-") as tmp:
        for suffix in ("", "-wal", "-shm"):
            raw = run_as_bytes("cat", "databases/glimpse.db" + suffix,
                               optional=bool(suffix))
            if raw is not None:
                (Path(tmp) / ("glimpse.db" + suffix)).write_bytes(raw)
        with sqlite3.connect(str(Path(tmp) / "glimpse.db")) as db:
            all_tables = {row[0] for row in db.execute(
                "SELECT name FROM sqlite_master WHERE type='table'"
            )}
            assert set(TABLES).issubset(all_tables), (
                "Installed Room schema tables not preserved"
            )
            rows = {}
            for table in TABLES:
                rows[table] = tuple(sorted(db.execute(
                    "SELECT * FROM " + table
                ).fetchall()))
            # Keep paths and hashes in memory only; never in CI artifacts.
            photos = list(db.execute(
                "SELECT sha256, localPath, byteLength FROM photos"
            ))
            assert len(photos) >= 3, "Real fixture photo registrations absent"
            for digest, original_path, length in photos:
                assert re.fullmatch(r"[0-9a-f]{64}", digest)
                basename = Path(original_path).name
                assert basename.startswith(digest + ".")
                assert basename.endswith((".jpg", ".png", ".webp"))
                data = run_as_bytes("cat", "files/originals/" + basename)
                assert len(data) == length and hashlib.sha256(data).hexdigest() == digest, (
                    "Installed private original fails byte-for-byte SHA integrity"
                )
            return rows, len(photos)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--old", type=Path, required=True)
    parser.add_argument("--new", type=Path, required=True)
    parser.add_argument("--report", type=Path, required=True)
    args = parser.parse_args()
    assert_emulator()
    assert args.old.is_file() and args.new.is_file()
    adb("wait-for-device")
    assert text("shell", "getprop", "sys.boot_completed") == "1"

    adb("install", "-r", str(args.old.resolve()))
    # Only ever clear the synthetic first-install app on a validated emulator.
    adb("shell", "pm", "clear", APP)
    run_as_bytes("rm", "-f", "files/visual-atlas-ready.txt", optional=True)
    adb("shell", "am", "start", "-n", APP + "/.debug.VisualFixtureLauncher",
        "--ei", "count", "3", "--ez", "active", "true")
    for _ in range(65):
        state = run_as_bytes("cat", "files/visual-atlas-ready.txt", optional=True)
        if state and state.startswith(b"READY:3:"):
            break
        time.sleep(1)
    else:
        raise RuntimeError("Old APK could not seed genuine Room and original-byte fixtures")
    time.sleep(3)
    adb("shell", "am", "force-stop", APP)
    old_rows, original_count = package_snapshot()

    # Do not clear/uninstall. This *must* be an Android signature-compatible
    # install over the already populated app-private data directory.
    installed = adb("install", "-r", str(args.new.resolve()), check=False)
    if installed.returncode:
        raise RuntimeError("Android rejected install-over-update: " +
                           installed.stderr.decode("utf-8", errors="replace")[:260])
    adb("shell", "am", "start", "-n", APP + "/.MainActivity")
    time.sleep(5)
    adb("shell", "am", "force-stop", APP)
    new_rows, updated_count = package_snapshot()
    assert old_rows == new_rows, (
        "App update changed fixture Room records despite no user edits"
    )
    assert updated_count == original_count
    args.report.parent.mkdir(parents=True, exist_ok=True)
    args.report.write_text(json.dumps({
        "result": "pass",
        "test": "same temporary signer, actual adb install -r, no uninstall",
        "room_tables_compared": len(TABLES),
        "verified_private_originals": updated_count,
        "provider_source": "synthetic emulator-only fixture",
        "production_signing_continuity": "not assessed"
    }, indent=2) + "\n", encoding="utf-8")
    print("PASS: same-signer Android APK install -r preserved all " +
          f"{len(TABLES)} Room tables and {updated_count} exact-byte private originals",
          flush=True)


if __name__ == "__main__":
    main()
