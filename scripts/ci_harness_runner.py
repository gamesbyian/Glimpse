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
SENSITIVE = re.compile(r"(?i)\b(password|authorization|bearer|token|secret|api[_-]?key)\b\s*[:=]\s*\S+")
PRIVATE_PATH = re.compile(r"(?i)(?:content://|file://|/data/|/storage/|/sdcard/|/home/|/mnt/)[^\s,)]+")
WINDOWS_PATH = re.compile(r"(?i)\b[A-Z]:\\[^\s,)]+")
LONG_ID = re.compile(r"\b[a-fA-F0-9]{32,}\b")
URL = re.compile(r"https?://[^\s,)]+")
EMBEDDED = re.compile(r"(?i)\b(?:photo|image|uri|path|filename)\s*[:=]\s*\S+")


def safe(value):
    first = str(value or "").splitlines()[0][:280]
    first = SENSITIVE.sub(r"\1=[REDACTED]", first)
    first = EMBEDDED.sub("[REDACTED_FIELD]", first)
    first = PRIVATE_PATH.sub("[REDACTED_PATH]", first)
    first = WINDOWS_PATH.sub("[REDACTED_PATH]", first)
    first = URL.sub("[REDACTED_URL]", first)
    first = LONG_ID.sub("[REDACTED_ID]", first)
    return first[:220]


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
    record = {
        "schemaVersion": 1,
        "outcome": "failure",
        "workflow": safe(workflow),
        "phase": safe(phase),
        "sourceSha": safe(env.get("GITHUB_SHA", "")),
        "emulatorApi": safe(api),
        "reproduction": " ".join(command)[:220],
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
