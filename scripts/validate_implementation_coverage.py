#!/usr/bin/env python3
"""Validate the 73-workflow Glimpse coverage ledger without guessing completion.

Status rows are authoritative. The four tier totals, overall totals, and
declared scenario-equivalent percentages must be derived from those rows.
No dependencies, network calls, Android SDK or GitHub credentials required.
"""
from collections import Counter
from pathlib import Path
import re
import sys

LEDGER = Path(__file__).resolve().parents[1] / "docs/quality/IMPLEMENTATION-COVERAGE-2026-10-28.md"
EXPECTED_IDS = {f"W{i:02d}" for i in range(1, 71)} | {f"F{i:03d}" for i in range(71, 74)}
TIERS = ("M1", "M2", "M3", "M4")
STATES = ("Implemented", "Partial", "Not implemented")
WORKFLOW = re.compile(r"^\|\s*(W\d{2}|F\d{3})\s*\|\s*(M[1-4])\s*\|\s*(Implemented|Partial|Not implemented)\s*\|")


def parse(text: str) -> tuple[dict[str, tuple[str, str]], dict[str, tuple[int, int, int, int, float]]]:
    records = {}
    duplicates = set()
    summary = {}
    for line in text.splitlines():
        match = WORKFLOW.match(line)
        if match:
            ident, tier, state = match.groups()
            if ident in records:
                duplicates.add(ident)
            records[ident] = (tier, state)
            continue
        if not line.startswith("|"):
            continue
        cells = [cell.strip().replace("**", "") for cell in line.strip("|").split("|")]
        if len(cells) != 6 or cells[0] not in (*TIERS, "All"):
            continue
        try:
            complete, partial, absent, total = (int(v) for v in cells[1:5])
            pct = float(cells[5].rstrip("%"))
        except ValueError:
            continue
        if cells[0] in summary:
            raise ValueError(f"Duplicate summary row: {cells[0]}")
        summary[cells[0]] = (complete, partial, absent, total, pct)
    if duplicates:
        raise ValueError(f"Duplicate workflow IDs: {', '.join(sorted(duplicates))}")
    return records, summary


def validate_text(text: str) -> str:
    records, summary = parse(text)
    missing = EXPECTED_IDS - records.keys()
    unexpected = records.keys() - EXPECTED_IDS
    if missing or unexpected:
        raise ValueError(f"Workflow IDs mismatch: missing={sorted(missing)} extra={sorted(unexpected)}")
    if set(summary) != set((*TIERS, "All")):
        raise ValueError(f"Summary rows mismatch: found={sorted(summary)}")
    for tier in (*TIERS, "All"):
        rows = [state for actual_tier, state in records.values()
                if tier == "All" or tier == actual_tier]
        counts = Counter(rows)
        values = (counts["Implemented"], counts["Partial"],
                  counts["Not implemented"], len(rows))
        written = summary[tier]
        if values != written[:4]:
            raise ValueError(f"{tier}: derived counts {values}, table says {written[:4]}")
        # A Partial receives 0.5 of a scenario-equivalent. Percentages are
        # labels, not empirical engineering estimates; accept 0 or 1 decimals.
        calculated = (values[0] + 0.5 * values[1]) * 100 / values[3]
        written_precision = len(str(written[4]).split(".")[-1])
        tolerance = 0.51 if written[4].is_integer() else 0.051
        if abs(calculated - written[4]) > tolerance:
            raise ValueError(
                f"{tier}: scenario-equivalent {calculated:.3f}% disagrees "
                f"with published {written[4]}%"
            )
    total = summary["All"]
    return (f"Coverage validated: {total[0]} complete, {total[1]} partial, "
            f"{total[2]} not implemented; {total[4]:g}% scenario-equivalent "
            f"across {total[3]} distinct contractual workflows.")


def main() -> int:
    try:
        print(validate_text(LEDGER.read_text(encoding="utf-8")))
    except (OSError, ValueError) as exc:
        print(f"ERROR: {exc}", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
