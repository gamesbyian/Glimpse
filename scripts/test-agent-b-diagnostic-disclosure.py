#!/usr/bin/env python3
"""Mutation checks: a source gate must reject reordered or unsafe disclosure."""
from pathlib import Path
import subprocess
import sys
import tempfile

root = Path(__file__).resolve().parents[1]
source = root / "design/proposals/support-plates/diagnostic-preview--expanded.svg"
validator = root / "scripts/validate-agent-b-diagnostic-disclosure.py"
original = source.read_text()

def probe(data, should_pass, name):
    with tempfile.TemporaryDirectory(prefix="glimpse-b-disclosure-") as d:
        p = Path(d) / "candidate.svg"
        p.write_text(data)
        run = subprocess.run([sys.executable, str(validator), "--source", str(p)],
                             text=True, capture_output=True)
        if (run.returncode == 0) != should_pass:
            raise AssertionError(f"{name}: unexpected return {run.returncode}: {run.stderr}{run.stdout}")
        print(("PASS" if should_pass else "REJECTED") + ": " + name)

probe(original, True, "current expanded disclosure")
probe(original.replace('y="605"', 'y="780"', 1), False, "late disclosure heading")
probe(original.replace('data-hit-height="48"', 'data-hit-height="24"', 1).replace('height="48" rx="20"', 'height="24" rx="20"', 1),
      False, "undersized action")
probe(original.replace('No images, source URIs or account tokens', 'Sensitive images are included', 1),
      False, "missing privacy exclusion")
# Keep coordinates unchanged, but alter XML child order to place first action before fields.
marker = '<rect x="40" y="582" width="348" height="103"'
action = '<text x="45" y="717"'
start = original.find(marker)
end = original.find(action)
assert start >= 0 and end > start, "fixture cannot find expected disclosure block"
prefix, disclosure, suffix = original[:start], original[start:end], original[end:]
point = suffix.find('<rect x="43" y="742"')
assert point >= 0, "fixture cannot find Copy button"
mutant = prefix + suffix[:point] + suffix[point:].replace('<rect x="43" y="742"', disclosure + '<rect x="43" y="742"', 1)
probe(mutant, False, "DOM action before disclosure")
