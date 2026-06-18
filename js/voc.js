/* ---------- 건의(VOC) ---------- */
let cmtReplyOpen=new Set();   // 대댓글 입력창 열린 "vockey::commentkey"
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
function vocReplyToComment(vk,ck,text){   // 대댓글 추가
  const who=((document.getElementById("vocName")||{}).value||"").trim();
  const r={t:Date.now(), who:who||"익명", text};
  if(remoteOK && window._replyAdd){ window._replyAdd(vk,ck,r); }
  else {
    const item=voc.find(v=>v.key===vk); const c=item&&item.comments&&item.comments[ck];
    if(!c) return;
    c.replies=c.replies||{}; c.replies["L"+r.t]=r;
    saveLocalVoc(); renderVoc();
  }
}
function vocReplyDel(vk,ck,rk){
  if(remoteOK && window._replyDel){ window._replyDel(vk,ck,rk); }
  else {
    const item=voc.find(v=>v.key===vk); const c=item&&item.comments&&item.comments[ck];
    if(c&&c.replies){ delete c.replies[rk]; saveLocalVoc(); renderVoc(); }
  }
}
async function translateText(text){   // 무료 번역(구글 gtx 엔드포인트, 비공식)
  const url="https://translate.googleapis.com/translate_a/single?client=gtx&sl=auto&tl=en&dt=t&q="+encodeURIComponent(text);
  const r=await fetch(url);
  if(!r.ok) throw new Error("net");
  const j=await r.json();
  return (j[0]||[]).map(s=>s[0]).join("");
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
function commentHTML(vk,c){
  const replies=c.replies?Object.entries(c.replies).map(([rk,rv])=>({rk,...rv})).sort((a,b)=>a.t-b.t):[];
  const repliesHTML=replies.map(r=>`<div class="cmt reply2">
      <span class="cmt-arrow">↳</span>
      <div class="cmt-body">
        <div class="cmt-head"><b>${esc(r.who||"익명")}</b><span class="cmt-time">${hhmm(r.t)}</span>
          <button class="tr-btn" title="영어 번역">🌐</button>
          <button class="rep-del" data-k="${vk}" data-ck="${c.ck}" data-rk="${r.rk}" title="삭제">×</button></div>
        <div class="cmt-text">${esc(r.text)}</div>
      </div>
    </div>`).join("");
  const open=cmtReplyOpen.has(vk+"::"+c.ck);
  return `<div class="cmt">
    <span class="cmt-arrow">↳</span>
    <div class="cmt-body">
      <div class="cmt-head"><b>${esc(c.who||"익명")}</b><span class="cmt-time">${hhmm(c.t)}</span>
        <button class="tr-btn" title="영어 번역">🌐</button>
        <button class="crep-btn" data-k="${vk}" data-ck="${c.ck}" title="대댓글">↳</button>
        <button class="cmt-del" data-k="${vk}" data-ck="${c.ck}" title="삭제">×</button></div>
      <div class="cmt-text">${esc(c.text)}</div>
      ${repliesHTML?`<div class="reply-list">${repliesHTML}</div>`:""}
      ${open?`<div class="cmt-add">
        <input class="rep-input" data-k="${vk}" data-ck="${c.ck}" placeholder="대댓글…" maxlength="200">
        <button class="rep-btn" data-k="${vk}" data-ck="${c.ck}">등록</button>
      </div>`:""}
    </div>
  </div>`;
}
function renderVoc(){
  const box=document.getElementById("vocList");
  if(!box) return;
  const ae=document.activeElement;   // 입력 중이면 다시 그리지 않음(포커스 보존)
  if(ae && box.contains(ae) && (ae.classList.contains("cmt-input")||ae.classList.contains("rep-input"))) return;
  if(!voc.length){ box.innerHTML=`<div class="nofree">아직 의견이 없어요. 처음으로 남겨보세요! 🙌</div>`; return; }
  box.innerHTML=voc.map(v=>{
    const cmts=v.comments?Object.entries(v.comments).map(([ck,cv])=>({ck,...cv})).sort((a,b)=>a.t-b.t):[];
    const cmtHTML=cmts.map(c=>commentHTML(v.key,c)).join("");
    const reactHTML=VOC_EMOJI.map(em=>{
      const users=(v.reactions&&v.reactions[em])||{};
      const cnt=Object.keys(users).length, mine=!!users[CLIENT];
      return `<button class="rx${mine?' on':''}" data-k="${v.key}" data-em="${em}">${em}${cnt?`<span>${cnt}</span>`:""}</button>`;
    }).join("");
    const open=vocReplyOpen.has(v.key);
    return `<div class="voc${v.done?' done':''}">
      <div class="voctop"><b>${esc(v.who||"익명")}</b><span class="vocwhen">${hhmm(v.t)}</span></div>
      <div class="voctext">${esc(v.text)}</div>
      <div class="rx-row">${reactHTML}</div>
      <div class="vocbtns">
        <button class="reply-btn" data-k="${v.key}">↳ 댓글${cmts.length?` ${cmts.length}`:""}</button>
        <button data-vdone="${v.key}">${v.done?"↩ 되돌리기":"✓ 완료"}</button>
        <button data-vdel="${v.key}">삭제</button>
      </div>
      ${cmtHTML?`<div class="cmt-list">${cmtHTML}</div>`:""}
      ${open?`<div class="cmt-add">
        <input class="cmt-input" data-k="${v.key}" placeholder="댓글 달기…" maxlength="200">
        <button class="cmt-btn" data-k="${v.key}">등록</button>
      </div>`:""}
    </div>`;
  }).join("");
  // 반응
  box.querySelectorAll(".rx").forEach(b=>b.onclick=()=>reactToggle(b.dataset.k,b.dataset.em));
  // 의견 댓글 열기 / 완료 / 삭제
  box.querySelectorAll(".reply-btn").forEach(b=>b.onclick=()=>{
    const k=b.dataset.k; vocReplyOpen.has(k)?vocReplyOpen.delete(k):vocReplyOpen.add(k);
    renderVoc(); const inp=box.querySelector(`.cmt-input[data-k="${k}"]`); if(inp) inp.focus();
  });
  box.querySelectorAll("[data-vdone]").forEach(b=>b.onclick=()=>vocToggle(b.dataset.vdone));
  box.querySelectorAll("[data-vdel]").forEach(b=>b.onclick=()=>{ if(confirm("이 의견을 삭제할까요?")) vocDel(b.dataset.vdel); });
  // 댓글 등록 / 삭제
  const csub=(k)=>{ const inp=box.querySelector(`.cmt-input[data-k="${k}"]`); const t=(inp.value||"").trim(); if(!t){inp.focus();return;} vocCommentAdd(k,t); };
  box.querySelectorAll(".cmt-btn").forEach(b=>b.onclick=()=>csub(b.dataset.k));
  box.querySelectorAll(".cmt-input").forEach(inp=>inp.onkeydown=(e)=>{ if(e.key==="Enter") csub(inp.dataset.k); });
  box.querySelectorAll(".cmt-del").forEach(b=>b.onclick=()=>{ if(confirm("댓글을 삭제할까요?")) vocCommentDel(b.dataset.k,b.dataset.ck); });
  // 대댓글 열기 / 등록 / 삭제
  box.querySelectorAll(".crep-btn").forEach(b=>b.onclick=()=>{
    const id=b.dataset.k+"::"+b.dataset.ck;
    cmtReplyOpen.has(id)?cmtReplyOpen.delete(id):cmtReplyOpen.add(id);
    renderVoc();
    const inp=box.querySelector(`.rep-input[data-k="${b.dataset.k}"][data-ck="${b.dataset.ck}"]`); if(inp) inp.focus();
  });
  const rsub=(k,ck)=>{ const inp=box.querySelector(`.rep-input[data-k="${k}"][data-ck="${ck}"]`); const t=(inp.value||"").trim(); if(!t){inp.focus();return;} vocReplyToComment(k,ck,t); };
  box.querySelectorAll(".rep-btn").forEach(b=>b.onclick=()=>rsub(b.dataset.k,b.dataset.ck));
  box.querySelectorAll(".rep-input").forEach(inp=>inp.onkeydown=(e)=>{ if(e.key==="Enter") rsub(inp.dataset.k,inp.dataset.ck); });
  box.querySelectorAll(".rep-del").forEach(b=>b.onclick=()=>{ if(confirm("대댓글을 삭제할까요?")) vocReplyDel(b.dataset.k,b.dataset.ck,b.dataset.rk); });
  // 영어 번역 (그 자리 토글)
  box.querySelectorAll(".tr-btn").forEach(b=>b.onclick=async()=>{
    const body=b.closest(".cmt-body"), textEl=body.querySelector(".cmt-text");
    const next=textEl.nextElementSibling;
    if(next&&next.classList.contains("cmt-tr")){ next.remove(); return; }
    b.textContent="…";
    try{ const tr=await translateText(textEl.textContent);
      textEl.insertAdjacentHTML("afterend",`<div class="cmt-tr">🌐 ${esc(tr)}</div>`);
    }catch(e){ alert("번역에 실패했어요. 잠시 후 다시 시도해주세요."); }
    b.textContent="🌐";
  });
}
