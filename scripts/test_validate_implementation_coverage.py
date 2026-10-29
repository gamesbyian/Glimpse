"""Regression checks for contractual implementation-percentage accounting."""
import importlib.util
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]
spec = importlib.util.spec_from_file_location(
    "coverage_validator", ROOT / "scripts/validate_implementation_coverage.py"
)
validator = importlib.util.module_from_spec(spec)
spec.loader.exec_module(validator)


class CoverageLedgerTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.ledger = validator.LEDGER.read_text(encoding="utf-8")

    def test_current_73_workflow_ledger_is_consistent(self):
        self.assertIn("73 distinct contractual workflows", validator.validate_text(self.ledger))

    def test_missing_one_workflow_fails(self):
        broken = "\n".join(
            line for line in self.ledger.splitlines() if not line.startswith("| W01 |")
        )
        with self.assertRaisesRegex(ValueError, "missing="):
            validator.validate_text(broken)

    def test_duplicate_workflow_id_fails(self):
        first = next(line for line in self.ledger.splitlines() if line.startswith("| W01 |"))
        with self.assertRaisesRegex(ValueError, "Duplicate workflow"):
            validator.validate_text(self.ledger + "\n" + first)

    def test_stale_overall_summary_fails(self):
        # The ledger is deliberately living documentation; the negative
        # assertion must not hardcode yesterday's legitimate coverage count.
        row = next(line for line in self.ledger.splitlines()
                   if line.startswith("| **All** |"))
        parts = row.split("|")
        actual = validator.parse(self.ledger)[1]["All"][1]
        self.assertGreater(actual, 0)
        parts[3] = f" **{actual - 1}** "
        wrong_row = "|".join(parts)
        with self.assertRaisesRegex(ValueError, "derived counts"):
            validator.validate_text(self.ledger.replace(row, wrong_row, 1))


if __name__ == "__main__":
    unittest.main()
