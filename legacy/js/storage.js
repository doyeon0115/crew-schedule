/* ---------- 저장/동기화 ---------- */
let vocMsgSeen=null;   // 새 건의/댓글/대댓글 감지용
let meetSeen=null;     // 새 약속 감지용
function collectVocMsgs(v){   // 모든 글의 고유 id → 내용
  const out={};
  for(const vk in v){
    const item=v[vk]||{};
    out["v:"+vk]={kind:"건의", who:item.who, text:item.text};
    const comments=item.comments||{};
    for(const ck in comments){
      const c=comments[ck]||{};
      out["c:"+vk+":"+ck]={kind:"댓글", who:c.who, text:c.text};
      const replies=c.replies||{};
      for(const rk in replies){
        const r=replies[rk]||{};
        out["r:"+vk+":"+ck+":"+rk]={kind:"대댓글", who:r.who, text:r.text};
      }
    }
  }
  return out;
}
function notify(title,body){
  try{
    if(!("Notification" in window) || Notification.permission!=="granted") return;
    if(navigator.serviceWorker && navigator.serviceWorker.ready){
      navigator.serviceWorker.ready.then(r=>r.showNotification(title,{body,icon:"icon.svg",badge:"icon.svg"}))
        .catch(()=>{ try{new Notification(title,{body,icon:"icon.svg"});}catch(e){} });
    } else { new Notification(title,{body,icon:"icon.svg"}); }
  }catch(e){}
}
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
      window._reactSet=(key,em,on)=> on                                                       // 이모지 반응 토글
        ? set(ref(db,"rooms/"+ROOM+"/voc/"+key+"/reactions/"+em+"/"+CLIENT),true)
        : remove(ref(db,"rooms/"+ROOM+"/voc/"+key+"/reactions/"+em+"/"+CLIENT));
      window._replyAdd=(key,ck,r)=>push(ref(db,"rooms/"+ROOM+"/voc/"+key+"/comments/"+ck+"/replies"),r);       // 대댓글 추가
      window._replyDel=(key,ck,rk)=>remove(ref(db,"rooms/"+ROOM+"/voc/"+key+"/comments/"+ck+"/replies/"+rk));  // 대댓글 삭제
      const meetRef=ref(db,"rooms/"+ROOM+"/meetups");
      window._meetAdd=(e)=>push(meetRef,e);                                          // 약속 추가
      window._meetDel=(key)=>remove(ref(db,"rooms/"+ROOM+"/meetups/"+key));          // 약속 삭제
      window._meetRsvp=(key,r)=>set(ref(db,"rooms/"+ROOM+"/meetups/"+key+"/rsvp/"+CLIENT),r);   // 참석 표시
      window._meetRsvpDel=(key)=>remove(ref(db,"rooms/"+ROOM+"/meetups/"+key+"/rsvp/"+CLIENT)); // 참석 취소
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
        const msgs=collectVocMsgs(v);
        if(vocMsgSeen!==null && document.hidden){   // 앱 안 보고 있을 때 새 글만 알림
          for(const id in msgs){
            if(!(id in vocMsgSeen)){
              const m=msgs[id];
              notify("새 "+m.kind+" 📮",(m.who||"누군가")+": "+String(m.text||"").slice(0,60));
            }
          }
        }
        vocMsgSeen=msgs;
        renderVoc();
      });
      onValue(meetRef,(snap)=>{
        const v=snap.val()||{};
        meetups=Object.entries(v).map(([key,val])=>({key,...val}));
        if(meetSeen!==null && document.hidden){
          Object.keys(v).filter(k=>!meetSeen.includes(k)).forEach(k=>{
            const it=v[k]; notify("📅 새 약속", dateLabel(it.date)+(it.time?" "+it.time:"")+(it.place?" · "+it.place:""));
          });
        }
        meetSeen=Object.keys(v);
        renderMeetups();
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
  try{ meetups=JSON.parse(localStorage.getItem("crew-meet")||"[]"); }catch(e){ meetups=[]; }
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
