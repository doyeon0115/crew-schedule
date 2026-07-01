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
