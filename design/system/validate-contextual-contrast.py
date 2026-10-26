#!/usr/bin/env python3
"""Source-only WCAG AA contract for provisional primary palettes and notice text.

Checks declared solid paints, not photographs/transparency or native Android
contrast. Deliberate negative fixtures guard the threshold implementation.
"""
from __future__ import annotations

import json
import math
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
TOKENS = ROOT / "design/system/tokens.v0.1.json"
PLATES = ROOT / "design/proposals/plates"
NAMES = (
    "quick-import", "settings", "quickstart", "discover", "destinations",
    "automation", "activity", "image-editor", "automation-editor",
    "source-detail", "schedule-simulator", "diagnostics", "widgets",
    "quick-settings-and-shortcuts", "backup-and-restore", "onboarding",
    "error-empty-permission", "accessibility-preferences",
)
TINTS = ("tintLavender", "tintBlush", "tintTan", "tintSage", "tintSand")
AA_SMALL_TEXT = 4.5


def luminance(hex_color: str) -> float:
    assert re.fullmatch(r"#[0-9a-fA-F]{6}", hex_color), hex_color
    rgb = [int(hex_color[i:i + 2], 16) / 255.0 for i in (1, 3, 5)]
    linear = [v / 12.92 if v <= 0.04045 else ((v + 0.055) / 1.055) ** 2.4 for v in rgb]
    return sum(w * v for w, v in zip((0.2126, 0.7152, 0.0722), linear))


def contrast(a: str, b: str) -> float:
    hi, lo = sorted((luminance(a), luminance(b)), reverse=True)
    return (hi + 0.05) / (lo + 0.05)


def phone_markup(svg: str) -> str:
    assert svg.startswith("<svg ") and svg.rstrip().endswith("</svg>")
    marker = '<rect x="439" y="14"'
    assert marker in svg, "annotated 401x890 phone boundary absent"
    return svg.split(marker, 1)[0]


def check() -> dict:
    tokens = json.loads(TOKENS.read_text(encoding="utf-8"))
    colors = tokens["color"]
    assert tokens["revision"] == "0.1.2"
    assert tokens["status"] == "DRAFT_PENDING_INTEGRATION_LOCK"
    assert colors["secondary"] == "#566861", "Paper token must remain stable"
    assert colors["secondaryOnTint"] == "#445851"
    assert colors["attentionText"] == "#703B27"
    assert colors["accent"] == "#1D5960"
    focus = tokens["focusVisible"]
    assert focus["indicatorColor"] == colors["accent"]
    assert focus["strokePxNominal"] >= 3
    assert contrast(focus["indicatorColor"], colors["paper"]) >= focus["minimumContrastAgainstPaper"]
    assert contrast(focus["indicatorColor"], colors["surface"]) >= focus["minimumContrastAgainstPaper"]
    for surface in ("paper", "surface", *TINTS):
        assert contrast(focus["indicatorColor"], colors[surface]) >= focus["minimumContrastAgainstPaper"], surface
    assert contrast(colors["secondary"], colors["paper"]) >= AA_SMALL_TEXT
    assert contrast(colors["onAccent"], colors["accent"]) >= AA_SMALL_TEXT

    tint_ratios = {}
    for key in TINTS:
        background = colors[key]
        small = contrast(colors["secondaryOnTint"], background)
        attention = contrast(colors["attentionText"], background)
        assert small >= AA_SMALL_TEXT, (key, "secondaryOnTint", small)
        assert attention >= AA_SMALL_TEXT, (key, "attentionText", attention)
        tint_ratios[key] = {"secondary": round(small, 3), "attention": round(attention, 3)}
    # Canary checks: old secondary fails on tan and old coral pointer fails on paper.
    assert contrast("#566861", colors["tintTan"]) < AA_SMALL_TEXT
    assert contrast("#E8876C", colors["paper"]) < AA_SMALL_TEXT

    notice_count = 0
    repaired = 0
    per_screen = {}
    for name in NAMES:
        raw = (PLATES / (name + ".svg")).read_text(encoding="utf-8")
        phone = phone_markup(raw)
        # Sand notices are opaque authored solid-color geometry; only the
        # small x=61 secondary detail under each title is checked.
        matches = list(re.finditer(
            r'<rect x="35" y="(\d+)" width="362" height="(?:65|66|78|83|84)" '
            r'rx="(?:12|13)" fill="#F5E6D1"[^>]*/>', phone
        ))
        count_this = 0
        for rect in matches:
            y = int(rect.group(1))
            target_ys = (y + 47, y + 48)
            detail = re.search(
                rf'<text x="61" y="(?:{target_ys[0]}|{target_ys[1]})" fill="(#[0-9A-Fa-f]{{6}})" '
                rf'font-size="13"[^>]*>([^<]*)</text>', phone
            )
            if detail is None:
                # A few notice designs use no detail or a differently located
                # caption. They are not certified by this targeted geometry rule.
                continue
            notice_count += 1
            count_this += 1
            color = detail.group(1)
            actual = contrast(color, colors["tintSand"])
            assert color == colors["secondaryOnTint"], (
                name, "notice detail must use stronger tint contextual token", color
            )
            assert actual >= AA_SMALL_TEXT, (name, y, actual)
            repaired += 1
        per_screen[name] = count_this
    assert repaired == 22, ("Unexpected count of tinted notice details", repaired)
    return {"revision": tokens["revision"], "status": "PROVISIONAL_SOURCE_ONLY",
            "tintRatios": tint_ratios, "sandNoticeDetails": repaired,
            "primaryPlates": len(NAMES), "perScreenSandNoticeDetails": per_screen,
            "AndroidVerified": False, "ownerApproved": False}


if __name__ == "__main__":
    print(json.dumps(check(), indent=2))
