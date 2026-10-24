#!/usr/bin/env python3
"""Source-truth guard for repaired Glimpse support variants.

Checks XML/source-visible draft claims only. This does not test hitboxes,
native UI, TalkBack, actual OS outcomes or owner acceptance.
"""
from pathlib import Path
from xml.etree import ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
PLATES = ROOT / "design/proposals/support-plates"


def phone(name: str) -> str:
    path = PLATES / (name + ".svg")
    raw = path.read_text(encoding="utf-8")
    root = ET.fromstring(raw)
    assert root.tag == "{http://www.w3.org/2000/svg}svg", name
    assert root.attrib["viewBox"] == "0 0 1160 920", name
    assert "SCREEN PROPOSAL • NOT USER-APPROVED" in raw, name
    assert '<rect x="440"' in raw, name
    return raw.split('<rect x="440"', 1)[0]


def requires(name: str, present=(), absent=()):
    s = phone(name)
    for phrase in present:
        assert phrase in s, (name, "missing", phrase)
    for phrase in absent:
        assert phrase not in s, (name, "forbidden", phrase)
    return s


def main():
    requires("notification-preferences--available",
             ["Off by default; choose to enable", "Activity"],
             ["When implemented", "features not"])
    s = requires("notification-preferences--not-implemented",
                 ["Not offered in this build"],
                 ["Off by default; choose to enable"])
    assert '<rect x="328" y="365"' not in s and '<rect x="328" y="457"' not in s

    requires("external-handler-fallback--chooser-cancelled",
             ["Sharing cancelled", "You closed the share chooser",
              "No recipient selected; nothing was sent", "Try sharing again", "Save locally"],
             ["No supported app found"])
    requires("external-handler-fallback--email-missing", ["No supported app found"])
    requires("credits-licences--rights-warning",
             ["Image rights need checking", "without verified reuse terms."])
    requires("permissions-capabilities--restricted",
             ["Lock target: unsupported or unknown", "Do not infer Lock support"])
    requires("crash-recovery--normal-exit",
             ["Resume your collection where you left off"],
             ["Glimpse may have closed unexpectedly"])
    for state in ("draft", "with-diagnostics"):
        requires(f"report-problem--{state}",
                 ["What did you expect to happen?", "Expected outcome (optional)"],
                 ["Optional steps to reproduce"])
    requires("whats-new--no-notes",
             ["This build has no offline release notes."],
             ["Photo Picker, offline collections and a"])
    requires("storage-management--low-storage",
             ["Device storage is low", "New imports may fail.", "Free storage safely"])
    confirm = requires("storage-management--delete-confirm",
                       ["Delete 22 local copies?", "Original provider photos stay intact.",
                        "Cancel", "Delete 22 copies"],
                       ["Confirm with affected count"])
    assert 'fill="#703B27"' in confirm
    requires("privacy--diagnostics-opt-in",
             ["Diagnostics selected for this report", "Review exact contents before sharing."])
    requires("help--article",
             ["Import photos from Android", "Wait for verified local copies.",
              "Selection only previews it.", "Browsing never applies automatically",
              "Back to help topics"], ["Read import guide"])
    requires("diagnostic-preview--minimal",
             ["Outgoing sample report", "provider-copy category (redacted)", "NEVER",
              "Show redacted detail fields"],
             ['<rect x="328" y="523"'])
    requires("diagnostic-preview--expanded",
             ["Outgoing sample report", "Extra included field examples",
              "IDs redacted", "No images, source URIs or account tokens", "NEVER"],
             ['<rect x="328" y="523"'])
    requires("help--topics", ["Get photos into Glimpse"])
    print("PASS: 15 corrected support variants have distinct, source-visible, unapproved state semantics")


if __name__ == "__main__":
    main()
