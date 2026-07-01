# infra

로컬 개발용 미들웨어와 운영 스크립트 모음.

## 미들웨어 (docker-compose)

```bash
# 기동 (PostgreSQL · Redis · Kafka)
docker compose -f infra/docker-compose.yml up -d

# 상태 확인
docker compose -f infra/docker-compose.yml ps

# 종료 (데이터 유지)
docker compose -f infra/docker-compose.yml down

# 종료 + 데이터 삭제
docker compose -f infra/docker-compose.yml down -v
```

| 서비스 | 포트 | 접속 정보 |
|--------|------|-----------|
| PostgreSQL 16 | 5432 | db `crew_schedule` / user `crew` / pw `crew1234` |
| Redis 7 | 6379 | — |
| Kafka 3.7 (KRaft) | 9092 | bootstrap `localhost:9092` |

> 백엔드 `application.yml` 의 기본값이 위 접속 정보와 일치한다.

## 예정 (Phase별 추가)

- `k6/` — 선착순 참가 부하 테스트 스크립트 (Phase 4)
- `grafana/`, `prometheus/` — 모니터링 (Phase 8)
- 앱 컨테이너 이미지 + 통합 compose (Phase 8)
