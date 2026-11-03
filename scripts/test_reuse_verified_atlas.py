"""Offline adversarial contracts for main atlas evidence-reuse eligibility."""
import copy
import io
import os
import tempfile
import unittest
import zipfile
from pathlib import Path
from unittest import mock

from reuse_verified_atlas import (
    REPO, WORKFLOW, WORKFLOW_NAME, ARTIFACT,
    pr_eligible, run_eligible, artifact_eligible, merge_tree_eligible,
    safe_extract, try_reuse
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
                       "repo", "run_started_after_merge"):
            bad = copy.deepcopy(run)
            if change == "head": bad["head_sha"] = "f" * 40
            if change == "event": bad["event"] = "workflow_dispatch"
            if change == "conclusion": bad["conclusion"] = "cancelled"
            if change == "path": bad["path"] = "other-workflow.yml"
            if change == "repo": bad["head_repository"]["id"] = 2
            if change == "run_started_after_merge":
                bad["created_at"] = "2026-11-03T07:00:00Z"
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
