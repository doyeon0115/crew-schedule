-- V3: 날짜 투표(Phase 3) - 여러 후보 날짜 중 다중 선택 투표

-- ============ 투표 ============
CREATE TABLE date_polls (
    id                  BIGSERIAL PRIMARY KEY,
    crew_id             BIGINT       NOT NULL REFERENCES crews (id) ON DELETE CASCADE,
    creator_id          BIGINT       NOT NULL REFERENCES users (id),
    title               VARCHAR(100) NOT NULL,
    status              VARCHAR(20)  NOT NULL DEFAULT 'OPEN',
    winner_candidate_id BIGINT,      -- CLOSED 상태에서만 세팅. 순환 참조라 FK는 생략.
    closed_at           TIMESTAMP,
    created_at          TIMESTAMP    NOT NULL,
    updated_at          TIMESTAMP    NOT NULL,
    CONSTRAINT ck_date_polls_status CHECK (status IN ('OPEN', 'CLOSED'))
);

CREATE INDEX idx_date_polls_crew ON date_polls (crew_id, status);

-- ============ 후보 날짜 ============
CREATE TABLE poll_candidates (
    id             BIGSERIAL PRIMARY KEY,
    poll_id        BIGINT    NOT NULL REFERENCES date_polls (id) ON DELETE CASCADE,
    candidate_date DATE      NOT NULL,
    start_time     TIME,                              -- null이면 시간 미정
    created_at     TIMESTAMP NOT NULL,
    updated_at     TIMESTAMP NOT NULL,
    CONSTRAINT uq_poll_candidates UNIQUE (poll_id, candidate_date, start_time)
);

CREATE INDEX idx_poll_candidates_poll ON poll_candidates (poll_id);

-- ============ 투표 ============
-- 다중 선택: 한 유저가 여러 후보에 투표 가능. 같은 후보에는 한 번만.
CREATE TABLE poll_votes (
    id            BIGSERIAL PRIMARY KEY,
    candidate_id  BIGINT    NOT NULL REFERENCES poll_candidates (id) ON DELETE CASCADE,
    user_id       BIGINT    NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    created_at    TIMESTAMP NOT NULL,
    updated_at    TIMESTAMP NOT NULL,
    CONSTRAINT uq_poll_votes UNIQUE (candidate_id, user_id)
);

CREATE INDEX idx_poll_votes_user ON poll_votes (user_id);
