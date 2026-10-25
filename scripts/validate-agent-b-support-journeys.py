#!/usr/bin/env python3
"""Agent B source-level support, geometry and storyboard transition audit.

Drawings cannot prove 320dp/200% Android layout, TalkBack focus, navigation,
recipient delivery, image rights, provider persistence or human comprehension.
"""
from __future__ import annotations
import hashlib
import json
from pathlib import Path
import re
from xml.etree import ElementTree as ET

ROOT=Path(__file__).resolve().parents[1]
NS="{http://www.w3.org/2000/svg}"
def load(p):
    return json.loads((ROOT/p).read_text(encoding="utf8"))
support=load("design/proposals/support-coverage.v1.json")
p0=load("design/proposals/storyboard-coverage.v1.json")
p1=load("design/proposals/advanced-storyboard-coverage.v1.json")
scenarios=load("design/proposals/audit-scenario-evidence.v1.json")
def rel_phone(source):
    return source.split('<rect x="440"',1)[0]
def content_font_floor(phone):
    """Smallest in-app body/action SVG font, excluding status/proposal footer."""
    sizes=[]
    for node in re.findall(r'<text\b[^>]*>[^<]*</text>',phone):
        y=re.search(r'\by="([0-9.]+)"',node)
        font=re.search(r'font-size="([0-9.]+)"',node)
        if y and font and 195<=float(y.group(1))<810:
            sizes.append(float(font.group(1)))
    return min(sizes,default=None)

errors=[]
sources={}
task_buttons=0
card_buttons=0
toggle_rows=0
for family in support["screens"]:
    for state in family["states"]:
        name=f"{family['id']}--{state}"
        p=Path(f"design/proposals/support-plates/{name}.svg")
        raw=(ROOT/p).read_text(encoding="utf8")
        root=ET.fromstring(raw)
        if root.get("viewBox")!="0 0 1160 920": errors.append(f"{p}: wrong viewBox")
        if '<g id="support-body" transform="translate(0,-24)">' not in raw:
            errors.append(f"{p}: stale non-app decorative banner or missing layout")
        phone=rel_phone(raw)
        assert 'SCREEN PROPOSAL • NOT USER-APPROVED' in phone
        controls={r.get("data-control"):r for r in root.iter(NS+"rect") if r.get("data-control")}
        toggles=[r for r in root.iter(NS+"rect") if (r.get("data-control") or "").startswith("toggle-")]
        toggle_rows+=len(toggles)
        for row in toggles:
            label=row.get("data-action-label","")
            if not label or f">{label}</text>" not in phone:
                errors.append(f"{p}: full-row choice missing matching visible label")
            if float(row.get("data-hit-width","0"))<48 or float(row.get("data-hit-height","0"))<48:
                errors.append(f"{p}: tiny painted switch knob must not be only accessible target")
        if name=="notification-preferences--not-implemented" and toggles:
            errors.append(f"{p}: unavailable notification capability must not advertise enabled toggles")
        study_cards=[r for r in root.iter(NS+"rect") if (r.get("data-control") or "").startswith("card-")]
        card_buttons+=len(study_cards)
        for card in study_cards:
            label=card.get("data-action-label","")
            if f">{label}</text>" not in phone or not label:
                errors.append(f"{p}: focusable task card lacks visible action label: {label}")
            if float(card.get("data-hit-width","0"))<48 or float(card.get("data-hit-height","0"))<48:
                errors.append(f"{p}: task card has sub-48 study hit area: {label}")
        study_actions=[r for r in root.iter(NS+"rect") if (r.get("data-control") or "").startswith("task-")]
        task_buttons+=len(study_actions)
        if name!="storage-management--delete-confirm":
            if len({r.get("data-control") for r in study_actions})!=len(study_actions):
                errors.append(f"{p}: duplicate painted CTA identity")
            if len({r.get("data-focus-order") for r in controls.values()})!=len(controls):
                errors.append(f"{p}: duplicate focus order")
            for r in study_actions:
                label=r.get("data-action-label","")
                if not label or f">{label}</text>" not in phone:
                    errors.append(f"{p}: CTA has no matching visible label: {label}")
                if int(r.get("data-hit-width","0"))!=round(float(r.get("width","0"))) or int(r.get("data-hit-height","0"))!=round(float(r.get("height","0"))):
                    errors.append(f"{p}: study hit geometry disagrees with painted CTA")
        elif study_actions:
            errors.append(f"{p}: blocked deletion page must not leave legacy task CTAs focusable")
        is_modal=name=="storage-management--delete-confirm"
        required={"cancel-delete","confirm-delete"} if is_modal else {"back","nav-home","nav-discover","nav-collections","nav-automation"}
        if not required<=controls.keys():errors.append(f"{p}: absent required focus/hit regions {required-controls.keys()}")
        if is_modal and any(x.startswith("nav-") for x in controls):
            errors.append(f"{p}: modal background navigation still actionable")
        for key,shape in controls.items():
            width=float(shape.get("data-hit-width","0"))
            height=float(shape.get("data-hit-height","0"))
            if width<48 or height<48: errors.append(f"{p}:{key} sub-48 provisional study target {width}x{height}")
            if not shape.get("data-focus-order"):errors.append(f"{p}:{key} absent focus proposal")
        if 'fill="#667975"' in phone: errors.append(f"{p}: low-contrast legacy secondary color")
        if name=="report-problem--with-diagnostics":
            if "ON · review fields before sharing" not in phone:
                errors.append(f"{p}: ON diagnostic still says default-off")
        if name=="diagnostic-preview--expanded":
            if "ON · technical fields included" not in phone:
                errors.append(f"{p}: technical detail toggle and subtitle conflict")
        if name in ("diagnostic-preview--expanded","diagnostic-preview--minimal"):
            if "Photographs excluded" not in phone or "Image bytes never enter diagnostics" not in phone:
                errors.append(f"{p}: impossible photo attachment presented as opt-in")
        if name=="external-handler-fallback--chooser-cancelled":
            for truth in ("Share chooser closed", "Delivery not confirmed by Android",
                          "Delivery status cannot be verified for this"):
                if truth not in phone:errors.append(f"{p}: cancelled chooser asserted delivery {truth}")
            for false_result in ("nothing was sent", "No recipient received"):
                if false_result in phone:errors.append(f"{p}: falsely certain external delivery state {false_result}")
        if name=="beta-feedback--share-preview":
            for text in ("Review outgoing feedback","Screenshot: none selected","Diagnostics: off","Message: draft, not yet sent","Open Android Sharesheet"):
                if text not in phone:errors.append(f"{p}: lost outbound consent distinction {text}")
        if name=="storage-management--delete-confirm":
            for text in ("delete-modal-scrim","Example count; Android must calculate it.","Original provider photos stay intact.","Review active rotation first"):
                if text not in phone:errors.append(f"{p}: missing destructive preflight {text}")
        if name in ("crash-recovery--normal-exit","crash-recovery--suspected-crash"):
            if phone.count('>Continue</text>') != 1:
                errors.append(f"{p}: duplicate or missing recovery action")
        if name=="report-problem--with-diagnostics" and "Diagnostics selected, not sent" not in phone:
            errors.append(f"{p}: diagnostic selection indistinguishable from sent result")
        if name=="report-problem--draft" and "Nothing sent; diagnostics off" not in phone:
            errors.append(f"{p}: draft data-transmission truth")
        sources[str(p)]={"sha256":hashlib.sha256(raw.encode("utf8")).hexdigest(),
                         "actions":sorted(controls),"modal":is_modal,
                         "minBodyFont":content_font_floor(phone)}
assert len(sources)==34
groups=p0["groups"]+p1["groups"]
if len(p0["groups"])!=12 or len(p1["groups"])!=11 or len(groups)!=23:
    errors.append("Unexpected 23-journey inventory")
frames=[]
for group in groups:
    items=group["frames"]
    if not items:errors.append(f"{group['id']}: empty journey")
    for index,f in enumerate(items,start=1):
        if f.get("ordinal")!=index:errors.append(f"{group['id']}: broken storyboard ordinal {f.get('id')}")
        path=f["path"]
        if not (ROOT/path).is_file():errors.append(f"{group['id']}: missing frame {path}")
        else:
            root=ET.parse(ROOT/path).getroot()
            if root.get("viewBox")!="0 0 1160 920":errors.append(f"{group['id']}: invalid phone frame {path}")
        frames.append(path)
if len(frames)!=137 or len(set(frames))!=137: errors.append(f"Expected 82+55 unique frames; got {len(frames)}")

# Production B journey safety redlines: checks present, distinguish no-handler,
# and require explicit final confirmation. Static text checks only, not OS acts.
risk={
 "design/proposals/storyboards/x02/05-delete-confirm.svg":
   ["Remove Rainbows", "Cancel", "22 memberships removed", "Active shuffle pauses"],
 "design/proposals/advanced-storyboards/x19/04-restore-conflict.svg":
   ["Confirm safe merge", "Cancel", "No changes until you confirm",
    "Keep existing ID and 22 current photos", "4 providers need reconnecting"],
 "design/proposals/advanced-storyboards/x22/03-consent.svg":
   ["Review outgoing example", "Technical details", "Device model", "OFF · optional and reversible"],
 "design/proposals/advanced-storyboards/x22/04-chooser.svg":
   ["Open Android Sharesheet", "Copy report", "Save locally", "Recipient not chosen. Nothing was sent.",
    "Example fields only; app must show actual bytes.", "Source URIs and tokens: redacted"],
 "design/proposals/advanced-storyboards/x13/04-cancel.svg":
   ["Delivery unverified", "Delivery not confirmed"],
 "design/proposals/advanced-storyboards/x22/05-absent.svg":
   ["No compatible share target", "No supported handler"]
}
for rel, words in risk.items():
    payload=(ROOT/rel).read_text(encoding="utf8")
    for phrase in words:
        if phrase not in payload:errors.append(f"{rel}: missing journey truth {phrase}")
for rel, keys in (
    ("design/proposals/storyboards/x02/05-delete-confirm.svg", ("cancel-collection-removal","confirm-collection-removal")),
    ("design/proposals/advanced-storyboards/x19/04-restore-conflict.svg", ("cancel-restore","confirm-merge")),
    ("design/proposals/advanced-storyboards/x22/03-consent.svg", ("technical-details-off","device-model-off")),
    ("design/proposals/advanced-storyboards/x22/04-chooser.svg", ("copy-report","save-report","open-android-sharesheet"))
):
    root=ET.parse(ROOT/rel).getroot()
    controls={r.get("data-control"):r for r in root.iter(NS+"rect") if r.get("data-control")}
    for key in keys:
        shape=controls.get(key)
        if shape is None:errors.append(f"{rel}: missing visible {key}")
        elif min(float(shape.get("data-hit-width","0")),float(shape.get("data-hit-height","0")))<48:
            errors.append(f"{rel}: {key} under 48px static study")

after=(ROOT/"design/proposals/storyboards/x02/06-empty-after-removal.svg").read_text(encoding="utf8")
after_phone=after.split('<rect x="442"',1)[0]
for phrase in ("Collections", "Last collection removed", "No collections remaining",
               "Saved imported photos remain here", "Create a collection"):
    if phrase not in after_phone:errors.append("x02 removal left a ghost collection: "+phrase)
if ">Rainbows</text>" in after_phone:
    errors.append("x02 deleted Rainbows is still the active collection heading")
before=(ROOT/"design/proposals/storyboards/x02/05-delete-confirm.svg").read_text(encoding="utf8")
if 'id="collection-delete-modal"' not in before:
    errors.append("x02 destructive confirmation not visually modalized")
# Direct full-137-frame optical review found misleading state claims
# and one escaped 401px card title. These are current phone-side source rules,
# not evidence that the Android OS state has actually been interrogated.
frame_truth={
 "design/proposals/storyboards/x03/01-preview.svg": (
   ["Last Glimpse-acknowledged Home: p01"], ["Currently applied Home"]),
 "design/proposals/storyboards/x03/02-choose-target.svg": (
   ["Home selected"], ["Home ✓"]),
 "design/proposals/storyboards/x03/07-unsupported.svg": (
   ["Home selected"], ["Home ✓"]),
 "design/proposals/storyboards/x03/06-failed.svg": (
   ["Last Glimpse-acknowledged: p01"], ["Last applied photo remains"]),
 "design/proposals/storyboards/x04/07-stopped.svg": (
   ["No new wallpaper change requested by Glimpse"], ["The Home wallpaper remains unchanged"]),
 "design/proposals/storyboards/x08/01-home-active.svg": (
   ["Last Glimpse-acknowledged Home", "LAST ACK."], ["Current Home wallpaper","CURRENT HOME"]),
 "design/proposals/storyboards/x04/05-paused.svg": (
   ["No scheduled changes while paused"], ["Next eligible 00:16 · foreground only"]),
 "design/proposals/storyboards/x04/07-stopped.svg": (
   ["No scheduled changes while stopped"], ["Next eligible 00:16 · foreground only"]),
 "design/proposals/storyboards/x06/01-first.svg": (
   ["No previous", 'data-control="previous-unavailable"', 'aria-disabled="true"'],
   [">Previous</text>"]),
 "design/proposals/storyboards/x06/02-single.svg": (
   ["No previous", "No next", 'data-control="previous-unavailable"',
    'data-control="next-unavailable"', 'aria-disabled="true"'],
   [">Previous</text>", ">Next photo</text>"]),
 "design/proposals/storyboards/x06/07-last.svg": (
   ["No next", 'data-control="next-unavailable"', 'aria-disabled="true"'],
   [">Next photo</text>"]),
 "design/proposals/storyboards/x08/06-status-collapsed.svg": (
   ["LAST ACK."], ["CURRENT HOME"]),
 "design/proposals/advanced-storyboards/x16/01-rules.svg": (
   ["Enabled rules may change Home when eligible"], ["Only explicit Apply changes wallpaper"]),
 "design/proposals/advanced-storyboards/x16/02-conflict.svg": (
   ["Enabled rules may change Home when eligible"], ["Only explicit Apply changes wallpaper"]),
 "design/proposals/advanced-storyboards/x16/03-precedence.svg": (
   ["Enabled rules may change Home when eligible"], ["Only explicit Apply changes wallpaper"]),
 "design/proposals/advanced-storyboards/x16/05-simulate.svg": (
   ["Simulation never applies wallpaper"], ["Only explicit Apply changes wallpaper"]),
 "design/proposals/advanced-storyboards/x22/02-redacted.svg": (
   ["Review report example", "Example build v0.1 · API 34"],
   ["Review exact outgoing fields", "App v0.1 · Android API 34"]),
 "design/proposals/storyboards/x08/05-long-title.svg": (
   ["Our favourite holiday…", 'aria-label="Our favourite holiday photographs and travels"'],
   [">Our favourite holiday photographs...</text>"]),
 "design/proposals/advanced-storyboards/x19/05-restore-result.svg": (
   ["1,282 entries restored", "2 skipped"], ["1,284 entries restored"]),
 "design/proposals/advanced-storyboards/x15/04-confirm-exclude.svg": (
   ["12 selected for exclusion", "Imported files stay untouched"],
   ["12 excluded from rotation", "Excluding keeps imported files"])
}
for rel,(required,forbidden) in frame_truth.items():
    raw=(ROOT/rel).read_text(encoding="utf8")
    # Phone body only: right-side annotation doesn't count as visible proof.
    phone=raw.split('<rect x="442"',1)[0]
    ET.fromstring(raw)
    for phrase in required:
        if phrase not in phone:errors.append(f"{rel}: missing frame truth {phrase}")
    for phrase in forbidden:
        if phrase in phone:errors.append(f"{rel}: obsolete or false frame truth {phrase}")
# Every gesture-study frame must offer legible non-touch Grid parity.
for frame_id in ("01-rest","02-touch-down","03-drag-18","04-drag-36",
                 "05-snap-72","06-reverse-18","07-interrupted","08-last"):
    rel=f"design/proposals/storyboards/x05/{frame_id}.svg"
    phone=(ROOT/rel).read_text(encoding="utf8").split('<rect x="442"',1)[0]
    if ">View all</text>" not in phone or ">Grid ↗</text>" in phone:
        errors.append(f"{rel}: illegible Grid / View all non-gesture parity")
# Selected images use painted vector checkmarks. Glyph-based ✓ appears
# as tofu in CairoSVG and falsely looks like a missing image/selection.
for state in ("03-range-selection","04-confirm-exclude","05-restore"):
    path=ROOT/f"design/proposals/advanced-storyboards/x15/{state}.svg"
    phone=path.read_text(encoding="utf8").split('<rect x="442"',1)[0]
    if phone.count('aria-label="Selected"')!=3:
        errors.append(f"{path}: expected three vector selected badges")
    if ">✓</text>" in phone:
        errors.append(f"{path}: unsupported text-glyph selection marker")
if task_buttons!=36:errors.append(f"Expected 36 visible support task-pill controls, found {task_buttons}")
if card_buttons!=42:errors.append(f"Expected 42 action-captioned full-height task cards, found {card_buttons}")
if toggle_rows!=7:errors.append(f"Expected seven accessible full-row opt-in studies, found {toggle_rows}")
ss=scenarios["scenarios"]
if len(ss)!=56:errors.append("Unexpected audit scenario count")
for sc in ss:
    if sc.get("evidenceState")!="PICTURED":errors.append(f"Scenario not pictured: {sc['scenario']}")
    for path in sc.get("sourcePaths",[]):
        if not (ROOT/path).is_file(): errors.append(f"Scenario missing source {sc['scenario']} -> {path}")
# Risk probe: don't conflate static SVG preview with Android's actual dynamic font scaling.
small=[(k,v["minBodyFont"]) for k,v in sources.items() if v["minBodyFont"] is not None and v["minBodyFont"]<14]
if small:errors.append("Support body fonts under 14px: "+str(small))
print(json.dumps({"scope":"Agent B unapproved source-only audit",
 "supportStates":len(sources),"supportTaskControls":task_buttons,"supportTaskCards":card_buttons,"supportToggleRows":toggle_rows,"journeys":len(groups),"storyboardFrames":len(frames),"illustratedScenarios":len(ss),
 "supportMinimumBodyFontBelow14":len(small),
 "fontScaleVerdict":"320dp/200% real Android reflow unverified: source fonts and targets are only static reference geometry",
 "navigationVerdict":"data-control rectangles are visual geometry proposals, not native semantics/focus tests",
 "sourceSHA256":sources, "errors":errors},indent=2))
if errors:raise SystemExit(1)
