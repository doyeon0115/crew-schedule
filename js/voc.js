/* ---------- 건의(VOC) ---------- */
function saveLocalVoc(){ localStorage.setItem("crew-voc",JSON.stringify(voc)); }
function vocAdd(text){
  const who=((document.getElementById("vocName")||{}).value||"").trim();
  const e={t:Date.now(), text, who:who||"익명", done:false};
  if(remoteOK && window._vocAdd){ window._vocAdd(e); }
  else { e.key="L"+e.t; voc.unshift(e); saveLocalVoc(); renderVoc(); }
}
function vocToggle(key){
  const item=voc.find(v=>v.key===key); if(!item) return;
  const nd=!item.done;
  if(remoteOK && window._vocSet){ window._vocSet(key,{done:nd}); }
  else { item.done=nd; saveLocalVoc(); renderVoc(); }
}
function vocDel(key){
  if(remoteOK && window._vocDel){ window._vocDel(key); }
  else { voc=voc.filter(v=>v.key!==key); saveLocalVoc(); renderVoc(); }
}
function vocCommentAdd(key,text){
  const who=((document.getElementById("vocName")||{}).value||"").trim();
  const c={t:Date.now(), who:who||"익명", text};
  if(remoteOK && window._vocCmtAdd){ window._vocCmtAdd(key,c); }
  else {
    const item=voc.find(v=>v.key===key); if(!item) return;
    item.comments=item.comments||{}; item.comments["L"+c.t]=c;
    saveLocalVoc(); renderVoc();
  }
}
function vocCommentDel(key,ckey){
  if(remoteOK && window._vocCmtDel){ window._vocCmtDel(key,ckey); }
  else {
    const item=voc.find(v=>v.key===key);
    if(item&&item.comments){ delete item.comments[ckey]; saveLocalVoc(); renderVoc(); }
  }
}
function hhmm(t){const d=new Date(t);return `${d.getMonth()+1}/${d.getDate()} ${String(d.getHours()).padStart(2,"0")}:${String(d.getMinutes()).padStart(2,"0")}`;}
function reactToggle(key,em){
  const item=voc.find(v=>v.key===key);
  const users=(item&&item.reactions&&item.reactions[em])||{};
  const mine=!!users[CLIENT];
  if(remoteOK && window._reactSet){ window._reactSet(key,em,!mine); }
  else {
    if(!item) return;
    item.reactions=item.reactions||{}; item.reactions[em]=item.reactions[em]||{};
    if(mine) delete item.reactions[em][CLIENT]; else item.reactions[em][CLIENT]=true;
    saveLocalVoc(); renderVoc();
  }
}
function renderVoc(){
  const box=document.getElementById("vocList");
  if(!box) return;
  const ae=document.activeElement;   // 답글 입력 중이면 다시 그리지 않음(포커스 보존)
  if(ae && box.contains(ae) && ae.classList.contains("cmt-input")) return;
  if(!voc.length){ box.innerHTML=`<div class="nofree">아직 의견이 없어요. 처음으로 남겨보세요! 🙌</div>`; return; }
  box.innerHTML=voc.map(v=>{
    const cmts=v.comments ? Object.entries(v.comments).map(([ck,cv])=>({ck,...cv})).sort((a,b)=>a.t-b.t) : [];
    const cmtHTML=cmts.map(c=>`<div class="cmt">
        <span class="cmt-arrow">↳</span>
        <div class="cmt-body">
          <div class="cmt-head"><b>${esc(c.who||"익명")}</b><span class="cmt-time">${hhmm(c.t)}</span>
            <button class="cmt-del" data-k="${v.key}" data-ck="${c.ck}" title="삭제">×</button></div>
          <div class="cmt-text">${esc(c.text)}</div>
        </div>
      </div>`).join("");
    const reactHTML=VOC_EMOJI.map(em=>{
      const users=(v.reactions&&v.reactions[em])||{};
      const cnt=Object.keys(users).length;
      const mine=!!users[CLIENT];
      return `<button class="rx${mine?' on':''}" data-k="${v.key}" data-em="${em}">${em}${cnt?`<span>${cnt}</span>`:""}</button>`;
    }).join("");
    const open=vocReplyOpen.has(v.key);
    return `<div class="voc${v.done?' done':''}">
      <div class="voctop"><b>${esc(v.who||"익명")}</b><span class="vocwhen">${hhmm(v.t)}</span></div>
      <div class="voctext">${esc(v.text)}</div>
      <div class="rx-row">${reactHTML}</div>
      <div class="vocbtns">
        <button class="reply-btn" data-k="${v.key}">↳ 답글${cmts.length?` ${cmts.length}`:""}</button>
        <button data-vdone="${v.key}">${v.done?"↩ 되돌리기":"✓ 완료"}</button>
        <button data-vdel="${v.key}">삭제</button>
      </div>
      ${cmtHTML?`<div class="cmt-list">${cmtHTML}</div>`:""}
      ${open?`<div class="cmt-add">
        <input class="cmt-input" data-k="${v.key}" placeholder="답글 달기…" maxlength="200">
        <button class="cmt-btn" data-k="${v.key}">등록</button>
      </div>`:""}
    </div>`;
  }).join("");
  box.querySelectorAll(".rx").forEach(b=>b.onclick=()=>reactToggle(b.dataset.k,b.dataset.em));
  box.querySelectorAll(".reply-btn").forEach(b=>b.onclick=()=>{
    const k=b.dataset.k;
    vocReplyOpen.has(k)?vocReplyOpen.delete(k):vocReplyOpen.add(k);
    renderVoc();
    const inp=box.querySelector(`.cmt-input[data-k="${k}"]`); if(inp) inp.focus();
  });
  box.querySelectorAll("[data-vdone]").forEach(b=>b.onclick=()=>vocToggle(b.dataset.vdone));
  box.querySelectorAll("[data-vdel]").forEach(b=>b.onclick=()=>{ if(confirm("이 의견을 삭제할까요?")) vocDel(b.dataset.vdel); });
  box.querySelectorAll(".cmt-del").forEach(b=>b.onclick=()=>{ if(confirm("댓글을 삭제할까요?")) vocCommentDel(b.dataset.k,b.dataset.ck); });
  const submit=(k)=>{ const inp=box.querySelector(`.cmt-input[data-k="${k}"]`); const text=(inp.value||"").trim(); if(!text){ inp.focus(); return; } vocCommentAdd(k,text); };
  box.querySelectorAll(".cmt-btn").forEach(b=>b.onclick=()=>submit(b.dataset.k));
  box.querySelectorAll(".cmt-input").forEach(inp=>inp.onkeydown=(e)=>{ if(e.key==="Enter") submit(inp.dataset.k); });
}
