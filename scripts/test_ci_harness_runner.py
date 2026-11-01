"""Negative controls for bounded Python harness error receipts."""
import io
import json
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch
from ci_harness_runner import observe, run, safe, safe_sha


class HarnessReceiptTests(unittest.TestCase):
    def test_first_actual_python_exception_not_adb_noise(self):
        lines = ["emulator: INFO: boot time 20s", "adb: device offline",
                 "Traceback (most recent call last):",
                 '  File "/home/runner/work/Glimpse/Glimpse/scripts/capture-android-shuffle-preview.py", line 55, in main',
                 "RuntimeError: Read-only Home shuffle preview never rendered"]
        self.assertEqual(observe(lines),
                         {"exceptionType": "RuntimeError",
                          "message": "Read-only Home shuffle preview never rendered",
                          "source": {"script": "scripts/capture-android-shuffle-preview.py",
                                     "line": 55}})

    def test_setup_warning_alone_cannot_be_claimed_as_harness_assertion(self):
        self.assertIsNone(observe(["adb: device offline", "Emulator failed to boot"]))

    def test_redacts_private_paths_tokens_urls_and_identifiers(self):
        for content in ("token: abcde", "Authorization=BearerAAA",
                        "content://example.private/item/123", "/sdcard/DCIM/secret.jpg",
                        "file:///data/user/0/app/file.jpg", "https://host/path?token=secret",
                        "photo=/sdcard/private.png", "a" * 64):
            with self.subTest(value=content):
                self.assertNotIn(content, safe(content))

    def test_chained_exception_uses_terminal_observed_error_only(self):
        lines = ["Traceback (most recent call last):",
                 '  File "scripts/one.py", line 3, in thing',
                 "ValueError: secret=never-export",
                 "During handling of the above exception, another exception occurred:",
                 "Traceback (most recent call last):",
                 '  File "scripts/capture-android-atlas.py", line 102, in main',
                 "RuntimeError: The requested UI action is missing"]
        self.assertEqual(observe(lines)["exceptionType"], "RuntimeError")
        self.assertNotIn("never-export", repr(observe(lines)))

    def test_success_has_no_failure_receipt(self):
        with tempfile.TemporaryDirectory() as folder:
            output = Path(folder) / "receipt.json"
            class Process:
                stdout = io.StringIO("PASS: real Android harness\n")
                def wait(self): return 0
            with patch("ci_harness_runner.subprocess.Popen", return_value=Process()):
                code = run(["python3", "scripts/capture-android-atlas.py"],
                           output=output, workflow="visual", phase="capture", api="34")
            self.assertEqual(code, 0)
            self.assertFalse(output.exists())

    def test_python_failure_receipt_and_exit_code_preserved(self):
        with tempfile.TemporaryDirectory() as folder:
            output = Path(folder) / "receipt.json"
            class Process:
                stdout = io.StringIO('Traceback (most recent call last):\n'
                                    '  File "scripts/capture-android-atlas.py", line 123, in main\n'
                                    'RuntimeError: Missing wallpaper action\n')
                def wait(self): return 7
            with patch("ci_harness_runner.subprocess.Popen", return_value=Process()):
                code = run(["python3", "scripts/capture-android-atlas.py"],
                           output=output, workflow="visual", phase="capture", api="34",
                           environ={"GITHUB_SHA": "a" * 40})
            self.assertEqual(code, 7)
            data = json.loads(output.read_text())
            self.assertEqual(data["exception"]["message"], "Missing wallpaper action")
            self.assertEqual(data["sourceSha"], "a" * 40)
            self.assertEqual(data["exitCode"], 7)

    def test_non_python_failure_is_unclassified_and_does_not_invent_trace(self):
        with tempfile.TemporaryDirectory() as folder:
            output = Path(folder) / "receipt.json"
            class Process:
                stdout = io.StringIO("adb: device unauthorized\n")
                def wait(self): return 1
            with patch("ci_harness_runner.subprocess.Popen", return_value=Process()):
                self.assertEqual(run(["python3", "scripts/capture-android-atlas.py"],
                                     output=output, workflow="visual", phase="setup", api="34"), 1)
            data = json.loads(output.read_text())
            self.assertEqual(data["evidence"], "unclassified_nonzero_exit")
            self.assertIsNone(data["exception"])

    def test_emulator_launch_failure_does_not_invent_a_python_assertion(self):
        with tempfile.TemporaryDirectory() as folder:
            output = Path(folder) / "receipt.json"
            with patch("ci_harness_runner.subprocess.Popen",
                       side_effect=OSError("emulator launch unavailable")):
                with self.assertRaises(OSError):
                    run(["python3", "scripts/capture-android-atlas.py"],
                        output=output, workflow="visual", phase="setup", api="34")
            self.assertFalse(output.exists())

    def test_cancellation_propagates_and_terminates_child_without_false_receipt(self):
        with tempfile.TemporaryDirectory() as folder:
            output = Path(folder) / "receipt.json"
            class InterruptingOutput:
                def __iter__(self):
                    raise KeyboardInterrupt()
            class Process:
                stdout = InterruptingOutput()
                terminated = False
                def terminate(self): self.terminated = True
                def wait(self): return -15
            child = Process()
            with patch("ci_harness_runner.subprocess.Popen", return_value=child):
                with self.assertRaises(KeyboardInterrupt):
                    run(["python3", "scripts/capture-android-atlas.py"],
                        output=output, workflow="visual", phase="capture", api="34")
            self.assertTrue(child.terminated)
            self.assertFalse(output.exists())

    def test_public_git_sha_survives_but_arbitrary_hex_content_is_redacted(self):
        self.assertEqual(safe_sha("A" * 40), "a" * 40)
        self.assertIsNone(safe_sha("not-a-valid-commit"))
        self.assertIsNone(safe_sha("b" * 64))
        self.assertNotIn("b" * 64, safe("Image digest: " + "b" * 64))

    def test_rejects_non_script_commands_without_execution(self):
        with tempfile.TemporaryDirectory() as folder:
            with self.assertRaises(ValueError):
                run(["bash", "evil"], output=Path(folder) / "r",
                    workflow="visual", phase="capture", api="34")


if __name__ == "__main__":
    unittest.main()
