"use strict";
/* Glimpse advanced visual inspection. Zero automatic network writes. */
(() => {
const $=id=>document.getElementById(id);
const api=()=>window.GlimpseReview;
const esc=s=>String(s??"").replace(/[&<>"']/g,c=>({"&":"&amp;","<":"&lt;",">":"&gt;",'"':"&quot;","'":"&#39;"}[c]));
const criteria=[["entry","Entry, goal and primary action are recognizable"],["continuity","Choices and visual continuity are coherent"],["recovery","Errors, interruption and cancellation are explained"],["exit","Successful finish, Back and non-gesture paths are clear"]];
const lens={mode:"off",value:50},visited={};
let lastKey="",playing=null,timeline=false,mapKind="primary";
function ctx(){const g=api();if(!g?.ready)return null;const it=g.currentRecord();return it?{g,it,key:it.kind+":"+it.id}:null}
function stop(){if(playing){clearInterval(playing);playing=null}$("journeyPlay").textContent="▶ Play";$("journeyPlay").setAttribute("aria-pressed","false")}
function pair(it){const g=api();return [it.options[0],it.options[g.current.option===0?1:g.current.option]]}
function paintLens(){
 const c=ctx();if(!c)return;
 const valid=c.it.kind!=="journey"&&c.it.options.length>1;
 if(!valid)lens.mode="off";
 $("comparisonControls").hidden=lens.mode==="off";
 $("diffWipe").disabled=$("diffBlend").disabled=!valid;
 $("diffWipe").setAttribute("aria-pressed",String(lens.mode==="wipe"));
 $("diffBlend").setAttribute("aria-pressed",String(lens.mode==="blend"));
 $("comparisonSlider").value=String(lens.value);
 $("comparisonValue").textContent=lens.value+"%";
 if(lens.mode==="off")return;
 const [a,b]=pair(c.it),stage=$("screens");
 stage.className="screens difference-mode";
 stage.innerHTML='<div class="frame difference-frame" aria-label="'+esc(c.it.title)+' comparison">'
 +'<div class="diff-bottom"><img class="diff-image" src="'+esc(c.g.sourceUrl(a.path))+'" draggable="false" alt="'+esc(a.label)+'"></div>'
 +'<div class="diff-top"><img class="diff-image" src="'+esc(c.g.sourceUrl(b.path))+'" draggable="false" alt="'+esc(b.label)+'"></div>'
 +'<div class="diff-divider"></div><span class="diff-corner left">Recommended</span><span class="diff-corner right">'+esc(b.label)+'</span></div>';
 $("comparisonLabel").textContent=lens.mode==="wipe"?"Reveal the alternative":"Blend two versions";
 applyLens();
}
function applyLens(){
 const f=$("screens .difference-frame");if(!f)return;
 const top=f.querySelector(".diff-top"),div=f.querySelector(".diff-divider");
 if(lens.mode==="wipe"){top.style.clipPath="inset(0 "+(100-lens.value)+"% 0 0)";top.style.opacity="1";div.hidden=false;div.style.left=lens.value+"%"}
 else{top.style.clipPath="none";top.style.opacity=String(lens.value/100);div.hidden=true}
 $("comparisonValue").textContent=lens.value+"%";
}
function setLens(mode){
 const c=ctx();if(!c)return;
 if(c.it.kind==="journey"||c.it.options.length<2){c.g.notify("This screen has no alternate version");return}
 lens.mode=lens.mode===mode?"off":mode;
 if(lens.mode!=="off"){c.g.current.compare=false;c.g.current.pinning=false;$("compare").setAttribute("aria-pressed","false")}
 c.g.render();
}
function journeyData(it){return api().getJourney(it.id)||{status:"unreviewed",checks:{},notes:"",frames:[],updatedAt:null}}
function journeyDraw(){
 const c=ctx();if(!c)return;
 const {it,g}=c,active=it.kind==="journey";
 $("journeyPlayer").hidden=$("journeyAcceptance").hidden=!active;
 if(!active){stop();return}
 const idx=g.current.option,opts=it.options,len=opts.length;
 $("journeyPrev").disabled=idx===0;$("journeyNext").disabled=idx===len-1;
 $("journeyPosition").textContent="Step "+(idx+1)+"/"+len+" · "+opts[idx].label;
 const seen=visited[it.id]||(visited[it.id]=new Set());seen.add(idx);
 $("journeyProgress").innerHTML=opts.map((o,n)=>'<button type="button" data-frame="'+n+'" title="'+esc(o.label)+'" aria-current="'+(n===idx)+'" class="'+(n===idx?"active":seen.has(n)?"visited":"")+'"></button>').join("");
 $("journeyFramesToggle").setAttribute("aria-expanded",String(timeline));
 $("journeyTimeline").hidden=!timeline;
 if(timeline)$("journeyTimeline").innerHTML=opts.map((o,n)=>'<button type="button" data-frame="'+n+'" aria-current="'+(n===idx)+'"><span class="map-thumb"><img loading="lazy" alt="" src="'+esc(g.sourceUrl(o.path))+'"></span><span>'+(n+1)+'. '+esc(o.label)+'</span></button>').join("");
 const r=journeyData(it),currentHashes=opts.map(o=>o.sha);
 const same=JSON.stringify(r.frames)===JSON.stringify(currentHashes);
 $("journeyChecks").innerHTML=criteria.map(([id,label])=>'<label class="check-label"><input type="checkbox" data-journey-check="'+id+'" '+(r.checks[id]?"checked":"")+'> '+esc(label)+'</label>').join("");
 const valid=criteria.every(([id])=>r.checks[id]);
 $("journeyApprove").disabled=!valid;
 $("journeyApprove").textContent=r.status==="accepted"&&same?"✓ Journey accepted":"Accept connected journey";
 $("journeyRevise").textContent=r.status==="changes"?"Revision requested":"Request journey revision";
 if(document.activeElement!==$("journeyNotes"))$("journeyNotes").value=r.notes||"";
 $("journeyAcceptance").querySelector(".journey-version-warning")?.remove();
 if(r.status==="accepted"&&!same){$("journeyChecks").insertAdjacentHTML("beforebegin",'<p class="journey-version-warning overview-alert">Source frames have changed since your review. Recheck the journey.</p>');$("journeyApprove").disabled=true}
}
function saveJourneyPatch(mod){
 const c=ctx();if(!c||c.it.kind!=="journey")return;
 const r={...journeyData(c.it),...mod,updatedAt:new Date().toISOString()};
 c.g.setJourney(c.it.id,r);
 journeyDraw();
}
function step(n){
 const c=ctx();if(!c||c.it.kind!=="journey")return;
 const next=Math.min(c.it.options.length-1,Math.max(0,c.g.current.option+n));
 if(next===c.g.current.option){stop();return}
 c.g.changeOption(next);
}
function play(){
 const c=ctx();if(!c||c.it.kind!=="journey")return;
 if(playing){stop();return}
 if(c.g.current.option===c.it.options.length-1)c.g.changeOption(0);
 $("journeyPlay").textContent="❚❚ Pause";$("journeyPlay").setAttribute("aria-pressed","true");
 playing=setInterval(()=>{
  const n=ctx();if(!n||n.it.id!==c.it.id||n.it.kind!=="journey"||document.hidden){stop();return}
  step(1);
 },1800);
}
function setFocus(){
 const on=document.body.classList.toggle("focus-mode");
 $("focusMode").setAttribute("aria-pressed",String(on));
 $("focusMode").textContent=on?"Restore panels":"Focus";
}
function openMap(){
 const c=ctx();if(!c)return;
 const g=c.g,items=g.records[mapKind];
 document.querySelectorAll("[data-map-kind]").forEach(b=>b.classList.toggle("active",b.dataset.mapKind===mapKind));
 $("setMapList").innerHTML=items.map(it=>{
  const status=(it.kind==="journey"?g.getJourney(it.id):g.review.items[it.kind+":"+it.id])?.status||"unreviewed";
  return '<button type="button" class="set-map-card" data-map-id="'+esc(it.id)+'"><span class="map-thumb"><img loading="lazy" alt="" src="'+esc(g.sourceUrl(it.options[0].path))+'"></span><strong>'+esc(it.title)+'</strong><small>'+esc(status)+'</small></button>';
 }).join("");
}
function markdown(){
 const c=ctx();if(!c)return "";
 const {g,it,key}=c,o=g.currentOption();
 const revision=g.build.sha||"agent/visual-state-reconciliation-20261023";
 const link="https://github.com/gamesbyian/Glimpse/blob/"+encodeURIComponent(revision)+"/"+o.path.split("/").map(encodeURIComponent).join("/");
 const notes=(g.review.notes[key]||[]).filter(n=>n.path===o.path&&n.sha===o.sha);
 const general=g.review.general[key]||"(No general notes)";
 const status=(it.kind==="journey"?g.getJourney(it.id):g.review.items[key])?.status||"unreviewed";
 let out="# Glimpse visual feedback: "+it.title+"\n\n";
 out+="**Source:** ["+o.path+"]("+link+")\n**SHA-256:** "+o.sha+"\n**Review status:** "+status+"\n**Option:** "+(g.current.option+1)+"/"+it.options.length+"\n\n";
 out+="## Comments\n"+general+"\n";
 if(it.kind==="journey")out+="\n## Journey continuity\n"+(g.getJourney(it.id)?.notes||"(No journey notes)")+"\n";
 out+="\n## Pinned annotations\n";
 out+=notes.length?notes.map(n=>"- "+n.severity+" at "+Math.round(n.x)+"% / "+Math.round(n.y)+"%: "+n.text.replace(/\n/g," ")).join("\n"):"(No pinned comments on this exact source version)";
 return out+"\n\nStatic design source only. No native verification or human approval inferred.\n";
}
function issue(){
 const c=ctx();if(!c)return;
 let body=markdown();if(body.length>4400)body=body.slice(0,4400)+"\n\nTruncated. Export JSON for complete annotations.";
 const url="https://github.com/gamesbyian/Glimpse/issues/new?title="+encodeURIComponent("Glimpse visual review: "+c.it.title)+"&body="+encodeURIComponent(body);
 if(confirm("Open a draft GitHub issue for this screenshot? Nothing is submitted until you explicitly create the issue on GitHub."))window.open(url,"_blank","noopener,noreferrer");
}
function copyMarkdown(){
 const t=markdown();if(navigator.clipboard?.writeText)navigator.clipboard.writeText(t).then(()=>api().notify("Source-bound feedback copied")).catch(()=>fallback(t));else fallback(t);
}
function fallback(text){const e=document.createElement("textarea");e.value=text;document.body.append(e);e.select();try{document.execCommand("copy");api().notify("Copied")}catch(err){api().notify("Clipboard unavailable")}e.remove()}
function update(){
 const c=ctx();if(!c)return;
 if(c.key!==lastKey){lastKey=c.key;stop();lens.mode="off"}
 paintLens();journeyDraw();
}
function bind(){
 $("diffWipe").addEventListener("click",()=>setLens("wipe"));
 $("diffBlend").addEventListener("click",()=>setLens("blend"));
 $("comparisonSlider").addEventListener("input",e=>{lens.value=Number(e.target.value);applyLens()});
 $("resetDiff").addEventListener("click",()=>{lens.value=50;$("comparisonSlider").value="50";applyLens()});
 $("focusMode").addEventListener("click",setFocus);
 $("journeyPrev").addEventListener("click",()=>step(-1));$("journeyNext").addEventListener("click",()=>step(1));
 $("journeyPlay").addEventListener("click",play);
 $("journeyFramesToggle").addEventListener("click",()=>{timeline=!timeline;journeyDraw()});
 for(const id of ["journeyProgress","journeyTimeline"])$(id).addEventListener("click",e=>{
  const b=e.target.closest("[data-frame]");if(b){stop();api().changeOption(Number(b.dataset.frame))}
 });
 $("journeyChecks").addEventListener("change",e=>{
  if(!e.target.matches("[data-journey-check]"))return;
  const c=ctx(),r=journeyData(c.it);
  saveJourneyPatch({checks:{...r.checks,[e.target.dataset.journeyCheck]:e.target.checked},status:r.status==="accepted"?"unreviewed":r.status});
 });
 $("journeyApprove").addEventListener("click",()=>{
  const c=ctx(),r=journeyData(c.it);if(!criteria.every(([id])=>r.checks[id]))return;
  saveJourneyPatch({status:"accepted",frames:c.it.options.map(o=>o.sha)});api().notify("Journey accepted against exact source versions");
 });
 $("journeyRevise").addEventListener("click",()=>{saveJourneyPatch({status:"changes"});api().notify("Journey revision requested")});
 $("journeyNotes").addEventListener("input",e=>{
  const c=ctx(),r=journeyData(c.it);saveJourneyPatch({notes:e.target.value,status:r.status==="accepted"?"unreviewed":r.status});
 });
 $("openSetMap").addEventListener("click",()=>{openMap();$("setMapDialog").showModal()});
 $("closeSetMap").addEventListener("click",()=>$("setMapDialog").close());
 document.querySelector(".map-filters").addEventListener("click",e=>{const b=e.target.closest("[data-map-kind]");if(b){mapKind=b.dataset.mapKind;openMap()}});
 $("setMapList").addEventListener("click",e=>{
  const b=e.target.closest("[data-map-id]");if(!b)return;
  $("setMapDialog").close();api().navigate(mapKind,b.dataset.mapId);
  document.body.classList.remove("focus-mode");$("focusMode").textContent="Focus";$("focusMode").setAttribute("aria-pressed","false");
 });
 $("helpButton").addEventListener("click",()=>$("helpDialog").showModal());
 $("closeHelp").addEventListener("click",()=>$("helpDialog").close());
 $("targetedIssue").addEventListener("click",issue);
 $("copyFeedback").addEventListener("click",copyMarkdown);
 document.addEventListener("keydown",e=>{
  if(e.key==="?"&&!e.target.closest("textarea,input,select,[contenteditable]")&&!$("overviewDialog").open&&!$("helpDialog").open){e.preventDefault();$("helpDialog").showModal()}
 });
 document.addEventListener("visibilitychange",()=>{if(document.hidden)stop()});
 window.addEventListener("glimpse:render",update);
 window.addEventListener("glimpse:save",()=>{const c=ctx();if(c?.it.kind==="journey")journeyDraw()});
}
bind();
if(api()?.ready)update();
})();
