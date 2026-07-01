# 🧭 Crew Schedule — 개발 계획서

Spring Boot + Next.js 전면 재설계를 위한 풀스택 개발 계획.
제품 기획([manyfast 기능명세서](manyfast-기능명세서.md))과 기술 아키텍처를 하나로 통합한 문서다.
이력서/포트폴리오 관점에서 **"왜 이 기술을 썼는지 설명 가능한"** 구조를 목표로 한다.

> 📎 원본 기획서(PRD): [`docs/manyfast-기능명세서.md`](manyfast-기능명세서.md) — 목표·KPI·리스크·수용 기준 원문
> 📎 레거시(바닐라 JS + Firebase): [`docs/legacy/README.firebase.md`](legacy/README.firebase.md)

---

## 1. 제품 개요 (PRD 요약)

- **한 줄 정의**: 친구 그룹의 스케줄을 모아 공통 빈 시간을 찾고, 약속을 확정하고, 실시간으로 소통하는 조율 플랫폼.
- **문제**: 친구들 근무/휴무가 제각각이라 "다 같이 되는 시간"을 매번 수동으로 맞추는 게 번거롭고 오래 걸림.
- **해결**: 스케줄 공유 → **공통 빈 시간 자동 추천** → 약속 생성/RSVP → 채팅·게시판·알림까지 원스톱.
- **타겟**: 근무가 유동적인 20대 후반~30대 초반 직장인·프리랜서 소모임(교대 근무 간호사, 서비스직 등).
- **카테고리**: 소셜/커뮤니티 · **역할**: `USER`, `ADMIN` · **기기**: Web, Mobile(PWA)
- **KPI**: MAU, 그룹 생성률·그룹당 평균 인원, 약속 확정 완료율, 재방문율, 신규 소셜 기능 활용도.
- **주요 리스크**: 사용자 확보/유지, 개인정보 보안, **Firebase→Java/Next 마이그레이션 안정성**, 기능 과부하, 수익모델 부재.

> 이 프로젝트는 초기 [MVP(바닐라 JS + Firebase)](legacy/README.firebase.md)를 정식 서비스로 발전시키는 리라이트다.
> 도메인 로직(공통 가용시간 계산)과 기능 명세를 계승하되, **백엔드를 직접 구현해 서버 역량을 증명**한다.

---

## 2. 기술 스택 결정 & 근거

| 영역 | 선택 | 근거 |
|------|------|------|
| 언어/프레임워크 | Java 21 · Spring Boot 3.3 | 취업 시장 표준, 생태계 성숙 |
| ORM | Spring Data JPA + QueryDSL | 스케줄/약속 복합 조회를 타입 안전하게 |
| 인증 | Spring Security + JWT + **OAuth2(소셜 로그인)** | 무상태 인증, 카카오/구글 로그인 |
| 실시간 | WebSocket(STOMP) + Redis Pub/Sub | 채팅·알림, 스케일아웃 대응 |
| 이벤트 | Apache Kafka | 알림 fan-out 비동기·결합도 분리 |
| 캐시/락 | Redis (Redisson) | 분산 락, 캐시, 프레즌스 |
| DB | MySQL 8 + Flyway | 관계형 도메인, 스키마 버전 관리 |
| 프론트 | Next.js 15 · TS · Tailwind | SSR/PWA, 실시간 UI, 배포 용이 |
| 인프라 | Docker Compose · GitHub Actions | 재현 가능한 환경, 자동화 |
| 테스트 | JUnit5 · Mockito · Testcontainers · k6 | 단위/통합/부하 |

> **레포 구조**: 백엔드(`backend/`)·프론트(`frontend/`)·인프라(`infra/`) 모노레포.
> **범위 결정(2026-07-01)**: 게시판·소셜로그인/프로필·관리자 시스템 **포함**, 다국어(i18n)는 **이번 범위 제외**(추후).

---

## 3. 기능 범위

기획서의 7개 기능 + 기술 심화 요소를 통합한 최종 범위. (다국어는 제외)

| # | 기능 | 출처 | 중요도 |
|---|------|------|--------|
| 1 | 그룹 스케줄 공유 + **공통 빈 시간 자동 조율** | PRD | 🔴 |
| 2 | 약속 생성 + RSVP(참석/미정/불참) | PRD | 🔴 |
| 3 | 그룹 생성 + 링크 초대 + 멤버십 | PRD | 🔴 |
| 4 | 실시간 알림 + 그룹 채팅 + **게시판(이모지/댓글/대댓글)** | PRD | 🟡 |
| 5 | 인증 + 프로필 (**소셜 로그인/비번찾기/탈퇴**) | PRD | 🟡 |
| 6 | **관리자 시스템** (대시보드/유저·그룹 관리/콘텐츠 제재) | PRD | 🟡 |
| 7 | **선착순 번개 모임 (동시성 제어)** | 기술 심화 | ⭐ |
| 8 | **날짜 투표** (동시 투표 정합성) | 기술 심화 | ⭐ |
| — | ~~다국어(i18n)~~ | PRD | ⛔ 범위 제외 |

> 7·8번은 기획서 기능7 "**트래픽 증가 대비 확장성**" 요구를 실제 구현·검증하는 형태로 정당화된다.
> (없는 트래픽을 가정하는 게 아니라, 확장성을 설계·부하테스트로 증명)

---

## 4. 도메인 모델

### 엔티티

**User** — 사용자
- `id, email(unique), password(encoded, 소셜은 null), nickname, profileImageUrl, provider(LOCAL|KAKAO|GOOGLE), providerId, role(USER|ADMIN), status(ACTIVE|WITHDRAWN), createdAt`

**Crew** — 그룹(모임체)
- `id, name, description, inviteCode(unique), ownerId, createdAt`

**CrewMember** — 크루 멤버십
- `id, crewId, userId, role(OWNER|MEMBER), joinedAt` · 제약: `(crewId, userId)` 유니크

**WeeklySlot** — 주간 반복 스케줄(요일별 1행)
- `id, userId, dayOfWeek(MON~SUN), off(boolean), startTime, endTime` · 제약: `(userId, dayOfWeek)` 유니크

**Meetup** — 약속(정원 있으면 선착순)
- `id, crewId, title, meetDate, meetTime, place, memo, capacity(nullable), status(OPEN|CLOSED|CANCELED), createdBy, version(낙관적 락)`

**MeetupRsvp** — 참석 응답
- `id, meetupId, userId, status(GOING|MAYBE|NO), createdAt` · 제약: `(meetupId, userId)` 유니크

**DatePoll / PollCandidate / PollVote** — 날짜 투표
- Poll: `id, crewId, title, status(OPEN|CLOSED), createdBy`
- Candidate: `id, pollId, candidateDate`
- Vote: `id, candidateId, userId` · 제약: `(candidateId, userId)` 유니크

**ChatMessage** — 크루 채팅
- `id, crewId, senderId, content, createdAt`

**Post (게시판/건의) · Comment · Reply · Reaction** — 소셜 게시판
- Post: `id, crewId, authorId, content, createdAt`
- Comment: `id, postId, authorId, content, createdAt`
- Reply: `id, commentId, authorId, content, createdAt`
- Reaction: `id, postId, userId, emoji` · 제약: `(postId, userId, emoji)` 유니크

**Notification** — 인앱 알림
- `id, userId, type, title, body, linkUrl, read(boolean), createdAt`

### 관계 요약
```
User 1—N CrewMember N—1 Crew
User 1—N WeeklySlot
Crew 1—N Meetup 1—N MeetupRsvp N—1 User
Crew 1—N DatePoll 1—N PollCandidate 1—N PollVote N—1 User
Crew 1—N ChatMessage N—1 User
Crew 1—N Post 1—N Comment 1—N Reply     (Post 1—N Reaction)
User 1—N Notification
User.role = ADMIN → 관리자 시스템 접근
```

---

## 5. REST API 설계 (초안)

> 인증: `Authorization: Bearer <JWT>`. 응답 공통 래퍼 `{ code, message, data }`.

### Auth / User
| Method | Path | 설명 |
|--------|------|------|
| POST | `/api/auth/signup` | 이메일 회원가입 |
| POST | `/api/auth/login` | 로그인 → Access/Refresh |
| GET | `/api/auth/oauth/{provider}` | 소셜 로그인(kakao/google) |
| POST | `/api/auth/reissue` | 토큰 재발급 |
| POST | `/api/auth/password/reset` | 비밀번호 찾기/재설정 |
| GET/PUT | `/api/users/me` | 내 프로필 조회/수정 |
| DELETE | `/api/users/me` | 회원 탈퇴 |

### Crew / Schedule
| Method | Path | 설명 |
|--------|------|------|
| POST | `/api/crews` · `/api/crews/join` | 크루 생성 · 초대코드 참가 |
| GET | `/api/crews/{id}/members` | 멤버 목록 |
| PUT | `/api/schedules/me` | 내 주간 스케줄 저장 |
| GET | `/api/crews/{id}/schedules` | 크루 전체 스케줄(한눈에 보기) |
| POST | `/api/crews/{id}/common-free` | 선택 멤버 공통 가용시간 계산 |

### Meetup / Poll
| Method | Path | 설명 |
|--------|------|------|
| POST | `/api/crews/{id}/meetups` | 약속 생성(정원 옵션) |
| POST | `/api/meetups/{id}/join` | 🔥 선착순 참가 |
| PUT | `/api/meetups/{id}/rsvp` | 참석 여부 |
| POST | `/api/crews/{id}/polls` | 날짜 투표 생성 |
| POST | `/api/polls/candidates/{id}/vote` | 후보 날짜 투표 |
| POST | `/api/polls/{id}/close` | 투표 마감 → 약속 확정 |

### Board (게시판)
| Method | Path | 설명 |
|--------|------|------|
| POST/GET | `/api/crews/{id}/posts` | 글 작성/목록 |
| POST | `/api/posts/{id}/comments` · `/comments/{id}/replies` | 댓글·대댓글 |
| PUT | `/api/posts/{id}/reactions` | 이모지 반응 토글 |

### Admin (`ROLE_ADMIN` 전용)
| Method | Path | 설명 |
|--------|------|------|
| GET | `/api/admin/dashboard` | 통계 대시보드(MAU·그룹·약속) |
| GET | `/api/admin/users` · `/api/admin/crews` | 유저·그룹 관리 |
| DELETE | `/api/admin/posts/{id}` | 부적절 콘텐츠 제재 |

### WebSocket
| 채널 | 설명 |
|------|------|
| `SUB /topic/crews/{id}/chat` · `SEND /app/crews/{id}/chat` | 크루 채팅 |
| `SUB /user/queue/notifications` | 개인 알림 |

---

## 6. 핵심 기술 도전 상세

### 6-1. 선착순 참가 동시성 (★ 하이라이트)
**시나리오**: `capacity=8` 모임에 동시 요청 300건.
1. **Naive**(조회 후 저장) — 오버부킹 재현 → 문제 증명
2. **낙관적 락**: `@Version` + 재시도(`@Retryable`)
3. **비관적 락**: `SELECT ... FOR UPDATE`
4. **분산 락**: Redisson `RLock` (다중 인스턴스)
5. **Redis 원자 연산**: Lua `DECR` → DB 비동기 반영

**검증**: k6로 (성공 수 == capacity 여부, p95, TPS) 측정 → `infra/k6/`, 그래프 `docs/benchmarks/`.

### 6-2. WebSocket + Redis Pub/Sub
- STOMP `/ws`, SimpleBroker → **Redis Pub/Sub 릴레이**로 교체(다중 서버 브로드캐스팅).
- 프레즌스: Redis Set + TTL (레거시 presence 계승).

### 6-3. Kafka 이벤트 알림
- 도메인 이벤트(`MeetupCreated/Joined`, `PollClosed`, `ScheduleChanged`) → 커밋 후 Kafka 발행 → 알림 컨슈머가 fan-out → 인앱(WebSocket)+웹푸시. 실패는 DLT로 분리.

### 6-4. 공통 가용시간 계산 (도메인 서비스)
레거시 `js/logic.js` 알고리즘을 Java 도메인 서비스로 이관:
```
freeIntervals(slot)      : 근무 제외, 약속가능시간대(10:00~24:00) 내 빈 구간
intersect(a, b)          : 교집합 중 60분 이상만
commonFree(members, day) : 멤버별 빈 구간을 reduce로 교집합
```
- 순수 함수 → **단위 테스트 커버리지 100%** 목표.

### 6-5. Firebase 데이터 마이그레이션
- 기획서 리스크 항목. 기존 Realtime DB(people/meetups/voc)를 JSON 익스포트 → 신규 스키마로 변환하는 일회성 마이그레이션 스크립트 작성. 롤백 대비 검증 절차 문서화.

---

## 7. 개발 로드맵 (Phase별)

| Phase | 내용 | 산출물 |
|-------|------|--------|
| **0** | 세팅 | Gradle, Docker Compose(MySQL·Redis·Kafka), GitHub Actions, Swagger |
| **1** | 인증·크루 | 이메일/소셜 로그인(JWT+OAuth2), 프로필/탈퇴, 크루 생성/초대/역할 |
| **2** | 스케줄 | 주간 스케줄 CRUD, 공통 가용시간 계산 + 단위테스트 |
| **3** | 약속·투표 | 약속 생성, RSVP, 날짜 투표/확정 |
| **4** | 🔥 동시성 | 선착순 참가 5단계 구현 + k6 부하테스트 + 벤치 문서 |
| **5** | 실시간 | WebSocket 채팅 + Redis Pub/Sub + 프레즌스 |
| **6** | 알림·게시판 | Kafka 이벤트 알림 + 게시판(이모지/댓글/대댓글) |
| **7** | 관리자 | 관리자 대시보드/유저·그룹 관리/콘텐츠 제재 |
| **8** | 프론트·배포 | Next.js 전체 화면 + PWA, 배포·모니터링, Firebase 데이터 마이그레이션, 문서 마감 |

> 이력서 코어는 **Phase 0~5**(인증·스케줄·약속·동시성·실시간). 6~8은 여유 되는 만큼.

---

## 8. 이력서/면접 대비 체크리스트

- [ ] README에 아키텍처·ERD·기술 선택 근거 명시
- [ ] 선착순 동시성 **벤치마크 그래프**(before/after)
- [ ] 트러블슈팅 기록(오버부킹 재현→해결)을 블로그/문서로
- [ ] 테스트 커버리지·CI 통과 배지
- [ ] "왜 Kafka?/왜 Redis Pub/Sub?/락 5종 차이" 답변 준비
- [ ] 제품 사고 어필: PRD의 KPI·리스크·타겟 사용자 인용
- [ ] 커밋: **Conventional Commits(전체 영어)**, 깔끔한 단위

---

## 9. 다음 액션

1. **Phase 0 스캐폴딩** — `backend`(Spring Initializr), `frontend`(create-next-app), `infra/docker-compose.yml`.
2. 도메인 엔티티 + Flyway 마이그레이션 작성.
3. 인증(이메일+소셜) → 스케줄 → 공통 가용시간 순 수직 슬라이스.
