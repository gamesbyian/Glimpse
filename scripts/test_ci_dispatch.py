"""Offline contract tests for the *real* GitHub Actions dispatch selectors.

PyYAML BaseLoader preserves the key 'on' as a string. A small GitHub-style
path-glob matcher covers the pattern subset in this repository; unsupported
syntax fails closed instead of silently allowing a false-green selector.
"""
from pathlib import Path
import re
import unittest
import yaml

ROOT = Path(__file__).resolve().parents[1]
WORKFLOWS = ROOT / ".github" / "workflows"


def glob_match(pattern, path):
    if any(c in pattern for c in "?![]{}+@"):
        raise AssertionError(f"Unsupported GitHub Actions glob in CI contract: {pattern}")
    out = ""
    i = 0
    while i < len(pattern):
        if pattern[i:i+3] == "**/":
            out += "(?:.*/)?"
            i += 3
        elif pattern[i:i+2] == "**":
            out += ".*"
            i += 2
        elif pattern[i] == "*":
            out += "[^/]*"
            i += 1
        else:
            out += re.escape(pattern[i])
            i += 1
    return re.fullmatch(out, path) is not None


def workflow(name):
    return yaml.load((WORKFLOWS / name).read_text(), Loader=yaml.BaseLoader)


def fires(name, event, paths):
    events = workflow(name).get("on", {})
    if isinstance(events, str):
        return events == event
    if event not in events:
        return False
    event_config = events[event]
    if not isinstance(event_config, dict) or "paths" not in event_config:
        return True
    patterns = event_config["paths"]
    return any(glob_match(pat, path) for pat in patterns for path in paths)


class DispatchSafetyTest(unittest.TestCase):
    def expect(self, file, event, path, expected=True):
        self.assertEqual(fires(file, event, [path]), expected, (file, event, path))

    def test_all_yaml_is_parseable(self):
        for path in WORKFLOWS.glob("*.yml"):
            with self.subTest(path=path.name):
                self.assertIsInstance(workflow(path.name), dict)

    def test_docs_only_does_not_spawn_android(self):
        for path in WORKFLOWS.glob("*.yml"):
            if path.name == "ci-contract.yml":
                continue
            with self.subTest(path=path.name):
                self.expect(path.name, "pull_request", "docs/CI-ENGINEERING-STANDARD.md", False)
        self.expect("ci-contract.yml", "pull_request", "docs/CI-ENGINEERING-STANDARD.md")

    def test_native_and_merge_queue(self):
        self.expect("android-check.yml", "pull_request", "app/src/main/java/io/github/gamesbyian/glimpse/EditedPhotoOutput.kt")
        self.expect("android-check.yml", "pull_request", "gradle/wrapper/gradle-wrapper.properties")
        self.expect("android-check.yml", "merge_group", "docs/changes.md")
        self.expect("android-check.yml", "pull_request", "design/proposals/example.svg", False)

    def test_immutable_originals_all_writers_and_recovery(self):
        base = "app/src/main/java/io/github/gamesbyian/glimpse/"
        for suffix in ["EditedPhotoOutput.kt", "data/CollectionZipRestorer.kt",
                       "data/PrivateOriginalVerifier.kt", "data/ImportJournal.kt",
                       "data/PrivatePublicationReceiptJournal.kt"]:
            for check in ("android-room-integrity.yml", "android-private-original-api29.yml",
                          "android-in-place-upgrade.yml"):
                with self.subTest(path=suffix, check=check):
                    self.expect(check, "pull_request", base + suffix)

    def test_shared_build_changes_fall_back_to_high_risk_checks(self):
        for source in ("app/build.gradle.kts", "build.gradle.kts", "settings.gradle.kts",
                       "gradle.properties", "gradle/wrapper/gradle-wrapper.properties"):
            for check in ("android-check.yml", "android-room-integrity.yml",
                          "android-private-original-api29.yml", "android-in-place-upgrade.yml"):
                with self.subTest(source=source, check=check):
                    self.expect(check, "pull_request", source)

    def test_visual_and_real_picker_boundaries(self):
        self.expect("visual-reference-check.yml", "pull_request", "design/proposals/mock.svg")
        self.expect("android-visual-atlas.yml", "pull_request", "app/src/main/res/drawable/hero.xml")
        self.expect("android-visual-atlas.yml", "pull_request", "app/src/main/AndroidManifest.xml")
        self.expect("agent-c-real-picker-batch.yml", "pull_request",
                    "app/src/main/java/io/github/gamesbyian/glimpse/data/PhotoImporter.kt")

    def test_each_workflow_self_change_dispatches(self):
        for path in WORKFLOWS.glob("*.yml"):
            if path.name in {"glimpse-pages-publisher.yml", "ci-contract.yml"}:
                continue
            with self.subTest(path=path.name):
                self.expect(path.name, "pull_request", ".github/workflows/" + path.name)

    def test_rename_union_conservatively_selects(self):
        target = "app/src/main/java/io/github/gamesbyian/glimpse/data/PhotoImporter.kt"
        self.assertFalse(fires("android-room-integrity.yml", "pull_request", ["docs/old.md"]))
        self.assertTrue(fires("android-room-integrity.yml", "pull_request", ["docs/old.md", target]))

    def test_one_pr_cannot_cancel_unrelated_ref(self):
        for path in WORKFLOWS.glob("*.yml"):
            if path.name == "glimpse-pages-publisher.yml":
                continue  # Singleton published site deliberately serializes.
            data = workflow(path.name)
            if "pull_request" not in data.get("on", {}):
                continue
            group = data.get("concurrency", {}).get("group", "")
            with self.subTest(path=path.name):
                self.assertTrue("github.ref" in group or "github.event.pull_request.number" in group, group)

    def test_prs_cannot_invoke_privileged_target_event(self):
        for path in WORKFLOWS.glob("*.yml"):
            events = workflow(path.name).get("on", {})
            if isinstance(events, dict):
                self.assertNotIn("pull_request_target", events, path.name)


if __name__ == "__main__":
    unittest.main()
