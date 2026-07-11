# 알려진 한계와 미완결 항목

Firebase MVP → Spring Boot + Postgres 마이그레이션 진행 중 정직히 남겨둔 갭 목록입니다.
"컴파일이 된다 ≠ 실제로 잘 동작한다" 를 구분해서 표기했어요.

## 검증되지 않은 것

### 1. 통합 테스트가 실제로 돌지 않았어요

컴파일은 통과했지만 **Testcontainers 기반 테스트는 한 번도 실행되지 않았습니다**.
개발 중 Docker Desktop이 꺼져 있어서, 다음 테스트들은 “작성만 됐고 검증 안 됨” 상태:

| 테스트 | 커버리지 |
|-------|---------|
| `AuthIntegrationTest` | 회원가입 → 로그인 → 리프레시 회전 → 로그아웃 |
| `UserProfileIntegrationTest` | 프로필 조회/수정, 401 흐름 |
| `PollServiceIntegrationTest` | 투표 생성·중복 방지·마감·타이브레이커 |
| `JoinConcurrencyTest` | 4가지 락 전략별 정합성 (NAIVE는 오버부킹, 나머지 정확히 정원) |
| `ChatBroadcastIntegrationTest` | REST → Redis Pub/Sub → STOMP 배달 |
| `NotificationPipelineTest` | Kafka 이벤트 → 컨슈머 → DB 저장 |
| `BoardServiceIntegrationTest` | 게시글/댓글/대댓글 트리 + 이모지 토글 |
| `AdminServiceIntegrationTest` | 정지·해제·refresh 폐기·신고 라이프사이클 |

**조치**: Docker 켜고 `./gradlew test` 실행 → 깨지는 것부터 잡아야 함.

### 2. 브라우저에서 실제 동작 확인 미완

`next build`와 `tsc --noEmit`은 통과했지만, 실제 서비스 흐름(WS 재연결, refresh rotation, 캐시 invalidate, 관리자 콘솔의 mutation)을 브라우저에서 눈으로 확인하지는 못했습니다.

### 3. OAuth2 실제 통신 미검증

카카오·구글 콘솔에서 client_id/secret 발급받아 실제 인가 코드 교환이 성공하는지는 확인하지 않았어요. 스캐폴딩은 되어있지만 실사용 검증 필요.

### 4. Firebase 이관 스크립트 dry-run만 확인

`example-export.json` 기반 dry-run은 통과. 하지만 실제 백엔드가 기동된 상태에서 API 호출 흐름은 안 돌려봤어요. 400/409 세부 케이스 처리는 미검증.

---

## 아키텍처 상 알려진 취약점

### 5. Kafka·Redisson auto-config가 컨텍스트에 영향

`@EnableKafka`와 `redisson-spring-boot-starter`는 Spring 컨텍스트 로딩 시 브로커·Redis에 연결하려 합니다.

결과:
- 통합 테스트 클래스마다 Kafka/Redis 컨테이너를 명시적으로 띄우지 않으면 컨텐스트 로딩 시 재연결 폭풍이 로그에 남음
- 최악의 경우 컨텍스트 초기화 실패

**조치안**:
- `application-test.yml` 만들고 test 프로필에서 Kafka listener를 `autoStartup: false`
- 또는 `KafkaAutoConfiguration.class`, `RedissonAutoConfiguration.class`를 `@EnableAutoConfiguration(exclude=...)`로 필요할 때만 로드

### 6. 정지된 유저의 access token 즉시 차단 안 됨

`AuthService.suspendUser`는 refresh 토큰을 폐기하지만 **이미 발급된 access token은 만료(기본 1시간)까지 유효**합니다.

즉시 차단 필요하면:
- `JwtAuthenticationFilter`에서 매 요청마다 유저 상태 조회 (DB 왕복 비용 큼)
- 또는 Redis 세션 캐시(“userId → 상태”)와 폐기 이벤트 브로드캐스트

### 7. `LocalDataInitializer` 시드 유저 로그인 불가

`@Profile("local")` 시드 유저 4명(지영/유진/선우/수민)은 비밀번호 없이 만들어져서 이메일 로그인 불가. 다른 유저 관점 데모 데이터로만 유용. 실사용자는 `/signup`으로 새로 만들어야 함.

### 8. 채팅 UI가 백엔드 WS URL과 직결

Next.js rewrites가 WebSocket을 프록시하지 못해 프론트가 `ws://localhost:8080/ws` 로 직접 연결합니다. 프로덕션에서는 nginx/ingress로 `/ws/*` 를 백엔드에 프록시하거나 `NEXT_PUBLIC_WS_URL`을 명시적으로 지정 필요.

---

## 미구현 기능

### 9. Web Push (VAPID)

README에는 "온라인이면 WebSocket, 백그라운드면 Web Push"라고 되어있지만 **Web Push는 미구현**. 백그라운드 알림을 받으려면:
- 백엔드: VAPID 키 생성, 구독 저장 테이블, Push API 호출
- 프론트: 구독 요청·저장, service worker의 `push` 이벤트 핸들러

### 10. 모니터링 (Prometheus·Grafana)

Actuator만 열려있고 Prometheus 스크레이핑 설정·Grafana 대시보드 JSON은 없습니다. `management.endpoints.web.exposure.include`에는 `prometheus`가 포함돼 있어 metrics endpoint는 제공됨.

### 11. 배포 파이프라인

- Dockerfile (backend·frontend) 없음
- GitHub Actions 워크플로 없음
- 환경별 설정 분리 안 됨 (`application-prod.yml` 등)

---

## 스코프 밖으로 남겨둔 것

### 12. 스케줄 예외(`schedule_exceptions`)

V1 마이그레이션에 테이블은 있지만 도메인·API 미구현. 특정 날짜 예외 처리는 후속으로.

### 13. voc[*].reactions 이관

Firebase 이관 스크립트에서 이모지 반응은 스킵. 세션 기반 익명이라 유저 매핑이 불가능해서. 필요하면 관리자가 대신 눌러주는 방식으로 확장 가능.

### 14. 로그(activity log) 이관

Firebase의 `logs` 서브트리(입장/퇴장/편집 감사 로그)는 이관 안 함. 새 백엔드에 대응 도메인이 없음.

---

## 개선 아이디어

- **테스트 격리**: 통합 테스트마다 Postgres 컨테이너를 새로 띄우고 있어 시간 소모. 테스트 컨테이너 재사용 + 트랜잭션 롤백으로 개선 여지.
- **CommentRepository.findPage(post) 없음**: 상세 조회 시 모든 댓글을 한 번에 가져옴. 댓글 수 만 개 이상 게시글에서는 페이지네이션 필요.
- **알림 정리 정책**: `notifications` 테이블에 무제한 축적. TTL 또는 배치 삭제 정책 필요.
- **채팅 히스토리 검색**: 현재는 최신순 페이지네이션만. 키워드 검색·유저 필터 미구현.
- **Rate limiting**: 로그인/회원가입/신고 등에 무제한 요청 가능. bucket4j 등으로 방어층 필요.

---

## 우선순위 요약

**즉시 잡아야 (Docker 켠 뒤)**:
1. 통합 테스트 전량 실행 → 깨진 것 수정
2. `application-test.yml` 만들어 Kafka/Redisson 문제 방지

**단기**:
3. 브라우저 흐름 실제 확인 → UX 이슈 발견
4. Dockerfile + 간단한 CI (compile + test)
5. Web Push 실장

**중기**:
6. OAuth 실제 통신 확인
7. Prometheus 스크레이핑 + Grafana 대시보드
8. 정지 시 즉시 차단(Redis 세션 캐시)
9. 프론트 E2E 테스트 (Playwright)
