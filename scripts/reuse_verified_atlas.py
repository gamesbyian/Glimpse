#!/usr/bin/env python3
"""Reuse a *verified identical-tree* real Android atlas on a trusted main push.

This is an optimization of source-exact evidence transport, NOT a test skip.
Any unknown, stale, missing, tampered or untrusted fact must run the original
Android emulator acceptance instead. Never execute code from a PR artifact.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import os
import re
import shutil
import stat
import subprocess
import sys
import tempfile
import urllib.error
import urllib.parse
import urllib.request
import zipfile
from pathlib import Path, PurePosixPath

from validate_android_atlas_evidence import validate

REPO = "gamesbyian/Glimpse"
WORKFLOW = ".github/workflows/android-visual-atlas.yml"
WORKFLOW_NAME = "Glimpse native visual atlas"
ARTIFACT = "glimpse-android-visual-atlas"
MAX_ARCHIVE_BYTES = 150_000_000
MAX_ENTRIES = 160


def require(condition, reason):
    if not condition:
        raise ValueError(reason)


def pr_eligible(prs, main_sha, main_parent, repo=REPO):
    """Only one safely associated, already merged, same-repository PR."""
    require(isinstance(prs, list) and len(prs) == 1,
            "not exactly one associated merged PR")
    pr = prs[0]
    require(pr.get("state") == "closed" and pr.get("merged_at"),
            "associated PR is not merged")
    require(pr.get("merge_commit_sha") == main_sha,
            "PR merge SHA differs from current main")
    require(pr.get("head", {}).get("repo", {}).get("full_name") == repo,
            "fork or unavailable PR head repository")
    require(pr.get("base", {}).get("repo", {}).get("full_name") == repo and
            pr.get("base", {}).get("ref") == "main",
            "PR target is not this repository main")
    require(pr["base"]["sha"] == main_parent,
            "PR base does not equal current squash parent")
    head = pr.get("head", {}).get("sha")
    require(isinstance(head, str) and bool(re.fullmatch(r"[a-f0-9]{40}", head)),
            "missing immutable PR source SHA")
    require(isinstance(pr.get("number"), int), "missing associated PR number")
    return pr


def run_eligible(run, pr, repo_id, repo=REPO):
    """A real, successful source-head PR run of the exact native workflow."""
    require(run.get("event") == "pull_request" and
            run.get("status") == "completed" and
            run.get("conclusion") == "success",
            "not a completed successful PR workflow")
    require(run.get("name") == WORKFLOW_NAME and run.get("path") == WORKFLOW,
            "wrong or renamed source workflow")
    require(run.get("head_sha") == pr["head"]["sha"] and
            run.get("head_branch") == pr["head"]["ref"],
            "PR run did not test the merged PR head/branch")
    require(run.get("repository", {}).get("full_name") == repo and
            run.get("head_repository", {}).get("full_name") == repo,
            "PR workflow was not from same repository")
    require(run["repository"]["id"] == repo_id and
            run["head_repository"]["id"] == repo_id,
            "workflow repo identity mismatch")
    require(run.get("created_at") and run.get("updated_at") and
            run["created_at"] <= pr["merged_at"] and
            run["updated_at"] <= pr["merged_at"],
            "PR workflow was not completed before the merge")
    require(isinstance(run.get("id"), int), "missing workflow run ID")
    return run


def artifact_eligible(items, run, repo_id):
    require(isinstance(items, list), "missing artifact collection")
    candidates = [x for x in items if x.get("name") == ARTIFACT and
                  not x.get("expired")]
    require(len(candidates) == 1, "missing or ambiguous complete atlas artifact")
    a = candidates[0]
    require(a.get("workflow_run", {}).get("id") == run["id"] and
            a["workflow_run"].get("repository_id") == repo_id and
            a["workflow_run"].get("head_repository_id") == repo_id and
            a["workflow_run"].get("head_sha") == run["head_sha"],
            "artifact workflow identity mismatch")
    require(isinstance(a.get("id"), int), "missing artifact identifier")
    require(isinstance(a.get("size_in_bytes"), int) and
            10_000_000 < a["size_in_bytes"] <= MAX_ARCHIVE_BYTES,
            "implausible native atlas archive size")
    require(re.fullmatch(r"sha256:[0-9a-f]{64}", a.get("digest", "")),
            "missing GitHub-signed artifact SHA256 digest")
    return a


def api(path, token):
    require(path == "/repos/" + REPO or
            path.startswith("/repos/" + REPO + "/"),
            "API path outside the protected repository")
    url = "https://api.github.com" + path
    req = urllib.request.Request(url, headers={
        "Authorization": "Bearer " + token,
        "Accept": "application/vnd.github+json",
        "X-GitHub-Api-Version": "2022-11-28",
        "User-Agent": "glimpse-verified-native-atlas-reuse/1"})
    with urllib.request.urlopen(req, timeout=25) as response:
        content = response.read(6_000_000)
    return json.loads(content)


class NoRedirect(urllib.request.HTTPRedirectHandler):
    def redirect_request(self, req, fp, code, msg, headers, newurl):
        return None


def download_artifact(artifact, token, destination):
    """Authenticated API first, anonymous signed object URL second.

    Never forward the GITHUB_TOKEN to the storage redirect host.
    """
    api_url = ("https://api.github.com/repos/" + REPO +
               "/actions/artifacts/" + str(artifact["id"]) + "/zip")
    req = urllib.request.Request(api_url, headers={
        "Authorization": "Bearer " + token,
        "Accept": "application/vnd.github+json",
        "X-GitHub-Api-Version": "2022-11-28",
        "User-Agent": "glimpse-verified-native-atlas-reuse/1"})
    opener = urllib.request.build_opener(NoRedirect)
    try:
        with opener.open(req, timeout=25) as response:
            # GitHub currently issues a redirect. Treat an unexpected 200 as
            # an error rather than accidentally reusing an unverified body.
            raise ValueError("artifact API did not provide a signed redirect")
    except urllib.error.HTTPError as error:
        require(error.code in (302, 303, 307, 308),
                "GitHub artifact API did not issue a download redirect")
        signed_url = error.headers.get("Location")
    require(signed_url, "GitHub artifact download location is missing")
    url = urllib.parse.urlsplit(signed_url)
    host = url.hostname or ""
    require(url.scheme == "https" and not url.username and not url.password and
            (host.endswith(".blob.core.windows.net") or
             host.endswith(".githubusercontent.com") or
             host.endswith(".actions.githubusercontent.com")),
            "signed download host is not an approved GitHub artifact endpoint")
    digest = hashlib.sha256()
    size = 0
    # Crucially, send NO authorization header on the signed URL request.
    with urllib.request.urlopen(
            urllib.request.Request(signed_url, headers={
                "User-Agent": "glimpse-verified-native-atlas-reuse/1"}),
            timeout=90) as response, destination.open("wb") as out:
        while True:
            blob = response.read(1024 * 1024)
            if not blob:
                break
            size += len(blob)
            require(size <= MAX_ARCHIVE_BYTES, "artifact ZIP exceeded maximum size")
            out.write(blob)
            digest.update(blob)
    require(size == artifact["size_in_bytes"], "artifact ZIP byte count mismatch")
    require("sha256:" + digest.hexdigest() == artifact["digest"],
            "artifact ZIP SHA256 differs from GitHub digest")
    return digest.hexdigest()


def safe_extract(archive_path, destination):
    with zipfile.ZipFile(archive_path) as archive:
        infos = archive.infolist()
        names = [info.filename for info in infos]
        require(90 <= len(infos) <= MAX_ENTRIES and
                len(set(names)) == len(names),
                "incomplete/duplicated atlas ZIP inventory")
        require(archive.testzip() is None, "corrupt ZIP member checksum")
        require("manifest.json" in names, "missing real native atlas manifest")
        for info in infos:
            name = info.filename
            p = PurePosixPath(name)
            require(name == str(p) and not p.is_absolute() and
                    ".." not in p.parts and "\\" not in name and
                    not name.endswith("/") and
                    len(p.parts) == 1 and
                    stat.S_IFMT(info.external_attr >> 16) != stat.S_IFLNK and
                    info.file_size <= 12_000_000,
                    "unsafe or oversized atlas ZIP entry")
            with archive.open(info, "r") as src, (destination / name).open("xb") as dst:
                shutil.copyfileobj(src, dst)


def merge_tree_eligible(merge_commit, pr, main_parent, main_tree):
    """Require the exact synthetic merge that GitHub's PR job checked out."""
    require(merge_commit.get("commit", {}).get("tree", {}).get("sha") == main_tree,
            "tested PR merge has different complete Git source tree")
    parents = [parent["sha"] for parent in merge_commit.get("parents", [])]
    require(parents == [main_parent, pr["head"]["sha"]],
            "tested PR merge was built against a different main/PR head")


def emit_output(reused, source_sha="", run_id=""):
    path = os.environ.get("GITHUB_OUTPUT")
    if path:
        with open(path, "a", encoding="utf-8") as file:
            file.write("reused=" + ("true" if reused else "false") + "\n")
            if reused:
                file.write("tested_sha=" + source_sha + "\n")
                file.write("source_run=" + str(run_id) + "\n")


def try_reuse(out_dir, main_sha, token):
    require(os.environ.get("GITHUB_EVENT_NAME") == "push" and
            os.environ.get("GITHUB_REF") == "refs/heads/main" and
            os.environ.get("GITHUB_REPOSITORY") == REPO,
            "not a trusted main push")
    require(re.fullmatch(r"[0-9a-f]{40}", main_sha or ""),
            "invalid current main SHA")
    git_sha = subprocess.check_output(
        ["git", "rev-parse", "HEAD"], text=True).strip()
    git_tree = subprocess.check_output(
        ["git", "rev-parse", "HEAD^{tree}"], text=True).strip()
    require(git_sha == main_sha, "current checkout is not the main push commit")

    main = api("/repos/" + REPO + "/commits/" + main_sha, token)
    parents = main.get("parents", [])
    require(len(parents) == 1, "current main push is not a squash commit")
    parent = parents[0]["sha"]
    main_tree = main.get("commit", {}).get("tree", {}).get("sha")
    require(main.get("sha") == main_sha and git_tree == main_tree,
            "GitHub/main checkout tree differs")

    prs = api("/repos/" + REPO + "/commits/" + main_sha + "/pulls", token)
    pr = pr_eligible(prs, main_sha, parent)
    # Verify the action environment against GitHub's authoritative repository
    # identity, not just a self-echo of GITHUB_REPOSITORY_ID.
    repo_info = api("/repos/" + REPO, token)
    repo_id = repo_info.get("id")
    require(isinstance(repo_id, int) and repo_id > 0 and
            int(os.environ.get("GITHUB_REPOSITORY_ID") or 0) == repo_id,
            "source repository identity is unknown or mismatched")
    url = ("/repos/" + REPO + "/actions/runs?head_sha=" +
           pr["head"]["sha"] + "&per_page=100")
    runs = api(url, token).get("workflow_runs", [])
    eligible_runs = []
    for run in runs:
        try:
            eligible_runs.append(run_eligible(run, pr, repo_id))
        except (ValueError, KeyError, TypeError):
            continue
    require(eligible_runs, "no accepted real Android PR workflow for merged head")
    eligible_runs.sort(key=lambda r: (r["updated_at"], r["id"]), reverse=True)

    out_dir.parent.mkdir(parents=True, exist_ok=True)
    with tempfile.TemporaryDirectory(prefix="glimpse-atlas-proof-",
                                     dir=out_dir.parent) as temp:
        workspace = Path(temp)
        for run in eligible_runs:
            try:
                jobs = api("/repos/" + REPO + "/actions/runs/" +
                           str(run["id"]) + "/jobs?per_page=100", token)
                capture_jobs = [j for j in jobs.get("jobs", [])
                                if j.get("name") == "capture"]
                require(len(capture_jobs) == 1 and
                        capture_jobs[0].get("conclusion") == "success",
                        "PR workflow did not have successful capture job")
                artifacts = api("/repos/" + REPO + "/actions/runs/" +
                                str(run["id"]) + "/artifacts?per_page=100", token)
                artifact = artifact_eligible(artifacts.get("artifacts"), run, repo_id)
                staging = workspace / "staged"
                staging.mkdir()
                archive = workspace / "source.zip"
                zip_sha = download_artifact(artifact, token, archive)
                safe_extract(archive, staging)
                manifest = json.loads((staging / "manifest.json").read_text())
                source = manifest.get("sourceSha")
                require(isinstance(source, str) and
                        re.fullmatch(r"[0-9a-f]{40}", source),
                        "artifact has no tested synthetic merge SHA")
                require(manifest.get("mode") == "full",
                        "PR accepted only a smoke atlas")
                synthetic = api("/repos/" + REPO + "/commits/" + source, token)
                merge_tree_eligible(synthetic, pr, parent, main_tree)
                proof = validate(staging, expected_sha=source)
                require(proof["captures"] == 46, "not the complete 46-screen atlas")
                receipt = {
                    "schema": "glimpse/verified-native-atlas-reuse/v1",
                    "decision": "reuse-real-native-evidence",
                    "main_sha": main_sha, "main_tree_sha": main_tree,
                    "tested_merge_sha": source,
                    "pr_number": pr["number"], "pr_head_sha": pr["head"]["sha"],
                    "upstream_workflow_run": run["id"],
                    "upstream_artifact_id": artifact["id"],
                    "upstream_artifact_sha256": zip_sha,
                    "verified_png_and_xml_pairs": proof["captures"],
                    "actual_android_was_executed_in_upstream_run": True,
                    "same_source_tree_independently_verified": True,
                    "no_emulator_executed_in_this_main_run": True
                }
                (staging / "reuse-provenance.json").write_text(
                    json.dumps(receipt, indent=2, sort_keys=True) + "\n")
                require(not out_dir.exists(), "refusing to replace atlas evidence")
                staging.rename(out_dir)
                return source, run["id"], receipt
            except (OSError, KeyError, ValueError, TypeError, json.JSONDecodeError,
                    urllib.error.URLError, zipfile.BadZipFile) as error:
                print("::notice::Cannot reuse PR run " + str(run["id"]) +
                      "; performing real Android acceptance instead: " +
                      type(error).__name__ + " " + str(error)[:160], flush=True)
                shutil.rmtree(workspace / "staged", ignore_errors=True)
                (workspace / "source.zip").unlink(missing_ok=True)
                continue
    raise ValueError("No exact-tree artifact survived full independent validation")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--main-sha", required=True)
    parser.add_argument("--out", type=Path, required=True)
    args = parser.parse_args()
    token = os.environ.get("GH_TOKEN", "")
    try:
        require(bool(token), "missing short-lived read-only GitHub token")
        source, run_id, receipt = try_reuse(args.out.resolve(), args.main_sha, token)
    except Exception as error:
        emit_output(False)
        print("::notice::Atlas reuse unavailable; full real Android run required: " +
              type(error).__name__ + " " + str(error)[:190], flush=True)
        return 0
    emit_output(True, source, run_id)
    print("::notice::Source-tree-exact real Android atlas reused, 46/46 PNG+XML: " +
          json.dumps(receipt, sort_keys=True), flush=True)
    return 0


if __name__ == "__main__":
    sys.exit(main())
