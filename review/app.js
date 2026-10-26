"use strict";
/* Glimpse Review Studio: static, local-first editorial feedback. Nothing writes to GitHub. */
(function () {
const $=id=>document.getElementById(id);
const esc=s=>String(s??"").replace(/[&<>"']/g,c=>({"&":"&amp;","<":"&lt;",">":"&gt;",'"':"&quot;","'":"&#39;"}[c]));
const nice=s=>String(s??"").replace(/^.*\//,"").replace(/\.svg$/,"").replace(/[-_]/g," ").replace(/\b\w/g,c=>c.toUpperCase());
const validPath=p=>typeof p==="string"&&p.startsWith("design/proposals/")&&p.endsWith(".svg")&&!p.includes("..")&&!p.includes("\\")&&!p.includes("?")&&!p.includes("#");
const src=p=>validPath(p)?"../"+p:"";
const names={primary:"Primary screens",support:"Support states",journey:"Connected journeys",library:"All source artwork",question:"Owner decisions"};
const reviewKey="glimpse.review.v1.gamesbyian.Glimpse";
const six=["home","reel","grid","photo-detail","collections","share"];
const questionPaths={
 D01:["home","design/proposals/audit-revisions/r01-photo-home-crescent.svg"],
 D02:["reel"],
 D03:["grid"],
 D04:["home","photo-detail"],
 D05:["quick-import","design/proposals/audit-revisions/r04-import-offline-17-of-22.svg"],
 D06:["share","design/proposals/audit-revisions/r06-share-dismissed.svg"],
 D07:["backup-and-restore"],
 D08:["home","collections","reel","grid"]
};
const questionChoices={
 D01:["Use tappable crescent","Use conventional card","Revise comparison"],
 D02:["Keep five-window paper reel","Revise physical reel","Try a different selector"],
 D03:["Prefer three-column Grid","Prefer four-column Grid","Change action hierarchy"],
 D04:["State distinctions are clear","Clarify applied/selected/unknown"],
 D05:["Recovery states are clear","Revise offline/provider recovery"],
 D06:["OS and scheduling claims are clear","Revise OS-owned flow claims"],
 D07:["Restore stages are clear","Revise consequences/recovery"],
 D08:["One coherent visual language","Revise cross-screen consistency"]
};
let data=null, records={}, activeList=[], current={section:"primary",id:"home",option:0,compare:false,full:false,peek:false,pinning:false,pin:null}, review, build={};
let toastTimeout=0,saveTimeout=0;
const fresh=()=>({schema:1,createdAt:new Date().toISOString(),items:{},questions:{},notes:{},general:{},chosen:{}});
try {review=JSON.parse(localStorage.getItem(reviewKey)||"null")||fresh(); if(review.schema!==1) review=fresh()} catch(e){review=fresh()}
function notify(message){const t=$("toast");t.textContent=message;t.classList.add("visible");clearTimeout(toastTimeout);toastTimeout=setTimeout(()=>t.classList.remove("visible"),2900)}
function save(){try{localStorage.setItem(reviewKey,JSON.stringify(review));$("saveIndicator").textContent="Saved locally · export to share";}catch(e){$("saveIndicator").textContent="Browser storage unavailable · export now";}updateCount()}
function debounceSave(){clearTimeout(saveTimeout);saveTimeout=setTimeout(save,180)}
function itemKey(it){return it.kind+":"+it.id}
function record(){return records[current.section]?.find(x=>x.id===current.id)}
function hash(path){return data.digests.get(path)||"UNTRACKED"}
function option(path,label){return {path,label:label||nice(path),sha:hash(path)}}
function startData(selection,snapshot,atlas){
 data={selection,snapshot,atlas,digests:new Map()};
 for(const s of snapshot.sources)data.digests.set(s.path,s.sourceSHA256);
 for(const c of atlas.chapters||[])for(const i of c.items||[])if(validPath(i.path))data.digests.set(i.path,i.sourceSHA256);
 records.primary=selection.primarySurfaces.map(p=>({
  kind:"primary",id:p.surfaceId,title:nice(p.surfaceId),subtitle:p.reviewWarning||"Candidate screen",
  options:[option(p.reviewCandidatePath,"Recommended · "+nice(p.reviewCandidatePath)),...(p.alternativesToDecide||[]).filter(validPath).map((v,i)=>option(v,"Option "+(i+2)+" · "+nice(v)))],
  evidence:p.visualEvidence||"",warning:p.reviewWarning||"",decision:p.candidateState
 }));
 records.support=selection.supportSurfaces.flatMap(group=>group.variants.map(v=>({
  kind:"support",id:group.surfaceId+"/"+v.stateId,title:nice(group.surfaceId)+" / "+nice(v.stateId),subtitle:v.reviewWarning||"Everyday support state",
  options:[option(v.reviewCandidatePath,"Current proposal")],evidence:v.reviewWarning||"",warning:"",decision:v.candidateState
 })));
 records.journey=selection.journeys.map(j=>({
  kind:"journey",id:j.id,title:j.id.toUpperCase()+" · "+j.task,subtitle:"Connected story · "+j.frames.length+" frames in source order",
  options:j.frames.filter(validPath).map((p,i)=>option(p,"Step "+(i+1)+" · "+nice(p))),frames:true,
  evidence:"A source-authored sequence, not a native Android recording.",warning:""
 }));
 const seen=new Map();
 for(const ch of atlas.chapters||[])for(const a of ch.items||[])if(validPath(a.path)&&!seen.has(a.path))seen.set(a.path,{kind:"library",id:a.path,title:a.label||nice(a.path),subtitle:ch.name,options:[option(a.path,"Source")],evidence:a.meaning||"",warning:""});
 // Also retain every selected/alternative source, including the coordinated CC0 set.
 for(const p of selection.primarySurfaces)for(const path of [p.reviewCandidatePath,...p.alternativesToDecide])if(validPath(path)&&!seen.has(path))seen.set(path,{kind:"library",id:path,title:nice(path),subtitle:"Owner review source",options:[option(path,"Source")],evidence:"",warning:""});
 records.library=[...seen.values()];
 records.question=selection.explicitOwnerQuestions.map(q=>{
  const paths=[];
  for(const value of questionPaths[q.id]||[]){
   const primary=records.primary.find(p=>p.id===value);
   if(primary){for(const o of primary.options.slice(0,2))paths.push(o.path);}
   else if(validPath(value))paths.push(value);
  }
  const unique=[...new Set(paths)];
  return {kind:"question",id:q.id,title:q.id+" · "+q.subject,subtitle:q.decision,options:unique.map((p,i)=>option(p,i===0?"Proposed":"Compare "+(i+1)+" · "+nice(p))),evidence:q.decision,warning:""};
 });
 if(!records.primary.length||!records.support.length||!records.journey.length)throw Error("Incomplete proposal records");
 const search=new URL(location.href).searchParams;
 const k=search.get("view"),id=search.get("id");
 if(k&&records[k]&&records[k].some(x=>x.id===id)){current.section=k;current.id=id;}
 else {current.section="primary";current.id="home";}
 const savedOption=search.get("option");
 current.option=Math.max(0,Math.min(25,parseInt(savedOption||"0",10)||0));
 bind();
 render();
 fetch("build.json",{cache:"no-store"}).then(r=>r.ok?r.json():{}).then(x=>{build=x;updateCount()}).catch(()=>{});
}
function bind(){
 $("sectionTabs").addEventListener("click",e=>{const btn=e.target.closest("[data-section]");if(btn)changeSection(btn.dataset.section)});
 $("search").addEventListener("input",renderNavigation);
 $("needsAttention").addEventListener("change",renderNavigation);
 $("items").addEventListener("click",e=>{const b=e.target.closest("[data-id]");if(b)navigate(current.section,b.dataset.id)});
 $("prevItem").addEventListener("click",()=>stepItem(-1));$("nextItem").addEventListener("click",()=>stepItem(1));
 $("options").addEventListener("click",e=>{const b=e.target.closest("[data-option]");if(b)changeOption(+b.dataset.option)});
 $("peek").addEventListener("pointerdown",()=>peek(true));$("peek").addEventListener("pointerup",()=>peek(false));$("peek").addEventListener("pointercancel",()=>peek(false));$("peek").addEventListener("pointerleave",()=>peek(false));
 $("compare").addEventListener("click",()=>{current.compare=!current.compare;current.peek=false;renderStage()});
 $("fullSource").addEventListener("click",()=>{current.full=!current.full;current.pinning=false;renderStage()});
 $("fullscreen").addEventListener("click",toggleFullscreen);
 $("openSource").addEventListener("click",()=>{const o=currentOption();if(o)window.open(src(o.path),"_blank","noopener,noreferrer")});
 $("reviewControls").addEventListener("click",e=>{const b=e.target.closest("[data-status]");if(b)markStatus(b.dataset.status);const p=e.target.closest("[data-use]");if(p)chooseOption()});
 $("decisionPanel").addEventListener("click",e=>{const b=e.target.closest("[data-choice]");if(b)chooseQuestion(b.dataset.choice)});
 $("commentList").addEventListener("click",e=>{const b=e.target.closest("[data-remove]");if(b)removePin(+b.dataset.remove)});
 $("generalComment").addEventListener("input",e=>{const it=record();if(it){review.general[itemKey(it)]=e.target.value;debounceSave();}});
 $("startPin").addEventListener("click",()=>{current.pinning=!current.pinning;current.full=false;renderStage();notify(current.pinning?"Tap the phone to place your comment":"Pin mode off")});
 $("screens").addEventListener("click",screenClick);
 $("cancelPin").addEventListener("click",cancelPin);$("savePin").addEventListener("click",savePin);
 $("copyReference").addEventListener("click",()=>{const o=currentOption();if(o)copyText(o.path+"@sha256:"+o.sha)});
 $("openBrowse").addEventListener("click",()=>togglePanel("browse",true));$("closeBrowse").addEventListener("click",()=>togglePanel("browse",false));
 $("openInspector").addEventListener("click",()=>togglePanel("inspector",true));$("closeInspector").addEventListener("click",()=>togglePanel("inspector",false));
 $("backdrop").addEventListener("click",closePanels);
 $("openDashboard").addEventListener("click",openOverview);$("closeOverview").addEventListener("click",()=>$("overviewDialog").close());
 $("exportButton").addEventListener("click",exportJSON);$("exportOverview").addEventListener("click",exportJSON);
 $("copySummary").addEventListener("click",()=>copyText(summary()));
 $("importButton").addEventListener("click",()=>$("importFile").click());
 $("importFile").addEventListener("change",importReview);
 $("openLegacy").addEventListener("click",()=>window.open("../design/atlas/whole-set-evidence-atlas.html","_blank","noopener,noreferrer"));
 document.addEventListener("keydown",keys);
}
function togglePanel(name,on){document.body.classList.toggle(name+"-open",on);$(name==="browse"?"openBrowse":"openInspector")?.setAttribute("aria-expanded",String(on))}
function closePanels(){document.body.classList.remove("browse-open","inspector-open")}
function changeSection(section){const first=records[section]?.[0];if(!first)return;current.section=section;current.id=first.id;current.option=0;current.compare=false;current.full=false;current.pinning=false;$("search").value="";render();closePanels()}
function navigate(section,id){if(!records[section]?.some(x=>x.id===id))return;current.section=section;current.id=id;current.option=0;current.compare=false;current.full=false;current.pinning=false;render();closePanels()}
function list(){let arr=records[current.section]||[];const q=$("search").value.toLowerCase().trim();if(q)arr=arr.filter(x=>(x.title+" "+x.id+" "+x.subtitle+" "+x.options.map(o=>o.path).join(" ")).toLowerCase().includes(q));if($("needsAttention").checked)arr=arr.filter(x=>{if(x.kind==="question")return !review.questions[x.id]?.choice;return (review.items[itemKey(x)]?.status||"unreviewed")!=="accepted"});return arr}
function stepItem(delta){const arr=records[current.section];let i=arr.findIndex(x=>x.id===current.id);const next=arr[(i+delta+arr.length)%arr.length];if(next)navigate(current.section,next.id)}
function changeOption(n){const it=record();if(!it)return;current.option=Math.max(0,Math.min(it.options.length-1,n));current.peek=false;current.pinning=false;render(); }
function currentOption(){const it=record();return it?.options[Math.min(current.option,it.options.length-1)]}
function targetOption(){const it=record();if(!it)return null;return it.options[(current.option===0&&it.options.length>1)?1:0]}
function peek(on){const it=record();if(!it||it.options.length<2||current.compare)return;current.peek=on;renderStage()}
function keys(e){
 if(e.target.closest("input,textarea,select,[contenteditable]")||$("overviewDialog").open)return;
 if(e.key==="ArrowRight"){e.preventDefault();changeOption(current.option+1)}
 if(e.key==="ArrowLeft"){e.preventDefault();changeOption(current.option-1)}
 if(e.key.toLowerCase()==="j"){e.preventDefault();stepItem(-1)}
 if(e.key.toLowerCase()==="k"){e.preventDefault();stepItem(1)}
 if(e.key.toLowerCase()==="f"){e.preventDefault();toggleFullscreen()}
 if(e.key===" "&&!e.repeat){e.preventDefault();peek(true)}
 if(e.key==="Escape"){current.pinning=false;cancelPin();closePanels();renderStage()}
}
document.addEventListener("keyup",e=>{if(e.key===" "&&current.peek){peek(false)}});
function toggleFullscreen(){const el=$("work");if(document.fullscreenElement){document.exitFullscreen().catch(()=>{});}else if(el.requestFullscreen){el.requestFullscreen().catch(()=>notify("Full screen unavailable in this browser"))}}
function render(){
 const it=record();if(!it)return;
 current.option=Math.min(current.option,it.options.length-1);
 document.querySelectorAll("#sectionTabs button").forEach(b=>b.classList.toggle("active",b.dataset.section===current.section));
 $("categoryName").textContent=names[it.kind].toUpperCase();
 $("screenTitle").textContent=it.title;
 $("screenSubtitle").textContent=it.subtitle;
 let arr=records[it.kind];const i=arr.indexOf(it);
 $("itemPosition").textContent=(i+1)+" / "+arr.length;
 $("prevItem").disabled=arr.length<2;$("nextItem").disabled=arr.length<2;
 $("options").innerHTML=it.options.map((o,n)=>'<button type="button" data-option="'+n+'" class="'+(n===current.option?'active':'')+'" aria-pressed="'+(n===current.option)+'" title="'+esc(o.path)+'">'+esc(it.frames?o.label:n===0?"Recommended · "+nice(o.path):o.label)+'</button>').join("");
 $("compare").disabled=it.options.length<2;$("peek").disabled=it.options.length<2;
 $("compare").setAttribute("aria-pressed",String(current.compare));
 $("fullSource").setAttribute("aria-pressed",String(current.full));
 const params=new URLSearchParams({view:current.section,id:current.id,option:String(current.option)});
 history.replaceState(null,"","?"+params.toString());
 renderNavigation();renderStage();renderInspector();updateCount();
}
function renderNavigation(){
 const shown=list();$("resultCount").textContent=shown.length+" of "+records[current.section].length+" entries";
 $("items").innerHTML=shown.map((it,i)=>{
  const key=itemKey(it),st=it.kind==="question"?(review.questions[it.id]?.choice?"accepted":"unreviewed"):(review.items[key]?.status||"unreviewed");
  return '<button type="button" class="navitem '+(it.id===current.id?"current":"")+'" data-id="'+esc(it.id)+'" aria-current="'+(it.id===current.id?"page":"false")+'"><span class="nav-index">'+(i+1).toString().padStart(2,"0")+'</span><span class="nav-copy"><strong>'+esc(it.title)+'</strong><small>'+esc(it.kind==="journey"?it.options.length+" frames":it.kind==="primary"?it.options.length+" options":it.subtitle.slice(0,38))+'</small></span><span class="status-dot '+esc(st)+'" aria-label="'+esc(st)+'"></span></button>'
 }).join("");
}
function renderStage(){
 const it=record();if(!it)return;
 const effective=(current.peek?targetOption():currentOption())||it.options[0];
 const second=current.compare&&it.options.length>1?targetOption():null;
 $("screens").className="screens"+(current.compare?" side-by-side":"")+(current.full?" full-art":"");
 const frame=(opt,idx)=>{
   if(!opt)return "";
   const key=itemKey(it);
   const notes=(review.notes[key]||[]).filter(n=>n.path===opt.path&&n.sha===opt.sha);
   const markers=current.full?"":notes.map((n,i)=>'<button class="pin" type="button" style="left:'+Math.min(99,Math.max(1,Number(n.x)))+'%;top:'+Math.min(99,Math.max(1,Number(n.y)))+'%" title="'+esc(n.text)+'" aria-label="Pinned '+esc(n.severity)+' comment">'+(i+1)+'</button>').join("");
   return '<div class="frame" data-frame="'+idx+'" data-path="'+esc(opt.path)+'" aria-label="'+esc(it.title+', '+opt.label)+'"><img src="'+esc(src(opt.path))+'" alt="'+esc(it.title+', draft '+opt.label)+'" draggable="false">'+markers+(current.pinning&&idx===0&&!current.full?'<div class="pin-mode" aria-label="Click to place a new comment"></div>':"")+'</div>';
 };
 $("screens").innerHTML=frame(effective,0)+frame(second,1);
 $("sourcePosition").textContent="Option "+(current.option+1)+"/"+it.options.length+(current.compare?" · comparison":"")+(current.peek?" · peek":"");
 $("sourceHash").textContent=effective.sha;
 $("sourcePath").textContent=effective.path;
 $("evidenceChip").textContent=it.kind==="journey"?"STORYBOARD · NOT NATIVE":it.kind==="library"?"HISTORICAL / DRAFT SOURCE":"EDITABLE SVG · NOT APPROVED";
 $("sourceCaveat").textContent=it.evidence||"Static source illustration; actual device behavior and human approval are separate evidence classes.";
 $("noteCounter").textContent="("+(review.notes[itemKey(it)]||[]).length+")";
 $("startPin").textContent=current.pinning?"Cancel point selection":"＋ Pin a point on the screen";
 $("startPin").disabled=current.full;
}
function stateOf(it){return review.items[itemKey(it)]||{}}
function chooseOption(){
 const it=record(),o=currentOption();if(!it||!o)return;
 if(it.kind!=="primary")return;
 review.chosen[itemKey(it)]={path:o.path,sha:o.sha,at:new Date().toISOString()};
 save();renderInspector();notify("Preferred source saved in this browser");
}
function markStatus(status){const it=record();if(!it||it.kind==="question")return;review.items[itemKey(it)]={status,path:currentOption().path,sha:currentOption().sha,at:new Date().toISOString()};save();renderInspector();renderNavigation()}
function chooseQuestion(choice){const it=record();if(!it||it.kind!=="question")return;review.questions[it.id]={choice,at:new Date().toISOString()};save();renderInspector();renderNavigation();notify("Decision "+it.id+" saved locally")}
function renderInspector(){
 const it=record();if(!it)return;const key=itemKey(it),o=currentOption(),item=stateOf(it);
 $("inspectorTitle").textContent=it.kind==="question"?"Decision "+it.id:"Screen decision";
 if(it.kind==="question"){
  const q=review.questions[it.id]||{};
  $("decisionPanel").innerHTML='<div class="selected-meta"><strong>'+esc(it.title)+'</strong><p class="question-prompt">'+esc(it.subtitle)+'</p><p class="muted">Compare the related screen choices at full size, then record the design direction here.</p></div><div class="choice-list">'+(questionChoices[it.id]||[]).map(v=>'<button type="button" data-choice="'+esc(v)+'" class="'+(v===q.choice?"chosen":"")+'" aria-pressed="'+(v===q.choice)+'">'+esc(v)+'</button>').join("")+'</div><p class="question-explainer">This records a review choice in your browser; it does not modify the approved source tree.</p>';
  $("reviewControls").innerHTML="";
 }else{
  const selected=review.chosen[key],selectedPath=selected?.path||it.options[0].path;
  $("decisionPanel").innerHTML='<div class="selected-meta"><strong>'+esc(it.title)+'</strong><p>'+esc(it.warning||it.subtitle)+'</p></div>'+(it.kind==="primary"?'<div class="state-label">Design chosen for this screen</div><div class="choice-list"><button type="button" class="chosen" disabled>'+esc(nice(selectedPath))+'</button></div>':"");
  $("reviewControls").innerHTML='<h3>Review status</h3><div class="review-actions">'+[
   ["accepted","Accept"],["changes","Revise"],["deferred","Defer"]
  ].map(v=>'<button type="button" data-status="'+v[0]+'" aria-pressed="'+(item.status===v[0])+'">'+v[1]+'</button>').join("")+'</div><p class="muted">'+(item.status?"Recorded against source "+esc((item.sha||"").slice(0,13))+"…":"No choice recorded yet.")+'</p>'+
  (it.kind==="primary"?'<button type="button" class="wide" data-use="1">Use displayed design as preferred</button>':"")+
  (selected&&selected.sha!==hash(selected.path)?'<p class="overview-alert">Saved preference refers to older source bytes. Re-examine it.</p>':"");
 }
 $("generalComment").value=review.general[key]||"";
 renderComments();
}
function renderComments(){
 const it=record();if(!it)return;const comments=review.notes[itemKey(it)]||[];
 $("commentCount").textContent=comments.length;
 $("commentList").innerHTML=comments.map((n,i)=>'<div class="comment"><button type="button" data-remove="'+i+'" aria-label="Delete comment">Remove</button><strong>'+esc(n.severity||"visual")+'</strong> · '+(n.x==null?"General":"pin "+Math.round(n.x)+"% / "+Math.round(n.y)+"%")+'<p>'+esc(n.text)+'</p><time>'+esc(n.at||"")+'</time>'+(n.sha!==hash(n.path)?'<p class="overview-alert">This comment belongs to a previous source revision.</p>':"")+'</div>').join("");
}
function screenClick(e){
 if(e.target.closest(".pin")){notify(e.target.title||"Pinned comment");return}
 if(!current.pinning||current.full)return;
 const el=e.target.closest(".frame");if(!el||el.dataset.frame!=="0")return;
 const r=el.getBoundingClientRect();const x=100*(e.clientX-r.left)/r.width,y=100*(e.clientY-r.top)/r.height;
 const o=currentOption();
 current.pin={x:Math.max(0,Math.min(100,x)),y:Math.max(0,Math.min(100,y)),path:o.path,sha:o.sha};
 $("pinLocation").textContent="Point "+Math.round(x)+"% across, "+Math.round(y)+"% down · "+nice(o.path);
 $("pinComposer").hidden=false;$("pinText").value="";$("pinText").focus();if(innerWidth<900)togglePanel("inspector",true);
}
function savePin(){
 if(!current.pin)return;const text=$("pinText").value.trim();if(!text){notify("Write a comment before saving");$("pinText").focus();return}
 const it=record(),key=itemKey(it);
 (review.notes[key]||(review.notes[key]=[])).push({...current.pin,severity:$("pinSeverity").value,text,at:new Date().toISOString()});
 current.pinning=false;current.pin=null;$("pinComposer").hidden=true;save();renderStage();renderComments();notify("Pinned to exact source version");
}
function cancelPin(){current.pin=null;$("pinComposer").hidden=true}
function removePin(i){const it=record(),notes=review.notes[itemKey(it)];if(!notes||i<0||i>=notes.length)return;notes.splice(i,1);save();renderStage();renderComments()}
function updateCount(){if(!records.primary)return;const all=[...records.primary,...records.support],count=all.filter(x=>review.items[itemKey(x)]?.status==="accepted").length;$("progressCompact").textContent=count+"/58"}
function selectedPath(it){return review.chosen[itemKey(it)]?.path||it.options[0].path}
function atomicValid(){
 const m=data.selection.primarySurfaces.filter(x=>six.includes(x.surfaceId));
 const chosen=Object.fromEntries(m.map(x=>[x.surfaceId,selectedPath(records.primary.find(p=>p.id===x.surfaceId))]));
 const mig=new Map(m.map(x=>[x.surfaceId,x.reviewCandidatePath]));
 const isCC0=six.every(k=>chosen[k]===mig.get(k));
 const historical=six.every(k=>{
  const r=records.primary.find(x=>x.id===k);
  return chosen[k]===r.options[1]?.path;
 });
 return {valid:isCC0||historical,mode:isCC0?"Coordinated CC0 set":historical?"Historical comparison set":"MIXED photo set · cannot approve as one",chosen};
}
function stats(){
 const all=[...records.primary,...records.support],counts={accepted:0,changes:0,deferred:0,unreviewed:0};
 all.forEach(it=>{const st=review.items[itemKey(it)]?.status||"unreviewed";counts[st]=(counts[st]||0)+1});
 const answered=records.question.filter(q=>review.questions[q.id]?.choice).length;
 const blockingPins=Object.values(review.notes).flat().filter(n=>n.severity==="blocking").length;
 const unresolved=counts.changes+counts.deferred+counts.unreviewed+blockingPins;
 const atomic=atomicValid();
 return {counts,answered,blockingPins,atomic,ready:counts.accepted===58&&answered===8&&unresolved===0&&atomic.valid};
}
function summary(){
 const s=stats(),n=Object.values(review.notes).flat().length;
 return "Glimpse draft visual review\nAccepted: "+s.counts.accepted+"/58 | Revisions: "+s.counts.changes+" | Deferred: "+s.counts.deferred+" | Unreviewed: "+s.counts.unreviewed+"\nDecisions: "+s.answered+"/8 | Comments: "+n+" | Blocking pins: "+s.blockingPins+"\nPhoto selection: "+s.atomic.mode+"\nFinal review packet complete: "+(s.ready?"YES (local declaration only)":"NO")+"\nBranch: agent/visual-state-reconciliation-20261023\nSource build: "+(build.sha||"open site build metadata")+"\nNo changes have been pushed to GitHub.";
}
function reviewPacket(){
 const selected=records.primary.map(it=>({id:it.id,path:selectedPath(it),sha:hash(selectedPath(it)),status:stateOf(it).status||"unreviewed"}));
 const support=records.support.map(it=>({id:it.id,path:it.options[0].path,sha:it.options[0].sha,status:stateOf(it).status||"unreviewed"}));
 const s=stats();
 return {format:"glimpse-design-review-v1",repository:"gamesbyian/Glimpse",branch:"agent/visual-state-reconciliation-20261023",sourceCommit:build.sha||null,exportedAt:new Date().toISOString(),snapshotSourceCount:data.snapshot.uniqueReferencedSources,approval:{wholeSetVisualAcceptanceRequested:s.ready,hasPendingChanges:!s.ready,repositoryOwnerApproved:false,notes:"This is an exported owner review record, not a direct repository approval."},summary:{...s,atomic:s.atomic.mode},selectedPrimary:selected,selectedSupport:support,decisions:review.questions,comments:review.notes,generalNotes:review.general,reviewStatus:review.items,chosen:review.chosen};
}
function download(filename,text,mime){const blob=new Blob([text],{type:mime}),u=URL.createObjectURL(blob),a=document.createElement("a");a.href=u;a.download=filename;document.body.append(a);a.click();a.remove();setTimeout(()=>URL.revokeObjectURL(u),900);}
function exportJSON(){const packet=reviewPacket();download("glimpse-owner-review-"+new Date().toISOString().slice(0,10)+".json",JSON.stringify(packet,null,2)+"\n","application/json");notify("Review exported. Attach the JSON to GitHub or this chat to submit it.")}
async function importReview(e){
 const file=e.target.files?.[0];e.target.value="";if(!file)return;
 try{
  if(file.size>3000000)throw Error("Review file is too large");
  const x=JSON.parse(await file.text());
  if(x.format!=="glimpse-design-review-v1"||x.repository!=="gamesbyian/Glimpse"||typeof x.reviewStatus!=="object"||typeof x.decisions!=="object")throw Error("Not a Glimpse review packet");
  if(!confirm("Replace your current locally saved review with this imported copy? Export your existing notes first if needed."))return;
  review={schema:1,createdAt:x.exportedAt,items:x.reviewStatus||{},questions:x.decisions||{},notes:x.comments||{},general:x.generalNotes||{},chosen:x.chosen||{}};
  save();render();$("overviewDialog").close();notify("Imported. Old source-version notes are flagged where necessary.");
 }catch(err){notify("Import failed: "+err.message)}
}
function openOverview(){
 const s=stats(),q=records.question,unreviewed=[...records.primary,...records.support].filter(it=>review.items[itemKey(it)]?.status!=="accepted");
 $("overviewBody").innerHTML=
  '<p class="muted">This is your private, browser-local working ledger. Choosing Accept does not write to GitHub. Export JSON to preserve or submit your decisions.</p>'+
  '<div class="summary-stat"><div><strong>'+s.counts.accepted+'/58</strong><small>Screens accepted</small></div><div><strong>'+s.answered+'/8</strong><small>Owner decisions recorded</small></div><div><strong>'+s.counts.changes+'</strong><small>Revision requests</small></div><div><strong>'+s.counts.deferred+'</strong><small>Deferred states</small></div><div><strong>'+Object.values(review.notes).flat().length+'</strong><small>Pinned comments</small></div><div><strong>'+(s.atomic.valid?'Coherent':'Mixed')+'</strong><small>Six-screen photographic set</small></div></div>'+
  '<h3>Eight required decisions</h3>'+
  q.map(it=>'<div class="overview-row"><span><strong>'+esc(it.id)+'</strong> '+esc(it.title.replace(it.id+" · ",""))+'<br><small>'+esc(review.questions[it.id]?.choice||"Not decided")+'</small></span><button type="button" data-jump="question|'+esc(it.id)+'">Open</button></div>').join("")+
  '<h3>Outstanding screen evaluations ('+unreviewed.length+')</h3>'+
  '<div class="overview-alert">'+(unreviewed.slice(0,10).map(x=>'<button type="button" data-jump="'+esc(x.kind+"|"+x.id)+'">'+esc(x.title)+'</button>').join(" ")||"No missing acceptances.")+(unreviewed.length>10?" · plus "+(unreviewed.length-10)+" others":"")+'</div>'+
  '<h3>Whole-set readiness</h3><div class="'+(s.ready?"overview-good":"overview-alert")+'">'+(s.ready?"All 58 selections and eight decisions recorded with a compatible six-photo set and no blocking pins. You may export a source-bound visual acceptance request. Repository signoff remains separate.":"Not yet qualified for a whole-set visual acceptance request. Pending work, revisions or mixed photographic selections remain. Feedback export works at any point.")+'</div>'+
  '<p class="muted">Source build: '+esc(build.sha||"current deployment")+'. Imported browser decisions are independently checked against current source hashes in the exported packet.</p>'+
  '<button type="button" class="wide" id="feedbackIssue">Open a GitHub feedback issue with summary ↗</button>';
 $("overviewBody").querySelectorAll("[data-jump]").forEach(b=>b.addEventListener("click",()=>{const [k,id]=b.dataset.jump.split("|");$("overviewDialog").close();navigate(k,id)}));
 $("feedbackIssue").addEventListener("click",()=>{const title="Glimpse visual design review feedback";const body=summary()+"\n\nAttach the exported review JSON to preserve exact screen/option SHA, individual decisions and annotations.";const u="https://github.com/gamesbyian/Glimpse/issues/new?title="+encodeURIComponent(title)+"&body="+encodeURIComponent(body);window.open(u,"_blank","noopener,noreferrer")});
 $("overviewDialog").showModal();
}
function copyText(text){if(navigator.clipboard?.writeText){navigator.clipboard.writeText(text).then(()=>notify("Copied")).catch(()=>fallbackCopy(text))}else fallbackCopy(text)}
function fallbackCopy(s){const t=document.createElement("textarea");t.value=s;document.body.append(t);t.select();try{document.execCommand("copy");notify("Copied")}catch(e){notify("Clipboard unavailable; use Export review instead")}t.remove()}
Promise.all([
 fetch("../design/proposals/whole-product-review-selection.v1.json").then(x=>{if(!x.ok)throw Error("selection "+x.status);return x.json()}),
 fetch("../design/atlas/whole-product-review-snapshot.v1.json").then(x=>{if(!x.ok)throw Error("snapshot "+x.status);return x.json()}),
 fetch("../design/atlas/whole-set-evidence-index.json").then(x=>{if(!x.ok)throw Error("atlas "+x.status);return x.json()})
]).then(([a,b,c])=>startData(a,b,c)).catch(err=>{
 $("screenTitle").textContent="Reviewer failed to load";
 $("screenSubtitle").textContent=err.message+". Use the GitHub Pages deployment or serve a checkout over HTTP; file:// blocks JSON fetch.";
 $("evidenceChip").textContent="DATA UNAVAILABLE";
 console.error(err);
});
})();