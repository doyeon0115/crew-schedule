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

/* 로그 탭은 관리자 IP에서만 보이게 (화면 숨김 수준) */
const ADMIN_IP="106.101.196.46";
function applyAdminVisibility(){
  const isAdmin = myIP===ADMIN_IP;
  const tab=document.querySelector('.tabs button[data-tab="log"]');
  if(tab) tab.style.display = isAdmin ? "" : "none";
  if(!isAdmin){   // 관리자 아닌데 로그 탭 보고 있으면 한눈에로 돌림
    const sec=document.getElementById("tab-log");
    if(sec && sec.style.display!=="none") document.querySelector('.tabs button[data-tab="week"]').click();
  }
}

/* PWA: 서비스워커 등록 (https에서만 동작, file://에선 무시됨) */
if("serviceWorker" in navigator){
  window.addEventListener("load",()=>navigator.serviceWorker.register("sw.js").catch(()=>{}));
}

/* 시작 */
(async function start(){
  fetchIP().then(applyAdminVisibility);   // IP 확인 후 로그 탭 노출 결정
  await initStorage();
  render();
  setupGate();          // Firebase 준비 후 게이트 → 기록이 올바른 곳에 저장됨
})();
