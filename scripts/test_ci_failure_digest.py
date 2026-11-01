"""Tests ensure failure reporting is truthful and excludes raw user/media data."""
import tempfile
from pathlib import Path
import unittest
from ci_failure_digest import first_failure, build_receipt, safe_line


class FailureReceiptTest(unittest.TestCase):
    def test_setup_failure_without_junit_is_unclassified(self):
        with tempfile.TemporaryDirectory() as d:
            result = build_receipt(d, "native", {"GITHUB_REPOSITORY": "gamesbyian/Glimpse",
                                                "GITHUB_RUN_ID": "123", "GITHUB_SHA": "abc"})
            self.assertIsNone(result["firstFailure"])
            self.assertEqual(result["classification"], "unclassified")
            self.assertEqual(result["runUrl"], "https://github.com/gamesbyian/Glimpse/actions/runs/123")

    def test_failing_assertion_without_system_out_or_stack_dump(self):
        with tempfile.TemporaryDirectory() as d:
            path = Path(d) / "app/build/test-results/testDebugUnitTest/TEST-sample.xml"
            path.parent.mkdir(parents=True)
            path.write_text('<testsuite><testcase classname="PhotoTest" name="noOverwrite">'
                            '<failure message="expected true, was false">secret private stack</failure>'
                            '<system-out>RAW PRIVATE PHOTO CONTENT</system-out></testcase></testsuite>')
            result = first_failure(d)
            self.assertEqual(result["test"], "PhotoTest.noOverwrite")
            self.assertEqual(result["message"], "expected true, was false")
            self.assertNotIn("PRIVATE PHOTO", repr(result))

    def test_bearer_auth_header_cannot_leak_trailing_secret(self):
        result = safe_line("Authorization: Bearer secret_part_one secret_part_two")
        self.assertNotIn("secret_part_one", result)
        self.assertNotIn("secret_part_two", result)
        self.assertIn("[REDACTED]", result)

    def test_sensitive_error_values_are_redacted(self):
        self.assertNotIn("12345", safe_line("authorization=12345"))
        self.assertNotIn("abcde", safe_line("token: abcde"))


if __name__ == "__main__":
    unittest.main()
