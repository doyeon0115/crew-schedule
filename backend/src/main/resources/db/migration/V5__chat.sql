-- V5: 크루 채팅(Phase 5)

CREATE TABLE chat_messages (
    id         BIGSERIAL PRIMARY KEY,
    crew_id    BIGINT       NOT NULL REFERENCES crews (id) ON DELETE CASCADE,
    sender_id  BIGINT       NOT NULL REFERENCES users (id),
    content    VARCHAR(2000) NOT NULL,
    created_at TIMESTAMP    NOT NULL,
    updated_at TIMESTAMP    NOT NULL
);

CREATE INDEX idx_chat_messages_crew_created ON chat_messages (crew_id, created_at DESC);
