"""Offline adversarial contracts for main atlas evidence-reuse eligibility."""
import copy
import io
import os
import tempfile
import hashlib
import urllib.error
import json
import struct
import zlib
import unittest
import zipfile
from pathlib import Path
from unittest import mock

from reuse_verified_atlas import (
    REPO, WORKFLOW, WORKFLOW_NAME, ARTIFACT,
    pr_eligible, run_eligible, artifact_eligible, merge_tree_eligible,
    safe_extract, download_artifact, try_reuse
)

MAIN = "a" * 40
PARENT = "b" * 40
HEAD = "c" * 40
SYNTHETIC = "d" * 40
TREE = "e" * 40
REPO_ID = 1430476913


def eligible_fixture():
    pr = {
        "number": 210, "state": "closed",
        "merged_at": "2026-11-03T06:18:13Z", "merge_commit_sha": MAIN,
        "base": {"sha": PARENT, "ref": "main",
                 "repo": {"full_name": REPO}},
        "head": {"sha": HEAD, "ref": "perf/fixture",
                 "repo": {"full_name": REPO}}
    }
    run = {
        "id": 41586891970, "name": WORKFLOW_NAME, "path": WORKFLOW,
        "event": "pull_request", "status": "completed", "conclusion": "success",
        "head_sha": HEAD, "head_branch": "perf/fixture",
        "repository": {"full_name": REPO, "id": REPO_ID},
        "head_repository": {"full_name": REPO, "id": REPO_ID},
        "created_at": "2026-11-03T05:57:21Z",
        "updated_at": "2026-11-03T06:15:27Z"
    }
    artifact = {
        "id": 13208199774, "name": ARTIFACT, "expired": False,
        "size_in_bytes": 72_367_734, "digest": "sha256:" + "1" * 64,
        "workflow_run": {
            "id": run["id"], "repository_id": REPO_ID,
            "head_repository_id": REPO_ID, "head_sha": HEAD
        }
    }
    synthetic = {
        "commit": {"tree": {"sha": TREE}},
        "parents": [{"sha": PARENT}, {"sha": HEAD}]
    }
    return pr, run, artifact, synthetic


class ProvenanceContracts(unittest.TestCase):
    def test_positive_pr_run_artifact_and_merge_tree(self):
        pr, run, art, synthetic = eligible_fixture()
        self.assertIs(pr_eligible([pr], MAIN, PARENT), pr)
        self.assertIs(run_eligible(run, pr, REPO_ID), run)
        self.assertIs(artifact_eligible([art], run, REPO_ID), art)
        merge_tree_eligible(synthetic, pr, PARENT, TREE)

    def test_direct_push_and_ambiguous_associated_pr_not_reusable(self):
        pr, _, _, _ = eligible_fixture()
        with self.assertRaisesRegex(ValueError, "not exactly one"):
            pr_eligible([], MAIN, PARENT)
        with self.assertRaisesRegex(ValueError, "not exactly one"):
            pr_eligible([pr, pr], MAIN, PARENT)

    def test_unmerged_or_fork_or_wrong_base_not_reusable(self):
        pr, _, _, _ = eligible_fixture()
        for change in ("unmerged", "fork", "wrong_base", "wrong_sha"):
            bad = copy.deepcopy(pr)
            if change == "unmerged": bad["merged_at"] = None
            if change == "fork": bad["head"]["repo"]["full_name"] = "other/fork"
            if change == "wrong_base": bad["base"]["sha"] = "f" * 40
            if change == "wrong_sha": bad["merge_commit_sha"] = "f" * 40
            with self.subTest(change=change), self.assertRaises(ValueError):
                pr_eligible([bad], MAIN, PARENT)

    def test_not_real_successful_head_source_workflow(self):
        pr, run, _, _ = eligible_fixture()
        for change in ("head", "event", "conclusion", "path",
                       "repo", "run_started_after_merge", "run_finished_after_merge"):
            bad = copy.deepcopy(run)
            if change == "head": bad["head_sha"] = "f" * 40
            if change == "event": bad["event"] = "workflow_dispatch"
            if change == "conclusion": bad["conclusion"] = "cancelled"
            if change == "path": bad["path"] = "other-workflow.yml"
            if change == "repo": bad["head_repository"]["id"] = 2
            if change == "run_started_after_merge":
                bad["created_at"] = "2026-11-03T07:00:00Z"
            if change == "run_finished_after_merge":
                bad["updated_at"] = "2026-11-03T07:00:00Z"
            with self.subTest(change=change), self.assertRaises(ValueError):
                run_eligible(bad, pr, REPO_ID)

    def test_wrong_or_expired_artifact_refused(self):
        _, run, art, _ = eligible_fixture()
        for change in ("expired", "digest", "shrunken", "head", "run"):
            bad = copy.deepcopy(art)
            if change == "expired": bad["expired"] = True
            if change == "digest": bad["digest"] = "broken"
            if change == "shrunken": bad["size_in_bytes"] = 100
            if change == "head": bad["workflow_run"]["head_sha"] = "f" * 40
            if change == "run": bad["workflow_run"]["id"] = 0
            with self.subTest(change=change), self.assertRaises(ValueError):
                artifact_eligible([bad], run, REPO_ID)

    def test_same_git_tree_requires_exact_original_synthetic_parents(self):
        pr, _, _, synthetic = eligible_fixture()
        merge_tree_eligible(synthetic, pr, PARENT, TREE)
        for key in ("tree", "base", "head"):
            bad = copy.deepcopy(synthetic)
            if key == "tree": bad["commit"]["tree"]["sha"] = "f" * 40
            if key == "base": bad["parents"][0]["sha"] = "f" * 40
            if key == "head": bad["parents"][1]["sha"] = "f" * 40
            with self.subTest(change=key), self.assertRaises(ValueError):
                merge_tree_eligible(bad, pr, PARENT, TREE)

    def test_safe_extract_refuses_zip_path_traversal(self):
        with tempfile.TemporaryDirectory() as tmp:
            p, dest = Path(tmp) / "archive.zip", Path(tmp) / "out"
            dest.mkdir()
            with zipfile.ZipFile(p, "w") as z:
                z.writestr("manifest.json", "{}")
                for i in range(89): z.writestr(f"safe-{i}.xml", "x")
                z.writestr("../escape.txt", "poison")
            with self.assertRaisesRegex(ValueError, "unsafe"):
                safe_extract(p, dest)
            self.assertFalse((Path(tmp) / "escape.txt").exists())

    def test_artifact_download_token_not_forwarded_to_signed_host(self):
        archive = b"real-zip-bytes"
        signed = "https://test.blob.core.windows.net/atlas?signature=opaque"
        redirected = urllib.error.HTTPError(
            "https://api.github.com/artifact", 302, "Found",
            {"Location": signed}, None)

        def open_signed(request, **kwargs):
            self.assertEqual(request.full_url, signed)
            self.assertNotIn("Authorization", request.headers)
            return io.BytesIO(archive)

        with tempfile.TemporaryDirectory() as directory, \
             mock.patch("reuse_verified_atlas.urllib.request.build_opener") as opener, \
             mock.patch("reuse_verified_atlas.urllib.request.urlopen",
                        side_effect=open_signed) as download:
            opener.return_value.open.side_effect = redirected
            expected = {"id": 77, "size_in_bytes": len(archive),
                        "digest": "sha256:" + hashlib.sha256(archive).hexdigest()}
            dest = Path(directory) / "source.zip"
            sha = download_artifact(expected, "read-only-token", dest)
            self.assertEqual(sha, hashlib.sha256(archive).hexdigest())
            self.assertEqual(dest.read_bytes(), archive)
            opener.return_value.open.assert_called_once()
            self.assertIn("Authorization",
                          opener.return_value.open.call_args.args[0].headers)
            download.assert_called_once()

    def test_untrusted_artifact_redirect_is_rejected_before_download(self):
        redirected = urllib.error.HTTPError(
            "https://api.github.com/artifact", 302, "Found",
            {"Location": "https://malicious.example/collect"}, None)
        with tempfile.TemporaryDirectory() as directory, \
             mock.patch("reuse_verified_atlas.urllib.request.build_opener") as opener, \
             mock.patch("reuse_verified_atlas.urllib.request.urlopen") as download:
            opener.return_value.open.side_effect = redirected
            a = {"id": 77, "size_in_bytes": 10, "digest": "sha256:" + "1" * 64}
            with self.assertRaisesRegex(ValueError, "not an approved"):
                download_artifact(a, "read-only-token",
                                  Path(directory) / "source.zip")
            download.assert_not_called()

    def test_full_prior_native_atlas_replay_and_wrong_tree_fallback(self):
        """Prove all 46 actually parsed PNG/XML before main may reuse."""
        pr, run, art, synthetic = eligible_fixture()
        with tempfile.TemporaryDirectory() as tmp:
            dest = Path(tmp) / "verified-native-atlas"
            def png_chunk(kind, data):
                return (struct.pack(">I", len(data)) + kind + data +
                        struct.pack(">I", zlib.crc32(data, zlib.crc32(kind)) & 0xffffffff))
            png = (b"\x89PNG\r\n\x1a\n" +
                   png_chunk(b"IHDR", struct.pack(">IIBBBBB", 1, 1, 8, 6, 0, 0, 0)) +
                   png_chunk(b"IDAT", zlib.compress(b"\x00\xff\x00\x00\xff")) +
                   png_chunk(b"IEND", b""))
            def fake_download(_artifact, _token, path):
                manifest = {
                    "sourceSha": SYNTHETIC, "mode": "full", "errors": [],
                    "hierarchyTiming": {"calls": 46, "totalSeconds": 120.0},
                    "captures": []}
                with zipfile.ZipFile(path, "w") as archive:
                    for n in range(46):
                        slug = f"native-frame-{n:02d}"
                        archive.writestr(slug + ".png", png)
                        archive.writestr(slug + ".xml",
                                         '<?xml version="1.0"?><hierarchy/>')
                        manifest["captures"].append({
                            "id": slug, "image": slug + ".png",
                            "hierarchy": slug + ".xml",
                            "widthPx": 1, "heightPx": 1})
                    archive.writestr("manifest.json", json.dumps(manifest))
                    archive.writestr("index.html", "<html>actual evidence</html>")
                return "4" * 64
            responses = {
                f"/repos/{REPO}/commits/{MAIN}": {
                    "sha": MAIN, "parents": [{"sha": PARENT}],
                    "commit": {"tree": {"sha": TREE}}},
                f"/repos/{REPO}/commits/{MAIN}/pulls": [pr],
                f"/repos/{REPO}": {"id": REPO_ID},
                f"/repos/{REPO}/actions/runs?head_sha={HEAD}&per_page=100": {
                    "workflow_runs": [run]},
                f"/repos/{REPO}/actions/runs/{run['id']}/jobs?per_page=100": {
                    "jobs": [{"name": "capture", "conclusion": "success"}]},
                f"/repos/{REPO}/actions/runs/{run['id']}/artifacts?per_page=100": {
                    "artifacts": [art]},
                f"/repos/{REPO}/commits/{SYNTHETIC}": synthetic
            }
            def shell(command, **kwargs):
                return (MAIN + "\n") if command[-1] == "HEAD" else (TREE + "\n")
            environment = {
                "GITHUB_EVENT_NAME": "push",
                "GITHUB_REF": "refs/heads/main",
                "GITHUB_REPOSITORY": REPO,
                "GITHUB_REPOSITORY_ID": str(REPO_ID)}
            with mock.patch.dict(os.environ, environment), \
                 mock.patch("reuse_verified_atlas.api",
                            side_effect=lambda path, token: responses[path]), \
                 mock.patch("reuse_verified_atlas.subprocess.check_output",
                            side_effect=shell), \
                 mock.patch("reuse_verified_atlas.download_artifact",
                            side_effect=fake_download):
                source, run_id, receipt = try_reuse(dest, MAIN, "token")
                self.assertEqual(source, SYNTHETIC)
                self.assertEqual(run_id, run["id"])
                self.assertEqual(receipt["verified_png_and_xml_pairs"], 46)
                self.assertEqual(receipt["main_tree_sha"], TREE)
                self.assertTrue((dest / "reuse-provenance.json").exists())
                self.assertEqual(len(list(dest.glob("*.png"))), 46)
                self.assertEqual(len(list(dest.glob("*.xml"))), 46)
                # A different tree is a hard rejection, never "close enough".
                responses[f"/repos/{REPO}/commits/{SYNTHETIC}"] = {
                    **synthetic, "commit": {"tree": {"sha": "f"*40}}}
                other = Path(tmp) / "must-run-emulator"
                with self.assertRaisesRegex(ValueError, "No exact-tree artifact"):
                    try_reuse(other, MAIN, "token")
                self.assertFalse(other.exists())

    def test_nonauthorized_events_stop_before_any_network(self):
        with mock.patch.dict(os.environ, {
                "GITHUB_EVENT_NAME": "pull_request",
                "GITHUB_REF": "refs/pull/55/merge",
                "GITHUB_REPOSITORY": REPO}, clear=False), \
             mock.patch("reuse_verified_atlas.api") as http:
            with self.assertRaisesRegex(ValueError, "not a trusted"):
                try_reuse(Path("/tmp/nonexistent-atlas"), MAIN, "fake-token")
            http.assert_not_called()


if __name__ == "__main__":
    unittest.main()
