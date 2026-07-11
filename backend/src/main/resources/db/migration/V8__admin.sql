-- V8: 관리자 시스템(Phase 7)
-- 유저·컨텐츠 상태 컬럼 + 신고 테이블.

ALTER TABLE users
    ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    ADD CONSTRAINT ck_users_status CHECK (status IN ('ACTIVE', 'SUSPENDED'));

ALTER TABLE posts
    ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    ADD CONSTRAINT ck_posts_status CHECK (status IN ('ACTIVE', 'HIDDEN'));

ALTER TABLE comments
    ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    ADD CONSTRAINT ck_comments_status CHECK (status IN ('ACTIVE', 'HIDDEN'));

-- 신고. 대상은 post/comment/chat_message/user 중 하나 (type + target_id).
-- 처리 상태: PENDING(대기) → RESOLVED(정지·삭제 등 조치 완료) 또는 DISMISSED(무시).
CREATE TABLE reports (
    id           BIGSERIAL PRIMARY KEY,
    reporter_id  BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    target_type  VARCHAR(20)  NOT NULL,
    target_id    BIGINT       NOT NULL,
    reason       VARCHAR(500) NOT NULL,
    status       VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    handled_by   BIGINT,
    handled_at   TIMESTAMP,
    admin_memo   VARCHAR(500),
    created_at   TIMESTAMP    NOT NULL,
    updated_at   TIMESTAMP    NOT NULL,
    CONSTRAINT ck_reports_target CHECK (target_type IN ('POST', 'COMMENT', 'CHAT_MESSAGE', 'USER')),
    CONSTRAINT ck_reports_status CHECK (status IN ('PENDING', 'RESOLVED', 'DISMISSED'))
);

CREATE INDEX idx_reports_status_created ON reports (status, created_at DESC);
CREATE INDEX idx_reports_target ON reports (target_type, target_id);
