"""Protect the actual privileged Pages workflow topology from regression."""
from pathlib import Path
import unittest
import yaml

ROOT = Path(__file__).resolve().parents[1]
PUBLISHER = ROOT / ".github/workflows/glimpse-pages-publisher.yml"


class PagesBoundaryTests(unittest.TestCase):
    def setUp(self):
        self.workflow = yaml.load(PUBLISHER.read_text(), Loader=yaml.BaseLoader)
        self.stage = self.workflow["jobs"]["stage"]
        self.publish = self.workflow["jobs"]["publish"]

    def test_non_main_and_pr_events_cannot_dispatch(self):
        events = self.workflow["on"]
        self.assertNotIn("pull_request", events)
        self.assertNotIn("pull_request_target", events)
        self.assertEqual(events["push"]["branches"], ["main"])
        self.assertEqual(events["workflow_run"]["types"], ["completed"])
        self.assertEqual(events["workflow_run"]["branches"],
                         ["agent/visual-state-reconciliation-20261023"])
        guard = self.stage["if"]
        for term in ("github.event.workflow_run.conclusion == 'success'",
                     "github.event.workflow_run.head_repository.full_name == 'gamesbyian/Glimpse'",
                     "github.event.workflow_run.head_repository.id == github.event.repository.id",
                     "github.event.workflow_run.head_branch == 'agent/visual-state-reconciliation-20261023'",
                     "Glimpse | Chromium owner reviewer interaction QA",
                     "Glimpse | Build complete draft visual evidence atlas",
                     "github.ref == 'refs/heads/main'"):
            self.assertIn(term, guard)

    def test_privileges_live_only_in_publish_job(self):
        self.assertEqual(self.workflow["permissions"], {"contents": "read"})
        self.assertEqual(self.stage["permissions"], {"contents": "read"})
        self.assertNotIn("environment", self.stage)
        self.assertEqual(self.publish["needs"], "stage")
        self.assertIn("needs.stage.result == 'success'", self.publish["if"])
        self.assertEqual(self.publish["permissions"]["pages"], "write")
        self.assertEqual(self.publish["permissions"]["id-token"], "write")
        self.assertEqual(self.publish["environment"]["name"], "github-pages")
        self.assertFalse(any("checkout@" in step.get("uses", "") for step in self.publish["steps"]))
        self.assertFalse(any("scripts/" in step.get("run", "") for step in self.publish["steps"]))
        self.assertTrue(any("upload-pages-artifact@" in step.get("uses", "")
                            for step in self.stage["steps"]))
        self.assertTrue(any("deploy-pages@" in step.get("uses", "")
                            for step in self.publish["steps"]))
        self.assertFalse(any("deploy-pages@" in step.get("uses", "")
                             for step in self.stage["steps"]))

    def test_exact_source_provenance_and_live_http_probe(self):
        checkout = next(s for s in self.stage["steps"] if "checkout@" in s.get("uses", ""))
        self.assertEqual(checkout["with"]["persist-credentials"], "false")
        self.assertIn("github.event.workflow_run.head_sha", checkout["with"]["ref"])
        identity = next(s for s in self.stage["steps"] if s.get("id") == "identity")
        self.assertIn('"$actual" != "$EXPECTED_SHA"', identity["run"])
        self.assertIn("steps.identity.outputs.sha", self.stage["outputs"]["candidate_sha"])
        probe = self.publish["steps"][-1]
        self.assertIn("needs.stage.outputs.candidate_sha", probe["run"])
        self.assertIn("review/build.json", probe["run"])
        for step in self.publish["steps"]:
            if step.get("uses"):
                self.assertRegex(step["uses"], r"@[0-9a-f]{40}$")


if __name__ == "__main__":
    unittest.main()
