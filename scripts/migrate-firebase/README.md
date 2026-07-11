# Firebase RTDB → Postgres 이관 스크립트

초기 MVP (`legacy/`)가 Firebase Realtime Database에 저장해둔 데이터를 새 백엔드(Spring Boot + Postgres)로 옮깁니다.

## 매핑

| Firebase RTDB (`rooms/{ROOM}/…`) | 새 백엔드 |
|--------------------------------|-----------|
| `people[id]` (name, sched) | User + `PUT /api/me/schedule` |
| `voc[id]` (who, text, t) | `POST /api/crews/{id}/posts` |
| `voc[id].comments[cid]` | `POST /api/posts/{id}/comments` (parentCommentId=null) |
| `voc[id].comments[cid].replies[rid]` | `POST /api/posts/{id}/comments` (parentCommentId=댓글 id) |
| `meetups[id]` (by, date, time, place) | `POST /api/crews/{id}/meetups` |
| `meetups[id].rsvp[sessionId]` | `POST /api/meetups/{id}/rsvp` |
| `logs`, `presence`, `voc[*].reactions` | **스킵** (감사 로그·임시 상태·세션 기반 익명) |

`who` / `by` / `rsvp.name` 이름 하나당 새 계정 하나가 만들어지고,
각자 자기 자신의 API로 데이터를 씁니다. 관리자 우회 없이 실사용 흐름 그대로.

## 사용법

### 1. Firebase RTDB에서 JSON export

Firebase 콘솔 → Realtime Database → `rooms/{ROOM}` 노드 우측 점 세 개 → **JSON 내보내기**.

받은 파일을 이 폴더에 두거나 경로를 인자로 전달합니다.

### 2. 백엔드 기동

```bash
cd backend
docker compose -f ../infra/docker-compose.yml up -d
./gradlew bootRun
```

### 3. 이관 실행

```bash
node migrate.mjs \
  --export ./our-crew.json \
  --backend http://localhost:8080 \
  --room our-crew \
  --crew-name "우리끼리" \
  --password "MigratedPass123!" \
  --verbose
```

**인자**

- `--export`   *(필수)* 콘솔에서 받은 JSON 파일 경로
- `--backend`  기본 `http://localhost:8080`
- `--room`     `rooms/{room}` 하위 트리 이름. 기본 `our-crew`
- `--crew-name` 새 백엔드에 만들 크루 이름. 기본 `"우리끼리 (legacy)"`
- `--password` 마이그레이션 유저 공통 비밀번호. 기본 `MigratedPass123!` — **바꿔 주세요**
- `--dry-run`  API 호출 없이 계획만 출력
- `--verbose`  진행 로그 상세 출력

먼저 `--dry-run`으로 무엇이 만들어질지 확인한 뒤 실행하시길 권합니다.

### 4. 재실행 안전(idempotent)

- 유저 이메일이 이미 있으면 회원가입 대신 로그인으로 넘어감
- 크루가 같은 이름으로 이미 만들어져 있으면 재사용
- 이미 크루 멤버면 join 스킵
- 스케줄은 PUT이므로 덮어씀

## 이관 후 로그인

각 유저는 자동 생성된 이메일 (`legacy-<hash>@legacy.local`) + `--password`로 지정한 공통 비밀번호로 로그인 가능합니다.
Verbose 모드 로그에서 각 이름의 email과 userId를 볼 수 있어요.

이관 후 각 유저에게 비밀번호를 개별로 재설정하도록 안내해 주세요. (또는 관리자 콘솔에서 강제 재설정 절차 준비.)

## 검증

```bash
# 크루/유저 수 확인
psql -h localhost -U crew -d crew_schedule -c \
  "SELECT COUNT(*) FROM users WHERE email LIKE 'legacy-%';"
psql ... -c "SELECT * FROM crews;"
psql ... -c "SELECT COUNT(*) FROM posts;"
psql ... -c "SELECT COUNT(*) FROM meetups;"
```

또는 프론트에서 이관된 계정으로 로그인해서 UI 확인.

## 스킵된 데이터

- **logs**: 감사 로그. 필요하면 별도 테이블로 벌크 인서트 스크립트 추가 가능.
- **presence**: 실시간 접속자 상태. 이관 무의미.
- **voc[*].reactions**: 이모지 반응이 세션(client id) 기반이라 유저 매핑 불가. 필요하면 첫 관리자가 대표로 눌러주는 방식으로 확장.

## 실행 예시 (dry-run)

`example-export.json`이 함께 들어 있어 로컬 검증용으로 쓸 수 있어요:

```bash
node migrate.mjs --export ./example-export.json --dry-run --verbose
```
