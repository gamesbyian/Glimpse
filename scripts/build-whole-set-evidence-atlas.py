#!/usr/bin/env python3
"""Build the offline, source-hashed Glimpse *evidence* atlas.

This exhaustive appendix is deliberately separate from the 39-source preferred
review spine. Browser rendering, source integrity, Android verification and
human approval are independent proof classes. No third-party Python packages.
"""
from __future__ import annotations

import argparse
import hashlib
import html
import json
import subprocess
from pathlib import Path
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
PROPOSALS = ROOT / "design/proposals"
ATLAS = ROOT / "design/atlas/whole-set-evidence-atlas.html"
INDEX = ROOT / "design/atlas/whole-set-evidence-index.json"
SVG_ROOT = "{http://www.w3.org/2000/svg}svg"


def read(relative):
    return json.loads((ROOT / relative).read_text(encoding="utf-8"))


def title_from(path):
    return Path(path).stem.replace("--", " / ").replace("-", " ").title()


def checked(path):
    if not path.startswith("design/proposals/") or not path.endswith(".svg"):
        raise ValueError("Bad proposal path: " + path)
    file = ROOT / path
    if not file.is_file():
        raise FileNotFoundError(path)
    raw = file.read_bytes()
    svg = ET.fromstring(raw)
    if svg.tag != SVG_ROOT or svg.get("viewBox") != "0 0 1160 920":
        raise ValueError("Bad SVG root or phone-space viewBox: " + path)
    return hashlib.sha256(raw).hexdigest()


def commit():
    try:
        return subprocess.check_output(
            ["git", "rev-parse", "HEAD"], cwd=ROOT, stderr=subprocess.DEVNULL,
            text=True, timeout=3,
        ).strip()
    except (OSError, subprocess.SubprocessError):
        return None


def build():
    spine = read("design/proposals/canonical-review-spine.v1.json")
    contract = read("docs/design/screen-contract.json")
    support = read("design/proposals/support-coverage.v1.json")
    core = read("design/proposals/core-state-evidence.v1.json")
    p0 = read("design/proposals/storyboard-coverage.v1.json")
    p1 = read("design/proposals/advanced-storyboard-coverage.v1.json")
    scenario_trace = read("design/proposals/audit-scenario-evidence.v1.json")
    if len(spine["groups"]) != 8 or sum(len(g["items"]) for g in spine["groups"]) != 39:
        raise ValueError("Preferred spine changed: reconcile the reviewer itinerary.")
    if len(contract["screens"]) != 24 or len(core["states"]) != 34:
        raise ValueError("Core contract/state count drift")
    chapters = []
    def add(name, proof_class, items, tier="APPENDIX"):
        if not items:
            raise ValueError("Empty chapter " + name)
        chapters.append({"name": name, "class": proof_class, "tier": tier, "items": items})
    def item(path, label=None, meaning=None):
        return {"path": path, "label": label or title_from(path), "meaning": meaning or ""}

    for group in spine["groups"]:
        add("Preferred route / " + group["title"], "preliminary-choice",
            [item(s["path"], meaning="Provisional preferred route; not approved") for s in group["items"]],
            "PREFERRED_DRAFT")
    add("Every contracted core surface (24)", "original-core",
        [item("design/proposals/plates/" + s["id"] + ".svg", s["id"].replace("-", " ").title())
         for s in contract["screens"]])
    critical = sorted((PROPOSALS / "critical-reel").glob("*.svg"))
    if len(critical) != 8:
        raise ValueError("Expected eight original critical reel variants")
    add("Original critical reel studies (8)", "original-reel",
        [item("design/proposals/critical-reel/" + p.name) for p in critical])
    support_items = [
        item("design/proposals/support-plates/" + s["id"] + "--" + state + ".svg")
        for s in support["screens"] for state in s["states"]
    ]
    if len(support_items) != 34:
        raise ValueError("Expected 34 support-state variants")
    add("Every original support variant (34)", "original-support", support_items)
    if any(s["evidenceLevel"] != "pictured-state" for s in core["states"]):
        raise ValueError("State mapping no longer declares 34 pictured-state sources")
    add("34 required named state illustrations", "named-state-evidence",
        [item(s["assetPath"], s["surfaceId"] + " / " + s["stateId"],
              s.get("interpretation", "")) for s in core["states"]])
    refinements = sorted((PROPOSALS / "state-refinements").glob("*.svg"))
    if len(refinements) != 25:
        raise ValueError("Expected 25 supplemental state refinements")
    add("Supplemental refinements (25)", "supplemental",
        [item("design/proposals/state-refinements/" + p.name) for p in refinements])
    if sum(len(g["frames"]) for g in p0["groups"]) != 82 or len(p0["groups"]) != 12:
        raise ValueError("P0 storyboard count drift")
    for group in p0["groups"]:
        add("P0 " + group["id"].upper() + " / " + group["title"], "P0-storyboard",
            [item(f["path"], f.get("caption"), f.get("invariant")) for f in group["frames"]])
    if sum(len(g["frames"]) for g in p1["groups"]) != 55 or len(p1["groups"]) != 11:
        raise ValueError("P1 storyboard count drift")
    for group in p1["groups"]:
        add("P1 " + group["id"].upper() + " / " + group["title"], "P1-storyboard",
            [item(f["path"], f.get("caption"), f.get("invariant")) for f in group["frames"]])
    revisions = sorted((PROPOSALS / "audit-revisions").glob("*.svg"))
    add("Audit-driven revision alternatives (R01–R08)", "audit-revision",
        [item("design/proposals/audit-revisions/" + p.name) for p in revisions])
    # A source may appear in multiple review chapters, but the baseline inventory
    # is never inflated by repeated references to that same SVG.
    hashes = {}
    for group in chapters:
        for it in group["items"]:
            if it["path"] not in hashes:
                hashes[it["path"]] = checked(it["path"])
            it["sourceSHA256"] = hashes[it["path"]]
    scenario_status = scenario_trace["counts"]
    if scenario_status["totalRequired"] != 56 or sum(
        scenario_status[key] for key in ("PICTURED","PARTIAL","MISSING_SPECIFIC")
    ) != 56:
        raise ValueError("Audit 56-scenario coverage count drift")
    for case in scenario_trace["scenarios"]:
        for rel in case["sourcePaths"]:
            if rel not in hashes:
                raise ValueError("Scenario trace refers to unindexed SVG: " + rel)
    expected = 203 + 25 + len(revisions)
    if len(hashes) != expected:
        raise ValueError(f"Unexpected unique source count {len(hashes)}; expected {expected}")
    for group in chapters[:8]:
        for it in group["items"]:
            if it["path"] not in hashes:
                raise ValueError("Missing preferred source " + it["path"])
    return {
        "schemaVersion": 1,
        "status": "ALL_DRAFT_SOURCE_EVIDENCE_NOT_HUMAN_APPROVED",
        "sourceCommit": commit(),
        "auditCycleA": {"PASS_SOURCE_ONLY": 18, "ISSUE": 8, "UNVERIFIED": 34},
        "counts": {"baseline": 203, "core": 24, "criticalReel": 8,
                   "support": 34, "P0": 82, "P1": 55, "supplemental": 25,
                   "auditRevisionAlternatives": len(revisions),
                   "preferredRoute": 39, "uniqueEvidenceSources": len(hashes),
                   "namedStateSlots": 34},
        "scenarioCoverage": scenario_status,
        "reviewGaps": [
            {"lane": x["lane"], "scenario": x["scenario"],
             "state": x["evidenceState"], "needed": x["nextProof"]}
            for x in scenario_trace["scenarios"] if x["evidenceState"] != "PICTURED"
        ],
        "readme": "Preferred is a proposed review sequence, not selected/approved native UX. "
                  "Appendix may contain superseded alternatives. Every SHA-256 hashes SVG source bytes, not rendered pixels.",
        "chapters": chapters,
    }


CSS = """
:root{font:15px/1.5 system-ui,Arial,sans-serif;color:#153f46;background:#eee9e1}
*{box-sizing:border-box}body{margin:0}header{background:#153f46;color:#fff8ee;padding:25px max(20px,calc((100vw - 1230px)/2))}
h1{font-size:clamp(26px,5vw,42px);line-height:1.15;margin:.15em 0}
header p{max-width:850px}main{max-width:1230px;margin:auto;padding:18px}
.notice{border-left:4px solid #b85e3f;background:#fff8eb;padding:15px 19px;margin:14px 0 23px}
.controls{position:sticky;top:0;background:#eee9e1ed;padding:10px 0;z-index:2;display:flex;gap:10px;align-items:center;flex-wrap:wrap}
input{padding:12px;border:2px solid #517171;border-radius:10px;font:inherit;flex:1;min-width:215px}
details{margin:15px 0;border:1px solid #c6baa8;border-radius:14px;background:#fffaf1;overflow:hidden}
summary{padding:13px 17px;cursor:pointer;font-weight:700;background:#f8f0e4}
details[data-tier="PREFERRED_DRAFT"] summary{background:#deebe8}
.cards{display:grid;grid-template-columns:repeat(auto-fill,minmax(221px,1fr));gap:13px;padding:15px}
article{border:1px solid #c7b7a5;border-radius:12px;background:white;padding:9px;min-width:0}
article h3{font-size:14px;line-height:1.25;margin:5px 0 8px;min-height:34px}
.crop{width:201px;height:445px;position:relative;overflow:hidden;border:1px solid #d4c8ba;background:#f6f0e6;margin:auto;border-radius:10px}
.crop img{position:absolute;width:1160px;height:920px;max-width:none;top:-7px;left:-7.5px;transform:scale(.5);transform-origin:top left}
.path{font:11px/1.4 ui-monospace,monospace;overflow-wrap:anywhere;margin:6px 0}
button{border:1px solid #205760;background:#fff;border-radius:8px;padding:8px 10px;color:#13404c;cursor:pointer;font:inherit}
button:focus-visible,a:focus-visible{outline:3px solid #c05d3c;outline-offset:2px}
a{color:#165963}article a{display:block;text-decoration:none}article a:hover .crop{box-shadow:0 0 0 3px #328087}
article p{font-size:12px;margin:6px 0;min-height:18px}
.actions{display:flex;gap:8px;margin-top:8px}
small,.muted{color:#506e6a}
#compare{background:#fff;border:2px solid #1d5960;border-radius:14px;padding:15px;margin:13px 0}
#compare .cards{display:flex;flex-wrap:wrap;gap:20px}
[hidden]{display:none!important}
@media(max-width:500px){main{padding:9px}.cards{justify-content:center;display:flex;flex-wrap:wrap;padding:9px}.controls{position:static}}
"""


def render(doc):
    pieces = ['<!doctype html><html lang="en"><head><meta charset="utf-8">',
              '<meta name="viewport" content="width=device-width, initial-scale=1">',
              '<title>Glimpse | Complete draft visual evidence atlas</title>',
              '<style>', CSS, '</style></head><body>',
              '<header><p>GLIMPSE · FULL EVIDENCE INDEX · NOT AN APPROVAL PACKET</p>',
              '<h1>Every reference. One inspection surface.</h1>',
              '<p>A preliminary 39-source preferred route followed by all original, supplemental, storyboard and audit-revision sources. '
              'Use the comparison tray to inspect competing phone drawings at the same scale. '
              'Click an image for its full editable annotation-rich SVG.</p></header>',
              '<main><div class="notice"><b>All draft. None owner-approved.</b> '
              'This is an exhaustive source evidence inventory, not a coherent approved visual specification. '
              'Alternatives in the appendix may conflict. Rendered browser previews are not native Android screenshots. '
              'Source hashes record exact SVG bytes, not a 401×890 optical PASS. '
              'Audit cycle A: 18 source passes, 8 issues, 34 unverified.</div>',
              '<p><b>Source commit:</b> <code>',
              html.escape(doc["sourceCommit"] or "unknown / run from Git checkout for pinned SHA"),
              '</code> · <b>Unique SVG references:</b> ', str(doc["counts"]["uniqueEvidenceSources"]),
              ' · <b>Original baseline:</b> 203 · <b>Supplemental:</b> 25 · <b>Audit alternatives:</b> ',
              str(doc["counts"]["auditRevisionAlternatives"]), '</p>',
              '<p><b>Audit scenarios:</b> ',
              str(doc["scenarioCoverage"]["PICTURED"]), ' pictured structurally / ',
              str(doc["scenarioCoverage"]["PARTIAL"]), ' partial / ',
              str(doc["scenarioCoverage"]["MISSING_SPECIFIC"]), ' missing-specific. ',
              'These are source-level categories, not native or visual scores. ',
              '<a href="../proposals/audit-scenario-evidence.v1.json">Trace each of the 56 cases</a>.</p>',
              '<p><a href="canonical-review-spine.html">39-frame preliminary itinerary</a> · ',
              '<a href="whole-set-evidence-index.json">Full indexed source SHA-256 evidence</a> · ',
              '<a href="../../docs/design/LATEST-GENERATION-HUMAN-REVIEW-GATES-2026-10-23.md">Review/approval gates</a></p>',
              '<div class="controls"><label for="find">Find state or asset</label>',
              '<input id="find" type="search" placeholder="e.g. lock, R07, offline, crop, p12c" aria-label="Filter review frames">',
              '<button id="clear" type="button">Clear search</button>',
              '<span id="shown" aria-live="polite"></span></div>',
              '<section id="compare" hidden aria-label="Two-phone comparison"><h2>Side-by-side comparison</h2>',
              '<p>At most two. Same 401×890 phone crop proportions. Select a third to replace the oldest.</p>',
              '<div class="cards" id="compared"></div><button type="button" id="reset">Clear comparison</button></section>']
    serial = 0
    for g in doc["chapters"]:
        label = html.escape(g["name"])
        tier = html.escape(g["tier"])
        pieces += [f'<details data-tier="{tier}"', ' open' if g["tier"] == "PREFERRED_DRAFT" else '',
                   f'><summary>{label} · {len(g["items"])} frames</summary><div class="cards">']
        for it in g["items"]:
            serial += 1
            path = it["path"]
            rel = "../proposals/" + path.removeprefix("design/proposals/")
            label_ = html.escape(it["label"] or title_from(path))
            rel_ = html.escape(rel, quote=True)
            path_ = html.escape(path)
            digest = it["sourceSHA256"]
            meaning = html.escape(it.get("meaning") or "Draft reference; inspect visual/semantic fidelity")
            find = html.escape((g["name"] + " " + it["label"] + " " + path + " " + (it.get("meaning") or "")).lower(), quote=True)
            pieces += [f'<article data-search="{find}"><h3>{label_}</h3>',
                       f'<a href="{rel_}" target="_blank" rel="noopener" title="Open full source">',
                       '<div class="crop">',
                       f'<img loading="lazy" src="{rel_}" alt="Phone-region crop of {label_}"></div></a>',
                       f'<p>{meaning}</p><div class="path">{path_}</div>',
                       f'<div class="path" title="SVG SHA-256">SHA-256: {digest[:15]}…</div>',
                       '<div class="actions">',
                       f'<button class="pick" type="button" data-src="{rel_}" data-label="{label_}" data-path="{path_}">Compare</button>',
                       f'<button class="copy" type="button" data-copy="{path_}@sha256:{digest}">Copy reference</button>',
                       '</div></article>']
        pieces += ['</div></details>']
    pieces += ['<p class="muted">Coverage inventory only. Approve exact selected source SHAs in a separate owner signoff. '
               'Validate final UI with Android device screenshots, gestures, screen-reader output and OS handoff behavior.</p>',
               '</main><script>',
               """(() => {
const input=document.getElementById('find'), shown=document.getElementById('shown');
const chapters=[...document.querySelectorAll('details')];
function filter(){
 const query=input.value.trim().toLowerCase(), terms=query.split(/\\s+/).filter(Boolean);
 let count=0;
 for(const section of chapters){
  let visible=0;
  for(const card of section.querySelectorAll('article')){
   const pass=terms.every(t=>card.dataset.search.includes(t));
   card.hidden=!pass; if(pass){visible++;count++}
  }
  section.hidden=!visible;
  if(query&&visible)section.open=true;
 }
 shown.textContent=count+' frame entries';
}
input.addEventListener('input',filter);
document.getElementById('clear').addEventListener('click',()=>{input.value='';filter()});
let selected=[];
const panel=document.getElementById('compare'), out=document.getElementById('compared');
function draw(){
 panel.hidden=!selected.length;
 out.replaceChildren();
 for(const data of selected){
  const card=document.createElement('article'), title=document.createElement('h3');
  title.textContent=data.label;card.append(title);
  const crop=document.createElement('div');crop.className='crop';
  const img=document.createElement('img');img.src=data.src;img.alt=data.label;crop.append(img);card.append(crop);
  const path=document.createElement('p');path.className='path';path.textContent=data.path;card.append(path);
  out.append(card);
 }
}
document.querySelectorAll('.pick').forEach(b=>b.addEventListener('click',()=>{
 const next={src:b.dataset.src,label:b.dataset.label,path:b.dataset.path};
 selected=selected.filter(x=>x.path!==next.path).slice(-1);selected.push(next);draw();
 panel.scrollIntoView({behavior:'smooth',block:'start'});
}));
document.getElementById('reset').addEventListener('click',()=>{selected=[];draw()});
document.querySelectorAll('.copy').forEach(b=>b.addEventListener('click',async()=>{
 try{await navigator.clipboard.writeText(b.dataset.copy);b.textContent='Copied'}
 catch(e){b.textContent='Clipboard unavailable: select source path'}
}));
filter();
})();""",
               '</script></body></html>']
    return "".join(pieces) + "\n"


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--check", action="store_true", help="validate source and manifests without writing")
    args = parser.parse_args()
    doc = build()
    page = render(doc)
    if not args.check:
        ATLAS.write_text(page, encoding="utf-8")
        INDEX.write_text(json.dumps(doc, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    print("PASS: 203 baseline, 25 supplemental, %s revisions, %s unique files; "
          "39 preliminary preferred route entries; 34 state slots; no approval"
          % (doc["counts"]["auditRevisionAlternatives"],doc["counts"]["uniqueEvidenceSources"]))


if __name__ == "__main__":
    main()
