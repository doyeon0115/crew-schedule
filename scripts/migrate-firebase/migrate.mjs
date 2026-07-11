#!/usr/bin/env node
// Firebase RTDB → Spring Boot 백엔드 이관 스크립트.
//
// 사용:
//   node migrate.mjs --export ./our-crew.json \
//                    --backend http://localhost:8080 \
//                    --room our-crew \
//                    --crew-name "우리끼리" \
//                    --password "MigratedPass123!" \
//                    [--dry-run] [--verbose]
//
// - Firebase 콘솔에서 rooms/{ROOM} 노드를 JSON으로 export한 파일을 입력으로 씀
// - who 이름 하나당 새 계정 하나 생성 → 그 계정으로 로그인 → 자기 자신의 API로 데이터 씀
//   (관리자 우회 없이 실사용 흐름 그대로)
// - 재실행 안전: 이메일 중복이면 signup 대신 login으로 이어감
//
// 매핑:
//   people[id]                       → User + PUT /me/schedule
//   voc[id]                          → POST /crews/{id}/posts
//   voc[id].comments[cid]            → POST /posts/{postId}/comments
//   voc[id].comments[cid].replies    → POST /posts/{postId}/comments (parentCommentId)
//   meetups[id]                      → POST /crews/{id}/meetups
//   meetups[id].rsvp[sessionId]      → POST /meetups/{id}/rsvp (name으로 유저 찾기)
//   logs / presence / voc[id].reactions → 스킵

import { readFile } from "node:fs/promises";
import { createHash } from "node:crypto";
import { argv, exit } from "node:process";

// ============ CLI ============

function parseArgs() {
  const args = {};
  for (let i = 2; i < argv.length; i++) {
    const a = argv[i];
    if (a === "--dry-run") args.dryRun = true;
    else if (a === "--verbose") args.verbose = true;
    else if (a.startsWith("--")) {
      args[a.slice(2).replace(/-([a-z])/g, (_, c) => c.toUpperCase())] = argv[++i];
    }
  }
  args.backend ??= "http://localhost:8080";
  args.room ??= "our-crew";
  args.crewName ??= "우리끼리 (legacy)";
  args.password ??= "MigratedPass123!";
  if (!args.export) {
    console.error("Missing --export path/to/rtdb-export.json");
    exit(1);
  }
  return args;
}

const args = parseArgs();
const log = (msg) => args.verbose && console.log(msg);
const info = (msg) => console.log(msg);

// ============ 백엔드 API 클라이언트 ============

/** 세션당 access/refresh 토큰을 보관. */
class Session {
  constructor(email, password, userId, accessToken, refreshToken) {
    this.email = email;
    this.password = password;
    this.userId = userId;
    this.accessToken = accessToken;
    this.refreshToken = refreshToken;
  }
}

async function apiCall(method, path, body, accessToken) {
  const headers = {};
  if (body !== undefined) headers["Content-Type"] = "application/json";
  if (accessToken) headers["Authorization"] = `Bearer ${accessToken}`;
  const res = await fetch(`${args.backend}${path}`, {
    method,
    headers,
    body: body === undefined ? undefined : JSON.stringify(body),
  });
  const text = await res.text();
  const parsed = text ? JSON.parse(text) : null;
  if (!res.ok) {
    const msg = parsed?.message ?? res.statusText;
    const err = new Error(`HTTP ${res.status} on ${method} ${path}: ${msg}`);
    err.status = res.status;
    err.code = parsed?.code;
    throw err;
  }
  return parsed?.data;
}

/**
 * 회원가입. 이미 있으면 login으로 fallback.
 * 재실행 시 기존 유저를 그대로 재사용 → idempotent.
 */
async function signupOrLogin(email, password, nickname) {
  try {
    const tokens = await apiCall("POST", "/api/auth/signup", { email, password, nickname });
    return new Session(email, password, tokens.user.id, tokens.accessToken, tokens.refreshToken);
  } catch (e) {
    if (e.code === "EMAIL_ALREADY_EXISTS") {
      const tokens = await apiCall("POST", "/api/auth/login", { email, password });
      return new Session(email, password, tokens.user.id, tokens.accessToken, tokens.refreshToken);
    }
    throw e;
  }
}

// ============ Firebase 덤프 파싱 ============

/**
 * unique nickname 수집: people 이름 + voc/meetup 안의 who·by·rsvp.name 전부.
 * 소문자로 정규화한 이름을 키로 유일화.
 */
function collectWhoNames(room) {
  const names = new Set();

  for (const p of Object.values(room.people ?? {})) {
    if (p?.name) names.add(p.name.trim());
  }
  for (const v of Object.values(room.voc ?? {})) {
    if (v?.who) names.add(v.who.trim());
    for (const c of Object.values(v.comments ?? {})) {
      if (c?.who) names.add(c.who.trim());
      for (const r of Object.values(c.replies ?? {})) {
        if (r?.who) names.add(r.who.trim());
      }
    }
  }
  for (const m of Object.values(room.meetups ?? {})) {
    if (m?.by) names.add(m.by.trim());
    for (const rsvp of Object.values(m.rsvp ?? {})) {
      if (rsvp?.name) names.add(rsvp.name.trim());
    }
  }
  return [...names].filter(Boolean);
}

/** 닉네임 → 결정론적 placeholder 이메일. 재실행 시 같은 이메일이 나와야 idempotent. */
function emailFor(nickname) {
  const hash = createHash("sha1").update(nickname).digest("hex").slice(0, 10);
  return `legacy-${hash}@legacy.local`;
}

/** Firebase RTDB의 sched {mon:{off,start,end}} → 백엔드 SlotRequest 배열 */
function toSlotRequests(sched) {
  const DAY_MAP = {
    mon: "MONDAY", tue: "TUESDAY", wed: "WEDNESDAY",
    thu: "THURSDAY", fri: "FRIDAY", sat: "SATURDAY", sun: "SUNDAY",
  };
  const requests = [];
  for (const [key, iso] of Object.entries(DAY_MAP)) {
    const slot = sched?.[key];
    if (!slot) {
      requests.push({ dayOfWeek: iso, off: true, startTime: null, endTime: null });
      continue;
    }
    if (slot.off) {
      requests.push({ dayOfWeek: iso, off: true, startTime: null, endTime: null });
    } else {
      requests.push({
        dayOfWeek: iso,
        off: false,
        startTime: `${slot.start}:00`,
        endTime: `${slot.end}:00`,
      });
    }
  }
  return requests;
}

// ============ 마이그레이션 단계 ============

/** 모든 유저를 signup + login. name → Session 맵 반환. */
async function migrateUsers(names) {
  const sessions = new Map();
  for (const name of names) {
    const email = emailFor(name);
    if (args.dryRun) {
      info(`[dry-run] signup ${name} → ${email}`);
      sessions.set(name, new Session(email, args.password, -1, "dummy", "dummy"));
      continue;
    }
    const s = await signupOrLogin(email, args.password, name);
    log(`  user "${name}" → id=${s.userId}`);
    sessions.set(name, s);
  }
  info(`✓ users: ${sessions.size}`);
  return sessions;
}

/**
 * 첫 유저가 크루를 만들고 나머지 유저는 초대코드로 join.
 * 재실행 시 이미 크루가 있으면 첫 유저의 크루 목록에서 이름 매칭으로 재사용.
 */
async function migrateCrew(sessions, crewName) {
  const [firstName, firstSession] = sessions.entries().next().value ?? [];
  if (!firstSession) throw new Error("no users to create crew for");

  if (args.dryRun) {
    info(`[dry-run] create crew "${crewName}" as ${firstName}, join ${sessions.size - 1} more`);
    return { id: -1, name: crewName, inviteCode: "DRYRUN" };
  }

  // 이미 만든 크루가 있는지 확인
  const myCrews = await apiCall("GET", "/api/crews", undefined, firstSession.accessToken);
  let crew = myCrews.find((c) => c.name === crewName);
  if (!crew) {
    crew = await apiCall("POST", "/api/crews", { name: crewName }, firstSession.accessToken);
    log(`  crew created: ${crew.name} (invite=${crew.inviteCode})`);
  } else {
    log(`  crew reused: ${crew.name} (invite=${crew.inviteCode})`);
  }

  for (const [name, session] of sessions.entries()) {
    if (name === firstName) continue;
    try {
      await apiCall("POST", "/api/crews/join", { inviteCode: crew.inviteCode }, session.accessToken);
      log(`  ${name} joined`);
    } catch (e) {
      if (e.code === "ALREADY_CREW_MEMBER") log(`  ${name} already member`);
      else throw e;
    }
  }
  info(`✓ crew: ${crew.name} (id=${crew.id})`);
  return crew;
}

async function migrateSchedules(people, sessions) {
  let count = 0;
  for (const person of Object.values(people ?? {})) {
    const session = sessions.get(person.name?.trim());
    if (!session) continue;
    const slots = toSlotRequests(person.sched);
    if (args.dryRun) {
      info(`[dry-run] PUT /me/schedule for ${person.name}: ${slots.length} slots`);
      count++;
      continue;
    }
    await apiCall("PUT", "/api/me/schedule", slots, session.accessToken);
    count++;
  }
  info(`✓ schedules: ${count}`);
}

async function migrateVoc(voc, crew, sessions) {
  let postCount = 0, commentCount = 0, replyCount = 0;
  for (const v of Object.values(voc ?? {})) {
    const authorSession = sessions.get(v.who?.trim());
    if (!authorSession || !v.text) continue;

    if (args.dryRun) {
      info(`[dry-run] POST post by ${v.who}: ${v.text.slice(0, 40)}...`);
      postCount++;
      for (const c of Object.values(v.comments ?? {})) {
        commentCount++;
        for (const _r of Object.values(c.replies ?? {})) replyCount++;
      }
      continue;
    }

    // 게시글의 title은 legacy에 없으므로 본문 앞부분에서 잘라 사용
    const title = v.text.length <= 60 ? v.text : v.text.slice(0, 60) + "…";
    const post = await apiCall(
      "POST",
      `/api/crews/${crew.id}/posts`,
      { title, content: v.text },
      authorSession.accessToken,
    );
    postCount++;

    // 댓글은 legacy id 순서(사전순)로 → 시간순과 근사
    const comments = Object.entries(v.comments ?? {}).sort(([a], [b]) => a.localeCompare(b));
    for (const [, c] of comments) {
      const commenterSession = sessions.get(c.who?.trim());
      if (!commenterSession || !c.text) continue;
      const created = await apiCall(
        "POST",
        `/api/posts/${post.id}/comments`,
        { content: c.text, parentCommentId: null },
        commenterSession.accessToken,
      );
      commentCount++;

      const replies = Object.entries(c.replies ?? {}).sort(([a], [b]) => a.localeCompare(b));
      for (const [, r] of replies) {
        const replierSession = sessions.get(r.who?.trim());
        if (!replierSession || !r.text) continue;
        await apiCall(
          "POST",
          `/api/posts/${post.id}/comments`,
          { content: r.text, parentCommentId: created.id },
          replierSession.accessToken,
        );
        replyCount++;
      }
    }
  }
  info(`✓ posts: ${postCount}, comments: ${commentCount}, replies: ${replyCount}`);
}

async function migrateMeetups(meetups, crew, sessions) {
  const RSVP_MAP = { go: "ATTEND", maybe: "MAYBE", no: "ABSENT" };
  let meetupCount = 0, rsvpCount = 0;

  for (const m of Object.values(meetups ?? {})) {
    const creatorSession = sessions.get(m.by?.trim());
    if (!creatorSession || !m.date) continue;

    const title = `${m.date}${m.place ? " · " + m.place : ""} 모임`;
    const startTime = m.time ? `${m.time}:00` : "19:00:00";

    if (args.dryRun) {
      info(`[dry-run] POST meetup by ${m.by} on ${m.date}`);
      meetupCount++;
      for (const _r of Object.values(m.rsvp ?? {})) rsvpCount++;
      continue;
    }

    const created = await apiCall(
      "POST",
      `/api/crews/${crew.id}/meetups`,
      {
        title,
        meetDate: m.date,
        startTime,
        location: m.place ?? null,
        memo: null,
        participantUserIds: [], // 크루 전원 초대
        capacity: null,
      },
      creatorSession.accessToken,
    );
    meetupCount++;

    for (const rsvp of Object.values(m.rsvp ?? {})) {
      const participantSession = sessions.get(rsvp.name?.trim());
      const mapped = RSVP_MAP[rsvp.status];
      if (!participantSession || !mapped) continue;
      try {
        await apiCall(
          "POST",
          `/api/meetups/${created.id}/rsvp`,
          { rsvp: mapped },
          participantSession.accessToken,
        );
        rsvpCount++;
      } catch (e) {
        if (e.code === "NOT_MEETUP_PARTICIPANT") {
          // 크루 밖 유저가 RSVP한 경우 — 스킵
          log(`  skip rsvp: ${rsvp.name} not a participant`);
        } else throw e;
      }
    }
  }
  info(`✓ meetups: ${meetupCount}, rsvps: ${rsvpCount}`);
}

// ============ Entry ============

async function main() {
  info(`Reading export from ${args.export}...`);
  const raw = await readFile(args.export, "utf-8");
  const dump = JSON.parse(raw);

  // export 형태가 두 가지 — rooms/our-crew부터 시작이면 dump.rooms[room], 아니면 dump가 곧 room
  const room = dump.rooms?.[args.room] ?? dump[args.room] ?? dump;
  if (!room || typeof room !== "object") {
    console.error(`Cannot find room "${args.room}" in export`);
    exit(1);
  }

  const names = collectWhoNames(room);
  info(`Found ${names.length} unique names: ${names.join(", ")}`);
  if (names.length === 0) {
    console.error("No participants found. Nothing to migrate.");
    exit(1);
  }

  const sessions = await migrateUsers(names);
  const crew = await migrateCrew(sessions, args.crewName);
  await migrateSchedules(room.people, sessions);
  await migrateVoc(room.voc, crew, sessions);
  await migrateMeetups(room.meetups, crew, sessions);

  info("\n✓ Migration complete.");
  if (!args.dryRun) {
    info(`\nCreated users can log in with password: ${args.password}`);
    info(`Emails follow the pattern: legacy-<hash>@legacy.local`);
  }
}

main().catch((e) => {
  console.error("\n✗ Migration failed:", e.message);
  if (args.verbose) console.error(e.stack);
  exit(1);
});
