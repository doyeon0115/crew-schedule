-- V6: 알림(Phase 6A) — Kafka로 팬아웃된 이벤트가 유저별로 여기 쌓인다.

CREATE TABLE notifications (
    id            BIGSERIAL PRIMARY KEY,
    user_id       BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    type          VARCHAR(40)  NOT NULL,
    crew_id       BIGINT,
    actor_id      BIGINT,
    -- 이벤트별 세부 payload(JSON 문자열). 도메인마다 다른 필드가 오므로 스키마리스로 저장.
    payload       TEXT         NOT NULL,
    read_at       TIMESTAMP,
    created_at    TIMESTAMP    NOT NULL,
    updated_at    TIMESTAMP    NOT NULL
);

-- 내 알림 목록(최신순), 미읽음 필터
CREATE INDEX idx_notifications_user_created ON notifications (user_id, created_at DESC);
CREATE INDEX idx_notifications_user_unread ON notifications (user_id) WHERE read_at IS NULL;
