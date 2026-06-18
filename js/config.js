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
const VOC_EMOJI=["👍","🤍","😂","😮","🔥"];   // 반응 이모지
let vocReplyOpen=new Set();        // 답글 입력창 열린 의견 key
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
