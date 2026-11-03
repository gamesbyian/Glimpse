#!/usr/bin/env python3
"""Stream a real Python Android harness and save only a bounded error receipt.

Stdout/stderr still reach the workflow log immediately. Never export the log.
No success receipt, no exception fabrication on environment/boot-only failures.
"""
import argparse
import json
import os
from pathlib import Path
import re
import subprocess
import sys

EXCEPTION = re.compile(r"^([A-Za-z_][A-Za-z_0-9.]*(?:Error|Exception|Exit|Interrupt))(?::\s*(.*))?$")
SOURCE = re.compile(r'File "[^"]*?(/?scripts/[^"/]+\.py)", line (\d+)')
# Multi-token headers and labeled credentials must be completely redacted.
# Redact the rest of the line, not only the first whitespace-delimited token.
AUTH_HEADER = re.compile(r"(?i)\b(?:proxy-)?authorization\s*[:=]\s*[^\r\n]*")
BARE_CREDENTIAL = re.compile(r"(?i)\bbearer\s+[^\r\n]+")
SENSITIVE = re.compile(r"(?i)\b(password|authorization|bearer|token|secret|api[_-]?key)\b\s*[:=]\s*[^\r\n]*")
PRIVATE_PATH = re.compile(r"(?i)(?:content://|file://|/data/|/storage/|/sdcard/|/home/|/mnt/)[^\s,)]+")
WINDOWS_PATH = re.compile(r"(?i)\b[A-Z]:\\[^\s,)]+")
LONG_ID = re.compile(r"\b[a-fA-F0-9]{32,}\b")
URL = re.compile(r"https?://[^\s,)]+")
EMBEDDED = re.compile(r"(?i)\b(?:photo|image|uri|path|filename)\s*[:=]\s*\S+")


def safe(value):
    lines = str(value or "").splitlines()
    # JUnit <failure> may legally omit its message attribute. A missing or
    # blank message must not crash the secondary diagnostics step and hide the
    # actual failing test/Android evidence.
    first = (lines[0] if lines else "")[:280]
    first = AUTH_HEADER.sub("authorization=[REDACTED]", first)
    first = BARE_CREDENTIAL.sub("bearer=[REDACTED]", first)
    first = SENSITIVE.sub(r"\1=[REDACTED]", first)
    first = EMBEDDED.sub("[REDACTED_FIELD]", first)
    first = PRIVATE_PATH.sub("[REDACTED_PATH]", first)
    first = WINDOWS_PATH.sub("[REDACTED_PATH]", first)
    first = URL.sub("[REDACTED_URL]", first)
    first = LONG_ID.sub("[REDACTED_ID]", first)
    return first[:220]


def safe_sha(value):
    """A GitHub commit SHA is public provenance, unlike arbitrary photo hashes."""
    candidate = str(value or "").strip()
    return candidate.lower() if re.fullmatch(r"[a-fA-F0-9]{40}", candidate) else None


def observe(lines):
    """Accept parsed stderr/stdout lines, retaining bounded traceback metadata only."""
    error = None
    source = None
    in_trace = False
    for line in lines:
        line = str(line).strip()
        if line == "Traceback (most recent call last):":
            in_trace = True
            source = None
            continue
        if in_trace:
            match = SOURCE.search(line)
            if match:
                source = {"script": match.group(1).lstrip("/"), "line": int(match.group(2))}
            match = EXCEPTION.match(line)
            if match:
                error = {"exceptionType": safe(match.group(1)),
                         "message": safe(match.group(2)),
                         "source": source}
                in_trace = False
    return error


def emit_exception(error, *, output="artifacts/ci-harness-failure.json",
                   workflow, phase, api):
    """Report an exception an atlas catches internally without exporting traceback data."""
    import traceback
    frames = traceback.extract_tb(error.__traceback__)
    frame = next((f for f in reversed(frames)
                  if re.search(r"(?:^|/)scripts/[a-zA-Z0-9_-]+\.py$", f.filename.replace("\\", "/"))),
                 None)
    source = None
    if frame:
        filename = frame.filename.replace("\\", "/")
        source = {"script": "scripts/" + filename.split("/scripts/")[-1].removeprefix("scripts/"),
                  "line": frame.lineno}
    record = {
        "schemaVersion": 1, "outcome": "failure",
        "workflow": safe(workflow), "phase": safe(phase),
        "sourceSha": safe_sha(os.environ.get("GITHUB_SHA", "")),
        "emulatorApi": safe(api), "reproduction": safe("python3 scripts/capture-android-atlas.py"),
        "exitCode": 1, "evidence": "observed_caught_python_exception",
        "exception": {"exceptionType": safe(type(error).__name__),
                      "message": safe(str(error)), "source": source},
    }
    path = Path(output)
    try:
        path.parent.mkdir(parents=True, exist_ok=True)
        with path.open("x", encoding="utf-8") as stream:
            json.dump(record, stream, sort_keys=True, indent=2)
            stream.write("\n")
    except OSError:
        # Diagnostics must never replace the primary Android failure.
        return False
    return True


def run(command, *, output, workflow, phase, api, environ=None):
    env = os.environ if environ is None else environ
    if not command or command[0] != "python3" or len(command) < 2:
        raise ValueError("Runner requires python3 and a known script")
    if not re.fullmatch(r"scripts/[a-zA-Z0-9_-]+\.py", command[1]):
        raise ValueError("Capture source must be a repository scripts/ Python entrypoint")
    # A bounded ring buffer is sufficient to locate the final Python exception
    # without exporting the often enormous or sensitive emulator log.
    recent = []
    process = subprocess.Popen(command, stdout=subprocess.PIPE,
                               stderr=subprocess.STDOUT, text=True,
                               encoding="utf-8", errors="replace", bufsize=1)
    assert process.stdout is not None
    try:
        for line in process.stdout:
            print(line, end="", flush=True)
            if "Traceback (most recent call last):" in line:
                recent = []
            recent.append(line[:300])
            if len(recent) > 80:
                recent.pop(0)
    except KeyboardInterrupt:
        process.terminate()
        process.wait()
        raise
    code = process.wait()
    if code == 0:
        return 0
    failure = observe(recent)
    root = Path(output)
    if root.exists():
        # An internally caught and already recorded exception owns this receipt.
        return code if code > 0 else 128 - code
    record = {
        "schemaVersion": 1,
        "outcome": "failure",
        "workflow": safe(workflow),
        "phase": safe(phase),
        "sourceSha": safe_sha(env.get("GITHUB_SHA", "")),
        "emulatorApi": safe(api),
        "reproduction": safe(" ".join(command)),
        "exitCode": code,
        "evidence": "observed_python_traceback" if failure else "unclassified_nonzero_exit",
        "exception": failure,
    }
    try:
        root.parent.mkdir(parents=True, exist_ok=True)
        # Exclusive creation refuses a forged or stale receipt target.
        with root.open("x", encoding="utf-8") as dest:
            json.dump(record, dest, sort_keys=True, indent=2)
            dest.write("\n")
    except OSError as e:
        print("::warning::Could not write harness receipt: " + safe(type(e).__name__), file=sys.stderr)
    return code if code > 0 else 128 - code


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--output", default="artifacts/ci-harness-failure.json")
    parser.add_argument("--workflow", required=True)
    parser.add_argument("--phase", required=True)
    parser.add_argument("--api", required=True)
    parser.add_argument("command", nargs=argparse.REMAINDER)
    args = parser.parse_args()
    command = args.command[1:] if args.command[:1] == ["--"] else args.command
    return run(command, output=args.output, workflow=args.workflow,
               phase=args.phase, api=args.api)


if __name__ == "__main__":
    sys.exit(main())
