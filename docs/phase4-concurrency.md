# Phase 4 — 선착순 참여 동시성 제어

정원 8명 모임에 수백 명이 동시에 참여 요청을 보낼 때 오버부킹 없이 정확히 8명만 성공하는지 검증한다.

## 참여 흐름

```
POST /api/meetups/{meetupId}/join   Bearer <token>
```

`Meetup.capacity`가 null이면 참여 불가(`MEETUP_NOT_JOINABLE`), 정원 도달 시 `MEETUP_FULL`, 이미 참여 중이면 `ALREADY_JOINED`.

## 락 전략 스위칭

```yaml
# application.yml
app:
  concurrency:
    join-strategy: PESSIMISTIC   # NAIVE | OPTIMISTIC | PESSIMISTIC | DISTRIBUTED_LOCK | REDIS_ATOMIC
    optimistic-retry-max: 5
```

또는 환경변수:

```bash
JOIN_STRATEGY=REDIS_ATOMIC ./gradlew bootRun
```

## 전략별 특성

| 전략 | 방식 | 정합성 | 처리량 | 요구 인프라 |
|------|------|--------|--------|--------------|
| `NAIVE` | 락 없음 | ❌ 오버부킹 | 최고 | Postgres |
| `OPTIMISTIC` | `@Version` + 재시도 | ✅ | 낮은 충돌에서 우수 | Postgres |
| `PESSIMISTIC` | `SELECT ... FOR UPDATE` | ✅ | 안정적, DB 락 대기 있음 | Postgres |
| `DISTRIBUTED_LOCK` | Redisson `RLock` | ✅ | 락 획득 오버헤드 | Postgres + Redis |
| `REDIS_ATOMIC` | Redis DECR (Lua) | ✅ | 최고 (게이팅 Redis, 저장 DB) | Postgres + Redis |

### 트레이드오프

- **낙관적** — 충돌이 잦으면 재시도 비용이 커진다. 정원이 크고 트래픽이 스파이크성이면 재시도 폭주 위험. 재시도 상한(`optimistic-retry-max`) 넘으면 `JOIN_LOCK_FAILED(503)`.
- **비관적** — DB row lock. 동일 meetup에 대해 요청이 직렬화. 단일 인스턴스에서는 가장 단순하고 확실.
- **분산 락(Redisson)** — 다중 서버 환경 필수. 락 획득 자체가 Redis 왕복 1회 이상 필요.
- **Redis 원자** — Redis가 카운터의 소스 오브 트루스. DB 부하가 가장 낮지만 Redis 장애 시 정합성 회복 로직 필요(현재 구현은 DB 저장 실패 시 INCR로 되돌림).

## 정합성 검증 (JUnit)

`JoinConcurrencyTest` — 정원 8, 참가자 50명이 동시에 join을 시도.

```
NAIVE                → success >= 8 (오버부킹 시연)
OPTIMISTIC / PESSIMISTIC / DISTRIBUTED_LOCK / REDIS_ATOMIC → success == 7 (창시자 1 + 참가자 7)
```

실행:
```bash
docker compose -f infra/docker-compose.yml up -d
cd backend && ./gradlew test --tests JoinConcurrencyTest
```

## 부하 테스트 (k6)

`infra/k6/`에 스크립트. 시드로 토큰 파일 만들고 각 전략으로 백엔드를 재기동해가며 실행.

```bash
# 1. 시드
cd infra/k6 && ./seed.sh 1000 8
# stdout에 MEETUP_ID=... TOKENS_FILE=tokens.json 출력됨

# 2. 각 전략으로 백엔드 재기동
JOIN_STRATEGY=NAIVE ./gradlew bootRun          # (다른 터미널)

# 3. k6 실행
k6 run -e HOST=http://localhost:8080 -e MEETUP_ID=<위 id> -e TOKENS_FILE=tokens.json infra/k6/join.js
```

수집할 지표:
- p50 / p95 / p99 latency
- 성공률 (정원 초과분은 실패이므로 대량 실패가 정상)
- DB CPU / Redis CPU

각 전략별 결과는 실제 측정 후 이 문서에 표로 갱신 예정.
