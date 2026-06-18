/* ===================================================================
   ① Firebase 설정  ── 실시간 공유를 켜려면 아래 값을 채우세요.
      (방법은 같이 드린 README.md 참고. 안 채우면 이 기기에만 저장됩니다)
   =================================================================== */
const firebaseConfig = {
  apiKey:            "AIzaSyDsgXKxeV9OFt3gjW5k9o3mtTB2Ohca_4o",
  authDomain:        "crew-schedule-6b784.firebaseapp.com",
  databaseURL:       "https://crew-schedule-6b784-default-rtdb.asia-southeast1.firebasedatabase.app",
  projectId:         "crew-schedule-6b784",
  storageBucket:     "crew-schedule-6b784.firebasestorage.app",
  messagingSenderId: "862399389044",
  appId:             "1:862399389044:web:7de40a2aa7bddb4489e021",
};
const ROOM = "our-crew";   // 친구 그룹 이름(아무 단어). 같은 값이면 같은 스케줄을 봅니다.

/* ===================================================================
   ② 기본 스케줄 (처음 한 번만 사용됨, 이후엔 수정한 내용이 유지됨)
      ※ 시간이 안 알려진 칸은 09:00~18:00로 임시로 넣어놨어요. 수정 탭에서 고치면 됩니다.
   =================================================================== */
const DAYS = [["mon","월"],["tue","화"],["wed","수"],["thu","목"],["fri","금"],["sat","토"],["sun","일"]];
const DAYLBL = {mon:"월",tue:"화",wed:"수",thu:"목",fri:"금",sat:"토",sun:"일"};
const W = (s,e)=>({off:false,start:s,end:e});
const OFF = {off:true,start:"09:00",end:"18:00"};
const DEFAULT_DATA = {
  people: {
    p1:{ name:"친구1", sched:{ mon:W("09:00","18:00"),tue:W("09:00","18:00"),wed:W("09:00","18:00"),thu:W("09:00","18:00"),fri:W("09:00","18:00"),sat:OFF,sun:OFF } },
    p2:{ name:"친구2", sched:{ mon:OFF,sun:OFF,tue:W("09:00","20:30"),wed:W("09:00","20:30"),thu:W("09:00","20:30"),fri:W("09:00","20:30"),sat:W("09:00","20:30") } },
    p3:{ name:"친구3", sched:{ mon:W("09:00","18:00"),tue:W("09:00","18:00"),wed:W("09:00","18:00"),thu:W("09:00","18:00"),fri:W("09:00","18:00"),sat:OFF,sun:OFF } },
    p4:{ name:"친구4", sched:{ mon:OFF,thu:OFF,tue:W("09:00","18:00"),wed:W("09:00","18:00"),fri:W("09:00","18:00"),sat:W("09:00","18:00"),sun:W("09:00","18:00") } },
  }
};

/* =================================================================== */
let data = clone(DEFAULT_DATA);
let selectedWho = null;           // 약속잡기에서 선택된 친구 id 집합
let editOpen = null;              // 수정 탭에서 펼쳐진 친구 id (아코디언)
let db=null, remoteOK=false;
let logs=[];                      // 기록(입장·변경)
let logPage=0;                    // 로그 페이지(0부터)
let voc=[];                       // 건의(의견 게시판)
let myIP="";                      // 내 공인 IP(비동기로 채워짐)
const UA=uaParse();
const DEV=UA.os+"·"+UA.br;        // 표시/프레즌스용 (예: iOS·Safari)
const INFO={ os:UA.os, br:UA.br, device:UA.device, ref:referrerInfo(),
  scr:(window.screen?window.screen.width+"×"+window.screen.height:""),
  lang:(navigator.language||"").split("-")[0] };
let CLIENT=sessionStorage.getItem("crew-cid");
if(!CLIENT){ CLIENT=(self.crypto&&crypto.randomUUID)?crypto.randomUUID():"c"+Math.random().toString(36).slice(2); sessionStorage.setItem("crew-cid",CLIENT); }

function clone(o){return JSON.parse(JSON.stringify(o));}
function uaParse(){
  const ua=navigator.userAgent||"";
  let os="기타";
  if(/Windows/.test(ua)) os="Windows";
  else if(/iPhone|iPad|iPod/.test(ua)) os="iOS";
  else if(/Android/.test(ua)) os="Android";
  else if(/Mac OS X|Macintosh/.test(ua)) os="Mac";
  else if(/Linux/.test(ua)) os="Linux";
  let br="기타";
  if(/Edg\//.test(ua)) br="Edge";
  else if(/SamsungBrowser/.test(ua)) br="삼성인터넷";
  else if(/Chrome\//.test(ua)) br="Chrome";
  else if(/Firefox\//.test(ua)) br="Firefox";
  else if(/Safari\//.test(ua)) br="Safari";
  const device=/iPad|Tablet/.test(ua) ? "태블릿"
              : /Mobile|Android|iPhone|iPod/.test(ua) ? "모바일" : "PC";
  return {os,br,device};
}
function referrerInfo(){   // 유입경로: referrer URL 그대로, 없으면 직접 접속
  return document.referrer || "직접 접속";
}
async function fetchIP(){
  try{ const r=await fetch("https://api.ipify.org?format=json"); const j=await r.json(); myIP=j.ip||""; }
  catch(e){ myIP=""; }
}
function showPresence(n){
  const el=document.getElementById("presence");
  if(!el) return;
  if(n>0){ el.style.display="inline-block"; el.textContent=`😺 ${n}명 접속 중`; }
  else { el.style.display="none"; }
}
function toMin(t){const [h,m]=t.split(":").map(Number);return h*60+m;}
function fmt(m){const h=Math.floor(m/60),mm=m%60;return String(h).padStart(2,"0")+":"+String(mm).padStart(2,"0");}
function shortT(t){const [h,m]=t.split(":");return m==="00"?String(+h):(+h)+":"+m;} // 09:00→9, 20:30→20:30
function isConfigured(){return !firebaseConfig.apiKey.includes("여기에");}

/* ---------- 저장/동기화 ---------- */
async function initStorage(){
  if(isConfigured()){
    try{
      const {initializeApp}=await import("https://www.gstatic.com/firebasejs/10.12.0/firebase-app.js");
      const {getDatabase,ref,onValue,set,push,update,onDisconnect,remove,serverTimestamp}=await import("https://www.gstatic.com/firebasejs/10.12.0/firebase-database.js");
      const app=initializeApp(firebaseConfig);
      db=getDatabase(app);
      const peopleRef=ref(db,"rooms/"+ROOM+"/people");
      const logsRef=ref(db,"rooms/"+ROOM+"/logs");
      const vocRef=ref(db,"rooms/"+ROOM+"/voc");
      window._set=()=>set(peopleRef,data.people);           // 스케줄만 저장(로그 안 건드림)
      window._logPush=(e)=>push(logsRef,e);                 // 기록 추가
      window._logClear=()=>set(logsRef,null);               // 기록 전체 삭제
      window._vocAdd=(e)=>push(vocRef,e);                   // 건의 추가
      window._vocSet=(key,patch)=>update(ref(db,"rooms/"+ROOM+"/voc/"+key),patch);  // 건의 수정(완료 등)
      window._vocDel=(key)=>remove(ref(db,"rooms/"+ROOM+"/voc/"+key));              // 건의 삭제
      window._vocCmtAdd=(key,c)=>push(ref(db,"rooms/"+ROOM+"/voc/"+key+"/comments"),c);       // 댓글 추가
      window._vocCmtDel=(key,ck)=>remove(ref(db,"rooms/"+ROOM+"/voc/"+key+"/comments/"+ck));  // 댓글 삭제
      // 실시간 접속자(프레즌스): 연결되면 등록, 끊기면 자동 제거
      const presRef=ref(db,"rooms/"+ROOM+"/presence");
      const myPres=ref(db,"rooms/"+ROOM+"/presence/"+CLIENT);
      onValue(ref(db,".info/connected"),(s)=>{
        if(s.val()===true){
          onDisconnect(myPres).remove();
          set(myPres,{os:DEV,t:Date.now()});
          // 퇴장 기록: 연결 끊기면 서버가 자동으로 로그 한 줄 남김(시각=실제 끊긴 시각)
          const leaveRef=push(logsRef);
          onDisconnect(leaveRef).set({t:serverTimestamp(),type:"leave",os:INFO.os,br:INFO.br,device:INFO.device,ref:INFO.ref,lang:INFO.lang,ip:myIP||"?",msg:"나갔어요"});
        }
      });
      onValue(presRef,(s)=>{ const v=s.val()||{}; showPresence(Object.keys(v).length); });
      onValue(peopleRef,(snap)=>{
        const v=snap.val();
        if(v){ data={people:v}; render(); }
        else { window._set(); }      // 비어있으면 기본값 업로드
      });
      onValue(logsRef,(snap)=>{
        const v=snap.val()||{};
        logs=Object.values(v).sort((a,b)=>b.t-a.t).slice(0,300);
        renderLog();
      });
      onValue(vocRef,(snap)=>{
        const v=snap.val()||{};
        voc=Object.entries(v).map(([key,val])=>({key,...val})).sort((a,b)=>b.t-a.t);
        renderVoc();
      });
      remoteOK=true;
      banner("🟢 실시간 공유 켜짐");
      return;
    }catch(e){
      banner("⚠️ 연결 실패 · 이 기기에만 저장");
    }
  } else {
    banner("ℹ️ 이 기기에만 저장됨");
  }
  // 로컬 폴백
  const saved=localStorage.getItem("crew-sched");
  if(saved){ try{data=JSON.parse(saved);}catch(e){} }
  try{ logs=JSON.parse(localStorage.getItem("crew-logs")||"[]"); }catch(e){ logs=[]; }
  try{ voc=JSON.parse(localStorage.getItem("crew-voc")||"[]"); }catch(e){ voc=[]; }
}
function persist(){
  if(remoteOK&&window._set){ window._set(); }
  else { localStorage.setItem("crew-sched",JSON.stringify(data)); }
  flashSave();
}
let saveT;
function flashSave(){
  const el=document.getElementById("saveState");
  el.textContent=remoteOK?"✓ 모두에게 저장됨":"✓ 저장됨(이 기기)";
  clearTimeout(saveT); saveT=setTimeout(()=>el.textContent="",1500);
}
function banner(html){
  const m=document.getElementById("bannerMsg");
  m.style.display="inline-block"; m.innerHTML=html;
}

/* ---------- 가용시간 계산 ---------- */
const MEET_START=600, MEET_END=1440;   // 약속 잡기 좋은 시간대: 10:00 ~ 24:00
function clampWin(s,e){                 // 시간대 안으로 자르고, 1시간 미만이면 버림
  s=Math.max(s,MEET_START); e=Math.min(e,MEET_END);
  return (e-s>=60)?[s,e]:null;
}
function freeIntervals(d){              // 근무시간 빼고, 약속 시간대 안의 빈 시간만
  if(d.off){ const w=clampWin(MEET_START,MEET_END); return w?[w]:[]; }
  const s=toMin(d.start), e=toMin(d.end), out=[];
  const before=clampWin(MEET_START,s); if(before) out.push(before);  // 출근 전
  const after =clampWin(e,MEET_END);   if(after)  out.push(after);   // 퇴근 후
  return out;
}
function intersect(a,b){
  const out=[];
  for(const [s1,e1] of a) for(const [s2,e2] of b){
    const s=Math.max(s1,s2), e=Math.min(e1,e2);
    if(e-s>=60) out.push([s,e]);   // 1시간 이상 겹칠 때만
  }
  return out;
}
function commonFree(ids,dayKey){
  const list=ids.map(id=>freeIntervals(data.people[id].sched[dayKey]||OFF));
  if(!list.length) return [];
  return list.reduce((acc,cur)=>intersect(acc,cur));
}
function offCount(ids,dayKey){
  return ids.filter(id=>(data.people[id].sched[dayKey]||OFF).off).length;
}

/* ---------- 기록(로그) ---------- */
function addLog(type,msg){
  const e={t:Date.now(), type, os:INFO.os, br:INFO.br, device:INFO.device, ref:INFO.ref, lang:INFO.lang, ip:myIP||"?", msg};
  if(remoteOK && window._logPush){ window._logPush(e); }
  else { logs.unshift(e); logs=logs.slice(0,300); localStorage.setItem("crew-logs",JSON.stringify(logs)); renderLog(); }
}
function logEntryOnce(){   // 새로고침 도배 방지: 30분에 한 번만 입장 기록
  const k="crew-lastenter", now=Date.now(), last=+localStorage.getItem(k)||0;
  if(now-last > 30*60*1000){ addLog("enter","입장했어요"); localStorage.setItem(k,String(now)); }
}
const LOG_PAGE=15;   // 페이지당 로그 수
function renderLog(){
  const box=document.getElementById("logList");
  if(!box) return;
  if(!logs.length){ box.innerHTML=`<div class="nofree">아직 기록이 없어요.</div>`; return; }
  const pages=Math.ceil(logs.length/LOG_PAGE);
  if(logPage>=pages) logPage=pages-1;
  if(logPage<0) logPage=0;
  const slice=logs.slice(logPage*LOG_PAGE, logPage*LOG_PAGE+LOG_PAGE);
  const items=slice.map(e=>{
    const d=new Date(e.t);
    const w=`${d.getMonth()+1}/${d.getDate()} ${String(d.getHours()).padStart(2,"0")}:${String(d.getMinutes()).padStart(2,"0")}`;
    const icon=e.type==="enter"?"👋":e.type==="leave"?"🚪":"✏️";
    const osbr=[e.os,e.br].filter(Boolean).join("·") || (e.dev||"");   // 옛 로그 호환
    const device=e.device||e.plat||"";
    const meta=[device, osbr, e.lang].filter(Boolean).map(esc).join(" · ");
    const ref=(e.ref && e.ref!=="직접 접속")?`유입: ${esc(e.ref)}`:"";
    const ip=e.ip?`IP ${esc(e.ip)}`:"";
    const sub=[meta, ref, ip].filter(Boolean).join("  ·  ");
    return `<div class="logitem">
      <div class="lrow1"><span class="lwhen">${w}</span><span>${icon}</span><span class="lmsg">${esc(e.msg)}</span></div>
      <div class="lrow2">${sub}</div>
    </div>`;
  }).join("");
  const pager = pages>1 ? `<div class="pager">
      <button id="logPrev" ${logPage===0?"disabled":""}>‹ 이전</button>
      <span>${logPage+1} / ${pages}</span>
      <button id="logNext" ${logPage>=pages-1?"disabled":""}>다음 ›</button>
    </div>` : "";
  box.innerHTML=items+pager;
  const prev=document.getElementById("logPrev"), next=document.getElementById("logNext");
  if(prev) prev.onclick=()=>{ logPage--; renderLog(); };
  if(next) next.onclick=()=>{ logPage++; renderLog(); };
}

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
function renderVoc(){
  const box=document.getElementById("vocList");
  if(!box) return;
  const ae=document.activeElement;   // 댓글 입력 중이면 다시 그리지 않음(포커스 보존)
  if(ae && box.contains(ae) && ae.classList.contains("cmt-input")) return;
  if(!voc.length){ box.innerHTML=`<div class="nofree">아직 의견이 없어요. 처음으로 남겨보세요! 🙌</div>`; return; }
  box.innerHTML=voc.map(v=>{
    const cmts=v.comments ? Object.entries(v.comments).map(([ck,cv])=>({ck,...cv})).sort((a,b)=>a.t-b.t) : [];
    const cmtHTML=cmts.map(c=>`<div class="cmt">
        <span class="cmt-meta"><b>${esc(c.who||"익명")}</b> ${hhmm(c.t)}</span>
        <span class="cmt-text">${esc(c.text)}</span>
        <button class="cmt-del" data-k="${v.key}" data-ck="${c.ck}" title="삭제">×</button>
      </div>`).join("");
    return `<div class="voc${v.done?' done':''}">
      <div class="voctop"><b>${esc(v.who||"익명")}</b><span class="vocwhen">${hhmm(v.t)}</span></div>
      <div class="voctext">${esc(v.text)}</div>
      <div class="vocbtns">
        <button data-vdone="${v.key}">${v.done?"↩ 되돌리기":"✓ 완료"}</button>
        <button data-vdel="${v.key}">삭제</button>
      </div>
      <div class="cmt-list">
        ${cmtHTML || '<div class="cmt-empty">아직 댓글이 없어요</div>'}
        <div class="cmt-add">
          <input class="cmt-input" data-k="${v.key}" placeholder="댓글 달기…" maxlength="200">
          <button class="cmt-btn" data-k="${v.key}">등록</button>
        </div>
      </div>
    </div>`;
  }).join("");
  box.querySelectorAll("[data-vdone]").forEach(b=>b.onclick=()=>vocToggle(b.dataset.vdone));
  box.querySelectorAll("[data-vdel]").forEach(b=>b.onclick=()=>{ if(confirm("이 의견을 삭제할까요?")) vocDel(b.dataset.vdel); });
  box.querySelectorAll(".cmt-del").forEach(b=>b.onclick=()=>{ if(confirm("댓글을 삭제할까요?")) vocCommentDel(b.dataset.k,b.dataset.ck); });
  box.querySelectorAll(".cmt-btn").forEach(b=>b.onclick=()=>{
    const inp=box.querySelector(`.cmt-input[data-k="${b.dataset.k}"]`);
    const text=(inp.value||"").trim(); if(!text){ inp.focus(); return; }
    vocCommentAdd(b.dataset.k,text);
  });
  box.querySelectorAll(".cmt-input").forEach(inp=>inp.onkeydown=(e)=>{
    if(e.key==="Enter"){ const text=(inp.value||"").trim(); if(text) vocCommentAdd(inp.dataset.k,text); }
  });
}

/* ---------- 렌더링 ---------- */
function render(){
  ensureWhoSelection();
  renderWeek(); renderMeet(); renderEdit(); renderCalendar(); renderLog(); renderVoc();
}
function ids(){   // ord(순서) 기준 정렬, 없으면 키 순서로 폴백
  return Object.keys(data.people).sort((a,b)=>{
    const oa=data.people[a].ord, ob=data.people[b].ord;
    const na=(oa==null?1e9:oa), nb=(ob==null?1e9:ob);
    if(na!==nb) return na-nb;
    return a<b?-1:a>b?1:0;
  });
}
function ensureOrds(){   // 모든 친구에게 현재 표시 순서대로 ord 부여
  ids().forEach((id,i)=>{ data.people[id].ord=i; });
}

/* ---------- 달력 ---------- */
const DOW2KEY=["sun","mon","tue","wed","thu","fri","sat"]; // JS getDay() 순서
let calY, calM, calSel=null;   // 표시중 연/월(0-based), 선택된 날짜(ISO)
function todayISO(){const d=new Date();return d.getFullYear()+"-"+(d.getMonth()+1)+"-"+d.getDate();}
function isoOf(y,m,d){return y+"-"+(m+1)+"-"+d;}
function renderCalendar(){
  if(calY==null){const t=new Date();calY=t.getFullYear();calM=t.getMonth();}
  document.getElementById("calTitle").textContent=calY+"년 "+(calM+1)+"월";
  const grid=document.getElementById("calGrid");
  const wd=["일","월","화","수","목","금","토"];
  let h=wd.map((w,i)=>`<div class="wd ${i===0?'sun':i===6?'sat':''}">${w}</div>`).join("");
  const first=new Date(calY,calM,1).getDay();
  const days=new Date(calY,calM+1,0).getDate();
  for(let i=0;i<first;i++) h+=`<div class="cell empty"></div>`;
  const chosen=[...(selectedWho||new Set(ids()))];
  const today=todayISO();
  for(let d=1;d<=days;d++){
    const dow=new Date(calY,calM,d).getDay();
    const key=DOW2KEY[dow];
    const off=offCount(chosen,key);
    const iso=isoOf(calY,calM,d);
    const cls=[ "cell", dow===0?"sun":dow===6?"sat":"",
      iso===today?"today":"", iso===calSel?"sel":"" ].join(" ");
    const dots=off>0?`<div class="dots">${"<span class='dot'></span>".repeat(off)}</div>`:"";
    h+=`<div class="${cls}" data-iso="${iso}">
        <span class="dn">${d}</span>${dots}
        ${off>0?`<span class="free">${off}명 휴무</span>`:""}
      </div>`;
  }
  grid.innerHTML=h;
  grid.querySelectorAll(".cell[data-iso]").forEach(c=>c.onclick=()=>{
    calSel=c.dataset.iso; renderCalendar();
  });
  renderCalDetail();
}
function renderCalDetail(){
  const box=document.getElementById("calDetail");
  if(!calSel){ box.innerHTML=`<div class="nofree">날짜를 선택하세요.</div>`; return; }
  const [y,m,d]=calSel.split("-").map(Number);
  const dow=new Date(y,m-1,d).getDay();
  const key=DOW2KEY[dow];
  const wdName=["일","월","화","수","목","금","토"][dow];
  const chosen=[...(selectedWho||new Set(ids()))];
  let rows="";
  for(const id of ids()){
    const s=data.people[id].sched[key]||OFF;
    rows+=`<div class="pstat">
      <span class="tag ${s.off?'off':'work'}">${s.off?'휴무':s.start+'~'+s.end}</span>
      <b>${esc(data.people[id].name)}</b></div>`;
  }
  const free=commonFree(chosen,key);
  const allDay=free.length===1&&free[0][0]===MEET_START&&free[0][1]===MEET_END;
  let slot;
  if(!chosen.length) slot=`<div class="nofree">"🥂 약속" 탭에서 모일 친구를 먼저 골라주세요.</div>`;
  else if(allDay) slot=`<div class="slots"><span class="slot">하루 종일 가능 🎉</span></div>`;
  else if(free.length) slot=`<div class="slots">${free.map(([a,b])=>`<span class="slot">${fmt(a)} ~ ${fmt(b)}</span>`).join("")}</div>`;
  else slot=`<div class="nofree">이날은 다 같이 비는 시간이 없어요 😢</div>`;
  box.innerHTML=`<h3>${m}월 ${d}일 (${wdName}) — 선택한 친구 다 같이 가능한 시간</h3>
    ${slot}
    <h3 style="margin-top:12px;font-size:13px;color:var(--muted)">전체 친구 이날 상태</h3>
    ${rows}`;
}
document.getElementById("calPrev").onclick=()=>{ calM--; if(calM<0){calM=11;calY--;} renderCalendar(); };
document.getElementById("calNext").onclick=()=>{ calM++; if(calM>11){calM=0;calY++;} renderCalendar(); };

function renderWeek(){
  const t=document.getElementById("weekTable");
  let h="<tr><th>이름</th>";
  DAYS.forEach(([k,l],i)=>h+=`<th class="${i>=5?'we':''}">${l}</th>`);
  h+="</tr>";
  for(const id of ids()){
    const p=data.people[id];
    h+=`<tr><td class="name">${avatarHTML(p,"sm")}${esc(p.name)}</td>`;
    DAYS.forEach(([k],i)=>{
      const d=p.sched[k]||OFF;
      const we=i>=5?" we":"";
      h+= d.off
        ? `<td class="off${we}">휴무</td>`
        : `<td class="work${we}"><span class="hrs">${shortT(d.start)}<b>~</b>${shortT(d.end)}</span></td>`;
    });
    h+="</tr>";
  }
  t.innerHTML=h;
}

function ensureWhoSelection(){
  if(!selectedWho) selectedWho=new Set(ids());
  // 삭제된 사람 정리 / 새 사람 자동 포함
  for(const id of [...selectedWho]) if(!data.people[id]) selectedWho.delete(id);
}
function renderMeet(){
  const who=document.getElementById("whoList");
  who.innerHTML=ids().map(id=>{
    const c=selectedWho.has(id)?"checked":"";
    return `<label><input type="checkbox" data-who="${id}" ${c}> ${avatarHTML(data.people[id],"sm")}${esc(data.people[id].name)}</label>`;
  }).join("");
  who.querySelectorAll("input").forEach(inp=>inp.onchange=()=>{
    const id=inp.dataset.who;
    inp.checked?selectedWho.add(id):selectedWho.delete(id);
    renderMeet();
  });

  const chosen=[...selectedWho];
  const ml=document.getElementById("meetList");
  if(chosen.length===0){ ml.innerHTML=`<div class="nofree">위에서 모일 친구를 골라주세요.</div>`; return; }

  // 각 요일 점수 계산 → 최고 점수에 ⭐
  const rows=DAYS.map(([k,l],i)=>{
    const free=commonFree(chosen,k);
    const offc=offCount(chosen,k);
    const allDay=free.length===1&&free[0][0]===MEET_START&&free[0][1]===MEET_END;
    const totalFree=free.reduce((s,[a,b])=>s+(b-a),0);
    const score=offc*1000 + totalFree;   // 다 쉬는 날 우선, 그다음 겹치는 시간 길이
    return {k,l,i,free,offc,allDay,totalFree,score};
  });
  const best=Math.max(...rows.map(r=>r.score));

  ml.innerHTML=rows.map(r=>{
    const isBest=r.score===best&&r.score>0;
    let body;
    if(r.allDay){
      body=`<div class="slots"><span class="slot">하루 종일 가능 🎉</span></div>`;
    } else if(r.free.length){
      body=`<div class="slots">${r.free.map(([a,b])=>`<span class="slot">${fmt(a)} ~ ${fmt(b)}</span>`).join("")}</div>`;
    } else {
      body=`<div class="nofree">다 같이 비는 시간이 없어요 😢</div>`;
    }
    const badge=r.offc>0?`<span class="badge">${r.offc}명 휴무</span>`:`<span class="badge none">전원 근무</span>`;
    return `<div class="meet-day ${isBest?'best':''}">
      <div class="top">
        <span class="d ${r.i>=5?'we':''}">${isBest?'<span class="star">⭐</span> ':''}${r.l}요일</span>
        ${badge}
      </div>${body}</div>`;
  }).join("");
}

function offSummary(p){
  const offs=DAYS.filter(([k])=>(p.sched[k]||OFF).off).map(([k,l])=>l);
  if(offs.length===0) return "휴무 없음";
  if(offs.length===7) return "매일 휴무";
  return "휴무 "+offs.join("·");
}
function avatarHTML(p, cls){   // 프로필 사진(없으면 이름 첫 글자)
  const c="avatar"+(cls?" "+cls:"");
  if(p.avatar) return `<img class="${c}" src="${p.avatar}" alt="">`;
  const ini=esc((p.name||"?").trim().slice(0,1)||"?");
  return `<span class="${c} ph">${ini}</span>`;
}
function pickAvatar(id){       // 갤러리/카메라에서 골라 128px JPEG로 줄여 저장
  const inp=document.createElement("input");
  inp.type="file"; inp.accept="image/*";
  inp.onchange=()=>{
    const f=inp.files&&inp.files[0]; if(!f) return;
    const img=new Image();
    img.onload=()=>{
      const size=128, cv=document.createElement("canvas");
      cv.width=cv.height=size;
      const ctx=cv.getContext("2d");
      const sc=Math.max(size/img.width,size/img.height);
      const w=img.width*sc, h=img.height*sc;
      ctx.drawImage(img,(size-w)/2,(size-h)/2,w,h);   // 정사각 가운데 크롭
      if(data.people[id]){
        data.people[id].avatar=cv.toDataURL("image/jpeg",0.7);
        addLog("edit","프로필 사진 변경");
        persist(); render();
      }
      URL.revokeObjectURL(img.src);
    };
    img.onerror=()=>{ alert("이미지를 불러오지 못했어요."); URL.revokeObjectURL(img.src); };
    img.src=URL.createObjectURL(f);
  };
  inp.click();
}
function renderEdit(){
  const wrap=document.getElementById("editList");
  // 이름/시간 입력칸을 치는 중이면 다시 그리지 않음(포커스·커서 보존)
  const ae=document.activeElement;
  if(ae && wrap.contains(ae) && (ae.classList.contains("nm") || ae.type==="time")) return;
  wrap.innerHTML="";
  for(const id of ids()){
    const p=data.people[id];
    const card=document.createElement("div");
    card.className="person"+(id===editOpen?" open":"");
    card.dataset.id=id;
    let rows="";
    DAYS.forEach(([k,l],i)=>{
      const d=p.sched[k]||OFF;
      rows+=`<div class="dayrow">
        <span class="daylbl ${i>=5?'we':''}">${l}</span>
        <div class="toggle ${d.off?'off':''}" data-id="${id}" data-day="${k}">${d.off?'휴무':'근무'}</div>
        <div class="times ${d.off?'hidden':''}">
          <input type="time" value="${d.start}" data-id="${id}" data-day="${k}" data-f="start">
          <span>~</span>
          <input type="time" value="${d.end}" data-id="${id}" data-day="${k}" data-f="end">
        </div>
      </div>`;
    });
    card.innerHTML=`
      <div class="hd">
        <span class="drag" title="드래그로 순서 변경">⠿</span>
        ${avatarHTML(p)}
        <span class="hdmain">
          <span class="pname">${esc(p.name)}</span>
          <span class="psum">${offSummary(p)}</span>
        </span>
        <span class="chev">▸</span>
      </div>
      <div class="body">
        <div class="profrow">
          <button class="avatarbtn" data-pick="${id}" title="사진 변경">
            ${avatarHTML(p,"big")}
            <!-- <span class="cam">📷</span> -->
          </button>
          <div class="namerow">
            <label>이름</label>
            <input class="nm" value="${esc(p.name)}" data-id="${id}" data-orig="${esc(p.name)}" data-f="name">
          </div>
        </div>
        ${rows}
        <button class="del" data-del="${id}">🗑 이 친구 삭제</button>
      </div>`;
    wrap.appendChild(card);
  }
  // 헤더 탭 → 펼치기/접기 (드래그 핸들은 제외, 한 번에 하나만 열림)
  wrap.querySelectorAll(".person .hd").forEach(hd=>hd.onclick=(e)=>{
    if(e.target.closest(".drag")) return;   // 핸들 클릭은 무시
    const id=hd.parentElement.dataset.id;
    editOpen=(editOpen===id)?null:id;
    wrap.querySelectorAll(".person").forEach(c=>c.classList.toggle("open", c.dataset.id===editOpen));
  });
  // 드래그로 순서 변경 (SortableJS, 핸들 ⠿ 로만)
  if(window.Sortable){
    if(wrap._sortable) wrap._sortable.destroy();
    wrap._sortable=Sortable.create(wrap,{
      handle:".drag", animation:150, delayOnTouchOnly:true, delay:80,
      onEnd:()=>{
        [...wrap.querySelectorAll(".person")].forEach((c,i)=>{ if(data.people[c.dataset.id]) data.people[c.dataset.id].ord=i; });
        addLog("edit","순서 변경");
        persist(); render();
      }
    });
  }
  // 이벤트 바인딩
  wrap.querySelectorAll("[data-pick]").forEach(b=>b.onclick=(e)=>{ e.stopPropagation(); pickAvatar(b.dataset.pick); });
  wrap.querySelectorAll(".toggle").forEach(t=>t.onclick=()=>{
    const p=data.people[t.dataset.id], d=p.sched[t.dataset.day];
    d.off=!d.off;
    addLog("edit",`${p.name} ${DAYLBL[t.dataset.day]} ${d.off?"근무→휴무":"휴무→근무"}`);
    persist(); render();
  });
  wrap.querySelectorAll("input[type=time]").forEach(inp=>inp.onchange=()=>{
    const p=data.people[inp.dataset.id], day=inp.dataset.day;
    p.sched[day][inp.dataset.f]=inp.value;
    const s=p.sched[day];
    addLog("edit",`${p.name} ${DAYLBL[day]} 시간 ${s.start}~${s.end}`);
    persist();
  });
  let nameT;
  wrap.querySelectorAll("input.nm").forEach(inp=>{
    inp.oninput=()=>{
      data.people[inp.dataset.id].name=inp.value;
      const pn=inp.closest(".person").querySelector(".pname");
      if(pn) pn.textContent=inp.value;   // 헤더 이름도 즉시 반영
      renderWeek();                       // 주간표도 즉시 반영
      clearTimeout(nameT);                // 저장은 입력이 멈춘 뒤 한 번만
      nameT=setTimeout(persist,500);
    };
    inp.onblur=()=>{ clearTimeout(nameT);
      if(inp.value!==inp.dataset.orig){
        addLog("edit",`이름 변경: ${inp.dataset.orig} → ${inp.value}`);
        inp.dataset.orig=inp.value;
      }
      persist();
    };
  });
  wrap.querySelectorAll("button.del").forEach(b=>b.onclick=()=>{
    const nm=data.people[b.dataset.del].name;
    if(confirm("정말 삭제할까요?")){ delete data.people[b.dataset.del]; addLog("edit",`${nm} 삭제`); persist(); render(); }
  });
}

document.getElementById("addPerson").onclick=()=>{
  const id="p"+Date.now();
  const sched={};
  DAYS.forEach(([k])=>sched[k]=W("09:00","18:00"));
  ensureOrds();                // 기존 친구들 순서 확정
  const maxOrd=Math.max(-1,...Object.values(data.people).map(p=>p.ord==null?-1:p.ord));
  data.people[id]={name:"새 친구",sched,ord:maxOrd+1};   // 맨 아래로
  editOpen=id;                 // 새 친구는 바로 펼쳐서 편집
  addLog("edit","친구 추가");
  persist(); render();
  document.querySelector('[data-tab=edit]').click();
};

/* ---------- 주간표 이미지로 저장 (canvas로 직접 그림 → PNG 다운로드) ---------- */
function downloadWeekImage(){
  const people=ids().map(id=>data.people[id]);
  if(!people.length){ alert("저장할 스케줄이 없어요."); return; }
  const S=2;                                   // 선명도(레티나)
  const nameW=92, dayW=80, titleH=54, headH=36, rowH=44;
  const W=nameW+dayW*7, H=titleH+headH+rowH*people.length;
  const cv=document.createElement("canvas");
  cv.width=W*S; cv.height=H*S;
  const ctx=cv.getContext("2d");
  ctx.scale(S,S);
  const FONT='-apple-system,"Malgun Gothic","Apple SD Gothic Neo",sans-serif';
  const clip=(s,n)=>{s=String(s);return s.length>n?s.slice(0,n)+"…":s;};
  const box=(x,y,w,h,bg,we)=>{ ctx.fillStyle=bg; ctx.fillRect(x,y,w,h);
    if(we){ctx.fillStyle="rgba(240,184,110,.08)";ctx.fillRect(x,y,w,h);}
    ctx.strokeStyle="#2a3140"; ctx.lineWidth=1; ctx.strokeRect(x+.5,y+.5,w,h); };
  const txt=(s,x,y,w,color,size,bold)=>{ ctx.fillStyle=color;
    ctx.font=(bold?"bold ":"")+size+"px "+FONT; ctx.textAlign="center"; ctx.textBaseline="middle";
    ctx.fillText(s,x+w/2,y); };
  // 배경 + 제목
  ctx.fillStyle="#11151c"; ctx.fillRect(0,0,W,H);
  ctx.fillStyle="#e7ecf3"; ctx.font="bold 17px "+FONT; ctx.textAlign="left"; ctx.textBaseline="middle";
  ctx.fillText("우리끼리 스케줄",14,22);
  let dateStr=""; try{ dateStr="기준 "+new Date().toLocaleDateString("ko-KR"); }catch(e){}
  ctx.fillStyle="#94a0b3"; ctx.font="11px "+FONT; ctx.fillText(dateStr,14,40);
  // 헤더
  const top=titleH;
  box(0,top,nameW,headH,"#1f2430"); txt("이름",0,top+headH/2,nameW,"#94a0b3",12,true);
  DAYS.forEach(([k,l],i)=>{ const we=i>=5, x=nameW+i*dayW;
    box(x,top,dayW,headH,"#1f2430",we); txt(l,x,top+headH/2,dayW,we?"#f0b86e":"#94a0b3",12,true); });
  // 행
  people.forEach((p,r)=>{ const y=top+headH+r*rowH;
    box(0,y,nameW,rowH,"#1f2430"); txt(clip(p.name,6),0,y+rowH/2,nameW,"#e7ecf3",13,true);
    DAYS.forEach(([k],i)=>{ const d=p.sched[k]||OFF, we=i>=5, x=nameW+i*dayW;
      if(d.off){ box(x,y,dayW,rowH,"#243b2b",we); txt("휴무",x,y+rowH/2,dayW,"#7ee2a6",12,false); }
      else{ box(x,y,dayW,rowH,"#3a2330",we);
        txt(shortT(d.start),x,y+rowH/2-8,dayW,"#f3a8c2",12,false);
        txt("~",x,y+rowH/2+1,dayW,"#f3a8c2",9,false);
        txt(shortT(d.end),x,y+rowH/2+10,dayW,"#f3a8c2",12,false); } });
  });
  cv.toBlob(b=>{ const a=document.createElement("a");
    a.href=URL.createObjectURL(b); a.download="우리끼리-스케줄.png";
    document.body.appendChild(a); a.click(); a.remove();
    setTimeout(()=>URL.revokeObjectURL(a.href),1000); },"image/png");
}
document.getElementById("dlImg").onclick=downloadWeekImage;

/* ---------- 입장: 비밀번호 게이트 → 이름 선택 ---------- */
const GATE_PW="000115";
function setupGate(){
  const gate=document.getElementById("gate");
  if(localStorage.getItem("crew-gate")==="ok"){ gate.classList.add("hidden"); logEntryOnce(); return; }
  const pw=document.getElementById("gatePw"),
        btn=document.getElementById("gateBtn"),
        err=document.getElementById("gateErr");
  const tryIn=()=>{
    if(pw.value===GATE_PW){ localStorage.setItem("crew-gate","ok"); gate.classList.add("hidden"); logEntryOnce(); }
    else { err.textContent="비밀번호가 틀렸어요 😅"; pw.value=""; pw.focus(); }
  };
  btn.onclick=tryIn;
  pw.onkeydown=(e)=>{ if(e.key==="Enter") tryIn(); };
  pw.focus();
}

/* 기록 지우기 */
document.getElementById("clearLog").onclick=()=>{
  if(!confirm("모든 기록을 지울까요? (모두에게서 사라져요)")) return;
  if(remoteOK && window._logClear){ window._logClear(); }
  else { logs=[]; localStorage.removeItem("crew-logs"); renderLog(); }
  addLog("edit","기록을 비웠어요");
};

/* 건의 올리기 */
document.getElementById("vocSubmit").onclick=()=>{
  const t=document.getElementById("vocText");
  const text=(t.value||"").trim();
  if(!text){ t.focus(); return; }
  vocAdd(text);
  t.value="";
};

/* 탭 전환 */
document.querySelectorAll(".tabs button").forEach(b=>b.onclick=()=>{
  document.querySelectorAll(".tabs button").forEach(x=>x.classList.remove("active"));
  b.classList.add("active");
  ["week","cal","meet","edit","voc","log"].forEach(t=>document.getElementById("tab-"+t).style.display="none");
  document.getElementById("tab-"+b.dataset.tab).style.display="block";
});

function esc(s){return String(s).replace(/[&<>"]/g,c=>({"&":"&amp;","<":"&lt;",">":"&gt;",'"':"&quot;"}[c]));}

/* 시작 */
(async function start(){
  fetchIP();            // 공인 IP 비동기로 가져오기(기록용)
  await initStorage();
  render();
  setupGate();          // Firebase 준비 후 게이트 → 기록이 올바른 곳에 저장됨
})();
