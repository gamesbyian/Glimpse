"""Trust boundaries for explicitly published Glimpse Android owner-test APKs."""
from pathlib import Path
import unittest

WORKFLOW = (Path(__file__).resolve().parents[1] /
            ".github/workflows/android-check.yml")


class OwnerTestApkPublicationTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.raw = WORKFLOW.read_text(encoding="utf-8")
        cls.normal, cls.publisher = cls.raw.split("\n  owner-test-publication:\n", 1)

    def test_owner_request_default_false(self):
        self.assertIn("publish_owner_test_apk:", self.normal)
        self.assertIn("default: false", self.normal)
        self.assertIn("github.event_name == 'workflow_dispatch' && inputs.publish_owner_test_apk && github.ref == 'refs/heads/main'", self.normal)

    def test_ordinary_pr_main_merge_queue_build_never_publishes(self):
        self.assertIn("permissions:\n  contents: read", self.normal)
        self.assertNotIn("contents: write", self.normal)
        self.assertNotIn("name: glimpse-debug-test-only", self.normal)
        self.assertIn("name: glimpse-owner-test-handoff", self.normal)
        self.assertIn("retention-days: 1", self.normal)
        self.assertIn("name: glimpse-debug-lint-evidence", self.normal)

    def test_privileged_job_only_after_successful_manual_main_build(self):
        self.assertIn("if: github.event_name == 'workflow_dispatch' && inputs.publish_owner_test_apk && github.ref == 'refs/heads/main'", self.publisher)
        self.assertIn("needs: build", self.publisher)
        self.assertIn("permissions:\n      contents: write", self.publisher)
        self.assertIn("sha256sum -c GLIMPSE-BUILD-SHA256.txt", self.publisher)
        self.assertIn("glimpse-signing-fingerprint.txt", self.publisher)

    def test_retire_only_after_successful_release_and_limit_namespace(self):
        self.assertLess(self.publisher.index("gh release create"),
                        self.publisher.index("gh release list"))
        self.assertLess(self.publisher.index("gh release view"),
                        self.publisher.index("gh release list"))
        self.assertIn('test("^glimpse-owner-test-[0-9a-f]{12}$")', self.publisher)
        self.assertIn('gh release delete "$obsolete"', self.publisher)
        self.assertIn('"$obsolete" != "$current"', self.publisher)
        self.assertNotIn("actions/upload-artifact@", self.publisher)

    def test_no_unsafe_installation_advice(self):
        self.assertIn("DO NOT uninstall an installed Glimpse", self.publisher)
        self.assertIn("signing mismatch", self.publisher)


if __name__ == "__main__":
    unittest.main()
