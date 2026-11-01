#!/usr/bin/env python3
"""Small, privacy-bounded failure receipt. Never export full logs or media fixtures."""
import argparse
import json
import os
from pathlib import Path
import re
import xml.etree.ElementTree as ET

SENSITIVE = re.compile(r"(?i)\b(password|authorization|bearer|token|secret|api[_-]?key)\b\s*[:=]\s*\S+")


def safe_line(value):
    # The same privacy policy applies to JUnit and Python harness messages.
    from ci_harness_runner import safe
    return safe(value)


def first_failure(root):
    """Read JUnit error metadata only. Ignore system-out, screenshots and fixtures."""
    for path in sorted(Path(root).rglob("TEST-*.xml")):
        if not path.is_file() or path.stat().st_size > 10_000_000:
            continue
        try:
            doc = ET.parse(path)
        except (OSError, ET.ParseError):
            continue
        for case in doc.iter("testcase"):
            for child in case:
                if child.tag in ("failure", "error"):
                    return {
                        "test": safe_line(case.attrib.get("classname", "") + "." + case.attrib.get("name", "")),
                        "message": safe_line(child.attrib.get("message")),
                        "junitFile": str(path).replace("\\", "/"),
                    }
    return None


def first_harness_failure(root):
    """Read one small, local JSON receipt; never read full captured logs."""
    from ci_harness_runner import safe
    path = Path(root) / "artifacts/ci-harness-failure.json"
    try:
        if path.is_symlink() or not path.is_file() or not 1 <= path.stat().st_size <= 8192:
            return None
        raw = json.loads(path.read_text(encoding="utf-8"))
        if (not isinstance(raw, dict) or raw.get("schemaVersion") != 1 or
                raw.get("outcome") != "failure"):
            return None
        exception = raw.get("exception")
        if not isinstance(exception, dict):
            return None
        kind = exception.get("exceptionType", "")
        source = exception.get("source")
        if not isinstance(kind, str) or not re.fullmatch(
                r"[A-Za-z_][A-Za-z_0-9.]*(?:Error|Exception|Exit|Interrupt)", kind):
            return None
        script, line = None, None
        if isinstance(source, dict):
            candidate = source.get("script")
            number = source.get("line")
            if (isinstance(candidate, str) and
                    re.fullmatch(r"scripts/[A-Za-z0-9_-]+\.py", candidate) and
                    isinstance(number, int) and 1 <= number <= 20000):
                script, line = candidate, number
        return {
            "exceptionType": safe(kind),
            "message": safe(exception.get("message")),
            "phase": safe(raw.get("phase")),
            "script": script,
            "line": line,
            "emulatorApi": safe(raw.get("emulatorApi")),
            "reproduction": safe(raw.get("reproduction")),
        }
    except (OSError, UnicodeError, json.JSONDecodeError, TypeError, ValueError):
        return None


def build_receipt(root, workflow, env):
    failure = first_failure(root)
    harness = first_harness_failure(root)
    repo = env.get("GITHUB_REPOSITORY", "")
    run = env.get("GITHUB_RUN_ID", "")
    url = f"https://github.com/{repo}/actions/runs/{run}" if repo and run else None
    return {
        "schemaVersion": 2, "workflow": workflow, "repository": repo,
        "sha": env.get("GITHUB_SHA"), "ref": env.get("GITHUB_REF"),
        "event": env.get("GITHUB_EVENT_NAME"), "job": env.get("GITHUB_JOB"),
        "runId": run, "runUrl": url, "outcome": "failure",
        "classification": "test_assertion" if failure else
                          ("python_harness_exception" if harness else "unclassified"),
        "evidence": "observed_junit" if failure else
                    ("observed_python_traceback" if harness else "unclassified_no_junit"),
        "firstFailure": failure, "firstHarnessFailure": harness, "fullLogs": url,
        "nextAction": ("Investigate the named JUnit test" if failure else
                       "Investigate the observed Python harness exception and reproduce the named phase" if harness else
                       "Read the earliest failed step: setup, compilation, emulator and harness failures may have no JUnit output"),
    }


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--workflow", required=True)
    parser.add_argument("--output", default="artifacts/ci-failure-digest.json")
    args = parser.parse_args()
    receipt = build_receipt(".", args.workflow, os.environ)
    path = Path(args.output)
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(receipt, sort_keys=True, indent=2) + "\n")
    if os.environ.get("GITHUB_STEP_SUMMARY"):
        with open(os.environ["GITHUB_STEP_SUMMARY"], "a", encoding="utf-8") as summary:
            summary.write("### CI failure receipt (schema 2)\n")
            summary.write(f"- Workflow: {receipt['workflow']}; SHA: `{receipt['sha']}`\n")
            summary.write(f"- Classification: {receipt['classification']} ({receipt['evidence']})\n")
            if receipt["firstFailure"]:
                summary.write(f"- Test: `{receipt['firstFailure']['test']}`\n")
                summary.write(f"- Error: `{receipt['firstFailure']['message']}`\n")
            elif receipt["firstHarnessFailure"]:
                observed = receipt["firstHarnessFailure"]
                summary.write(f"- Python: `{observed['exceptionType']}` in `{observed['phase']}`\n")
                summary.write(f"- Error: `{observed['message']}`\n")
            else:
                summary.write("- No observed JUnit/Python assertion. Inspect the earliest failed workflow step.\n")
            if receipt["runUrl"]:
                summary.write(f"- [Complete run logs]({receipt['runUrl']})\n")
    print(f"Saved failure receipt ({receipt['classification']}) at {path}")


if __name__ == "__main__":
    main()
