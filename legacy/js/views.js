/* ---------- 렌더링 ---------- */
function render(){
  ensureWhoSelection();
  renderWeek(); renderMeet(); renderMeetups(); renderEdit(); renderCalendar(); renderLog(); renderVoc();
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

/* ---------- 약속 확정(meetups) ---------- */
function getMyName(){   // 약속·참석 표시용 내 이름 (한 번 입력 후 기기에 저장)
  let n=localStorage.getItem("crew-myname")||"";
  if(!n){ n=(prompt("이름을 입력하세요 (약속·참석 표시용)")||"").trim(); if(n) localStorage.setItem("crew-myname",n); }
  return n||"익명";
}
function saveLocalMeet(){ localStorage.setItem("crew-meet",JSON.stringify(meetups)); }
function dateLabel(d){
  if(!d) return "";
  const [y,m,day]=d.split("-").map(Number);
  const wd=["일","월","화","수","목","금","토"][new Date(y,m-1,day).getDay()];
  return `${m}/${day} (${wd})`;
}
function toggleMeetForm(show){ const f=document.getElementById("meetForm"); if(f) f.classList.toggle("hidden",!show); }
function meetAdd(){
  const date=document.getElementById("mDate").value;
  if(!date){ alert("날짜를 골라주세요."); return; }
  const e={t:Date.now(), date, time:document.getElementById("mTime").value||"",
    place:document.getElementById("mPlace").value.trim(), by:getMyName()};
  if(remoteOK && window._meetAdd){ window._meetAdd(e); }
  else { e.key="L"+e.t; meetups.push(e); saveLocalMeet(); renderMeetups(); }
  document.getElementById("mPlace").value=""; toggleMeetForm(false);
}
function meetDel(key){
  if(remoteOK && window._meetDel){ window._meetDel(key); }
  else { meetups=meetups.filter(m=>m.key!==key); saveLocalMeet(); renderMeetups(); }
}
function meetRsvp(key,status){
  const m=meetups.find(x=>x.key===key);
  const cur=((m&&m.rsvp&&m.rsvp[CLIENT])||{}).status;
  if(cur===status){   // 같은 거 다시 누르면 취소
    if(remoteOK && window._meetRsvpDel){ window._meetRsvpDel(key); }
    else { if(m&&m.rsvp){ delete m.rsvp[CLIENT]; saveLocalMeet(); renderMeetups(); } }
    return;
  }
  const r={name:getMyName(), status};
  if(remoteOK && window._meetRsvp){ window._meetRsvp(key,r); }
  else { if(!m) return; m.rsvp=m.rsvp||{}; m.rsvp[CLIENT]=r; saveLocalMeet(); renderMeetups(); }
}
function renderMeetups(){
  const box=document.getElementById("meetups");
  if(!box) return;
  if(!meetups.length){ box.innerHTML=`<div class="nofree" style="margin-top:8px">아직 잡힌 약속이 없어요. 위 버튼으로 잡아보세요!</div>`; return; }
  const sorted=[...meetups].sort((a,b)=>(a.date+(a.time||"")).localeCompare(b.date+(b.time||"")));
  const today=new Date().toISOString().slice(0,10);
  const names=(arr,emo)=>arr.length?`<div class="rsvp-line">${emo} ${arr.map(r=>esc(r.name||"익명")).join(", ")}</div>`:"";
  box.innerHTML=sorted.map(m=>{
    const rsvp=m.rsvp||{}, vals=Object.values(rsvp);
    const go=vals.filter(r=>r.status==="go"), maybe=vals.filter(r=>r.status==="maybe"), no=vals.filter(r=>r.status==="no");
    const mine=((rsvp[CLIENT])||{}).status;
    const past=m.date<today;
    return `<div class="meetup${past?" past":""}">
      <div class="meetup-top">
        <span class="meetup-when">📅 ${dateLabel(m.date)}${m.time?` · ${m.time}`:""}${past?" (지남)":""}</span>
        <button class="mdel" data-k="${m.key}" title="삭제">🗑</button>
      </div>
      ${m.place?`<div class="meetup-place">📍 ${esc(m.place)}</div>`:""}
      <div class="meetup-by">제안: ${esc(m.by||"익명")}</div>
      <div class="rsvp-btns">
        <button class="rsvp${mine==="go"?" on":""}" data-k="${m.key}" data-s="go">✅ 갈게${go.length?` ${go.length}`:""}</button>
        <button class="rsvp${mine==="maybe"?" on":""}" data-k="${m.key}" data-s="maybe">🤔 미정${maybe.length?` ${maybe.length}`:""}</button>
        <button class="rsvp${mine==="no"?" on":""}" data-k="${m.key}" data-s="no">❌ 안 가${no.length?` ${no.length}`:""}</button>
      </div>
      ${names(go,"✅")}${names(maybe,"🤔")}${names(no,"❌")}
    </div>`;
  }).join("");
  box.querySelectorAll(".rsvp").forEach(b=>b.onclick=()=>meetRsvp(b.dataset.k,b.dataset.s));
  box.querySelectorAll(".mdel").forEach(b=>b.onclick=()=>{ if(confirm("이 약속을 삭제할까요?")) meetDel(b.dataset.k); });
}
document.getElementById("newMeetBtn").onclick=()=>{ toggleMeetForm(true); const d=document.getElementById("mDate"); if(d&&!d.value) d.value=new Date().toISOString().slice(0,10); };
document.getElementById("mCancel").onclick=()=>toggleMeetForm(false);
document.getElementById("mSave").onclick=meetAdd;
