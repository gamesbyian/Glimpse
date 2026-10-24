#!/usr/bin/env python3
"""Build an offline, searchable, never-owner-approved Glimpse review candidate UI."""
from __future__ import annotations
import argparse,html,json
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
SRC=ROOT/"design/proposals/whole-product-review-selection.v1.json"
DEST=ROOT/"design/atlas/whole-product-review-selection.html"

def attr(s):return html.escape(str(s),quote=True)
def title(s):return html.escape(str(s))
def url(path):return "../proposals/"+path.removeprefix("design/proposals/")
def phone(rel,label,path):
    target=url(path); alt=f"Phone crop proposal for {label}, draft and unapproved"
    return (f'<a href="{attr(target)}" target="_blank" rel="noopener" title="Open annotated editable SVG">'
            f'<div class="phone"><img loading="lazy" width="1160" height="920" src="{attr(target)}" alt="{attr(alt)}"></div></a>')
def card(label,path,extra="",note=""):
    altlinks=" ".join(f'<a href="{attr(url(x))}" target="_blank" rel="noopener">{title(Path(x).stem.replace("-"," "))}</a>' for x in extra)
    return (f'<article class="item" data-filter="{attr((label+" "+path+" "+" ".join(extra)).lower())}">'
            f'<h3>{title(label)}</h3>{phone(url(path),label,path)}'
            f'<p class="path">{title(path)}</p>'
            f'<p class="flag">Draft reference · no owner approval</p>'
            +(f'<p class="note">{title(note)}</p>' if note else '')
            +(f'<div class="alternates"><b>Also compare:</b> {altlinks}</div>' if extra else '')+'</article>')
def build():
    d=json.loads(SRC.read_text(encoding="utf-8"))
    if not len(d["primarySurfaces"])==24 or not len(d["supportSurfaces"])==16:
        raise ValueError("Expected complete selection candidate")
    p=[]
    p.append('''<!doctype html><html lang="en"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>Glimpse | Whole-product owner review candidate</title><style>
:root{font:15px/1.5 system-ui,Arial,sans-serif;color:#153f46;background:#eee8dd}
*{box-sizing:border-box}body{margin:0}header{background:#153f46;color:#fff9ed;padding:24px max(18px,calc((100vw - 1200px)/2))}
header h1{font-size:clamp(25px,4vw,42px);line-height:1.15}header p{max-width:840px}
main{max-width:1250px;margin:auto;padding:16px}a{color:#17606b}a:focus-visible,input:focus-visible,summary:focus-visible{outline:3px solid #aa513c;outline-offset:2px}
.notice{border-left:5px solid #a95742;padding:15px 18px;background:#fff9ed;margin:8px 0 20px}
.toolbar{position:sticky;top:0;z-index:3;padding:12px 0;background:#eee8ddf5}
input{font:inherit;width:100%;padding:12px;border:2px solid #456a6b;border-radius:12px}
details{background:#fffaf1;border:1px solid #c3b6a4;border-radius:14px;margin:18px 0}
summary{padding:15px;cursor:pointer;font-weight:700;font-size:18px;background:#f5ecdf;border-radius:14px}
.gallery{display:grid;grid-template-columns:repeat(auto-fill,minmax(242px,1fr));gap:14px;padding:15px}
.item{border:1px solid #d1c5b5;background:#fff;border-radius:12px;padding:13px;min-width:0}
.item h3{font-size:17px;margin:2px 0 8px}.phone{width:221px;height:490px;overflow:hidden;position:relative;background:#f6f0e6;border-radius:12px;border:1px solid #cfbfad;margin:auto}
.phone img{position:absolute;max-width:none;width:1160px;height:920px;left:-8.25px;top:-7.7px;transform:scale(.55);transform-origin:0 0}
a:hover .phone{box-shadow:0 0 0 3px #2e7078}
.path{font:11px/1.4 ui-monospace,monospace;overflow-wrap:anywhere}.flag{font-size:12px;color:#97452c;font-weight:700}.note{font-size:12px;color:#536865}
.alternates{font-size:12px;border-top:1px solid #d2c4b3;padding-top:8px;line-height:2}.alternates a{display:block}
.question{margin:14px;background:#fff;padding:14px;border-radius:10px;border:1px solid #c9bcac}
.journey{padding:12px 16px;border-bottom:1px solid #e2d6c7}.journey b{display:block}.journey a{margin-right:12px}
@media(max-width:540px){.gallery{display:flex;flex-wrap:wrap;justify-content:center;padding:9px}main{padding:8px}.toolbar{position:static}}
@media print{header{color:#153f46;background:white}.toolbar{display:none}.item{break-inside:avoid}}
</style></head><body><header><p>GLIMPSE / DESIGN REVIEW SELECTION / DRAFT</p>
<h1>One product, one review route</h1><p>24 provisional primary screen candidates, 34 everyday support-state drawings,
23 complete interaction journey groups and eight human decisions. This is a curated way into the larger evidence library, never a record of approval.</p></header>
<main><div class="notice"><b>Every frame remains unapproved.</b> Source SVGs illustrate intent but do not establish Android behavior, accessible font reflow,
OS chooser fidelity or owner signoff. An older screen can remain a baseline pending a better current-generation treatment.
Source fingerprints live in <a href="whole-product-review-snapshot.v1.json">the candidate SHA-256 snapshot</a>;
the <a href="whole-set-evidence-atlas.html">exhaustive atlas</a> preserves every alternative.
The 60-question desk audit remains 18 source-level PASS / 8 ISSUE / 34 UNVERIFIED.</div>
<div class="toolbar"><label for="find">Filter screen or source</label>
<input id="find" type="search" placeholder="Home, share, 200%, photo, restore…" aria-label="Search review candidate screens"></div>''')
    p.append('<details open><summary>Primary app surfaces · 24 proposed choices</summary><div class="gallery">')
    for item in d["primarySurfaces"]:
        p.append(card(item["surfaceId"].replace("-"," ").title(),item["reviewCandidatePath"],
                      item["alternativesToDecide"],
                      item.get("reviewWarning", "Structural reference: inspect required/forbidden UI and adjacent actions before choosing")))
    p.append('</div></details>')
    p.append('<details><summary>Support and everyday states · 34 draft variants</summary><div class="gallery">')
    for family in d["supportSurfaces"]:
        for variant in family["variants"]:
            label=family["surfaceId"].replace("-"," ").title()+" / "+variant["stateId"].replace("-"," ").title()
            p.append(card(label,variant["reviewCandidatePath"],note=variant.get("reviewWarning", "Original support draft; native evidence unavailable")))
    p.append('</div></details>')
    p.append('<details><summary>Interaction stories · 23 complete P0/P1 packages, 137 frame sources</summary>')
    for flow in d["journeys"]:
        p.append(f'<div class="journey"><b>{title(flow["id"].upper()+" · "+flow["task"])}</b>')
        for i,path in enumerate(flow["frames"],1):
            p.append(f'<a href="{attr(url(path))}" target="_blank" rel="noopener">{i:02d}: {title(Path(path).stem.replace("-"," "))}</a>')
        p.append('</div>')
    p.append('</details>')
    p.append('<details open><summary>Eight product-owner decisions · deliberately unresolved</summary>')
    for q in d["explicitOwnerQuestions"]:
        p.append(f'<div class="question"><b>{title(q["id"]+" · "+q["subject"])}</b><p>{title(q["decision"])}</p></div>')
    p.append('</details>')
    p.append('''<p>Human review protocol: compare full 401×890 phone images and full annotated SVG, inspect
the relevant incoming/outgoing states, record observed defect versus preference, then approve only a uniquely
identified <em>whole set</em> of source hashes. Keep Android screenshots/hierarchy and owner feedback in separate evidence categories.</p>
</main><script>
const input=document.getElementById('find');const cards=[...document.querySelectorAll('.item')];
input.addEventListener('input',()=>{const q=input.value.toLowerCase().trim().split(/\\s+/).filter(Boolean);
for(const c of cards)c.hidden=!q.every(v=>c.dataset.filter.includes(v));
if(q.length)for(const section of document.querySelectorAll('details')){if(section.querySelector('.item:not([hidden])'))section.open=true;}});
</script></body></html>''')
    return "".join(p)+"\n"
def main():
    ap=argparse.ArgumentParser()
    ap.add_argument("--check",action="store_true")
    args=ap.parse_args()
    result=build()
    if args.check:
        assert DEST.read_text(encoding="utf-8")==result
    else:
        DEST.parent.mkdir(parents=True,exist_ok=True)
        DEST.write_text(result,encoding="utf-8")
    print("PASS: 24 primary candidate screens, 34 support variants, 23 journeys and 8 owner decisions (all draft)")
if __name__=="__main__":main()
