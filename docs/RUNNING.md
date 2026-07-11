# 로컬 실행 가이드

이 문서는 Crew Schedule을 로컬에서 처음 돌려보는 데 필요한 모든 단계를 담고 있습니다.

## 필요 도구

| 도구 | 최소 버전 | 확인 |
|------|-----------|------|
| Java | 21 | `java --version` (Temurin/OpenJDK 21+) |
| Node.js | 18 | `node --version` |
| Docker Desktop | 최신 | `docker info` 로 daemon 상태 확인 |
| Git | 2.30+ | `git --version` |
| psql (선택) | 16 | 이관 검증·직접 쿼리용 |
| k6 (선택) | 0.50+ | Phase 4 부하 테스트용 |

Java는 시스템 전역이 아니어도 됩니다. Gradle Wrapper가 필요 시 toolchain으로 다운로드해요.

## 1. 인프라 기동 (Postgres · Redis · Kafka)

```bash
cd /Applications/crew-schedule
docker compose -f infra/docker-compose.yml up -d
docker compose -f infra/docker-compose.yml ps  # STATUS: running 확인
```

- **Postgres**: `localhost:5432`, DB=`crew_schedule`, user=`crew`/`crew1234`
- **Redis**: `localhost:6379`
- **Kafka**: `localhost:9092` (KRaft, 단일 브로커)

중지: `docker compose -f infra/docker-compose.yml down`
초기화(볼륨까지 삭제): `docker compose ... down -v`

## 2. 백엔드 실행

```bash
cd backend
chmod +x gradlew  # 처음 한 번만
./gradlew bootRun
```

주요 로그:
- `Started CrewScheduleBackendApplication in ...` → 정상
- Flyway가 `V1__init.sql`부터 `V8__admin.sql`까지 순차 실행

기본 포트: **8080**. Swagger UI는 <http://localhost:8080/swagger-ui.html>.

### 환경변수 (선택)

`application.yml`이 기본값을 갖고 있어 로컬에선 대부분 그대로 됩니다. 다만 다음은 상황에 따라 오버라이드:

```bash
# 소셜 로그인 실동작 확인용 (없으면 이메일 로그인만 됨)
export KAKAO_CLIENT_ID=... KAKAO_CLIENT_SECRET=...
export GOOGLE_CLIENT_ID=... GOOGLE_CLIENT_SECRET=...

# Phase 4 락 전략 스위칭 (기본 PESSIMISTIC)
export JOIN_STRATEGY=REDIS_ATOMIC

# JWT 서명 키 (prod에서는 반드시 변경)
export JWT_SECRET="long-random-secret-32-bytes-minimum"

./gradlew bootRun
```

## 3. 프론트엔드 실행

```bash
cd frontend
npm install       # 처음 한 번만
npm run dev
```

기본 포트: **3000**. <http://localhost:3000> 접속.

`next.config.ts`가 `/api/*` 요청을 `http://localhost:8080`으로 프록시하므로 CORS 걱정 없음.

WebSocket은 프록시가 안 되어서 직결. 기본 URL은 `ws://localhost:8080/ws`. 다른 주소를 쓰려면:

```bash
NEXT_PUBLIC_WS_URL=wss://api.example.com/ws npm run dev
```

## 4. 첫 사용 흐름 (브라우저)

1. <http://localhost:3000> → `/signup`으로 이동 → 계정 만들기
2. 홈 → "크루 온보딩" → **새로 만들기** (예: "우리끼리")
3. "스케줄 수정" → 요일별 근무/휴무 입력 → 저장
4. **다른 브라우저(시크릿 창)** 에서 다른 계정 만들기 → 홈의 **초대 코드로 가입**에 크루장 초대코드 붙여넣기
5. "약속 잡기"에서 시간 고르고 제안 → 다른 계정에서 참석/미정/불참
6. "채팅"에서 실시간 대화 (양쪽 창 모두 실시간 반영)
7. "게시판"에서 글·댓글·이모지
8. "날짜 투표"로 여러 후보 만들고 각자 투표

### 관리자 콘솔 접근

시드 관리자가 없어서 SQL로 승격해야 합니다:

```bash
docker compose -f infra/docker-compose.yml exec postgres \
  psql -U crew crew_schedule \
  -c "UPDATE users SET role='ADMIN' WHERE email='본인이메일';"
```

재로그인하면 헤더 우측에 **관리자** 탭이 뜹니다.

## 5. 통합 테스트

**전제**: Docker가 반드시 켜져 있어야 합니다 (Testcontainers가 Postgres·Redis·Kafka 컨테이너를 임시로 띄움).

```bash
cd backend
./gradlew test                                                # 전체
./gradlew test --tests JoinConcurrencyTest                    # Phase 4만
./gradlew test --tests NotificationPipelineTest --info        # Kafka 파이프라인
```

리포트: `backend/build/reports/tests/test/index.html`

프론트는 아직 유닛/E2E 테스트가 없고 정적 분석만:

```bash
cd frontend
npx tsc --noEmit    # 타입 체크
npx next build      # 프로덕션 빌드 검증
```

## 6. Phase 4 k6 부하 테스트

각 락 전략별로 처리량·정합성 비교. Docker + 백엔드 기동 상태에서:

```bash
# 시드 (유저·크루·정원 있는 meetup 자동 생성 + 토큰 파일 저장)
cd infra/k6
./seed.sh 1000 8
# stdout에 MEETUP_ID=..., TOKENS_FILE=tokens.json 출력됨

# 각 전략으로 백엔드를 다시 기동 (별도 터미널)
JOIN_STRATEGY=OPTIMISTIC ./gradlew bootRun

# k6 실행
k6 run -e HOST=http://localhost:8080 -e MEETUP_ID=<위 id> -e TOKENS_FILE=tokens.json join.js
```

전략을 갈아끼우며 반복. 결과 비교는 `docs/phase4-concurrency.md`.

## 7. Firebase → Postgres 이관

Legacy MVP(`legacy/`)가 쓰던 Firebase Realtime Database의 데이터를 새 백엔드로 옮기려면:

```bash
# 1. Firebase 콘솔 → RTDB → rooms/{ROOM} 노드 → JSON 내보내기
# 2. 파일을 scripts/migrate-firebase/에 두거나 경로 지정

cd scripts/migrate-firebase
node migrate.mjs --export ./our-crew.json --dry-run --verbose   # 계획만 확인
node migrate.mjs --export ./our-crew.json --verbose             # 실제 이관
```

상세는 [`scripts/migrate-firebase/README.md`](../scripts/migrate-firebase/README.md).

## 트러블슈팅

**백엔드가 기동 실패: `Could not connect to Redis`**
→ `docker compose ps`로 redis STATUS 확인. Redisson auto-config가 시작 시 연결하므로 Redis가 반드시 살아있어야 함.

**백엔드 기동 실패: `Flyway ... checksum mismatch`**
→ V*.sql 파일을 수정했다면 `docker compose down -v && up -d`로 DB 초기화.

**프론트에서 401 반복**
→ localStorage `crew-schedule-auth`가 오래된 토큰. 개발자도구 → Application → Local Storage에서 삭제 후 재로그인.

**채팅이 연결되지 않음**
→ `NEXT_PUBLIC_WS_URL` 확인. 프록시 뒤에서는 반드시 wss:// 필요.

**통합 테스트가 `ContainerFetchException`**
→ Docker Desktop이 꺼져 있음.

**정지된 유저가 즉시 차단되지 않음**
→ 알려진 한계. access token 만료(1시간)까지 유효. [`LIMITATIONS.md`](./LIMITATIONS.md) 참고.

## 정리 명령 모음

```bash
# 모든 인프라 중지 + 데이터 삭제
docker compose -f infra/docker-compose.yml down -v

# 백엔드 gradle 캐시 · 빌드 초기화
cd backend && ./gradlew clean

# 프론트 next 캐시 초기화
cd frontend && rm -rf .next node_modules && npm install
```
