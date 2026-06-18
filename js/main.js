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
