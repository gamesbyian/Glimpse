"""No private bytes or invented successes in machine-readable Python CI diagnosis."""
import json
import os
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch

from ci_failure_digest import first_harness_failure, build_receipt
from ci_harness_runner import emit_exception


class HarnessDigestTests(unittest.TestCase):
    def _receipt(self, folder, value):
        path = Path(folder) / "artifacts/ci-harness-failure.json"
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(json.dumps(value))
        return path

    def _valid(self):
        return {"schemaVersion": 1, "outcome": "failure",
                "phase": "filmstrip-ui", "emulatorApi": "34",
                "reproduction": "python3 scripts/capture-android-shuffle-preview.py",
                "exception": {
                    "exceptionType": "RuntimeError",
                    "message": "Read-only Home shuffle preview never rendered",
                    "source": {"script": "scripts/capture-android-shuffle-preview.py",
                               "line": 42}}}

    def test_observed_python_failure_is_actionable_without_junit(self):
        with tempfile.TemporaryDirectory() as folder:
            self._receipt(folder, self._valid())
            report = build_receipt(folder, "shuffle", {"GITHUB_SHA": "abc"})
            self.assertEqual(report["schemaVersion"], 2)
            self.assertEqual(report["classification"], "python_harness_exception")
            self.assertEqual(report["evidence"], "observed_python_traceback")
            self.assertIsNone(report["firstFailure"])
            self.assertEqual(report["firstHarnessFailure"]["script"],
                             "scripts/capture-android-shuffle-preview.py")
            self.assertEqual(report["firstHarnessFailure"]["line"], 42)

    def test_missing_invalid_empty_oversized_and_untrusted_receipts(self):
        with tempfile.TemporaryDirectory() as folder:
            self.assertIsNone(first_harness_failure(folder))
            path = self._receipt(folder, {})
            self.assertIsNone(first_harness_failure(folder))
            path.write_text("not json")
            self.assertIsNone(first_harness_failure(folder))
            path.write_text("x" * 8193)
            self.assertIsNone(first_harness_failure(folder))
            path.unlink()
            outside = Path(folder) / "outside"
            outside.write_text(json.dumps(self._valid()))
            try:
                path.symlink_to(outside)
            except (NotImplementedError, OSError):
                return
            self.assertIsNone(first_harness_failure(folder))

    def test_forged_exception_type_is_rejected(self):
        with tempfile.TemporaryDirectory() as folder:
            payload = self._valid()
            payload["exception"]["exceptionType"] = "user photo pixels"
            self._receipt(folder, payload)
            self.assertIsNone(first_harness_failure(folder))

    def test_secret_bearing_message_and_path_are_sanitized(self):
        with tempfile.TemporaryDirectory() as folder:
            payload = self._valid()
            payload["exception"]["message"] = "token: verysecret /sdcard/DCIM/secret.jpg"
            payload["exception"]["source"]["script"] = "/sdcard/private.py"
            self._receipt(folder, payload)
            report = first_harness_failure(folder)
            self.assertNotIn("verysecret", repr(report))
            self.assertNotIn("/sdcard/DCIM", repr(report))
            self.assertIsNone(report["script"])

    def test_caught_exception_receipt_writes_exclusive_without_changing_primary_error(self):
        with tempfile.TemporaryDirectory() as folder:
            path = Path(folder) / "artifacts/ci-harness-failure.json"
            try:
                raise RuntimeError("No next photo preview control")
            except RuntimeError as error:
                with patch.dict(os.environ, {"GITHUB_SHA": "abc"}):
                    self.assertTrue(emit_exception(
                        error, output=path, workflow="atlas",
                        phase="atlas-ui", api="34"))
                    self.assertFalse(emit_exception(
                        error, output=path, workflow="atlas",
                        phase="atlas-ui", api="34"))
            data = json.loads(path.read_text())
            self.assertEqual(data["evidence"], "observed_caught_python_exception")
            self.assertEqual(data["exception"]["exceptionType"], "RuntimeError")
            self.assertIn("No next photo preview", data["exception"]["message"])

    def test_success_without_harness_receipt_stays_unclassified_on_unrelated_failure(self):
        with tempfile.TemporaryDirectory() as folder:
            report = build_receipt(folder, "native", {})
            self.assertEqual(report["classification"], "unclassified")
            self.assertIsNone(report["firstHarnessFailure"])


if __name__ == "__main__":
    unittest.main()
